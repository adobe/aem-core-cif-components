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

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.AvailableShippingMethod;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartAddressInput;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ShippingCartAddress;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetShippingAddressToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsAvailableShippingMethods() throws Exception {
        Money price = mock(Money.class);
        when(price.getValue()).thenReturn(5.0);
        when(price.getCurrency()).thenReturn(CurrencyEnum.USD);

        AvailableShippingMethod flatRate = mock(AvailableShippingMethod.class);
        when(flatRate.getCarrierCode()).thenReturn("flatrate");
        when(flatRate.getCarrierTitle()).thenReturn("Flat Rate");
        when(flatRate.getMethodCode()).thenReturn("flatrate");
        when(flatRate.getMethodTitle()).thenReturn("Fixed");
        when(flatRate.getAmount()).thenReturn(price);

        ShippingCartAddress address = mock(ShippingCartAddress.class);
        when(address.getAvailableShippingMethods()).thenReturn(Collections.singletonList(flatRate));

        Cart cart = mock(Cart.class);
        when(cart.getShippingAddresses()).thenReturn(Collections.singletonList(address));

        SetShippingAddressTool tool = new SetShippingAddressTool() {
            @Override
            protected Cart setEmailAndAddress(StoreContext ctx, String cartId, String email, CartAddressInput cartAddress) {
                assertEquals("cart-1", cartId);
                assertEquals("test@example.com", email);
                assertEquals("Austin", cartAddress.getCity());
                assertEquals("US", cartAddress.getCountryCode());
                return cart;
            }
        };

        com.fasterxml.jackson.databind.node.ObjectNode args = mapper.createObjectNode()
            .put("cart_id", "cart-1")
            .put("email", "test@example.com")
            .put("firstname", "Test")
            .put("lastname", "User")
            .put("street", "123 Main St")
            .put("city", "Austin")
            .put("region", "TX")
            .put("postcode", "78701")
            .put("country_code", "US")
            .put("telephone", "5555555555")
            .put("confirm", true);

        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
        assertTrue(out.get("confirmed").asBoolean());
        JsonNode method = out.get("shipping_methods").get(0);
        assertEquals("flatrate", method.get("carrier_code").asText());
        assertEquals("flatrate", method.get("method_code").asText());
        assertEquals(5.0, method.get("price").asDouble(), 0.001);
        assertEquals("USD", method.get("currency").asText());
        assertEquals("set_shipping_address", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void previewsWithoutCommittingWhenConfirmOmitted() throws Exception {
        SetShippingAddressTool tool = new SetShippingAddressTool() {
            @Override
            protected Cart setEmailAndAddress(StoreContext ctx, String cartId, String email, CartAddressInput cartAddress) {
                throw new AssertionError("should not commit the address when confirm is not true");
            }
        };

        com.fasterxml.jackson.databind.node.ObjectNode args = mapper.createObjectNode()
            .put("cart_id", "cart-1")
            .put("email", "test@example.com")
            .put("firstname", "Test")
            .put("lastname", "User")
            .put("street", "123 Main St")
            .put("city", "Austin")
            .put("region", "TX")
            .put("postcode", "78701")
            .put("country_code", "US")
            .put("telephone", "5555555555");

        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
        assertFalse(out.get("confirmed").asBoolean());
        JsonNode pending = out.get("pending_shipping_address");
        assertEquals("Austin", pending.get("city").asText());
        assertEquals("123 Main St", pending.get("street").asText());
    }

    @Test
    public void requiresAllFields() {
        JsonNode args = mapper.createObjectNode().put("cart_id", "cart-1");
        assertThrows(IllegalArgumentException.class, () -> new SetShippingAddressTool().call(mock(StoreContext.class), args));
    }
}
