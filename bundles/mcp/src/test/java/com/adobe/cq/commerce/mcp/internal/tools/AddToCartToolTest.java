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
import java.util.List;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.BundleItem;
import com.adobe.cq.commerce.magento.graphql.BundleItemOption;
import com.adobe.cq.commerce.magento.graphql.BundleProduct;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddToCartToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void createsCartWhenNoCartIdSupplied() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("new-cart"));
        ProductInterface simpleProduct = mock(ProductInterface.class);

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return simpleProduct;
            }

            @Override
            protected String createEmptyCart(StoreContext ctx) {
                return "new-cart";
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                assertEquals("new-cart", cartId);
                assertEquals("VSK05", sku);
                assertEquals(1.0, quantity, 0.001);
                assertEquals(Collections.emptyList(), selectedOptions);
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
        ProductInterface simpleProduct = mock(ProductInterface.class);

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return simpleProduct;
            }

            @Override
            protected String createEmptyCart(StoreContext ctx) {
                throw new AssertionError("should not create a new cart when cart_id is supplied");
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                assertEquals("existing-cart", cartId);
                return cart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class),
            mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1).put("cart_id", "existing-cart"));
        assertEquals("existing-cart", out.get("cart_id").asText());
    }

    @Test
    public void resolvesConfigurableOptionsBeforeAddingItem() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        when(blue.getUid()).thenReturn(new ID("value-blue"));
        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));
        ConfigurableProduct configurableProduct = mock(ConfigurableProduct.class);
        when(configurableProduct.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return configurableProduct;
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                assertEquals(Collections.singletonList(new ID("value-blue")), selectedOptions);
                return cart;
            }
        };

        com.fasterxml.jackson.databind.node.ObjectNode args = mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1)
            .put("cart_id", "cart-1");
        args.putObject("options").put("fashion_color", "Blue");
        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
    }

    @Test
    public void throwsDescriptiveErrorWhenConfigurableOptionMissing() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));
        ConfigurableProduct configurableProduct = mock(ConfigurableProduct.class);
        when(configurableProduct.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return configurableProduct;
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                throw new AssertionError("should not add to cart when a required option is missing");
            }
        };

        JsonNode args = mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1).put("cart_id", "cart-1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tool.call(mock(StoreContext.class), args));
        assertEquals("Color is required. Available values: Blue", ex.getMessage());
    }

    @Test
    public void treatsJsonNullOptionValueAsMissingNotAsTheStringNull() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));
        ConfigurableProduct configurableProduct = mock(ConfigurableProduct.class);
        when(configurableProduct.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return configurableProduct;
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                throw new AssertionError("should not add to cart when a required option is null");
            }
        };

        com.fasterxml.jackson.databind.node.ObjectNode args = mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1)
            .put("cart_id", "cart-1");
        args.putObject("options").putNull("fashion_color");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tool.call(mock(StoreContext.class), args));
        assertEquals("Color is required. Available values: Blue", ex.getMessage());
    }

    @Test
    public void resolvesBundleOptionsBeforeAddingItem() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        BundleItemOption carmina = mock(BundleItemOption.class);
        when(carmina.getLabel()).thenReturn("Carmina Necklace");
        when(carmina.getUid()).thenReturn(new ID("bundle/2/2/1"));
        BundleItem necklace = mock(BundleItem.class);
        when(necklace.getTitle()).thenReturn("Necklace");
        when(necklace.getRequired()).thenReturn(true);
        when(necklace.getOptions()).thenReturn(Collections.singletonList(carmina));
        BundleProduct bundleProduct = mock(BundleProduct.class);
        when(bundleProduct.getItems()).thenReturn(Collections.singletonList(necklace));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return bundleProduct;
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                assertEquals(Collections.singletonList(new ID("bundle/2/2/1")), selectedOptions);
                return cart;
            }
        };

        com.fasterxml.jackson.databind.node.ObjectNode args = mapper.createObjectNode().put("sku", "VA24").put("quantity", 1)
            .put("cart_id", "cart-1");
        args.putObject("bundle_options").put("Necklace", "Carmina Necklace");
        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
    }

    @Test
    public void throwsDescriptiveErrorWhenRequiredBundleOptionMissing() {
        BundleItemOption carmina = mock(BundleItemOption.class);
        when(carmina.getLabel()).thenReturn("Carmina Necklace");
        BundleItem necklace = mock(BundleItem.class);
        when(necklace.getTitle()).thenReturn("Necklace");
        when(necklace.getRequired()).thenReturn(true);
        when(necklace.getOptions()).thenReturn(Collections.singletonList(carmina));
        BundleProduct bundleProduct = mock(BundleProduct.class);
        when(bundleProduct.getItems()).thenReturn(Collections.singletonList(necklace));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return bundleProduct;
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                throw new AssertionError("should not add to cart when a required bundle option is missing");
            }
        };

        JsonNode args = mapper.createObjectNode().put("sku", "VA24").put("quantity", 1).put("cart_id", "cart-1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tool.call(mock(StoreContext.class), args));
        assertEquals("Necklace is required. Available values: Carmina Necklace", ex.getMessage());
    }

    @Test
    public void rejectsFractionalQuantityInsteadOfSilentlyTruncating() {
        ProductInterface simpleProduct = mock(ProductInterface.class);

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return simpleProduct;
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                throw new AssertionError("1.9 must not silently truncate to 1 and proceed");
            }
        };

        JsonNode args = mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1.9);
        assertThrows(IllegalArgumentException.class, () -> tool.call(mock(StoreContext.class), args));
    }
}
