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
 * MCP write tool that binds the <em>root category</em> of a CIF catalog (PLP) page by writing
 * {@code magentoRootCategoryId} (+ {@code magentoRootCategoryIdType}) on the page's {@code jcr:content} node —
 * the properties consumed by {@code SiteStructureImpl} / {@code NavigationImpl} to scope the page's navigation
 * (see {@code SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER = "magentoRootCategoryId"}). This is a
 * page-level structural binding; to pin an individual product-list/carousel component to a category use
 * {@code configure_productlist_component} instead.
 * <p>
 * The category is written as a UID ({@code magentoRootCategoryIdType = "uid"}), matching the tool's
 * {@code categoryUid} argument. {@code showMainCategories} is also written (default {@code false}): unless it
 * is {@code false}, the configured root category does not scope the page's landing navigation. Writes run
 * under the caller's {@link ResourceResolver} so JCR ACLs are enforced.
 */
@Component(service = McpTool.class)
public class ConfigureCatalogPageTool implements McpTool {

    // Consumed by SiteStructureImpl / NavigationImpl (constants:
    // SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER / _TYPE, Navigation.PN_SHOW_MAIN_CATEGORIES).
    private static final String ROOT_CATEGORY_PROPERTY = "magentoRootCategoryId";
    private static final String ROOT_CATEGORY_TYPE_PROPERTY = "magentoRootCategoryIdType";
    private static final String ROOT_CATEGORY_TYPE_UID = "uid";
    private static final String SHOW_MAIN_CATEGORIES_PROPERTY = "showMainCategories";

    /**
     * Known CIF catalog (PLP) page resource types (see {@code SiteStructure.RT_CATALOG_PAGE} and
     * {@code RT_CATALOG_PAGE_V3}; there is no v2). Matching is done via {@link Resource#isResourceType(String)},
     * which follows {@code sling:resourceSuperType}, so proxied project components (e.g. Venia's
     * {@code venia/components/...}) that super-type one of these are also accepted.
     */
    private static final List<String> CIF_CATALOG_PAGE_TYPES = Arrays.asList(
        "core/cif/components/structure/catalogpage/v1/catalogpage",
        "core/cif/components/structure/catalogpage/v3/catalogpage");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_catalog_page";
    }

    @Override
    public String description() {
        return "Bind the root category of a CIF catalog (PLP) page by category UID, so the page's navigation is "
            + "scoped to that category. Optional showMainCategories (default false): when false the navigation shows "
            + "the children of the bound root category. Targets the catalog page; to pin a product-list component to a "
            + "category use configure_productlist_component.";
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
        properties.putObject("showMainCategories").put("type", "boolean");
        schema.putArray("required").add("path").add("categoryUid");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        String categoryUid = args.path("categoryUid").asText(null);
        boolean showMainCategories = args.path("showMainCategories").asBoolean(false);
        if (path == null || categoryUid == null || !path.startsWith("/content/")) {
            throw new IllegalArgumentException("path (under /content) and categoryUid are required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource content = resolver.getResource(path + "/jcr:content");
        if (content == null) {
            throw new IllegalArgumentException("page not found: " + path);
        }
        if (CIF_CATALOG_PAGE_TYPES.stream().noneMatch(content::isResourceType)) {
            throw new IllegalArgumentException("resource is not a CIF catalog page: " + path);
        }
        ModifiableValueMap properties = content.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new IllegalArgumentException("page not modifiable: " + path);
        }
        properties.put(ROOT_CATEGORY_PROPERTY, categoryUid);
        properties.put(ROOT_CATEGORY_TYPE_PROPERTY, ROOT_CATEGORY_TYPE_UID);
        properties.put(SHOW_MAIN_CATEGORIES_PROPERTY, showMainCategories);
        resolver.commit();

        // Post-write verification: re-read the persisted root category so we never report success for a write
        // that did not take (the property the v3/v1 catalog page actually consumes is magentoRootCategoryId).
        Resource persisted = resolver.getResource(path + "/jcr:content");
        boolean updated = persisted != null
            && categoryUid.equals(persisted.getValueMap().get(ROOT_CATEGORY_PROPERTY, String.class));

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("categoryUid", categoryUid);
        out.put("showMainCategories", showMainCategories);
        out.put("updated", updated);
        return out;
    }
}
