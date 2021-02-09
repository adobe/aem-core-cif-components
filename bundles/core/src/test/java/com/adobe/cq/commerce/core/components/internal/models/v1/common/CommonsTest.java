/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import org.apache.sling.api.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.day.cq.wcm.api.Page;

public class CommonsTest {

    private Page productPage;

    @Before
    public void setup() {
        productPage = Mockito.mock(Page.class);
        Resource contentResource = Mockito.mock(Resource.class);

        Mockito.when(productPage.getContentResource()).thenReturn(contentResource);
    }

    @Test
    public void testCommerceIdentifierCreateProduct() {
        String sku = "expected";

        CommerceIdentifier identifier = CommerceIdentifierImpl.fromProductSku(sku);

        Assert.assertEquals("The sku is the expected one", sku, identifier.getValue());
        Assert.assertEquals("The identifier type is SKU", CommerceIdentifier.IdentifierType.SKU, identifier.getType());
        Assert.assertEquals("The entity type is Product", CommerceIdentifier.EntityType.PRODUCT, identifier.getEntityType());
    }

    @Test
    public void testCreateProductListItem() {
        String sku="expected";
        String urlKey = "expectedUrlKey";

        CommerceIdentifier identifier = new CommerceIdentifierImpl(urlKey, CommerceIdentifier.IdentifierType.URL_KEY, CommerceIdentifier.EntityType.PRODUCT);
        ProductListItem productListItem = new ProductListItemImpl(identifier,"", productPage);

        Assert.assertEquals(urlKey,productListItem.getSlug());

        identifier = new CommerceIdentifierImpl(sku, CommerceIdentifier.IdentifierType.SKU, CommerceIdentifier.EntityType.PRODUCT);
        productListItem = new ProductListItemImpl(identifier,"", productPage);
        Assert.assertEquals(sku,productListItem.getSKU());
    }
}
