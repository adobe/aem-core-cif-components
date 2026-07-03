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
 * {@code initial/jcr:content/root/container/container} (the responsive grid), classified by its own
 * {@code sling:resourceType} or (so Venia's proxy components -- e.g. {@code venia/components/commerce/product}
 * super-typing {@code core/cif/components/commerce/product/v3/product} -- match too) its declared
 * {@code sling:resourceSuperType}:
 * <ul>
 * <li>{@code product}: a component matching {@code core/cif/components/commerce/product/*} (the PRODUCT component;
 * checked <em>after</em> ruling out {@code productlist}, since the literal string {@code "product"} is a prefix of
 * {@code "productlist"}'s resource type segment).</li>
 * <li>{@code category}: a component matching {@code core/cif/components/commerce/productlist/*}.</li>
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
 */
@Component(service = McpTool.class)
public class SuggestTemplateForPageTypeTool implements McpTool {

    private static final String TEMPLATES_QUERY_ROOT = "/conf";
    private static final String TEMPLATES_SUFFIX = "settings/wcm/templates";
    private static final String INITIAL_GRID_PATH = "initial/jcr:content/root/container/container";

    private static final String PRODUCT_RESOURCE_TYPE_SEGMENT = "core/cif/components/commerce/product/";
    private static final String PRODUCTLIST_RESOURCE_TYPE_SEGMENT = "core/cif/components/commerce/productlist/";

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
            // Check productlist BEFORE product: "core/cif/components/commerce/productlist/..." also starts with
            // the literal prefix "core/cif/components/commerce/product" (minus the trailing slash), so testing
            // product first would misclassify every category template as a product template.
            if (matchesTypeSegment(component, PRODUCTLIST_RESOURCE_TYPE_SEGMENT)) {
                return new Signal("category", "resourceSuperType", title);
            }
            if (matchesTypeSegment(component, PRODUCT_RESOURCE_TYPE_SEGMENT)) {
                return new Signal("product", "resourceSuperType", title);
            }
        }

        // Grid has children, but none is a recognized commerce component: no signal, not a fallback candidate
        // either (title fallback is reserved for when the grid can't be read at all).
        return null;
    }

    /**
     * True if the component's own {@code sling:resourceType}, or its immediate {@code sling:resourceSuperType}
     * (Venia's proxy components, e.g. {@code venia/components/commerce/product}, declare the real CIF type as
     * their direct super type -- see catalog &sect;9's mapping table), starts with the given
     * {@code core/cif/components/commerce/...} segment prefix. Prefix comparison (not
     * {@link Resource#isResourceType(String)} against one exact type) is used because the CIF type itself is
     * versioned ({@code v1}/{@code v2}/{@code v3}) and this only needs to know the commerce family, not the exact
     * version. Reads the {@code sling:resourceSuperType} property directly, rather than relying on
     * {@code ResourceResolver#getParentResourceType}, since that requires the type hierarchy to be registered as
     * resources under a search path (e.g. {@code /apps}), which a plain {@code /conf} proxy's declared property
     * does not require.
     */
    private boolean matchesTypeSegment(Resource component, String segmentPrefix) {
        String resourceType = component.getResourceType();
        if (resourceType != null && resourceType.startsWith(segmentPrefix)) {
            return true;
        }
        String superType = component.getValueMap().get("sling:resourceSuperType", String.class);
        return superType != null && superType.startsWith(segmentPrefix);
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
