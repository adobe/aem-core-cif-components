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
import com.adobe.cq.commerce.magento.graphql.SetShippingMethodsOnCartInput;
import com.adobe.cq.commerce.magento.graphql.ShippingMethodInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool selecting a shipping method on a cart (from the {@code shipping_methods} a prior
 * {@code set_shipping_address} call returned). Returns the available payment methods, so the agent doesn't have to
 * guess a valid code for {@code set_payment_method}.
 * <p>
 * Requires explicit confirmation: without {@code confirm: true}, nothing is committed &mdash; the tool just echoes
 * back the method it received. Call again with {@code confirm: true} to actually apply it.
 */
@Component(service = McpTool.class)
public class SetShippingMethodTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "set_shipping_method";
    }

    @Override
    public String description() {
        return "Select a shipping method on a cart (carrier_code/method_code from set_shipping_address's result). "
            + "Requires confirm: true to actually apply it -- without it, returns the method for review only. "
            + "Once confirmed, returns the available payment methods.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("cart_id").put("type", "string");
        properties.putObject("carrier_code").put("type", "string");
        properties.putObject("method_code").put("type", "string");
        properties.putObject("confirm").put("type", "boolean");
        schema.putArray("required").add("cart_id").add("carrier_code").add("method_code");
        return schema;
    }

    protected Cart setShippingMethod(StoreContext ctx, String cartId, String carrierCode, String methodCode) {
        ShippingMethodInput methodInput = new ShippingMethodInput(carrierCode, methodCode);
        return mutationClient
            .execute(ctx,
                m -> m.setShippingMethodsOnCart(
                    a -> a.input(new SetShippingMethodsOnCartInput(cartId, Collections.singletonList(methodInput))),
                    out -> out.cart(c -> c.availablePaymentMethods(pm -> pm.code().title()))))
            .getSetShippingMethodsOnCart()
            .getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        String carrierCode = args.path("carrier_code").asText(null);
        String methodCode = args.path("method_code").asText(null);
        if (cartId == null || carrierCode == null || methodCode == null) {
            throw new IllegalArgumentException("cart_id, carrier_code and method_code are required");
        }

        boolean confirm = args.path("confirm").asBoolean(false);
        if (!confirm) {
            ObjectNode preview = mapper.createObjectNode();
            preview.put("cart_id", cartId);
            preview.put("confirmed", false);
            ObjectNode pending = preview.putObject("pending_shipping_method");
            pending.put("carrier_code", carrierCode);
            pending.put("method_code", methodCode);
            preview.put("message",
                "Confirm this shipping method with the customer, then call set_shipping_method again with confirm: true to apply it.");
            return preview;
        }

        Cart cart = setShippingMethod(ctx, cartId, carrierCode, methodCode);

        ObjectNode out = mapper.createObjectNode();
        out.put("cart_id", cartId);
        out.put("confirmed", true);
        ArrayNode methods = out.putArray("payment_methods");
        if (cart.getAvailablePaymentMethods() != null) {
            cart.getAvailablePaymentMethods().forEach(pm -> {
                ObjectNode methodNode = methods.addObject();
                methodNode.put("code", pm.getCode());
                methodNode.put("title", pm.getTitle());
            });
        }
        return out;
    }
}
