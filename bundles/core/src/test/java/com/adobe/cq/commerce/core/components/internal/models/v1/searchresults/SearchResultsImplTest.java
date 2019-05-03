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

package com.adobe.cq.commerce.core.components.internal.models.v1.searchresults;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.Price;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductPrices;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.day.cq.wcm.api.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JUnit test suite for {@link SearchResultsImpl}
 */
public class SearchResultsImplTest {

    private static final String SEARCH_TERM = "glove";

    private static final String QUERY_STRING = "{products(filter:{name:{like:\"%glove%\"}}){items{__typename,id,url_key,name,small_image{label,url},price{regularPrice{amount{value,currency}}}}}}";

    private SearchResultsImpl modelUnderTest;

    private JsonElement resultRoot;

    private GraphqlResponse<Query, Error> successfulResponse;

    @Before
    public void setUp() throws IOException {
        modelUnderTest = new SearchResultsImpl();

        Whitebox.setInternalState(modelUnderTest, "searchTerm", SEARCH_TERM);

        mockPage();

        // Search results
        String json = IOUtils.toString(this.getClass()
                                           .getResourceAsStream("/graphql/magento-graphql-search-result.json"), StandardCharsets.UTF_8);
        JsonParser parser = new JsonParser();
        resultRoot = parser.parse(json);
    }

    @Test
    public void testSearchTermProcessing() {
        String expectedProcessedTerm = "%" + SEARCH_TERM + "%";
        String actualProcessedTerm = modelUnderTest.processSearchTerm(SEARCH_TERM);

        Assert.assertEquals("Process the search term", expectedProcessedTerm, actualProcessedTerm);
        String emptyProcessedTerm = modelUnderTest.processSearchTerm("%a3@$%@^@%^!@#$!@%^&*(*&^%$#@'aaaaaaa%");

        Assert.assertEquals("Empty search term if bogus characters are entered","", emptyProcessedTerm);
    }

    @Test
    public void testGenerateQueryString() {
        String actualQueryString = modelUnderTest.generateQueryString("%" + SEARCH_TERM + "%");

        Assert.assertEquals("The query string is generated", QUERY_STRING, actualQueryString);
    }

    @Test
    public void testCheckErrors() {
        boolean checked = modelUnderTest.checkAndLogErrors(mockErrorResponse());
        Assert.assertTrue("Returns <true> in case of errors", checked);
     
        checked = modelUnderTest.checkAndLogErrors(mockSuccessfulResponse());
        Assert.assertFalse("Returns <false> if there are no errors", checked);
    }

    @Test
    public void testGenerateProductsFromResponse() {
        List<ProductListItem> products = modelUnderTest.extractProductsFromResponse(mockSuccessfulResponse());

        Assert.assertEquals("Return the correct number of products", 2, products.size());
    }

    private void mockPage() {
        Page productPage = mock(Page.class);
        when(productPage.getLanguage(false)).thenReturn(Locale.US);
        when(productPage.getPath()).thenReturn("/content/test-product-page");
        Whitebox.setInternalState(this.modelUnderTest, "productPage", productPage);
    }

    private GraphqlResponse<Query, Error> mockErrorResponse() {
        List<Error> errors = new ArrayList<>();
        for (int idx = 0; idx < 2; idx++) {
            Error err = new Error();
            err.setCategory("graphql");
            err.setMessage("Error " + idx);
            errors.add(err);
        }

        GraphqlResponse<Query, Error> mockResponse = new GraphqlResponse<>();

        mockResponse.setData(mock(Query.class).setProducts(mock(Products.class)));
        mockResponse.setErrors(errors);
        return mockResponse;
    }

    private GraphqlResponse<Query, Error> mockSuccessfulResponse() {
        if (successfulResponse != null) {
            return successfulResponse;
        }

        List<ProductInterface> productElements = new ArrayList<>();
        JsonArray items = resultRoot.getAsJsonObject()
                                    .get("data")
                                    .getAsJsonObject()
                                    .get("products")
                                    .getAsJsonObject()
                                    .get("items")
                                    .getAsJsonArray();
        items.iterator()
             .forEachRemaining(jsonElement -> {
                 JsonObject jsonObject = jsonElement.getAsJsonObject();
                 Integer id = jsonObject.get("id")
                                        .getAsInt();
                 String name = jsonObject.get("name")
                                         .getAsString();
                 String urlKey = jsonObject.get("url_key")
                                           .getAsString();
                 JsonObject priceAmount = jsonObject.get("price")
                                                    .getAsJsonObject()
                                                    .get("regularPrice")
                                                    .getAsJsonObject()
                                                    .get("amount")
                                                    .getAsJsonObject();
                 double price = priceAmount.get("value")
                                           .getAsDouble();
                 String currency = priceAmount.get("currency")
                                              .getAsString();
                 String imageUrl = jsonObject.get("small_image")
                                             .getAsJsonObject()
                                             .get("url")
                                             .getAsString();
                 productElements.add(mockProduct(id, name, urlKey, price, currency, imageUrl));
             });
        Products products = new Products();
        products.setItems(productElements);

        Query rootQuery = new Query();
        rootQuery.setProducts(products);

        successfulResponse = new GraphqlResponse<>();
        successfulResponse.setData(rootQuery);
        successfulResponse.setErrors(Collections.emptyList());

        return successfulResponse;

    }

    private ProductInterface mockProduct(Integer id, String name, String urlKey, double price, String currency, String url) {

        Money amount = new Money();
        amount.setCurrency(CurrencyEnum.valueOf(currency));
        amount.setValue(price);

        Price regularPrice = new Price();
        regularPrice.setAmount(amount);

        ProductPrices productPrices = new ProductPrices();
        productPrices.setRegularPrice(regularPrice);

        ProductImage productImage = new ProductImage();
        productImage.setUrl(url);

        ProductInterface product = mock(ProductInterface.class);
        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn(name);
        when(product.getUrlKey()).thenReturn(urlKey);
        when(product.getPrice()).thenReturn(productPrices);
        when(product.getSmallImage()).thenReturn(productImage);

        return product;
    }

}
