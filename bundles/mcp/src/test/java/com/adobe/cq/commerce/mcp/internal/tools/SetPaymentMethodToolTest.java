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

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.SelectedPaymentMethod;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetPaymentMethodToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void confirmsPaymentMethodSelected() throws Exception {
        SelectedPaymentMethod selected = mock(SelectedPaymentMethod.class);
        when(selected.getCode()).thenReturn("checkmo");

        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));
        when(cart.getSelectedPaymentMethod()).thenReturn(selected);

        SetPaymentMethodTool tool = new SetPaymentMethodTool() {
            @Override
            protected Cart setPaymentMethod(StoreContext ctx, String cartId, String paymentMethod) {
                assertEquals("cart-1", cartId);
                assertEquals("checkmo", paymentMethod);
                return cart;
            }
        };

        JsonNode args = mapper.createObjectNode().put("cart_id", "cart-1").put("payment_method", "checkmo").put("confirm", true);
        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
        assertEquals("checkmo", out.get("payment_method").asText());
        assertTrue(out.get("confirmed").asBoolean());
        assertTrue(out.get("ready_to_place_order").asBoolean());
        assertEquals("set_payment_method", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void previewsWithoutCommittingWhenConfirmOmitted() throws Exception {
        SetPaymentMethodTool tool = new SetPaymentMethodTool() {
            @Override
            protected Cart setPaymentMethod(StoreContext ctx, String cartId, String paymentMethod) {
                throw new AssertionError("should not commit the payment method when confirm is not true");
            }
        };

        JsonNode args = mapper.createObjectNode().put("cart_id", "cart-1").put("payment_method", "checkmo");
        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
        assertFalse(out.get("confirmed").asBoolean());
        assertEquals("checkmo", out.get("pending_payment_method").asText());
    }

    @Test
    public void requiresAllFields() {
        JsonNode args = mapper.createObjectNode().put("cart_id", "cart-1");
        assertThrows(IllegalArgumentException.class, () -> new SetPaymentMethodTool().call(mock(StoreContext.class), args));
    }
}
