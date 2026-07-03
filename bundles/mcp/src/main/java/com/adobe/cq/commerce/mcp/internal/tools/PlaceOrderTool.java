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

import com.adobe.cq.commerce.magento.graphql.Order;
import com.adobe.cq.commerce.magento.graphql.PlaceOrderInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool placing the order for a cart that has an email, shipping address, shipping method and payment method
 * already set (via {@code set_shipping_address}, {@code set_shipping_method}, {@code set_payment_method}). This is
 * the one cart tool that is <b>not idempotent and not reversible</b> &mdash; it creates a real order.
 */
@Component(service = McpTool.class)
public class PlaceOrderTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "place_order";
    }

    @Override
    public String description() {
        return "Place the order for a cart that already has a shipping address, shipping method and payment method "
            + "set. Creates a real order -- not reversible.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("cart_id").put("type", "string");
        schema.putArray("required").add("cart_id");
        return schema;
    }

    protected Order placeOrder(StoreContext ctx, String cartId) {
        return mutationClient
            .execute(ctx, m -> m.placeOrder(a -> a.input(new PlaceOrderInput(cartId)), out -> out.order(o -> o.orderNumber().orderId())))
            .getPlaceOrder()
            .getOrder();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            throw new IllegalArgumentException("cart_id is required");
        }

        Order order = placeOrder(ctx, cartId);

        ObjectNode out = mapper.createObjectNode();
        out.put("order_number", order.getOrderNumber());
        return out;
    }
}
