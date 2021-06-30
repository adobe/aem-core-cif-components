/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.models.retriever;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractProductRetrieverTest {

    private MagentoGraphqlClient client;
    private AbstractProductRetriever productRetriever;

    @Before
    public void setUp() {
        client = mock(MagentoGraphqlClient.class);
        productRetriever = new TestProductRetriever(client);
    }

    @Test
    public void testGetSimpleProductByUrlKey() throws IOException {
        productRetriever.setIdentifier(UrlProvider.ProductIdentifierType.URL_KEY, "test-simple");

        GraphqlResponse<Query, Error> response = mock(GraphqlResponse.class);

        when(client.execute(any())).thenReturn(response);
        when(response.getData()).thenReturn(Utils.getQueryFromResource("graphql/magento-graphql-multiple-products-result.json"));

        Assert.assertEquals("xyz", productRetriever.fetchProduct().getSku());
    }

    @Test
    public void testGetBundleProductBySku() throws IOException {
        productRetriever.setIdentifier(UrlProvider.ProductIdentifierType.SKU, "abc");

        GraphqlResponse<Query, Error> response = mock(GraphqlResponse.class);

        when(client.execute(any())).thenReturn(response);
        when(response.getData()).thenReturn(Utils.getQueryFromResource("graphql/magento-graphql-multiple-products-result.json"));

        Assert.assertEquals("test-bundle", productRetriever.fetchProduct().getUrlKey());
    }
}