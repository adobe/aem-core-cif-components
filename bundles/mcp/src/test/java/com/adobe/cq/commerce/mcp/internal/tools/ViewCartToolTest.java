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

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViewCartToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsCartForCartId() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getClient()).thenReturn(mock(MagentoGraphqlClient.class));

        ViewCartTool tool = new ViewCartTool() {
            @Override
            protected Cart fetch(StoreContext c, String cartId) {
                return cart;
            }
        };
        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("cart_id", "cart-1"));
        assertEquals("cart-1", out.get("cart_id").asText());
        assertEquals("view_cart", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requiresCartId() throws Exception {
        StoreContext ctx = mock(StoreContext.class);
        new ViewCartTool().call(ctx, mapper.createObjectNode());
    }
}
