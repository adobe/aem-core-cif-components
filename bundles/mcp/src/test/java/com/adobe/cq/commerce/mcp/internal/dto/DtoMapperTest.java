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
package com.adobe.cq.commerce.mcp.internal.dto;

import java.util.Collections;

import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemInterface;
import com.adobe.cq.commerce.magento.graphql.CartItemPrices;
import com.adobe.cq.commerce.magento.graphql.CartPrices;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DtoMapperTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void mapsProductListItem() {
        Price price = mock(Price.class);
        when(price.getFinalPrice()).thenReturn(19.99);
        when(price.getCurrency()).thenReturn("USD");
        ProductListItem item = mock(ProductListItem.class);
        when(item.getSKU()).thenReturn("VT01");
        when(item.getTitle()).thenReturn("Valeria Two-Layer Tank");
        when(item.getSlug()).thenReturn("valeria-tank");
        when(item.getURL()).thenReturn("/content/venia/us/en/products/valeria-tank.html");
        when(item.getImageURL()).thenReturn("http://x/img.jpg");
        when(item.getImageAlt()).thenReturn("tank");
        when(item.getPriceRange()).thenReturn(price);

        ObjectNode dto = DtoMapper.product(mapper, item);
        assertEquals("VT01", dto.get("sku").asText());
        assertEquals("Valeria Two-Layer Tank", dto.get("name").asText());
        assertEquals("valeria-tank", dto.get("slug").asText());
        assertEquals("/content/venia/us/en/products/valeria-tank.html", dto.get("url").asText());
        assertEquals("http://x/img.jpg", dto.get("imageUrl").asText());
        assertEquals("tank", dto.get("imageAlt").asText());
        assertEquals(19.99, dto.get("price").asDouble(), 0.001);
        assertEquals("USD", dto.get("currency").asText());
    }

    @Test
    public void mapsProductListItemWithNullPrice() {
        ProductListItem item = mock(ProductListItem.class);
        when(item.getSKU()).thenReturn("VT01");
        when(item.getTitle()).thenReturn("Valeria Two-Layer Tank");
        when(item.getSlug()).thenReturn("valeria-tank");
        when(item.getImageURL()).thenReturn("http://x/img.jpg");
        when(item.getImageAlt()).thenReturn("tank");
        when(item.getPriceRange()).thenReturn(null);

        ObjectNode dto = DtoMapper.product(mapper, item);
        assertEquals("VT01", dto.get("sku").asText());
        assertFalse(dto.has("price"));
        assertFalse(dto.has("currency"));
    }

    @Test
    public void mapsCategoryWithoutChildren() {
        CategoryTree category = mock(CategoryTree.class);
        when(category.getUid()).thenReturn(new ID("cat-1"));
        when(category.getName()).thenReturn("Tops");
        when(category.getUrlPath()).thenReturn("tops");

        ObjectNode dto = DtoMapper.category(mapper, category, false, c -> "/plp/" + c.getUrlPath());
        assertEquals("cat-1", dto.get("uid").asText());
        assertEquals("Tops", dto.get("name").asText());
        assertEquals("tops", dto.get("urlPath").asText());
        assertEquals("/plp/tops", dto.get("url").asText());
        assertFalse(dto.has("children"));
    }

    @Test
    public void mapsCategoryWithChildren() {
        CategoryTree child = mock(CategoryTree.class);
        when(child.getUid()).thenReturn(new ID("cat-2"));
        when(child.getName()).thenReturn("Blouses");
        when(child.getUrlPath()).thenReturn("tops/blouses");

        CategoryTree parent = mock(CategoryTree.class);
        when(parent.getUid()).thenReturn(new ID("cat-1"));
        when(parent.getName()).thenReturn("Tops");
        when(parent.getUrlPath()).thenReturn("tops");
        when(parent.getChildren()).thenReturn(Collections.singletonList(child));

        ObjectNode dto = DtoMapper.category(mapper, parent, true, c -> "/plp/" + c.getUrlPath());
        assertEquals("/plp/tops", dto.get("url").asText());
        assertTrue(dto.has("children"));
        ArrayNode children = (ArrayNode) dto.get("children");
        assertEquals(1, children.size());
        assertEquals("cat-2", children.get(0).get("uid").asText());
        assertEquals("Blouses", children.get(0).get("name").asText());
        assertEquals("/plp/tops/blouses", children.get(0).get("url").asText());
        assertFalse(children.get(0).has("children"));
    }

    @Test
    public void mapsCategoryWithNullUid() {
        CategoryTree category = mock(CategoryTree.class);
        when(category.getUid()).thenReturn(null);
        when(category.getName()).thenReturn("Tops");
        when(category.getUrlPath()).thenReturn("tops");

        ObjectNode dto = DtoMapper.category(mapper, category, false, null);
        assertTrue(dto.get("uid").isNull());
        assertFalse(dto.has("url"));
    }

    @Test
    public void mapsCart() {
        ProductInterface product = mock(ProductInterface.class);
        when(product.getSku()).thenReturn("VSK05");
        when(product.getName()).thenReturn("Agatha Skirt");

        Money price = mock(Money.class);
        when(price.getValue()).thenReturn(78.0);
        when(price.getCurrency()).thenReturn(CurrencyEnum.USD);

        CartItemPrices itemPrices = mock(CartItemPrices.class);
        when(itemPrices.getPrice()).thenReturn(price);
        when(itemPrices.getRowTotal()).thenReturn(price);

        CartItemInterface item = mock(CartItemInterface.class);
        when(item.getUid()).thenReturn(new ID("item-1"));
        when(item.getProduct()).thenReturn(product);
        when(item.getQuantity()).thenReturn(2.0);
        when(item.getPrices()).thenReturn(itemPrices);

        CartPrices cartPrices = mock(CartPrices.class);
        when(cartPrices.getGrandTotal()).thenReturn(price);

        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));
        when(cart.getItems()).thenReturn(Collections.singletonList(item));
        when(cart.getPrices()).thenReturn(cartPrices);
        when(cart.getTotalQuantity()).thenReturn(2.0);

        ObjectNode dto = DtoMapper.cart(mapper, cart);
        assertEquals("cart-1", dto.get("cart_id").asText());
        assertEquals(1, dto.get("items").size());
        JsonNode itemNode = dto.get("items").get(0);
        assertEquals("item-1", itemNode.get("uid").asText());
        assertEquals("VSK05", itemNode.get("sku").asText());
        assertEquals("Agatha Skirt", itemNode.get("name").asText());
        assertEquals(2.0, itemNode.get("quantity").asDouble(), 0.001);
        assertEquals(78.0, itemNode.get("price").asDouble(), 0.001);
        assertEquals("USD", itemNode.get("currency").asText());
        assertEquals(78.0, itemNode.get("rowTotal").asDouble(), 0.001);
        assertEquals(78.0, dto.get("grandTotal").asDouble(), 0.001);
        assertEquals("USD", dto.get("currency").asText());
        assertEquals(2.0, dto.get("totalQuantity").asDouble(), 0.001);
    }
}
