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
import com.adobe.cq.commerce.magento.graphql.PaymentMethodInput;
import com.adobe.cq.commerce.magento.graphql.SetPaymentMethodOnCartInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool selecting a payment method on a cart (code from {@code set_shipping_method}'s result, e.g.
 * {@code "checkmo"} for Check / Money order). Once confirmed, the cart is ready for {@code place_order}.
 * <p>
 * Requires explicit confirmation: without {@code confirm: true}, nothing is committed &mdash; the tool just echoes
 * back the payment method it received. Call again with {@code confirm: true} to actually apply it.
 */
@Component(service = McpTool.class)
public class SetPaymentMethodTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "set_payment_method";
    }

    @Override
    public String description() {
        return "Select a payment method on a cart (code from set_shipping_method's result). Requires confirm: true "
            + "to actually apply it -- without it, returns the method for review only. Once confirmed the cart is "
            + "ready for place_order.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("cart_id").put("type", "string");
        properties.putObject("payment_method").put("type", "string");
        properties.putObject("confirm").put("type", "boolean");
        schema.putArray("required").add("cart_id").add("payment_method");
        return schema;
    }

    protected Cart setPaymentMethod(StoreContext ctx, String cartId, String paymentMethod) {
        return mutationClient
            .execute(ctx,
                m -> m.setPaymentMethodOnCart(
                    a -> a.input(new SetPaymentMethodOnCartInput(cartId, new PaymentMethodInput(paymentMethod))),
                    out -> out.cart(c -> c.selectedPaymentMethod(spm -> spm.code()))))
            .getSetPaymentMethodOnCart()
            .getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        String paymentMethod = args.path("payment_method").asText(null);
        if (cartId == null || paymentMethod == null) {
            throw new IllegalArgumentException("cart_id and payment_method are required");
        }

        boolean confirm = args.path("confirm").asBoolean(false);
        if (!confirm) {
            ObjectNode preview = mapper.createObjectNode();
            preview.put("cart_id", cartId);
            preview.put("confirmed", false);
            preview.put("pending_payment_method", paymentMethod);
            preview.put("message",
                "Confirm this payment method with the customer, then call set_payment_method again with confirm: true to apply it.");
            return preview;
        }

        Cart cart = setPaymentMethod(ctx, cartId, paymentMethod);

        ObjectNode out = mapper.createObjectNode();
        out.put("cart_id", cartId);
        out.put("confirmed", true);
        out.put("payment_method",
            cart.getSelectedPaymentMethod() != null ? cart.getSelectedPaymentMethod().getCode() : paymentMethod);
        out.put("ready_to_place_order", true);
        return out;
    }
}
