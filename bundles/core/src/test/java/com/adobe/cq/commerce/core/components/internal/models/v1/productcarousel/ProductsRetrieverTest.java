/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductsRetrieverTest {

    private ProductsRetriever retriever;
    private MagentoGraphqlClient mockClient;

    @Before
    public void setUp() {
        mockClient = mock(MagentoGraphqlClient.class);
        GraphqlResponse mockResponse = mock(GraphqlResponse.class);
        Query mockQuery = mock(Query.class, RETURNS_DEEP_STUBS);

        when(mockClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.getData()).thenReturn(mockQuery);
        when(mockQuery.getProducts().getItems()).thenReturn(Collections.emptyList());
        when(mockQuery.getStoreConfig().getSecureBaseMediaUrl()).thenReturn("");

        retriever = new ProductsRetriever(mockClient);
    }

    @Test
    public void testQueryOverride() {
        String sampleQuery = "{ my_sample_query }";
        retriever.setQuery(sampleQuery);
        retriever.fetchProducts();

        verify(mockClient, times(1)).execute(sampleQuery);
    }

    @Test
    public void testCategoryListQuery() {
        retriever.setCategoryUid("uid-1");
        retriever.setProductCount(5);

        retriever.fetchProducts();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{categoryList(filters:{category_uid:{eq:\"uid-1\"}}){uid,url_key,url_path,products(currentPage:1,pageSize:5){items{__typename,sku,name,thumbnail{label,url},url_key,url_path,url_rewrites{url},price_range{minimum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}},... on ConfigurableProduct{price_range{maximum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}},... on BundleProduct{price_range{maximum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}}}}}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }

    @Test
    public void testExtendedProductQuery() {
        retriever.extendProductQueryWith(p -> p.createdAt()
            .addCustomSimpleField("is_returnable"));
        retriever.setIdentifiers(Arrays.asList("sku-a", "sku-b"));
        retriever.fetchProducts();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{products(filter:{sku:{in:[\"sku-a\",\"sku-b\"]}}){items{__typename,sku,name,thumbnail{label,url},url_key,url_path,url_rewrites{url},price_range{minimum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}},... on ConfigurableProduct{variants{product{sku,name,thumbnail{label,url},price_range{minimum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}}},price_range{maximum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}},... on BundleProduct{price_range{maximum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}},created_at,is_returnable_custom_:is_returnable}}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }

    @Test
    public void testExtendedVariantQuery() {
        retriever.extendVariantQueryWith(p -> p.weight()
            .addCustomSimpleField("volume"));
        retriever.setIdentifiers(Arrays.asList("sku-a", "sku-b"));
        retriever.fetchProducts();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{products(filter:{sku:{in:[\"sku-a\",\"sku-b\"]}}){items{__typename,sku,name,thumbnail{label,url},url_key,url_path,url_rewrites{url},price_range{minimum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}},... on ConfigurableProduct{variants{product{sku,name,thumbnail{label,url},price_range{minimum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}},weight,volume_custom_:volume}},price_range{maximum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}},... on BundleProduct{price_range{maximum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}}}}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }

    @Test
    public void testExtendedCategoryListQuery() {
        retriever.extendProductQueryWith(p -> p.createdAt().addCustomSimpleField("is_returnable"));
        retriever.setCategoryUid("uid-1");
        retriever.setProductCount(5);

        retriever.fetchProducts();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{categoryList(filters:{category_uid:{eq:\"uid-1\"}}){uid,url_key,url_path,products(currentPage:1,pageSize:5){items{__typename,sku,name,thumbnail{label,url},url_key,url_path,url_rewrites{url},price_range{minimum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}},... on ConfigurableProduct{price_range{maximum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}},... on BundleProduct{price_range{maximum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}},created_at,is_returnable_custom_:is_returnable}}}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }
}
