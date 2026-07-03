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

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool fetching the current contents of a guest cart by cart id.
 */
@Component(service = McpTool.class)
public class ViewCartTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "view_cart";
    }

    @Override
    public String description() {
        return "Fetch the current contents of a guest cart by cart id.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("cart_id").put("type", "string");
        schema.putArray("required").add("cart_id");
        return schema;
    }

    protected Cart fetch(StoreContext ctx, String cartId) {
        String query = Operations.query(q -> q.cart(cartId, CartMutationClient.cartFields())).toString();
        // force POST (bypasses MagentoGraphqlClientImpl's GET-based response cache): cart contents mutate on every
        // add/update/remove and must never be served stale, regardless of whether caching is ever enabled for this
        // resource type
        GraphqlResponse<Query, Error> response = ctx.getClient().execute(query, HttpMethod.POST);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new IllegalArgumentException(response.getErrors().get(0).getMessage());
        }
        return response.getData().getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            throw new IllegalArgumentException("cart_id is required");
        }
        Cart cart = fetch(ctx, cartId);
        return DtoMapper.cart(mapper, cart);
    }
}
