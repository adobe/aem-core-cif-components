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

public class UpdateCartItemToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void positiveQuantityUpdatesItem() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        UpdateCartItemTool tool = new UpdateCartItemTool() {
            @Override
            protected Cart updateQuantity(StoreContext ctx, String cartId, String uid, double quantity) {
                assertEquals("cart-1", cartId);
                assertEquals("item-1", uid);
                assertEquals(3.0, quantity, 0.001);
                return cart;
            }

            @Override
            protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
                throw new AssertionError("should not remove when quantity >= 1");
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class),
            mapper.createObjectNode().put("cart_id", "cart-1").put("uid", "item-1").put("quantity", 3));
        assertEquals("cart-1", out.get("cart_id").asText());
        assertEquals("update_cart_item", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void zeroQuantityRemovesItem() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        UpdateCartItemTool tool = new UpdateCartItemTool() {
            @Override
            protected Cart updateQuantity(StoreContext ctx, String cartId, String uid, double quantity) {
                throw new AssertionError("should not update when quantity == 0");
            }

            @Override
            protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
                assertEquals("cart-1", cartId);
                assertEquals("item-1", uid);
                return cart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class),
            mapper.createObjectNode().put("cart_id", "cart-1").put("uid", "item-1").put("quantity", 0));
        assertEquals("cart-1", out.get("cart_id").asText());
    }
}
