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
 * MCP write tool binding a category UID to a CIF catalog (PLP) page's {@code jcr:content} node, via the
 * caller's {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * The category binding is stored in the {@code category} property, matching the property read by
 * {@code com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl} (see
 * {@code CATEGORY_PROPERTY = "category"}, bound via {@code @ValueMapValue(name = "category")}) and written
 * by the productlist component dialogs (e.g. {@code cifcategoryfield name="./category"}).
 */
@Component(service = McpTool.class)
public class ConfigureCatalogPageTool implements McpTool {
    private static final String CATEGORY_PROPERTY = "category";

    /**
     * Known CIF catalog (PLP) page resource types (see {@code SiteStructure.RT_CATALOG_PAGE} and
     * {@code RT_CATALOG_PAGE_V3}). Matching is done via {@link Resource#isResourceType(String)}, which
     * follows {@code sling:resourceSuperType}, so proxied project components (e.g. Venia's
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
        return "Bind a category UID to a CIF catalog (PLP) page.";
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
        properties.put(CATEGORY_PROPERTY, categoryUid);
        resolver.commit();

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("categoryUid", categoryUid);
        out.put("updated", true);
        return out;
    }
}
