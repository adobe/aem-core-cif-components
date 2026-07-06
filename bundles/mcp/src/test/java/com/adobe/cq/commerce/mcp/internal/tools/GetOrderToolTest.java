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

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * {@link GetOrderTool#fetchGuestOrder} does the actual GraphQL call (a hand-written query since {@code guestOrder}
 * has no typed builder); these tests exercise the JSON-to-DTO mapping only, by overriding it to return a canned
 * response. The GraphQL call itself is validated by live testing, matching the same accepted testing limitation
 * documented for {@code CartMutationClient.resolveGraphqlClient}'s production path.
 */
public class GetOrderToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    public void returnsOrderDetailsFromTheGuestOrderQuery() throws Exception {
        JsonObject guestOrder = parse("{"
            + "\"status\": \"Pending\","
            + "\"order_date\": \"03/07/2026 06:25:43\","
            + "\"number\": \"000000032\","
            + "\"shipping_method\": \"Flat Rate - Fixed\","
            + "\"payment_methods\": [{\"name\": \"Check / Money order\", \"type\": \"checkmo\"}],"
            + "\"billing_address\": {\"firstname\": \"Test\", \"lastname\": \"Example\", \"street\": [\"123 Main St\"], "
            + "\"city\": \"Austin\", \"region\": \"Texas\", \"postcode\": \"78701\", \"country_code\": \"US\", "
            + "\"telephone\": \"5555555555\"},"
            + "\"shipping_address\": {\"firstname\": \"Test\", \"lastname\": \"Example\", \"street\": [\"123 Main St\"], "
            + "\"city\": \"Austin\", \"region\": \"Texas\", \"postcode\": \"78701\", \"country_code\": \"US\", "
            + "\"telephone\": \"5555555555\"},"
            + "\"items\": [{\"product_name\": \"Flora Tank Dress\", \"product_sku\": \"VD06-PE-M\", \"quantity_ordered\": 1}, "
            + "{\"product_name\": \"Carmina Necklace\", \"product_sku\": \"VA13-GO-NA\", \"quantity_ordered\": 2}],"
            + "\"total\": {\"grand_total\": {\"value\": 319.0, \"currency\": \"USD\"}, "
            + "\"subtotal\": {\"value\": 304.0, \"currency\": \"USD\"}, "
            + "\"total_shipping\": {\"value\": 15.0, \"currency\": \"USD\"}}"
            + "}");

        GetOrderTool tool = new GetOrderTool() {
            @Override
            protected JsonObject fetchGuestOrder(StoreContext ctx, String orderNumber, String email, String lastname) {
                assertEquals("000000032", orderNumber);
                assertEquals("test123@example1.com", email);
                assertEquals("Example", lastname);
                return guestOrder;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode()
            .put("order_number", "000000032")
            .put("email", "test123@example1.com")
            .put("lastname", "Example"));

        assertEquals("000000032", out.get("order_number").asText());
        assertEquals("Pending", out.get("status").asText());
        assertEquals("03/07/2026 06:25:43", out.get("order_date").asText());
        assertEquals("Flat Rate - Fixed", out.get("shipping_method").asText());
        assertEquals("checkmo", out.get("payment_method").asText());

        JsonNode shippingAddress = out.get("shipping_address");
        assertEquals("Test", shippingAddress.get("firstname").asText());
        assertEquals("Example", shippingAddress.get("lastname").asText());
        assertEquals("123 Main St", shippingAddress.get("street").asText());
        assertEquals("Austin", shippingAddress.get("city").asText());
        assertEquals("Texas", shippingAddress.get("region").asText());
        assertEquals("78701", shippingAddress.get("postcode").asText());
        assertEquals("US", shippingAddress.get("country_code").asText());
        assertEquals("5555555555", shippingAddress.get("telephone").asText());

        JsonNode billingAddress = out.get("billing_address");
        assertEquals("Austin", billingAddress.get("city").asText());

        assertEquals(2, out.get("items").size());
        assertEquals("VD06-PE-M", out.get("items").get(0).get("sku").asText());
        assertEquals("Flora Tank Dress", out.get("items").get(0).get("name").asText());
        assertEquals(1.0, out.get("items").get(0).get("quantity").asDouble(), 0.001);
        assertEquals("VA13-GO-NA", out.get("items").get(1).get("sku").asText());
        assertEquals(2.0, out.get("items").get(1).get("quantity").asDouble(), 0.001);

        assertEquals(319.0, out.get("grand_total").asDouble(), 0.001);
        assertEquals(304.0, out.get("subtotal").asDouble(), 0.001);
        assertEquals(15.0, out.get("shipping_total").asDouble(), 0.001);
        assertEquals("USD", out.get("currency").asText());

        assertEquals("get_order", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void omitsAddressAndPaymentFieldsWhenAbsent() throws Exception {
        JsonObject guestOrder = parse("{\"status\": \"Pending\", \"number\": \"000000032\", \"items\": []}");

        GetOrderTool tool = new GetOrderTool() {
            @Override
            protected JsonObject fetchGuestOrder(StoreContext ctx, String orderNumber, String email, String lastname) {
                return guestOrder;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode()
            .put("order_number", "000000032")
            .put("email", "test123@example1.com")
            .put("lastname", "Example"));

        assertFalse(out.has("shipping_address"));
        assertFalse(out.has("billing_address"));
        assertFalse(out.has("payment_method"));
        assertFalse(out.has("grand_total"));
        assertEquals(0, out.get("items").size());
    }

    @Test
    public void requiresOrderNumberEmailAndLastname() {
        assertThrows(IllegalArgumentException.class,
            () -> new GetOrderTool().call(mock(StoreContext.class),
                mapper.createObjectNode().put("email", "a@b.com").put("lastname", "Example")));
        assertThrows(IllegalArgumentException.class,
            () -> new GetOrderTool().call(mock(StoreContext.class),
                mapper.createObjectNode().put("order_number", "1").put("lastname", "Example")));
        assertThrows(IllegalArgumentException.class,
            () -> new GetOrderTool().call(mock(StoreContext.class),
                mapper.createObjectNode().put("order_number", "1").put("email", "a@b.com")));
    }
}
