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

import com.adobe.cq.commerce.magento.graphql.BillingAddressInput;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartAddressInput;
import com.adobe.cq.commerce.magento.graphql.SetBillingAddressOnCartInput;
import com.adobe.cq.commerce.magento.graphql.SetGuestEmailOnCartInput;
import com.adobe.cq.commerce.magento.graphql.SetShippingAddressesOnCartInput;
import com.adobe.cq.commerce.magento.graphql.ShippingAddressInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool setting the guest email and shipping address on a cart (billing address defaults to the same address).
 * Returns the available shipping methods for that address, so the agent doesn't have to guess valid
 * carrier_code/method_code values for {@code set_shipping_method}.
 * <p>
 * Requires explicit confirmation: without {@code confirm: true}, nothing is committed to the cart &mdash; the tool
 * just echoes back the address it received so the caller can review it with the customer first. Call again with
 * {@code confirm: true} to actually apply it.
 */
@Component(service = McpTool.class)
public class SetShippingAddressTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "set_shipping_address";
    }

    @Override
    public boolean commerceJourney() {
        return true;
    }

    @Override
    public String description() {
        return "Set the guest email and shipping address on a cart (billing address defaults to the same address). "
            + "Requires confirm: true to actually apply it -- without it, returns the address for review only. "
            + "Once confirmed, returns the available shipping methods for that address.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("cart_id").put("type", "string");
        properties.putObject("email").put("type", "string");
        properties.putObject("firstname").put("type", "string");
        properties.putObject("lastname").put("type", "string");
        properties.putObject("street").put("type", "string");
        properties.putObject("city").put("type", "string");
        properties.putObject("region").put("type", "string");
        properties.putObject("postcode").put("type", "string");
        properties.putObject("country_code").put("type", "string");
        properties.putObject("telephone").put("type", "string");
        properties.putObject("confirm").put("type", "boolean");
        schema.putArray("required").add("cart_id").add("email").add("firstname").add("lastname").add("street")
            .add("city").add("region").add("postcode").add("country_code").add("telephone");
        return schema;
    }

    protected Cart setEmailAndAddress(StoreContext ctx, String cartId, String email, CartAddressInput address) {
        mutationClient.execute(ctx,
            m -> m.setGuestEmailOnCart(a -> a.input(new SetGuestEmailOnCartInput(cartId, email)), out -> out.cart(c -> c.id())));

        ShippingAddressInput shippingAddressInput = new ShippingAddressInput().setAddress(address);
        Cart cart = mutationClient
            .execute(ctx,
                m -> m.setShippingAddressesOnCart(
                    a -> a.input(new SetShippingAddressesOnCartInput(cartId, Collections.singletonList(shippingAddressInput))),
                    out -> out.cart(c -> c.shippingAddresses(sa -> sa.availableShippingMethods(sm -> sm
                        .carrierCode()
                        .carrierTitle()
                        .methodCode()
                        .methodTitle()
                        .amount(mo -> mo.value().currency()))))))
            .getSetShippingAddressesOnCart()
            .getCart();

        mutationClient.execute(ctx,
            m -> m.setBillingAddressOnCart(
                a -> a.input(new SetBillingAddressOnCartInput(new BillingAddressInput().setSameAsShipping(true), cartId)),
                out -> out.cart(c -> c.id())));

        return cart;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        String email = args.path("email").asText(null);
        String firstname = args.path("firstname").asText(null);
        String lastname = args.path("lastname").asText(null);
        String street = args.path("street").asText(null);
        String city = args.path("city").asText(null);
        String region = args.path("region").asText(null);
        String postcode = args.path("postcode").asText(null);
        String countryCode = args.path("country_code").asText(null);
        String telephone = args.path("telephone").asText(null);
        if (cartId == null || email == null || firstname == null || lastname == null || street == null || city == null
            || region == null || postcode == null || countryCode == null || telephone == null) {
            throw new IllegalArgumentException(
                "cart_id, email, firstname, lastname, street, city, region, postcode, country_code and telephone are required");
        }

        boolean confirm = args.path("confirm").asBoolean(false);
        if (!confirm) {
            ObjectNode preview = mapper.createObjectNode();
            preview.put("cart_id", cartId);
            preview.put("confirmed", false);
            ObjectNode pending = preview.putObject("pending_shipping_address");
            pending.put("email", email);
            pending.put("firstname", firstname);
            pending.put("lastname", lastname);
            pending.put("street", street);
            pending.put("city", city);
            pending.put("region", region);
            pending.put("postcode", postcode);
            pending.put("country_code", countryCode);
            pending.put("telephone", telephone);
            preview.put("message",
                "Confirm this address with the customer, then call set_shipping_address again with confirm: true to apply it.");
            return preview;
        }

        CartAddressInput address = new CartAddressInput(city, countryCode, firstname, lastname,
            Collections.singletonList(street), telephone).setRegion(region).setPostcode(postcode);
        Cart cart = setEmailAndAddress(ctx, cartId, email, address);

        ObjectNode out = mapper.createObjectNode();
        out.put("cart_id", cartId);
        out.put("confirmed", true);
        ArrayNode methods = out.putArray("shipping_methods");
        if (cart.getShippingAddresses() != null) {
            cart.getShippingAddresses().forEach(sa -> {
                if (sa.getAvailableShippingMethods() != null) {
                    sa.getAvailableShippingMethods().forEach(sm -> {
                        ObjectNode methodNode = methods.addObject();
                        methodNode.put("carrier_code", sm.getCarrierCode());
                        methodNode.put("carrier_title", sm.getCarrierTitle());
                        methodNode.put("method_code", sm.getMethodCode());
                        methodNode.put("method_title", sm.getMethodTitle());
                        if (sm.getAmount() != null) {
                            methodNode.put("price", sm.getAmount().getValue());
                            methodNode.put("currency",
                                sm.getAmount().getCurrency() != null ? sm.getAmount().getCurrency().toString() : null);
                        }
                    });
                }
            });
        }
        return out;
    }
}
