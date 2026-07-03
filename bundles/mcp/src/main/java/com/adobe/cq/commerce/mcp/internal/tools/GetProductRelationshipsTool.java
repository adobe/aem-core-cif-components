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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductLinksInterface;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool returning a product's related/upsell/crosssell links.
 */
@Component(service = McpTool.class)
public class GetProductRelationshipsTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_product_relationships";
    }

    @Override
    public String description() {
        return "List a product's related, upsell and crosssell product links.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sku").put("type", "string");
        properties.putObject("linkType").put("type", "string");
        schema.putArray("required").add("sku");
        return schema;
    }

    protected ProductInterface fetch(StoreContext ctx, String sku) {
        McpProductRelationshipsRetriever retriever = new McpProductRelationshipsRetriever(ctx.getClient());
        retriever.setIdentifier(sku);
        return retriever.fetchProduct();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String sku = args.path("sku").asText(null);
        if (StringUtils.isBlank(sku)) {
            throw new IllegalArgumentException("sku is required");
        }
        String linkTypeFilter = args.path("linkType").asText(null);

        ProductInterface product = fetch(ctx, sku);

        ObjectNode out = mapper.createObjectNode();
        out.put("sku", sku);
        ArrayNode links = out.putArray("links");
        if (product != null) {
            List<ProductLinksInterface> productLinks = product.getProductLinks();
            if (productLinks != null) {
                for (ProductLinksInterface link : productLinks) {
                    if (StringUtils.isNotBlank(linkTypeFilter) && !linkTypeFilter.equalsIgnoreCase(link.getLinkType())) {
                        continue;
                    }
                    ObjectNode linkNode = links.addObject();
                    linkNode.put("linkType", link.getLinkType());
                    linkNode.put("sku", link.getSku());
                    linkNode.put("linkedProductSku", link.getLinkedProductSku());
                    linkNode.put("linkedProductType", link.getLinkedProductType());
                    linkNode.put("position", link.getPosition());
                }
            }
        }
        return out;
    }
}
