/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.breadcrumb;

import java.lang.reflect.Type;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.google.gson.reflect.TypeToken;

public class BreadcrumbRetrieverTest {

    @Test
    public void testMissingIdentifier() throws Exception {
        MagentoGraphqlClient client = Mockito.mock(MagentoGraphqlClient.class);
        BreadcrumbRetriever retriever = new BreadcrumbRetriever(client);

        retriever.fetchCategoriesBreadcrumbs();
        retriever.fetchProduct();
        Mockito.verifyZeroInteractions(client);
    }

    @Test
    public void testProductQueryWithSku() throws Exception {
        String testSKU = "sku";
        String json = Utils.getResource("graphql/magento-graphql-product-breadcrumb-result.json");

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> response = QueryDeserializer.getGson().fromJson(json, type);

        MagentoGraphqlClient client = Mockito.mock(MagentoGraphqlClient.class);
        Mockito.when(client.execute(Mockito.any())).thenReturn(response);

        BreadcrumbRetriever retriever = new BreadcrumbRetriever(client);

        retriever.setProductIdentifierHook(input -> input.setSku(new FilterEqualTypeInput().setEq(testSKU)));
        retriever.fetchCategoriesBreadcrumbs();
        retriever.fetchCategoriesBreadcrumbs();
        retriever.fetchProduct();
        retriever.fetchProduct();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        // Checks that the method is only called once
        Mockito.verify(client).execute(captor.capture());

        // Checks that the query is done with the SKU
        Assert.assertTrue(captor.getValue().startsWith("{products(filter:{sku:{eq:\"" + testSKU + "\"}})"));
    }
}
