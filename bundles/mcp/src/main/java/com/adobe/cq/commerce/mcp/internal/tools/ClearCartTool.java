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

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemInterface;
import com.adobe.cq.commerce.magento.graphql.RemoveItemFromCartInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify.graphql.support.ID;

/**
 * MCP tool removing every item from a guest cart. Magento has no single "empty cart" mutation, so this fetches the
 * current items and removes them one at a time, stopping on the first failure (no silent partial clears).
 */
@Component(service = McpTool.class)
public class ClearCartTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ViewCartTool viewCartTool = new ViewCartTool();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "clear_cart";
    }

    @Override
    public boolean commerceJourney() {
        return true;
    }

    @Override
    public String description() {
        return "Remove every item from a guest cart.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("cart_id").put("type", "string");
        schema.putArray("required").add("cart_id");
        return schema;
    }

    protected Cart fetch(StoreContext ctx, String cartId) {
        return viewCartTool.fetch(ctx, cartId);
    }

    protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
        RemoveItemFromCartInput input = new RemoveItemFromCartInput(cartId).setCartItemUid(new ID(uid));
        return mutationClient
            .execute(ctx, m -> m.removeItemFromCart(args -> args.input(input), out -> out.cart(CartMutationClient.cartFields())))
            .getRemoveItemFromCart()
            .getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            throw new IllegalArgumentException("cart_id is required");
        }
        Cart cart = fetch(ctx, cartId);
        if (cart.getItems() != null) {
            for (CartItemInterface item : cart.getItems()) {
                cart = removeItem(ctx, cartId, item.getUid().toString());
            }
        }
        return DtoMapper.cart(mapper, cart);
    }
}
