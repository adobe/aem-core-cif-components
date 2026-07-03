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

import com.adobe.cq.commerce.magento.graphql.AvailablePaymentMethod;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetShippingMethodToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsAvailablePaymentMethods() throws Exception {
        AvailablePaymentMethod checkmo = mock(AvailablePaymentMethod.class);
        when(checkmo.getCode()).thenReturn("checkmo");
        when(checkmo.getTitle()).thenReturn("Check / Money order");

        Cart cart = mock(Cart.class);
        when(cart.getAvailablePaymentMethods()).thenReturn(Collections.singletonList(checkmo));

        SetShippingMethodTool tool = new SetShippingMethodTool() {
            @Override
            protected Cart setShippingMethod(StoreContext ctx, String cartId, String carrierCode, String methodCode) {
                assertEquals("cart-1", cartId);
                assertEquals("flatrate", carrierCode);
                assertEquals("flatrate", methodCode);
                return cart;
            }
        };

        JsonNode args = mapper.createObjectNode().put("cart_id", "cart-1").put("carrier_code", "flatrate")
            .put("method_code", "flatrate").put("confirm", true);
        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
        assertTrue(out.get("confirmed").asBoolean());
        JsonNode method = out.get("payment_methods").get(0);
        assertEquals("checkmo", method.get("code").asText());
        assertEquals("Check / Money order", method.get("title").asText());
        assertEquals("set_shipping_method", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void previewsWithoutCommittingWhenConfirmOmitted() throws Exception {
        SetShippingMethodTool tool = new SetShippingMethodTool() {
            @Override
            protected Cart setShippingMethod(StoreContext ctx, String cartId, String carrierCode, String methodCode) {
                throw new AssertionError("should not commit the shipping method when confirm is not true");
            }
        };

        JsonNode args = mapper.createObjectNode().put("cart_id", "cart-1").put("carrier_code", "flatrate")
            .put("method_code", "flatrate");
        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
        assertFalse(out.get("confirmed").asBoolean());
        JsonNode pending = out.get("pending_shipping_method");
        assertEquals("flatrate", pending.get("carrier_code").asText());
        assertEquals("flatrate", pending.get("method_code").asText());
    }

    @Test
    public void requiresAllFields() {
        JsonNode args = mapper.createObjectNode().put("cart_id", "cart-1");
        assertThrows(IllegalArgumentException.class, () -> new SetShippingMethodTool().call(mock(StoreContext.class), args));
    }
}
