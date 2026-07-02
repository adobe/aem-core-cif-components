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

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool fetching a single product by SKU.
 */
@Component(service = McpTool.class)
public class GetProductTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_product";
    }

    @Override
    public String description() {
        return "Fetch a single product by SKU.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("sku").put("type", "string");
        schema.putArray("required").add("sku");
        return schema;
    }

    protected ProductInterface fetch(StoreContext ctx, String sku) {
        McpProductRetriever retriever = new McpProductRetriever(ctx.getClient());
        retriever.setIdentifier(sku);
        return retriever.fetchProduct();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String sku = args.path("sku").asText(null);
        if (sku == null) {
            throw new IllegalArgumentException("sku is required");
        }
        ProductInterface product = fetch(ctx, sku);
        ObjectNode out = mapper.createObjectNode();
        if (product == null) {
            out.putNull("sku");
            return out;
        }
        out.put("sku", product.getSku());
        out.put("name", product.getName());
        out.put("urlKey", product.getUrlKey());
        return out;
    }
}
