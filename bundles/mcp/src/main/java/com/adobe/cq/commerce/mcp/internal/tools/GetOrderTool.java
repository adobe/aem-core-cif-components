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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.MutationDeserializer;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * MCP tool looking up a previously placed guest order by order number, email and last name, via Magento's
 * {@code guestOrder} query &mdash; works at any later time, independent of any cart (unlike {@code view_cart}, which
 * stops working the moment a cart converts to an order), and requires no authentication. {@code guestOrder} is not
 * modeled in the {@code magento-graphql} Java client library used by every other tool in this module (verified by
 * inspecting the library's generated classes: no {@code guestOrder} method exists on the query builder in any
 * available version), so this tool sends a hand-written GraphQL query &mdash; with variables, not string-interpolated
 * values, to avoid injecting untrusted input into the query text &mdash; and reads the response as a raw
 * {@link JsonObject} instead of a typed class.
 */
@Component(service = McpTool.class)
public class GetOrderTool implements McpTool {
    private static final String QUERY = "query GuestOrder($number: String!, $email: String!, $lastname: String!) { "
        + "guestOrder(input: {number: $number, email: $email, lastname: $lastname}) { "
        + "status order_date number shipping_method "
        + "payment_methods { name type } "
        + "billing_address { firstname lastname street city region postcode country_code telephone } "
        + "shipping_address { firstname lastname street city region postcode country_code telephone } "
        + "items { product_name product_sku quantity_ordered } "
        + "total { grand_total { value currency } subtotal { value currency } total_shipping { value currency } } "
        + "} }";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_order";
    }

    @Override
    public boolean commerceJourney() {
        return true;
    }

    @Override
    public String description() {
        return "Look up a previously placed guest order by order number, email and last name. Works at any later "
            + "time, independent of any cart.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("order_number").put("type", "string");
        properties.putObject("email").put("type", "string");
        properties.putObject("lastname").put("type", "string");
        schema.putArray("required").add("order_number").add("email").add("lastname");
        return schema;
    }

    protected JsonObject fetchGuestOrder(StoreContext ctx, String orderNumber, String email, String lastname) {
        GraphqlClient graphqlClient = CartMutationClient.resolveGraphqlClient(ctx.getResource());
        if (graphqlClient == null) {
            throw new IllegalStateException("GraphQL client not available for resource " + ctx.getResource().getPath());
        }

        GraphqlRequest request = new GraphqlRequest(QUERY);
        Map<String, Object> variables = new HashMap<>();
        variables.put("number", orderNumber);
        variables.put("email", email);
        variables.put("lastname", lastname);
        request.setVariables(variables);

        RequestOptions options = new RequestOptions()
            .withGson(MutationDeserializer.getGson())
            .withHeaders(ctx.getClient() != null ? CartMutationClient.toHeaders(ctx.getClient().getHttpHeaderMap()) : null)
            .withHttpMethod(HttpMethod.POST);

        GraphqlResponse<JsonObject, Error> response = graphqlClient.execute(request, JsonObject.class, Error.class, options);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new IllegalArgumentException(response.getErrors().get(0).getMessage());
        }

        JsonObject data = response.getData();
        JsonElement guestOrder = data != null ? data.get("guestOrder") : null;
        if (guestOrder == null || guestOrder.isJsonNull()) {
            throw new IllegalArgumentException("No order found for that order number, email and last name");
        }
        return guestOrder.getAsJsonObject();
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : null;
    }

    private static Double getDouble(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsDouble() : null;
    }

    private static String getStreet(JsonObject address) {
        JsonElement el = address.get("street");
        if (el == null || el.isJsonNull() || !el.isJsonArray()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (JsonElement e : el.getAsJsonArray()) {
            parts.add(e.getAsString());
        }
        return String.join(", ", parts);
    }

    private static void putAddress(ObjectNode out, String key, JsonElement addressEl) {
        if (addressEl == null || addressEl.isJsonNull()) {
            return;
        }
        JsonObject address = addressEl.getAsJsonObject();
        ObjectNode node = out.putObject(key);
        node.put("firstname", getString(address, "firstname"));
        node.put("lastname", getString(address, "lastname"));
        node.put("street", getStreet(address));
        node.put("city", getString(address, "city"));
        node.put("region", getString(address, "region"));
        node.put("postcode", getString(address, "postcode"));
        node.put("country_code", getString(address, "country_code"));
        node.put("telephone", getString(address, "telephone"));
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String orderNumber = args.path("order_number").asText(null);
        String email = args.path("email").asText(null);
        String lastname = args.path("lastname").asText(null);
        if (orderNumber == null || email == null || lastname == null) {
            throw new IllegalArgumentException("order_number, email and lastname are required");
        }

        JsonObject guestOrder = fetchGuestOrder(ctx, orderNumber, email, lastname);

        ObjectNode out = mapper.createObjectNode();
        out.put("order_number", getString(guestOrder, "number"));
        out.put("status", getString(guestOrder, "status"));
        out.put("order_date", getString(guestOrder, "order_date"));
        out.put("shipping_method", getString(guestOrder, "shipping_method"));

        JsonElement paymentMethodsEl = guestOrder.get("payment_methods");
        if (paymentMethodsEl != null && paymentMethodsEl.isJsonArray()) {
            JsonArray paymentMethods = paymentMethodsEl.getAsJsonArray();
            if (paymentMethods.size() > 0) {
                out.put("payment_method", getString(paymentMethods.get(0).getAsJsonObject(), "type"));
            }
        }

        putAddress(out, "shipping_address", guestOrder.get("shipping_address"));
        putAddress(out, "billing_address", guestOrder.get("billing_address"));

        ArrayNode items = out.putArray("items");
        JsonElement itemsEl = guestOrder.get("items");
        if (itemsEl != null && itemsEl.isJsonArray()) {
            for (JsonElement itemEl : itemsEl.getAsJsonArray()) {
                JsonObject item = itemEl.getAsJsonObject();
                ObjectNode itemNode = items.addObject();
                itemNode.put("sku", getString(item, "product_sku"));
                itemNode.put("name", getString(item, "product_name"));
                itemNode.put("quantity", getDouble(item, "quantity_ordered"));
            }
        }

        JsonElement totalEl = guestOrder.get("total");
        if (totalEl != null && totalEl.isJsonObject()) {
            JsonObject total = totalEl.getAsJsonObject();
            JsonElement grandTotalEl = total.get("grand_total");
            if (grandTotalEl != null && !grandTotalEl.isJsonNull()) {
                JsonObject grandTotal = grandTotalEl.getAsJsonObject();
                out.put("grand_total", getDouble(grandTotal, "value"));
                out.put("currency", getString(grandTotal, "currency"));
            }
            JsonElement subtotalEl = total.get("subtotal");
            if (subtotalEl != null && !subtotalEl.isJsonNull()) {
                out.put("subtotal", getDouble(subtotalEl.getAsJsonObject(), "value"));
            }
            JsonElement shippingTotalEl = total.get("total_shipping");
            if (shippingTotalEl != null && !shippingTotalEl.isJsonNull()) {
                out.put("shipping_total", getDouble(shippingTotalEl.getAsJsonObject(), "value"));
            }
        }

        return out;
    }
}
