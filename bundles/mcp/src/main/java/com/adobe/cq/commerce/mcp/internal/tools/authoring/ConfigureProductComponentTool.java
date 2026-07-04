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
package com.adobe.cq.commerce.mcp.internal.tools.authoring;

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
 * MCP write tool binding a product SKU to a CIF product component resource, via the caller's
 * {@link ResourceResolver} so that JCR ACLs are enforced.
 */
@Component(service = McpTool.class)
public class ConfigureProductComponentTool implements McpTool {

    /**
     * Known CIF product component resource types (see {@code ProductImpl.RESOURCE_TYPE} in v1/v2/v3 of
     * {@code com.adobe.cq.commerce.core.components.internal.models.*.product}). Matching is done via
     * {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType}, so proxied
     * project components (e.g. Venia's {@code venia/components/...}) that super-type one of these are
     * also accepted.
     */
    private static final List<String> CIF_PRODUCT_COMPONENT_TYPES = Arrays.asList(
        "core/cif/components/commerce/product/v1/product",
        "core/cif/components/commerce/product/v2/product",
        "core/cif/components/commerce/product/v3/product");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_product_component";
    }

    @Override
    public String description() {
        return "Bind a product SKU to a CIF product component resource.";
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
        properties.putObject("sku").put("type", "string");
        schema.putArray("required").add("path").add("sku");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        String sku = args.path("sku").asText(null);
        if (path == null || sku == null || !path.startsWith("/content/")) {
            throw new IllegalArgumentException("path (under /content) and sku are required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = resolver.getResource(path);
        if (target == null) {
            throw new IllegalArgumentException("resource not found: " + path);
        }
        if (CIF_PRODUCT_COMPONENT_TYPES.stream().noneMatch(target::isResourceType)) {
            throw new IllegalArgumentException("resource is not a CIF product component: " + path);
        }

        ModifiableValueMap properties = target.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new IllegalArgumentException("resource not modifiable: " + path);
        }
        properties.put("selection", sku);
        properties.put("selectionType", "combinedSku");
        resolver.commit();

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("sku", sku);
        out.put("updated", true);
        return out;
    }
}
