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

import com.adobe.cq.commerce.magento.graphql.Order;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlaceOrderToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsOrderNumber() throws Exception {
        Order order = mock(Order.class);
        when(order.getOrderNumber()).thenReturn("000000123");

        PlaceOrderTool tool = new PlaceOrderTool() {
            @Override
            protected Order placeOrder(StoreContext ctx, String cartId) {
                assertEquals("cart-1", cartId);
                return order;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode().put("cart_id", "cart-1"));
        assertEquals("000000123", out.get("order_number").asText());
        assertEquals("place_order", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void requiresCartId() {
        assertThrows(IllegalArgumentException.class,
            () -> new PlaceOrderTool().call(mock(StoreContext.class), mapper.createObjectNode()));
    }
}
