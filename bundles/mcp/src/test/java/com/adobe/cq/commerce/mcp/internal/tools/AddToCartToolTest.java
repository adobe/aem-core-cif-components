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
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddToCartToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void createsCartWhenNoCartIdSupplied() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("new-cart"));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected String createEmptyCart(StoreContext ctx) {
                return "new-cart";
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity) {
                assertEquals("new-cart", cartId);
                assertEquals("VSK05", sku);
                assertEquals(1.0, quantity, 0.001);
                return cart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1));
        assertEquals("new-cart", out.get("cart_id").asText());
        assertEquals("add_to_cart", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void reusesSuppliedCartId() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("existing-cart"));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected String createEmptyCart(StoreContext ctx) {
                throw new AssertionError("should not create a new cart when cart_id is supplied");
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity) {
                assertEquals("existing-cart", cartId);
                return cart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class),
            mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1).put("cart_id", "existing-cart"));
        assertEquals("existing-cart", out.get("cart_id").asText());
    }
}
