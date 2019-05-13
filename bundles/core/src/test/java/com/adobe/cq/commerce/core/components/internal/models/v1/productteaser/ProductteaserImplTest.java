/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
package com.adobe.cq.commerce.core.components.internal.models.v1.productteaser;

import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.service.ProductGraphqlService;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductteaserImplTest {

    ProductteaserImpl slingModel;

    private static final String TEST_URL="/testUrl/a/b";
    private static final String TEST_SLUG="dummyslug";
    private static final String TEST_PRODUCTNAME="TESTPRODUCT";
    private static final String TEST_PRODUCTPRICE="$400.00";


    @Before
    public void setup() {
        this.slingModel = new ProductteaserImpl();
        ProductInterface gqlProduct = mock(ProductInterface.class);
        Page ProductPage = mock(Page.class);

        when(gqlProduct.getName()).thenReturn(TEST_PRODUCTNAME);
        when(ProductPage.getPath()).thenReturn(TEST_URL);
        when(gqlProduct.getUrlKey()).thenReturn(TEST_SLUG);

        //Data for testing getFormattedPrice
        ProductPrices prices = new ProductPrices();
        Price price = new Price();
        Money money = new Money();
        money.setValue(400.00);
        prices.setRegularPrice(price);
        price.setAmount(money);
        when(gqlProduct.getPrice()).thenReturn(prices);

        //Data for testing getting product Assets
        List<Asset> listAssets = new ArrayList<Asset>();
        Asset assetone = mock(Asset.class);
        listAssets.add(assetone);
        MediaGalleryEntry assetA = mock(MediaGalleryEntry.class);
        List<MediaGalleryEntry> listM = new ArrayList<MediaGalleryEntry>();
        listM.add(assetA);
        when(gqlProduct.getMediaGalleryEntries()).thenReturn(listM);
        ProductGraphqlService gqlservice = mock(ProductGraphqlService.class);
        when(gqlservice.filterAndSortAssets(gqlProduct.getMediaGalleryEntries())).thenReturn(listAssets);

        //Setup internal instances

        Whitebox.setInternalState(this.slingModel, "productGraphqlService", gqlservice);
        Whitebox.setInternalState(this.slingModel, "priceFormatter", NumberFormat.getCurrencyInstance(Locale.US));
        Whitebox.setInternalState(this.slingModel, "productPage", ProductPage);
        Whitebox.setInternalState(this.slingModel, "product", gqlProduct);
    }

    @Test
    public void TestName() {
        Assert.assertEquals(TEST_PRODUCTNAME, slingModel.getName());
    }

    @Test
    public void testGetFormattedPrice() {
        Assert.assertEquals(TEST_PRODUCTPRICE, slingModel.getFormattedPrice());
    }

    @Test
    public void testGetUrl() {
        Assert.assertEquals(TEST_URL+"."+TEST_SLUG+".html", slingModel.getUrl());
    }

    @Test
    public void testImage() {
        List<Asset> assetslist = slingModel.getAssets();
        Assert.assertNotNull(assetslist);
        Assert.assertEquals(1, assetslist.size());
    }

}
