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
package com.adobe.cq.commerce.core.components.internal.models.v1.categorylist;

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
        retriever.setIdentifiers(Arrays.asList("5", "6"));
    }

    @Test
    public void testQueryOverride() {
        String sampleQuery = "{ my_sample_query }";
        retriever.setQuery(sampleQuery);
        retriever.fetchCategories();

        verify(mockClient, times(1)).execute(sampleQuery);
    }

    @Test
    public void testExtendCategoryQuery() {
        retriever.extendCategoryQueryWith(c -> c.childrenCount()
            .addCustomSimpleField("level"));
        retriever.fetchCategories();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(captor.capture());

        String expectedQuery = "{category__category_5:category(id:5){id,name,url_path,position,image,children_count,level_custom_:level},category__category_6:category(id:6){id,name,url_path,position,image,children_count,level_custom_:level}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }

    @Test
    public void testChangingIdentifier() {
        retriever.fetchCategories();

        final ArgumentCaptor<String> firstCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(1)).execute(firstCaptor.capture());

        retriever.setIdentifiers(Arrays.asList("6"));
        retriever.fetchCategories();

        final ArgumentCaptor<String> secondCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockClient, times(2)).execute(secondCaptor.capture());

        Assert.assertTrue(firstCaptor.getValue().contains("category(id:5)"));
        Assert.assertTrue(firstCaptor.getValue().contains("category(id:6)"));
        Assert.assertFalse(secondCaptor.getValue().contains("category(id:5)"));
        Assert.assertTrue(secondCaptor.getValue().contains("category(id:6)"));
    }

}
