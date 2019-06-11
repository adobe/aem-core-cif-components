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

package com.adobe.cq.commerce.core.components.internal.models.v1.productlist;

import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductListImplTest {

    Page productPage;

    SlingHttpServletRequest incomingRequest;

    private ProductList slingModel;
    private CategoryInterface categoryQueryResult;

    @Before
    public void setUp() throws Exception {
        this.slingModel = new ProductListImpl();

        // ProductList entry items
        String json = IOUtils.toString(this.getClass()
                .getResourceAsStream("/graphql/magento-graphql-category-result.json"), StandardCharsets.UTF_8);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        categoryQueryResult = rootQuery.getCategory();
        Whitebox.setInternalState(this.slingModel, "category", categoryQueryResult);

        // AEM page
        productPage = mock(Page.class);
        when(productPage.getLanguage(false)).thenReturn(Locale.US);
        when(productPage.getPath()).thenReturn("/content/test-product-page");
        Map<String, Object> pageProperties = new HashMap<>();

        pageProperties.put(ProductList.PN_PAGE_SIZE, 6); //setting page size to 6

        ValueMapDecorator vMD = new ValueMapDecorator(pageProperties);

        when(productPage.getProperties()).thenReturn(vMD);

        Whitebox.setInternalState(this.slingModel, "productPage", productPage);
    }

    @Test
    public void getTitle() {
        String title = this.slingModel.getTitle();
        Assert.assertEquals(categoryQueryResult.getName(), title);
    }

    @Test
    public void getProducts() {
        Collection<ProductListItem> products = this.slingModel.getProducts();
        Assert.assertNotNull(products);
        Assert.assertEquals(true, categoryQueryResult.getProducts().getItems().size() <= products.size());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        List<ProductListItem> results = products.stream().collect(Collectors.toList());
        for (int i = 0; i < results.size(); i++) {
            // get raw GraphQL object
            ProductInterface productInterface = categoryQueryResult.getProducts().getItems().get(i);
            // get mapped product list item
            ProductListItem item = results.get(i);

            Assert.assertEquals(productInterface.getName(), item.getTitle());
            Assert.assertEquals(productInterface.getSku(), item.getSKU());
            Assert.assertEquals(productInterface.getUrlKey(), item.getSlug());
            Assert.assertEquals(String.format("/content/test-product-page.%s.html", productInterface.getUrlKey()),
                    item.getURL());
            Assert.assertEquals(productInterface.getPrice().getRegularPrice().getAmount().getValue(),
                    item.getPrice(), 0);
            Assert.assertEquals(productInterface.getPrice().getRegularPrice().getAmount().getCurrency().toString(),
                    item.getCurrency());
            priceFormatter.setCurrency(Currency.getInstance(productInterface.getPrice().getRegularPrice().getAmount().getCurrency().toString()));
            Assert.assertEquals(priceFormatter.format(productInterface.getPrice().getRegularPrice().getAmount().getValue()),
                    item.getFormattedPrice());
            Assert.assertTrue(StringUtils.endsWith(item.getImageURL(), productInterface.getSmallImage().getUrl()));
        }
    }

    @Test
    public void testPagination() {
        Assert.assertTrue(((Integer) this.productPage.getProperties().get(ProductList.PN_PAGE_SIZE)) >= this.slingModel.getProducts().size());
        Assert.assertTrue(((Integer) this.productPage.getProperties().get(ProductList.PN_PAGE_SIZE)) <= categoryQueryResult.getProductCount());

        incomingRequest = mock(SlingHttpServletRequest.class);

        when(incomingRequest.getParameter("page")).thenReturn("" + 1);
        Whitebox.setInternalState(this.slingModel, "request", incomingRequest);
        ((ProductListImpl) this.slingModel).setNavPageCursor();
        ((ProductListImpl) this.slingModel).setupPagination();

        Assert.assertTrue(this.slingModel.getCurrentNavPage() == 1);
        Assert.assertTrue(this.slingModel.getNextNavPage() == 2);
        Assert.assertTrue(this.slingModel.getProducts().size() <= (Integer) this.productPage.getProperties().get(ProductList.PN_PAGE_SIZE));

        when(incomingRequest.getParameter("page")).thenReturn("" + 2);
        Whitebox.setInternalState(this.slingModel, "request", incomingRequest);
        ((ProductListImpl) this.slingModel).setNavPageCursor();
        ((ProductListImpl) this.slingModel).setupPagination();

        Assert.assertTrue(this.slingModel.getCurrentNavPage() == 2);
        Assert.assertTrue(this.slingModel.getNextNavPage() == 3);
        Assert.assertTrue(this.slingModel.getProducts().size() <= (Integer) this.productPage.getProperties().get(ProductList.PN_PAGE_SIZE));

        when(incomingRequest.getParameter("page")).thenReturn("" + 3);
        Whitebox.setInternalState(this.slingModel, "request", incomingRequest);
        ((ProductListImpl) this.slingModel).setNavPageCursor();
        ((ProductListImpl) this.slingModel).setupPagination();

        Assert.assertTrue(this.slingModel.getCurrentNavPage() == 3);
        Assert.assertTrue(this.slingModel.getNextNavPage() == 3);
        Assert.assertTrue(this.slingModel.getProducts().size() <= (Integer) this.productPage.getProperties().get(ProductList.PN_PAGE_SIZE));
    }


}