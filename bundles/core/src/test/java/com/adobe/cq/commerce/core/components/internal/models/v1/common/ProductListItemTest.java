/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;

public class ProductListItemTest {

    private Page productPage;

    @Before
    public void setUp() {
        Resource contentResource = Mockito.mock(Resource.class);
        productPage = Mockito.mock(Page.class);
        Mockito.when(productPage.getContentResource()).thenReturn(contentResource);
    }

    @Test
    public void testCreateProductListItem() {
        String sku = "expected";
        String urlKey = "expectedUrlKey";

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
        String sku = "product-sku";
        String name = "product-name";
        String urlKey = "product-url_key";
        String imageUrl = "http://www.image.com/image.jpg";
        String imageAlt = "Some image";

        ProductListItem productListItem = new ProductListItemImpl(sku, urlKey, name, null, imageUrl, imageAlt, productPage, null, null,
            null, "1", false);

        Assert.assertEquals(sku, productListItem.getSKU());
        Assert.assertEquals(urlKey, productListItem.getSlug());
        Assert.assertEquals(imageUrl, productListItem.getImageURL());
        Assert.assertEquals(imageAlt, productListItem.getImageAlt());
        Assert.assertEquals(StringUtils.EMPTY, productListItem.getURL());
    }

    @Test
    public void testGetComponentData() {
        CommerceIdentifier identifier = new CommerceIdentifierImpl("none", CommerceIdentifier.IdentifierType.URL_KEY,
            CommerceIdentifier.EntityType.PRODUCT);
        TestInheritedItemImpl productListItem = new TestInheritedItemImpl(identifier, "", productPage);

        Assert.assertNotNull("Component data retrieved successfully", productListItem.getSomeData());
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
