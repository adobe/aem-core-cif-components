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

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.tools.McpCategoryRetriever;
import com.adobe.cq.commerce.mcp.internal.tools.McpProductRetriever;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool batch-validating whether a set of product SKUs and/or category UIDs still resolve against the live
 * catalog. For each identifier a fresh single-use retriever is used (one query per identifier) and a null fetch or
 * a non-empty {@code getErrors()} result is treated as "does not resolve".
 */
@Component(service = McpTool.class)
public class ValidateContentBindingsTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "validate_content_bindings";
    }

    @Override
    public String description() {
        return "Batch-check whether product SKUs and/or category UIDs still resolve against the live catalog.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("products").put("type", "array").putObject("items").put("type", "string");
        properties.putObject("categories").put("type", "array").putObject("items").put("type", "string");
        return schema;
    }

    protected boolean productResolves(StoreContext ctx, String sku) {
        McpProductRetriever retriever = new McpProductRetriever(ctx.getClient());
        retriever.setIdentifier(sku);
        ProductInterface product = retriever.fetchProduct();
        return product != null && retriever.getErrors().isEmpty();
    }

    protected boolean categoryResolves(StoreContext ctx, String uid) {
        McpCategoryRetriever retriever = new McpCategoryRetriever(ctx.getClient());
        retriever.setIdentifier(uid);
        CategoryInterface category = retriever.fetchCategory();
        return category != null && retriever.getErrors().isEmpty();
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;

        ArrayNode productsArg = args.has("products") && args.get("products").isArray() ? (ArrayNode) args.get(
            "products") : null;
        ArrayNode categoriesArg = args.has("categories") && args.get("categories").isArray() ? (ArrayNode) args.get(
            "categories") : null;

        boolean hasProducts = productsArg != null && productsArg.size() > 0;
        boolean hasCategories = categoriesArg != null && categoriesArg.size() > 0;
        if (!hasProducts && !hasCategories) {
            throw new IllegalArgumentException("At least one of 'products' or 'categories' must be a non-empty array");
        }

        ObjectNode out = mapper.createObjectNode();
        ArrayNode products = out.putArray("products");
        if (productsArg != null) {
            for (JsonNode skuNode : productsArg) {
                if (skuNode.isNull()) {
                    continue;
                }
                String sku = skuNode.asText();
                ObjectNode entry = products.addObject();
                entry.put("sku", sku);
                entry.put("resolves", productResolves(ctx, sku));
            }
        }

        ArrayNode categories = out.putArray("categories");
        if (categoriesArg != null) {
            for (JsonNode uidNode : categoriesArg) {
                if (uidNode.isNull()) {
                    continue;
                }
                String uid = uidNode.asText();
                ObjectNode entry = categories.addObject();
                entry.put("uid", uid);
                entry.put("resolves", categoryResolves(ctx, uid));
            }
        }

        return out;
    }
}
