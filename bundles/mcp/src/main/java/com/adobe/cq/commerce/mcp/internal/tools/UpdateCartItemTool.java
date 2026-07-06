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

import java.util.Collections;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemUpdateInput;
import com.adobe.cq.commerce.magento.graphql.RemoveItemFromCartInput;
import com.adobe.cq.commerce.magento.graphql.UpdateCartItemsInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify.graphql.support.ID;

/**
 * MCP tool changing a cart line item's quantity, or removing it when quantity is 0.
 */
@Component(service = McpTool.class)
public class UpdateCartItemTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "update_cart_item";
    }

    @Override
    public String description() {
        return "Update a cart line item's quantity (quantity 0 removes the item).";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("cart_id").put("type", "string");
        properties.putObject("uid").put("type", "string");
        properties.putObject("quantity").put("type", "integer");
        schema.putArray("required").add("cart_id").add("uid").add("quantity");
        return schema;
    }

    protected Cart updateQuantity(StoreContext ctx, String cartId, String uid, double quantity) {
        CartItemUpdateInput itemInput = new CartItemUpdateInput().setCartItemUid(new ID(uid)).setQuantity(quantity);
        UpdateCartItemsInput input = new UpdateCartItemsInput(cartId, Collections.singletonList(itemInput));
        return mutationClient
            .execute(ctx, m -> m.updateCartItems(args -> args.input(input), out -> out.cart(CartMutationClient.cartFields())))
            .getUpdateCartItems()
            .getCart();
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
        String uid = args.path("uid").asText(null);
        JsonNode quantityNode = args.get("quantity");
        if (cartId == null || uid == null || quantityNode == null || !quantityNode.isIntegralNumber()
            || quantityNode.asInt() < 0) {
            throw new IllegalArgumentException("cart_id, uid and a non-negative whole-number quantity are required");
        }
        int quantity = quantityNode.asInt();
        Cart cart = quantity == 0 ? removeItem(ctx, cartId, uid) : updateQuantity(ctx, cartId, uid, quantity);
        return DtoMapper.cart(mapper, cart);
    }
}
