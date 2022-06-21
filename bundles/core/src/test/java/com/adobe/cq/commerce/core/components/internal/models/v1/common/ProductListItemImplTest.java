/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.PriceRange;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductListItemImplTest {

    final static String sku = "product-sku";
    final static String name = "product-name";
    final static String urlKey = "product-url_key";
    final static String imageUrl = "http://www.image.com/image.jpg";
    final static String imageAlt = "Some image";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PriceRange priceRange;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Money money;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProductImage image;
    @Rule
    public final AemContext aemContext = new AemContext();

    private Page productPage;
    private ProductInterface product;

    @Before
    public void setUp() {
        productPage = aemContext.create().page("/my/page");

        // setup test product
        product = mock(ProductInterface.class);
        when(product.getSku()).thenReturn(sku);
        when(product.getName()).thenReturn(name);
        when(product.getUrlKey()).thenReturn(urlKey);
        when(image.getLabel()).thenReturn(imageAlt);
        when(image.getUrl()).thenReturn(imageUrl);
        when(product.getSmallImage()).thenReturn(image);
        when(money.getCurrency()).thenReturn(CurrencyEnum.USD);
        when(money.getValue()).thenReturn(12.34);
        when(priceRange.getMinimumPrice().getFinalPrice()).thenReturn(money);
        when(priceRange.getMinimumPrice().getRegularPrice()).thenReturn(money);
        when(product.getPriceRange()).thenReturn(priceRange);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateEmptyProductListItem() {
        assertNotNull(new ProductListItemImpl.Builder(null, null, null, null).build());
    }

    @Test
    public void testCreateProductListItemFromProduct() {
        ProductListItem productListItem = new ProductListItemImpl.Builder("1", productPage, null, null)
            .product(product)
            .build();

        Assert.assertEquals(product.getSku(), productListItem.getSKU());
        Assert.assertEquals(product.getSku(), productListItem.getCombinedSku().getBaseSku());
        Assert.assertEquals(product.getSku(), productListItem.getCommerceIdentifier().getValue());
        Assert.assertEquals(CommerceIdentifier.EntityType.PRODUCT, productListItem.getCommerceIdentifier().getEntityType());
        Assert.assertEquals(CommerceIdentifier.IdentifierType.SKU, productListItem.getCommerceIdentifier().getType());
        Assert.assertEquals(product.getName(), productListItem.getTitle());
        Assert.assertEquals(product.getUrlKey(), productListItem.getSlug());
        Assert.assertEquals(imageUrl, productListItem.getImageURL());
        Assert.assertEquals(imageAlt, productListItem.getImageAlt());
        Assert.assertTrue(StringUtils.startsWith(productListItem.getId(), "1"));
        Assert.assertEquals(product, productListItem.getProduct());
    }

    @Test
    public void testCreateProductListItemFromProductWithOverwrites() {
        ProductImage anotherImage = mock(ProductImage.class);
        when(anotherImage.getUrl()).thenReturn("http://foo.bar/another-image.jpg");
        ProductListItem productListItem = new ProductListItemImpl.Builder("1", productPage, null, null)
            .product(product)
            .sku("my-sku")
            .urlKey("my-url-key")
            .variantSku("my-variant-sku")
            .image(anotherImage)
            .name("my-name")
            .build();

        Assert.assertEquals("my-sku", productListItem.getSKU());
        Assert.assertEquals("my-sku", productListItem.getCombinedSku().getBaseSku());
        Assert.assertEquals("my-variant-sku", productListItem.getCombinedSku().getVariantSku());
        Assert.assertEquals("my-variant-sku", productListItem.getCommerceIdentifier().getValue());
        Assert.assertEquals(CommerceIdentifier.EntityType.PRODUCT, productListItem.getCommerceIdentifier().getEntityType());
        Assert.assertEquals(CommerceIdentifier.IdentifierType.SKU, productListItem.getCommerceIdentifier().getType());
        Assert.assertEquals("my-name", productListItem.getTitle());
        Assert.assertEquals("my-url-key", productListItem.getSlug());
        Assert.assertEquals("http://foo.bar/another-image.jpg", productListItem.getImageURL());
        Assert.assertEquals("my-name", productListItem.getImageAlt());
        Assert.assertTrue(StringUtils.startsWith(productListItem.getId(), "1"));
        Assert.assertEquals(product, productListItem.getProduct());
    }

    @Test
    public void testCreateProductListItem() {
        CommerceIdentifier identifier = new CommerceIdentifierImpl(urlKey, CommerceIdentifier.IdentifierType.URL_KEY,
            CommerceIdentifier.EntityType.PRODUCT);
        ProductListItem productListItem = new ProductListItemImpl(sku, identifier, "", productPage);

        Assert.assertEquals(urlKey, productListItem.getSlug());

        identifier = new CommerceIdentifierImpl(sku, CommerceIdentifier.IdentifierType.SKU, CommerceIdentifier.EntityType.PRODUCT);
        productListItem = new ProductListItemImpl(sku, identifier, "", productPage);
        Assert.assertEquals(sku, productListItem.getSKU());
    }

    @Test
    public void testGetComponentData() {
        CommerceIdentifier identifier = new CommerceIdentifierImpl("none", CommerceIdentifier.IdentifierType.URL_KEY,
            CommerceIdentifier.EntityType.PRODUCT);
        TestInheritedItemImpl productListItem = new TestInheritedItemImpl(sku, identifier, "", productPage);

        Assert.assertNotNull("Component data retrieved successfully", productListItem.getSomeData());
    }

    @Test
    public void testGenerateIdForEmptySku() {
        // test for null empty or blank
        String[] skus = new String[] { null, "", "    " };
        String expected = "foobar-item-e3b0c44298";

        for (String sku : skus) {
            // test all constructors
            CommerceIdentifier commerceIdentifier = new CommerceIdentifierImpl(sku, CommerceIdentifier.IdentifierType.SKU,
                CommerceIdentifier.EntityType.PRODUCT);
            ProductListItem item = new ProductListItemImpl(sku, commerceIdentifier, "foobar", productPage);
            assertEquals(expected, item.getId());

            item = new ProductListItemImpl.Builder("foobar", productPage, null, null)
                .sku(sku)
                .build();
            assertEquals(expected, item.getId());
        }
    }

    @Test
    public void testGenerateIdForVariantSku() {
        // test for null empty or blank
        String baseSku = "foobar";
        Map<String, String> expectations = new HashMap<>();
        expectations.put(null, "foobar-item-c3ab8ff137");
        expectations.put("", "foobar-item-c3ab8ff137");
        expectations.put("variant", "foobar-item-5643622b76");

        for (Map.Entry<String, String> sku : expectations.entrySet()) {
            String variant = sku.getKey();
            String expected = sku.getValue();
            // test all constructors that support variant sku
            ProductListItem item = new ProductListItemImpl.Builder("foobar", productPage, null, null)
                .sku(baseSku)
                .variantSku(variant)
                .build();
            assertEquals(expected, item.getId());
        }
    }

    private class TestInheritedItemImpl extends ProductListItemImpl {

        public TestInheritedItemImpl(String sku, CommerceIdentifier identifier, String parentId, Page productPage) {
            super(sku, identifier, parentId, productPage);
        }

        public ComponentData getSomeData() {
            return getComponentData();
        }
    }

}
