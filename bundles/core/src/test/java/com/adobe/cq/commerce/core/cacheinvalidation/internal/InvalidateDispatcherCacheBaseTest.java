/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.*;

import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InvalidateDispatcherCacheBaseTest {

    private InvalidateDispatcherCacheBase cacheBase;

    @Mock
    private Page page;

    @Mock
    private UrlProviderImpl urlProvider;

    @Mock
    private MagentoGraphqlClient graphqlClient;

    @Mock
    private Session session;

    @Mock
    private QueryManager queryManager;

    @Mock
    private Query query;

    @Mock
    private QueryResult queryResult;

    @Mock
    private RowIterator rowIterator;

    @Mock
    private Row row;

    @Before
    public void setUp() {
        cacheBase = new InvalidateDispatcherCacheBase() {};
    }

    @Test
    public void testGetProductPaths_WithMissingRequiredFields() {
        Map<String, Object> item = new HashMap<>();
        item.put("sku", "test-sku");
        // Missing url_key

        Set<String> result = cacheBase.getProductPaths(page, urlProvider, item);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetCategoryPaths_WithValidInputs() {
        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> category = new HashMap<>();
        category.put("uid", "test-uid");
        category.put("url_key", "test-url-key");
        category.put("url_path", "test-url-path");
        categories.add(category);

        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUid("test-uid");
        params.setUrlKey("test-url-key");
        params.setUrlPath("test-url-path");
        when(urlProvider.toCategoryUrl(any(), any(), any(CategoryUrlFormat.Params.class))).thenReturn("/test/category.html");

        Set<String> result = cacheBase.getCategoryPaths(page, urlProvider, categories);

        assertFalse(result.isEmpty());
        assertTrue(result.contains("/test/category"));
    }

    @Test
    public void testGetCategoryPaths_WithMissingRequiredFields() {
        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> category = new HashMap<>();
        category.put("uid", "test-uid");
        // Missing url_key and url_path
        categories.add(category);

        Set<String> result = cacheBase.getCategoryPaths(page, urlProvider, categories);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRemoveUpToDelimiter() {
        String input = "test/path/to/file.html";
        String result = cacheBase.removeUpToDelimiter(input, ".html", true);
        assertEquals("test/path/to/file", result);
    }

    @Test
    public void testRemoveUpToDelimiter_WithNullInput() {
        String result = cacheBase.removeUpToDelimiter(null, ".html", true);
        assertNull(result);
    }

    @Test
    public void testRemoveUpToDelimiter_WithNullDelimiter() {
        String result = cacheBase.removeUpToDelimiter("test/path", null, true);
        assertEquals("test/path", result);
    }

    @Test
    public void testRemoveUpToDelimiter_WithFirstOccurrence() {
        String input = "test.html/path.html";
        String result = cacheBase.removeUpToDelimiter(input, ".html", false);
        assertEquals("test", result);
    }

    @Test
    public void testProcessItems() {
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("url_path", "test-path");
        item.put("url_key", "test-key");
        item.put("uid", "test-uid");
        items.add(item);

        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUrlPath("test-path");
        params.setUrlKey("test-key");
        params.setUid("test-uid");
        when(urlProvider.toCategoryUrl(any(), any(), any(CategoryUrlFormat.Params.class))).thenReturn("/test/category.html");

        Set<String> result = cacheBase.processItems(page, urlProvider, items);

        assertFalse(result.isEmpty());
        assertTrue(result.contains("/test/category"));
    }

    @Test
    public void testProcessItems_WithNullItem() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(null);

        Set<String> result = cacheBase.processItems(page, urlProvider, items);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetQueryResult() throws Exception {
        when(session.getWorkspace()).thenReturn(mock(org.apache.jackrabbit.api.JackrabbitWorkspace.class));
        when(session.getWorkspace().getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath("content")).thenReturn("/content/test-page/jcr:content");

        String[] result = cacheBase.getQueryResult(query);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("/content/test-page.html", result[0]);
    }

    @Test
    public void testGetQueryResult_WithNullResult() throws Exception {
        when(session.getWorkspace()).thenReturn(mock(org.apache.jackrabbit.api.JackrabbitWorkspace.class));
        when(session.getWorkspace().getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);
        when(query.execute()).thenReturn(null);

        String[] result = cacheBase.getQueryResult(query);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testGetQueryResult_WithNullRows() throws Exception {
        when(session.getWorkspace()).thenReturn(mock(org.apache.jackrabbit.api.JackrabbitWorkspace.class));
        when(session.getWorkspace().getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(null);

        String[] result = cacheBase.getQueryResult(query);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testGetGraphqlResponseData_WithNullInputs() {
        com.adobe.cq.commerce.magento.graphql.Query result = cacheBase.getGraphqlResponseData(null, null);
        assertNull(result);
    }

    @Test
    public void testGetGraphqlResponseData_WithValidInputs() {
        String query = "test query";
        GraphqlResponse<com.adobe.cq.commerce.magento.graphql.Query, Error> response = mock(GraphqlResponse.class);
        com.adobe.cq.commerce.magento.graphql.Query data = mock(com.adobe.cq.commerce.magento.graphql.Query.class);

        when(graphqlClient.execute(query)).thenReturn(response);
        when(response.getErrors()).thenReturn(null);
        when(response.getData()).thenReturn(data);

        com.adobe.cq.commerce.magento.graphql.Query result = cacheBase.getGraphqlResponseData(graphqlClient, query);

        assertNotNull(result);
        assertEquals(data, result);
    }

    @Test
    public void testGetGraphqlResponseData_WithErrors() {
        String query = "test query";
        GraphqlResponse<com.adobe.cq.commerce.magento.graphql.Query, Error> response = mock(GraphqlResponse.class);
        List<Error> errors = new ArrayList<>();
        errors.add(new Error());

        when(graphqlClient.execute(query)).thenReturn(response);
        when(response.getErrors()).thenReturn(errors);

        com.adobe.cq.commerce.magento.graphql.Query result = cacheBase.getGraphqlResponseData(graphqlClient, query);
        assertNull(result);
    }

    @Test
    public void testGetGraphqlResponseData_WithNullData() {
        String query = "test query";
        GraphqlResponse<com.adobe.cq.commerce.magento.graphql.Query, Error> response = mock(GraphqlResponse.class);

        when(graphqlClient.execute(query)).thenReturn(response);
        when(response.getErrors()).thenReturn(null);
        when(response.getData()).thenReturn(null);

        com.adobe.cq.commerce.magento.graphql.Query result = cacheBase.getGraphqlResponseData(graphqlClient, query);
        assertNull(result);
    }

    @Test
    public void testExtractPagePath() {
        String fullPath = "/content/test-page/jcr:content";
        String result = cacheBase.extractPagePath(fullPath);
        assertEquals("/content/test-page", result);
    }

    @Test
    public void testExtractPagePath_WithNullInput() {
        String result = cacheBase.extractPagePath(null);
        assertNull(result);
    }

    @Test
    public void testExtractPagePath_WithoutJcrContent() {
        String fullPath = "/content/test-page";
        String result = cacheBase.extractPagePath(fullPath);
        assertEquals("/content/test-page", result);
    }

    @Test
    public void testFormatList() {
        String[] items = { "item1", "item2" };
        String result = cacheBase.formatList(items, ",", "prefix-%s");
        assertEquals("prefix-item1,prefix-item2", result);
    }

    @Test
    public void testFormatList_WithEmptyArray() {
        String[] items = {};
        String result = cacheBase.formatList(items, ",", "prefix-%s");
        assertEquals("", result);
    }

    @Test
    public void testFormatList_WithNullArray() {
        String result = cacheBase.formatList(null, ",", "prefix-%s");
        assertEquals("", result);
    }
}
