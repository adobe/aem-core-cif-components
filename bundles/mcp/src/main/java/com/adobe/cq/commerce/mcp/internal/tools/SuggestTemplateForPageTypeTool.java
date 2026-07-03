/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP read tool listing editable-template candidates under {@code /conf/*&#47;settings/wcm/templates/*} whose
 * pre-placed {@code initial} content matches a requested page-type ({@code product}/{@code category}/
 * {@code catalog}) -- see catalog &sect;9. This is a discovery aid for the (currently unimplemented) Tier-3 page
 * creation tools (e.g. {@code create_specific_pdp}): given a template path, those tools would seed a new page's
 * {@code initial} content via {@code PageManager.create(parentPath, name, templatePath, title)}.
 * <p>
 * <b>Signal, in priority order</b>: the pre-placed component under
 * {@code initial/jcr:content/root/container/container} (the responsive grid), classified via
 * {@link Resource#isResourceType(String)} against the known CIF commerce resource types, checked per version (see
 * {@code ProductImpl}/{@code ProductListImpl} {@code RESOURCE_TYPE} constants in {@code bundles/core}'s
 * {@code v1}/{@code v2}/{@code v3} {@code …components.internal.models.*} packages -- those constants live in a
 * non-exported internal package, so the literal strings are hardcoded and verified here instead of referenced).
 * {@code isResourceType} follows a resource's {@code sling:resourceSuperType} chain -- including one registered on
 * an {@code /apps} component definition, not just a node property -- so Venia's proxy components (e.g.
 * {@code venia/components/commerce/product}, whose super-type {@code core/cif/components/commerce/product/v3/product}
 * is declared on the {@code /apps} component, not on the {@code /conf} node itself) resolve correctly in real AEM:
 * <ul>
 * <li>{@code product}: a component matching {@code core/cif/components/commerce/product/v1/product},
 * {@code .../v2/product}, or {@code .../v3/product}.</li>
 * <li>{@code category}: a component matching {@code core/cif/components/commerce/productlist/v1/productlist} or
 * {@code .../v2/productlist}.</li>
 * <li>{@code catalog}: the grid has no children at all (structural-only page, filled in later).</li>
 * </ul>
 * When the grid has children but none is a recognized commerce component (e.g. a plain WCM Core Components text
 * component), the template matches nothing and is omitted -- it is not empty, so it is not a "catalog" template,
 * and it carries no product/category signal either.
 * <p>
 * <b>Fallback signal</b>: only when the {@code initial} grid can't be read at all (missing/malformed) does this
 * fall back to matching the template's own {@code jcr:title} case-insensitively against the Venia convention
 * ({@code "Product page"}/{@code "Category page"}/{@code "Catalog Page"}) -- see catalog &sect;9's explicit
 * warning that title-matching is an inferred Venia convention, not a CIF standard.
 * <p>
 * <b>aem-mock limitation</b> (mirrors {@link CheckSpecificPageCapabilityTool}'s documented caveat): the pinned
 * aem-mock's {@code isResourceType} matches by exact identity only -- it does not walk a resource's super-type
 * chain through {@code /apps} component definitions the way real AEM does. The unit test fixture therefore sets
 * the pre-placed component's {@code sling:resourceType} directly to the core type (as if the {@code /apps} proxy
 * resolution had already happened), which is sufficient to exercise the identity-match case in-process; the real
 * Venia proxy path (node {@code resourceType=venia/...}, {@code /apps} superType registered on the component
 * definition) can only be verified against a live AEM instance.
 */
@Component(service = McpTool.class)
public class SuggestTemplateForPageTypeTool implements McpTool {

    private static final String TEMPLATES_QUERY_ROOT = "/conf";
    private static final String TEMPLATES_SUFFIX = "settings/wcm/templates";
    private static final String INITIAL_GRID_PATH = "initial/jcr:content/root/container/container";

    /**
     * CIF PRODUCT component resource types, by version (verified against {@code ProductImpl.RESOURCE_TYPE} in
     * {@code bundles/core}'s {@code v1}/{@code v2}/{@code v3} {@code …models.*.product} packages).
     */
    private static final List<String> PRODUCT_RESOURCE_TYPES = Arrays.asList(
        "core/cif/components/commerce/product/v1/product",
        "core/cif/components/commerce/product/v2/product",
        "core/cif/components/commerce/product/v3/product");

    /**
     * CIF PRODUCTLIST component resource types, by version (verified against {@code ProductListImpl.RESOURCE_TYPE}
     * in {@code bundles/core}'s {@code v1}/{@code v2} {@code …models.*.productlist} packages -- there is no v3
     * productlist).
     */
    private static final List<String> PRODUCTLIST_RESOURCE_TYPES = Arrays.asList(
        "core/cif/components/commerce/productlist/v1/productlist",
        "core/cif/components/commerce/productlist/v2/productlist");

    private static final List<String> VALID_KINDS = Arrays.asList("product", "category", "catalog");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "suggest_template_for_page_type";
    }

    @Override
    public String description() {
        return "List editable-template candidates under /conf/*/settings/wcm/templates/ whose pre-placed initial "
            + "content matches a page type: kind=product (product-detail template), kind=category (product-list/PLP "
            + "template), or kind=catalog (empty structural grid, filled in later). Signal is the pre-placed "
            + "commerce component's resourceType/resourceSuperType where available, falling back to the template's "
            + "jcr:title only when no component signal is present.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode kind = properties.putObject("kind").put("type", "string");
        kind.putArray("enum").add("product").add("category").add("catalog");
        schema.putArray("required").add("kind");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String kind = args.path("kind").asText(null);
        if (!VALID_KINDS.contains(kind)) {
            throw new IllegalArgumentException("kind must be one of " + VALID_KINDS + ": " + kind);
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        ObjectNode out = mapper.createObjectNode();
        out.put("kind", kind);
        ArrayNode templatesNode = out.putArray("templates");

        Resource confRoot = resolver.getResource(TEMPLATES_QUERY_ROOT);
        if (confRoot == null) {
            return out;
        }

        for (Resource configRoot : confRoot.getChildren()) {
            Resource templatesFolder = configRoot.getChild(TEMPLATES_SUFFIX);
            if (templatesFolder == null) {
                continue;
            }
            for (Resource template : templatesFolder.getChildren()) {
                Signal signal = classify(template);
                if (signal != null && signal.kind.equals(kind)) {
                    ObjectNode entry = templatesNode.addObject();
                    entry.put("path", template.getPath());
                    entry.put("title", signal.title);
                    entry.put("signal", signal.source);
                }
            }
        }

        return out;
    }

    /**
     * Classifies a single {@code /conf/*&#47;settings/wcm/templates/<name>} resource by its {@code initial} grid
     * content, falling back to {@code jcr:title} only when the grid itself can't be read.
     */
    private Signal classify(Resource template) {
        String title = title(template);
        Resource grid = template.getChild(INITIAL_GRID_PATH);
        if (grid == null) {
            return classifyByTitle(title);
        }

        Iterator<Resource> children = grid.listChildren();
        if (!children.hasNext()) {
            return new Signal("catalog", "resourceSuperType", title);
        }

        while (children.hasNext()) {
            Resource component = children.next();
            // Check productlist BEFORE product: both families are matched via isResourceType against distinct,
            // fully-versioned literal strings (e.g. ".../product/v3/product" vs. ".../productlist/v2/productlist"),
            // so there is no prefix-collision risk here -- the ordering is kept for readability/symmetry with the
            // rest of the tool, not because it is load-bearing.
            if (matchesAnyType(component, PRODUCTLIST_RESOURCE_TYPES)) {
                return new Signal("category", "resourceSuperType", title);
            }
            if (matchesAnyType(component, PRODUCT_RESOURCE_TYPES)) {
                return new Signal("product", "resourceSuperType", title);
            }
        }

        // Grid has children, but none is a recognized commerce component: no signal, not a fallback candidate
        // either (title fallback is reserved for when the grid can't be read at all).
        return null;
    }

    /**
     * True if the component resolves to any of the given CIF core resource types via
     * {@link Resource#isResourceType(String)}. In real AEM this follows the full {@code sling:resourceSuperType}
     * chain, including one registered on an {@code /apps} component definition -- so a Venia proxy component (node
     * {@code sling:resourceType=venia/components/commerce/product}, with
     * {@code core/cif/components/commerce/product/v3/product} declared as its super type on the {@code /apps}
     * component, not as a property on this node) still matches. The pinned aem-mock's {@code isResourceType}
     * only matches by exact identity (no {@code /apps} chain walk), so the unit test fixture sets the pre-placed
     * node's {@code sling:resourceType} directly to the core type to exercise this path -- see the class
     * javadoc's "aem-mock limitation" note, mirroring {@link CheckSpecificPageCapabilityTool}.
     */
    private boolean matchesAnyType(Resource component, List<String> coreResourceTypes) {
        for (String coreResourceType : coreResourceTypes) {
            if (component.isResourceType(coreResourceType)) {
                return true;
            }
        }
        return false;
    }

    private Signal classifyByTitle(String title) {
        if (title == null) {
            return null;
        }
        String normalized = title.trim().toLowerCase(Locale.ROOT);
        if ("product page".equals(normalized)) {
            return new Signal("product", "title", title);
        }
        if ("category page".equals(normalized)) {
            return new Signal("category", "title", title);
        }
        if ("catalog page".equals(normalized)) {
            return new Signal("catalog", "title", title);
        }
        return null;
    }

    private String title(Resource template) {
        Resource content = template.getChild("jcr:content");
        return content == null ? null : content.getValueMap().get("jcr:title", String.class);
    }

    private static final class Signal {
        private final String kind;
        private final String source;
        private final String title;

        private Signal(String kind, String source, String title) {
            this.kind = kind;
            this.source = source;
            this.title = title;
        }
    }
}
