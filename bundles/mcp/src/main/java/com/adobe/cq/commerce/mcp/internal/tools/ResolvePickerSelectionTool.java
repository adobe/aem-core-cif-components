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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool resolving display data (name) for a set of SKUs, for authoring pickers.
 */
@Component(service = McpTool.class)
public class ResolvePickerSelectionTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "resolve_picker_selection";
    }

    @Override
    public String description() {
        return "Resolve display data (name) for selected SKUs, for authoring pickers.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode skus = schema.putObject("properties").putObject("skus");
        skus.put("type", "array");
        skus.putObject("items").put("type", "string");
        return schema;
    }

    protected ProductInterface fetch(StoreContext ctx, String sku) {
        McpProductRetriever retriever = new McpProductRetriever(ctx.getClient());
        retriever.setIdentifier(sku);
        return retriever.fetchProduct();
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        ObjectNode out = mapper.createObjectNode();
        ArrayNode items = out.putArray("items");
        for (JsonNode skuNode : args.path("skus")) {
            String sku = skuNode.asText();
            ProductInterface product = fetch(ctx, sku);
            ObjectNode item = items.addObject();
            item.put("sku", sku);
            item.put("name", product == null ? null : product.getName());
        }
        return out;
    }
}
