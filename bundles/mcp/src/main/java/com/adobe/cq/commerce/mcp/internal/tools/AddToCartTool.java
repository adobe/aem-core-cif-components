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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.AddProductsToCartOutput;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemInput;
import com.adobe.cq.commerce.magento.graphql.CartUserInputError;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify.graphql.support.ID;

/**
 * MCP tool adding a SKU/quantity to a guest cart, creating the cart first if no cart id is supplied. Supports
 * configurable products via an optional {@code options} argument (e.g. {@code {"color": "Blue"}}), resolved to
 * Magento option-value UIDs by {@link ConfigurableOptionResolver}.
 */
@Component(service = McpTool.class)
public class AddToCartTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();
    private final ConfigurableOptionResolver optionResolver = new ConfigurableOptionResolver();

    @Override
    public String name() {
        return "add_to_cart";
    }

    @Override
    public String description() {
        return "Add a product to a guest cart, creating the cart first if no cart_id is supplied. For configurable "
            + "products (e.g. size/color variants), supply an 'options' object such as {\"color\": \"Blue\"}.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sku").put("type", "string");
        properties.putObject("quantity").put("type", "integer");
        properties.putObject("cart_id").put("type", "string");
        properties.putObject("options").put("type", "object");
        schema.putArray("required").add("sku").add("quantity");
        return schema;
    }

    protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
        McpProductRetriever retriever = new McpProductRetriever(ctx.getClient());
        retriever.setIdentifier(sku);
        retriever.extendProductQueryWith(q -> q.onConfigurableProduct(cp -> cp.configurableOptions(co -> co
            .attributeCode()
            .label()
            .values(v -> v.label().uid()))));
        return retriever.fetchProduct();
    }

    protected String createEmptyCart(StoreContext ctx) {
        return mutationClient.execute(ctx, m -> m.createEmptyCart()).getCreateEmptyCart();
    }

    protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
        CartItemInput cartItem = new CartItemInput(quantity, sku);
        if (selectedOptions != null && !selectedOptions.isEmpty()) {
            cartItem.setSelectedOptions(selectedOptions);
        }
        AddProductsToCartOutput output = mutationClient
            .execute(ctx, m -> m.addProductsToCart(cartId, Collections.singletonList(cartItem), out -> out
                .cart(c -> c
                    .id()
                    .totalQuantity()
                    .items(i -> i
                        .uid()
                        .quantity()
                        .product(p -> p.sku().name())
                        .prices(pr -> pr.price(mo -> mo.value().currency()).rowTotal(mo -> mo.value().currency())))
                    .prices(cp -> cp.grandTotal(mo -> mo.value().currency())))
                .userErrors(e -> e.code().message())))
            .getAddProductsToCart();

        if (output.getUserErrors() != null && !output.getUserErrors().isEmpty()) {
            throw new IllegalArgumentException(
                output.getUserErrors().stream().map(CartUserInputError::getMessage).collect(Collectors.joining("; ")));
        }
        return output.getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String sku = args.path("sku").asText(null);
        JsonNode quantityNode = args.get("quantity");
        if (sku == null || quantityNode == null || quantityNode.asInt() < 1) {
            throw new IllegalArgumentException("sku and a positive quantity are required");
        }

        Map<String, String> suppliedOptions = new HashMap<>();
        JsonNode optionsNode = args.get("options");
        if (optionsNode != null && optionsNode.isObject()) {
            optionsNode.fields().forEachRemaining(entry -> suppliedOptions.put(entry.getKey(), entry.getValue().asText()));
        }

        ProductInterface product = fetchProduct(ctx, sku);
        List<ID> selectedOptions = optionResolver.resolve(product, suppliedOptions);

        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            cartId = createEmptyCart(ctx);
        }
        Cart cart = addItem(ctx, cartId, sku, quantityNode.asInt(), selectedOptions);
        return DtoMapper.cart(mapper, cart);
    }
}
