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
package com.adobe.cq.commerce.core.components.internal.models.v1.button;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Query;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CategoryRetrieverTest {

    private CategoryRetriever retriever;
    private MagentoGraphqlClient mockClient;

    @Before
    public void setUp() {
        mockClient = mock(MagentoGraphqlClient.class);
        GraphqlResponse mockResponse = mock(GraphqlResponse.class);
        Query mockQuery = mock(Query.class, RETURNS_DEEP_STUBS);
        CategoryTree mockCategory = mock(CategoryTree.class);

        when(mockClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.getData()).thenReturn(mockQuery);
        when(mockQuery.get(any())).thenReturn(mockCategory);

        retriever = new CategoryRetriever(mockClient);
    }

    private void testDefaultCategoryQuery(String identifier) {
        retriever.fetchCategory();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{categoryList(filters:{category_uid:{eq:\"" + identifier + "\"}}){uid,url_key,url_path";
        Assert.assertTrue(captor.getValue().startsWith(expectedQuery));
    }

    @Test
    public void testCategoryUrlPathQueryFallback() {
        retriever.setIdentifier("Mg==");
        testDefaultCategoryQuery("Mg==");
    }

    @Test
    public void testCategoryUrlPathQueryByUID() {
        retriever.setIdentifier("Mg==");
        testDefaultCategoryQuery("Mg==");
    }

    @Test
    public void testCategoryUrlPathQuery() {
        retriever.setIdentifier("Mg==");
        retriever.fetchCategory();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{categoryList(filters:{category_uid:{eq:\"Mg==\"}}){uid,url_key,url_path}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }

    @Test
    public void testExtendedButtonQuery() {
        retriever.setIdentifier("Mg==");
        retriever.extendCategoryQueryWith(c -> c.image());
        retriever.fetchCategory();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{categoryList(filters:{category_uid:{eq:\"Mg==\"}}){uid,url_key,url_path,image";
        Assert.assertTrue(captor.getValue().startsWith(expectedQuery));
    }
}
