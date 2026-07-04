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
import java.util.List;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool that pins a CIF product-list-family <em>component</em> (product list / product carousel) to a
 * specific category by setting its {@code category} property — the "Manual Category Selection" read by
 * {@code ProductListImpl}/{@code ProductCarouselImpl} ({@code CATEGORY_PROPERTY = "category"}) — so the
 * component shows that category's products instead of resolving the category from the request URL.
 * <p>
 * This is the category-to-component counterpart of {@code configure_product_component} (which pins a product
 * component to a SKU). It targets a <em>component resource</em>, not a page's {@code jcr:content}; to bind the
 * root category of a catalog (PLP) page use {@code configure_catalog_page} instead. Writes run under the
 * caller's {@link ResourceResolver} so JCR ACLs are enforced.
 */
@Component(service = McpTool.class)
public class ConfigureProductListComponentTool implements McpTool {
    private static final String CATEGORY_PROPERTY = "category";

    /**
     * CIF component resource types that read the {@code category} manual-selection property (see
     * {@code ProductListImpl.RESOURCE_TYPE} v1/v2 and {@code ProductCarouselImpl.RESOURCE_TYPE}). Matching is
     * done via {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType}, so
     * proxied project components (e.g. Venia's {@code venia/components/...}) that super-type one of these are
     * also accepted.
     */
    private static final List<String> CIF_PRODUCTLIST_COMPONENT_TYPES = Arrays.asList(
        "core/cif/components/commerce/productlist/v1/productlist",
        "core/cif/components/commerce/productlist/v2/productlist",
        "core/cif/components/commerce/productcarousel/v1/productcarousel");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_productlist_component";
    }

    @Override
    public String description() {
        return "Pin a CIF product list or product carousel component to a specific category (its 'category' manual "
            + "selection), so it shows that category's products instead of resolving the category from the page URL. "
            + "Targets the component resource; to set a catalog page's root category use configure_catalog_page.";
    }

    @Override
    public boolean writesContent() {
        return true;
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("path").put("type", "string");
        properties.putObject("categoryUid").put("type", "string");
        schema.putArray("required").add("path").add("categoryUid");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        String categoryUid = args.path("categoryUid").asText(null);
        if (path == null || categoryUid == null || !path.startsWith("/content/")) {
            throw new IllegalArgumentException("path (under /content) and categoryUid are required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = resolver.getResource(path);
        if (target == null) {
            throw new IllegalArgumentException("resource not found: " + path);
        }
        if (CIF_PRODUCTLIST_COMPONENT_TYPES.stream().noneMatch(target::isResourceType)) {
            throw new IllegalArgumentException("resource is not a CIF product list / carousel component: " + path);
        }
        ModifiableValueMap properties = target.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new IllegalArgumentException("resource not modifiable: " + path);
        }
        properties.put(CATEGORY_PROPERTY, categoryUid);
        resolver.commit();

        // Post-write verification: re-read the persisted value so we never report success for a write that did
        // not take (the component consumes exactly this property, validated by the resource-type check above).
        Resource persisted = resolver.getResource(path);
        boolean updated = persisted != null && categoryUid.equals(persisted.getValueMap().get(CATEGORY_PROPERTY, String.class));

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("categoryUid", categoryUid);
        out.put("updated", updated);
        return out;
    }
}
