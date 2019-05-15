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

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductTeaserImplTest {

    ProductTeaserImpl slingModel;
    private static final String TEST_PRODUCT_PAGE_URL = "/content/test-product-page";
    private ProductInterface queryResultProduct;

    @Before
    public void setup() throws Exception {
        this.slingModel = new ProductTeaserImpl();
        String json = IOUtils.toString(this.getClass()
                .getResourceAsStream("/graphql/magento-graphql-product-result.json"), StandardCharsets.UTF_8);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        List<ProductInterface> products = rootQuery.getProducts().getItems();
        this.queryResultProduct = products.get(0);
        Whitebox.setInternalState(this.slingModel, "product", products.get(0));
        Page productPage = mock(Page.class);
        when(productPage.getLanguage(false)).thenReturn(Locale.US);
        when(productPage.getPath()).thenReturn("/content/test-product-page");
        Whitebox.setInternalState(this.slingModel, "productPage", productPage);
        Whitebox.setInternalState(this.slingModel, "priceFormatter", NumberFormat.getCurrencyInstance(Locale.US));
    }

    @Test
    public void verifyProduct() {
        Assert.assertNotNull(queryResultProduct);
        Assert.assertEquals(queryResultProduct.getName(), slingModel.getName());
        Assert.assertEquals(TEST_PRODUCT_PAGE_URL + "." + queryResultProduct.getUrlKey() + ".html", slingModel.getUrl());
        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        priceFormatter.setCurrency(Currency.getInstance(queryResultProduct.getPrice().getRegularPrice().getAmount().getCurrency().toString()));
        Assert.assertEquals(priceFormatter.format(queryResultProduct.getPrice().getRegularPrice().getAmount().getValue()),
                slingModel.getFormattedPrice());
        Assert.assertNotNull(slingModel.getImage());
        Assert.assertTrue(StringUtils.endsWith( slingModel.getImage(),queryResultProduct.getSwatchImage()));
    }
}
