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

    @Test
    public void testCreateProductListItem() {
        CommerceIdentifier identifier = new CommerceIdentifierImpl(urlKey, CommerceIdentifier.IdentifierType.URL_KEY,
            CommerceIdentifier.EntityType.PRODUCT);
        ProductListItem productListItem = new ProductListItemImpl(identifier, "", productPage);

        Assert.assertEquals(urlKey, productListItem.getSlug());

        identifier = new CommerceIdentifierImpl(sku, CommerceIdentifier.IdentifierType.SKU, CommerceIdentifier.EntityType.PRODUCT);
        productListItem = new ProductListItemImpl(identifier, "", productPage);
        Assert.assertEquals(sku, productListItem.getSKU());
    }

    @Test
    public void testCreateProductListItem2() {
        ProductListItem productListItem = new ProductListItemImpl(sku, urlKey, name, null, imageUrl, imageAlt, productPage, null, null,
            null, "1", false);

        Assert.assertEquals(name, productListItem.getTitle());
        Assert.assertEquals(sku, productListItem.getSKU());
        Assert.assertEquals(urlKey, productListItem.getSlug());
        Assert.assertEquals(imageUrl, productListItem.getImageURL());
        Assert.assertEquals(imageAlt, productListItem.getImageAlt());
        Assert.assertEquals(StringUtils.EMPTY, productListItem.getURL());
    }

    @Test
    public void testCreateProductListItem3() {
        ProductListItem productListItem = new ProductListItemImpl(product, productPage, null, null,
            null, "1");

        Assert.assertEquals(product.getSku(), productListItem.getSKU());
        Assert.assertEquals(product.getName(), productListItem.getTitle());
        Assert.assertEquals(product.getUrlKey(), productListItem.getSlug());
        Assert.assertEquals(imageUrl, productListItem.getImageURL());
        Assert.assertEquals(imageAlt, productListItem.getImageAlt());
        Assert.assertTrue(productListItem.getId().indexOf("1") == 0);
        Assert.assertEquals(product, productListItem.getProduct());
    }

    @Test
    public void testGetComponentData() {
        CommerceIdentifier identifier = new CommerceIdentifierImpl("none", CommerceIdentifier.IdentifierType.URL_KEY,
            CommerceIdentifier.EntityType.PRODUCT);
        TestInheritedItemImpl productListItem = new TestInheritedItemImpl(identifier, "", productPage);

        Assert.assertNotNull("Component data retrieved successfully", productListItem.getSomeData());
    }

    @Test
    public void testGenerateIdForEmptySku() {
        // test for null empty or blank
        String[] skus = new String[] { null, "", "    " };
        String expected = "foobar-item-e3b0c44298";

        for (String sku : skus) {
            // test all constructors
            CommerceIdentifier commerceIdentifier = mock(CommerceIdentifier.class);
            when(commerceIdentifier.getType()).thenReturn(CommerceIdentifier.IdentifierType.SKU);
            when(commerceIdentifier.getValue()).thenReturn(sku);
            ProductListItemImpl item = new ProductListItemImpl(commerceIdentifier, "foobar", productPage);
            assertEquals(expected, item.getId());

            item = new ProductListItemImpl(sku, null, null, null, null, null, productPage, null, null, null, "foobar", false);
            assertEquals(expected, item.getId());

            when(product.getSku()).thenReturn(sku);
            item = new ProductListItemImpl(product, productPage, null, null, null, "foobar");
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
            ProductListItem item = new ProductListItemImpl(baseSku, null, null, null, null, null, productPage, variant, null, null,
                "foobar", false);
            assertEquals(expected, item.getId());

            when(product.getSku()).thenReturn(baseSku);
            item = new ProductListItemImpl(product, productPage, variant, null, null, "foobar");
            assertEquals(expected, item.getId());
        }
    }

    private class TestInheritedItemImpl extends ProductListItemImpl {

        public TestInheritedItemImpl(CommerceIdentifier identifier, String parentId, Page productPage) {
            super(identifier, parentId, productPage);
        }

        public ComponentData getSomeData() {
            return getComponentData();
        }
    }

}
