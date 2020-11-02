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
package com.adobe.cq.commerce.core.components.internal.models.v1.teaser;

import java.util.Arrays;

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

public class CategoriesRetrieverTest {

    private CategoriesRetriever retriever;
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

        retriever = new CategoriesRetriever(mockClient);
        retriever.setIdentifiers(Arrays.asList("31", "45"));
    }

    @Test
    public void testCategoryUrlPathQuery() {
        retriever.fetchCategories();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{category__category_31:category(id:31){id,url_path},category__category_45:category(id:45){id,url_path}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }

    @Test
    public void testExtendCategoryQuery() {
        retriever.extendCategoryQueryWith(c -> c.name());
        retriever.fetchCategories();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{category__category_31:category(id:31){id,url_path,name},category__category_45:category(id:45){id,url_path,name}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }
}
