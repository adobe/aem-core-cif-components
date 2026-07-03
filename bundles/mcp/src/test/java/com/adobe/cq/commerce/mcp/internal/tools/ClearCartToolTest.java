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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClearCartToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void removesEveryItemThenReturnsEmptyCart() throws Exception {
        CartItemInterface item1 = mock(CartItemInterface.class);
        when(item1.getUid()).thenReturn(new ID("item-1"));
        CartItemInterface item2 = mock(CartItemInterface.class);
        when(item2.getUid()).thenReturn(new ID("item-2"));

        Cart cartWithItems = mock(Cart.class);
        when(cartWithItems.getItems()).thenReturn(Arrays.asList(item1, item2));

        Cart emptyCart = mock(Cart.class);
        when(emptyCart.getId()).thenReturn(new ID("cart-1"));

        List<String> removedUids = new ArrayList<>();
        ClearCartTool tool = new ClearCartTool() {
            @Override
            protected Cart fetch(StoreContext ctx, String cartId) {
                return cartWithItems;
            }

            @Override
            protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
                removedUids.add(uid);
                return emptyCart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode().put("cart_id", "cart-1"));
        assertEquals(Arrays.asList("item-1", "item-2"), removedUids);
        assertEquals("cart-1", out.get("cart_id").asText());
        assertEquals("clear_cart", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void returnsCartUnchangedWhenAlreadyEmpty() throws Exception {
        Cart emptyCart = mock(Cart.class);
        when(emptyCart.getId()).thenReturn(new ID("cart-1"));
        when(emptyCart.getItems()).thenReturn(Collections.emptyList());

        ClearCartTool tool = new ClearCartTool() {
            @Override
            protected Cart fetch(StoreContext ctx, String cartId) {
                return emptyCart;
            }

            @Override
            protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
                throw new AssertionError("should not remove anything from an already-empty cart");
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode().put("cart_id", "cart-1"));
        assertEquals("cart-1", out.get("cart_id").asText());
    }
}
