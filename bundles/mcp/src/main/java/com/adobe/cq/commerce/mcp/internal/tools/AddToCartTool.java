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
 * configurable products via an optional {@code options} argument (e.g. {@code {"color": "Blue"}}), resolved by
 * {@link ConfigurableOptionResolver}, and bundle products via {@code bundle_options} (e.g.
 * {@code {"Necklace": "Carmina Necklace"}}), resolved by {@link BundleOptionResolver}. Both resolve to
 * {@code CartItemInput.selectedOptions} UIDs and go through the same {@code addProductsToCart} mutation as simple
 * products &mdash; verified live that a bundle choice's own {@code uid} works exactly like a configurable
 * option-value UID, so no separate {@code addBundleProductsToCart} call is needed.
 */
@Component(service = McpTool.class)
public class AddToCartTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();
    private final ConfigurableOptionResolver optionResolver = new ConfigurableOptionResolver();
    private final BundleOptionResolver bundleOptionResolver = new BundleOptionResolver();

    @Override
    public String name() {
        return "add_to_cart";
    }

    @Override
    public String description() {
        return "Add a product to a guest cart, creating the cart first if no cart_id is supplied. For configurable "
            + "products (e.g. size/color variants), supply an 'options' object such as {\"color\": \"Blue\"}. For "
            + "bundle products, supply a 'bundle_options' object keyed by each bundle item's title, such as "
            + "{\"Necklace\": \"Carmina Necklace\"}.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sku").put("type", "string");
        properties.putObject("quantity").put("type", "integer");
        properties.putObject("cart_id").put("type", "string");
        properties.putObject("options").put("type", "object");
        properties.putObject("bundle_options").put("type", "object");
        schema.putArray("required").add("sku").add("quantity");
        return schema;
    }

    protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
        McpProductRetriever retriever = new McpProductRetriever(ctx.getClient());
        retriever.setIdentifier(sku);
        retriever.extendProductQueryWith(q -> q
            .onConfigurableProduct(cp -> cp.configurableOptions(co -> co
                .attributeCode()
                .label()
                .values(v -> v.label().uid())))
            .onBundleProduct(bp -> bp.items(i -> i
                .title()
                .required()
                .options(o -> o.label().uid()))));
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
                .cart(CartMutationClient.cartFields())
                .userErrors(e -> e.code().message())))
            .getAddProductsToCart();

        if (output.getUserErrors() != null && !output.getUserErrors().isEmpty()) {
            throw new IllegalArgumentException(
                output.getUserErrors().stream().map(CartUserInputError::getMessage).collect(Collectors.joining("; ")));
        }
        return output.getCart();
    }

    private static Map<String, String> toStringMap(JsonNode objectNode) {
        Map<String, String> result = new HashMap<>();
        if (objectNode != null && objectNode.isObject()) {
            objectNode.fields().forEachRemaining(entry -> {
                if (!entry.getValue().isNull()) {
                    result.put(entry.getKey(), entry.getValue().asText());
                }
            });
        }
        return result;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String sku = args.path("sku").asText(null);
        JsonNode quantityNode = args.get("quantity");
        if (sku == null || quantityNode == null || !quantityNode.isIntegralNumber() || quantityNode.asInt() < 1) {
            throw new IllegalArgumentException("sku and a positive whole-number quantity are required");
        }

        ProductInterface product = fetchProduct(ctx, sku);
        List<ID> selectedOptions = new ArrayList<>();
        selectedOptions.addAll(optionResolver.resolve(product, toStringMap(args.get("options"))));
        selectedOptions.addAll(bundleOptionResolver.resolve(product, toStringMap(args.get("bundle_options"))));

        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            cartId = createEmptyCart(ctx);
        }
        Cart cart = addItem(ctx, cartId, sku, quantityNode.asInt(), selectedOptions);
        return DtoMapper.cart(mapper, cart);
    }
}
