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

package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductRetrieverTest {

    private ProductRetriever retriever;
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

        retriever = new ProductRetriever(mockClient);
    }

    @Test
    public void testQueryOverride() {
        String sampleQuery = "{ my_sample_query }";
        retriever.setQuery(sampleQuery);
        retriever.fetchProduct();

        verify(mockClient, times(1)).execute(sampleQuery);
    }

    @Test
    public void testExtendedProductQuery() {
        retriever.extendProductQueryWith(p -> p.createdAt()
            .addCustomSimpleField("is_returnable"));
        retriever.fetchProduct();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        Assert.assertTrue(captor.getValue().endsWith("created_at,is_returnable_custom_:is_returnable}}}"));
    }

    @Test
    public void testExtendedVariantQuery() {
        retriever.extendVariantQueryWith(p -> p.weight()
            .addCustomSimpleField("volume"));
        retriever.fetchProduct();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        Assert.assertTrue(captor.getValue().contains("weight,volume_custom_:volume}}},... on GroupedProduct"));
    }

    @Test
    public void testSkuIdentifierType() {
        retriever.setIdentifier(ProductIdentifierType.SKU, "my-sku");
        retriever.fetchProduct();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());
        String queryStartsWith = "{products(filter:{sku:{eq:\"my-sku\"}})";
        Assert.assertTrue(captor.getValue().startsWith(queryStartsWith));
    }

    @Test
    public void testUrlKeyIdentifierType() {
        retriever.setIdentifier(ProductIdentifierType.URL_KEY, "my-slug");
        retriever.fetchProduct();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());
        String queryStartsWith = "{products(filter:{url_key:{eq:\"my-slug\"}})";
        Assert.assertTrue(captor.getValue().startsWith(queryStartsWith));
    }
}
