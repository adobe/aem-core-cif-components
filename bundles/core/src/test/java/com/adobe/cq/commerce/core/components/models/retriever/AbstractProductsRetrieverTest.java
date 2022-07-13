/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.models.retriever;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class AbstractProductsRetrieverTest {

    private AbstractProductsRetriever subject;

    @Mock
    private MagentoGraphqlClient client;
    @Mock
    private GraphqlResponse<Query, Error> response;
    @Mock
    private Query query;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        subject = new AbstractProductsRetriever(client) {
            @Override
            protected ProductInterfaceQueryDefinition generateProductQuery() {
                return q -> q.sku();
            }
        };

        when(response.getData()).thenReturn(query);
    }

    @Test
    public void testErrorResponse() {
        // given
        Error error = new Error();
        error.setMessage("foobar");
        when(client.execute(any())).thenReturn(response);
        when(response.getErrors()).thenReturn(Collections.singletonList(error));

        // when no identifier, then
        List<? extends ProductInterface> products = subject.fetchProducts();
        assertTrue(products.isEmpty());

        // when with identifiers, then
        subject.setIdentifiers(Arrays.asList("a", "b", "c"));
        products = subject.fetchProducts();
        assertTrue(products.isEmpty());
    }

    @Test
    public void testExtendFilterWithHook() {
        subject.extendProductFilterWith(f -> f.setName(new FilterMatchTypeInput().setMatch("my-name")));

        String query = subject.generateQuery(Collections.singletonList("abc"));
        Assert.assertEquals("{products(filter:{name:{match:\"my-name\"},sku:{in:[\"abc\"]}}){items{__typename,sku}}}", query);
    }

    @Test
    public void testReplaceAndExtendFilterWithHook() {
        subject.extendProductFilterWith(f -> new ProductAttributeFilterInput().setUrlKey(new FilterEqualTypeInput().setEq("my-product")));
        subject.extendProductFilterWith(f -> f.setName(new FilterMatchTypeInput().setMatch("my-name")));

        String query = subject.generateQuery(Collections.singletonList("abc"));
        Assert.assertEquals("{products(filter:{name:{match:\"my-name\"},url_key:{eq:\"my-product\"}}){items{__typename,sku}}}", query);
    }
}
