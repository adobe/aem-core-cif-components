/*******************************************************************************
 *
 *    Copyright 2024 Adobe. All rights reserved.
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
import javax.jcr.Workspace;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.day.cq.wcm.api.Page;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import org.apache.commons.httpclient.methods.PostMethod;

import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.slf4j.Logger;


import javax.jcr.Session;
import javax.jcr.query.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateDispatcherCacheImplTest {

    @InjectMocks
    private InvalidateDispatcherCacheImpl invalidateDispatcherCacheImpl;

    @Mock
    private UrlProviderImpl urlProvider;
    @Mock
    private PostMethod postMethod;


    @Mock
    private SlingSettingsService slingSettingsService;

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private ValueMap valueMap;


    @Mock
    private HttpClient httpClient;

    @Mock
    private Session session;

    @Mock
    private Workspace workspace;

    @Mock
    private QueryManager queryManager;

    @Mock
    private Query query;

    @Mock
    private QueryResult queryResult;

    @Mock
    private RowIterator rowIterator;


    @Mock
    private Logger logger;


    private ComponentsConfiguration componentsConfiguration,commerceProperties;

    @Mock
    private GraphqlClient graphqlClient;


    @Mock
    private Row row;



    @Mock
    private Map<String, Object> properties;

    @Mock
    private Map<String, String[]> dynamicProperties;

    private static final String STORE_PATH = "/store/path";
    private static final String PATH_DELIMITER = "/";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Mock the ComponentsConfiguration object
        componentsConfiguration = new ComponentsConfiguration(valueMap);

        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, (String) null)).thenReturn("graphqlClientId");
        logger = mock(Logger.class);

        when(invalidateCacheSupport.getClient(anyString())).thenReturn(graphqlClient);

        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
    }



    @Test(expected = RuntimeException.class)
    public void testFlushCache_IOException1() throws Exception {
        String handle = "/content/path";
        when(httpClient.executeMethod(postMethod)).thenThrow(new IOException("IO error"));

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("flushCache", String.class);
        method.setAccessible(true);
        try {
            method.invoke(invalidateDispatcherCacheImpl, handle);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void testInvalidateCache_AuthorMode() {
        when(slingSettingsService.getRunModes()).thenReturn(Set.of("author"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("/store/path");
        when(invalidateCacheSupport.getCommerceProperties(resourceResolver, "/store/path")).thenReturn(componentsConfiguration);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        invalidateDispatcherCacheImpl.invalidateCache("/content/path");

        verify(logger, never()).error(anyString());
    }


    @Test
    public void testInvalidateCache_ResourceNotFound1() {
        when(slingSettingsService.getRunModes()).thenReturn(Set.of("author"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(null);

        invalidateDispatcherCacheImpl.invalidateCache("/content/path");

        verify(resourceResolver).close();
    }


    @Test
    public void testGenerateQuery() throws Exception {
        String[] entries = new String[]{"entry1", "entry2"};
        String key = "productSkus";

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("generateQuery", String[].class, String.class);
        method.setAccessible(true);

        String expectedQuery = "{products(filter:{sku:{in:[\"entry1\",\"entry2\"]}}){items{__typename,sku,url_key,url_rewrites{url},categories{__typename,uid,url_key,url_path}}}}";
        String result = (String) method.invoke(invalidateDispatcherCacheImpl, entries, key);

        assertEquals(expectedQuery, result);
    }

    @Test
    public void testGetQueryResult() throws Exception {
        query = mock(Query.class);
        queryResult = mock(QueryResult.class);
        rowIterator = mock(RowIterator.class);
        row = mock(Row.class);

        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath("content")).thenReturn("/content/path");

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getQueryResult", Query.class);
        method.setAccessible(true);

        String[] expectedPaths = new String[]{"/content/path.html"};
        String[] result = (String[]) method.invoke(invalidateDispatcherCacheImpl, query);

        assertArrayEquals(expectedPaths, result);
    }

    @Test
    public void testGetSkuBasedInvalidPaths() throws Exception {
        String storePath = "/store/path";
        Map<String, Object> data = new HashMap<>();
        data.put("products", Map.of("items", List.of(
                Map.of("sku", "sku1", "url_key", "url1", "url_rewrites", List.of(Map.of("url", "/path1"))),
                Map.of("sku", "sku2", "url_key", "url2", "url_rewrites", List.of(Map.of("url", "/path2")))
        )));

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getSkuBasedInvalidPaths", ResourceResolver.class, Map.class, String.class);
        method.setAccessible(true);

        when(urlProvider.toProductUrl(any(SlingHttpServletRequest.class), any(Page.class), any(ProductUrlFormat.Params.class)))
                .thenReturn("/path1")
                .thenReturn("/path2");

        String[] result = (String[]) method.invoke(invalidateDispatcherCacheImpl, resourceResolver, data, storePath);

        String[] expectedPaths = new String[]{"/path1", "/path2"};
        Arrays.sort(result);
        Arrays.sort(expectedPaths);
        assertArrayEquals(expectedPaths, result);
    }


    @Test
    public void testExceptionMessage() {
        String message = "Test message";
        InvalidateDispatcherCacheImpl.CacheInvalidationException exception = new InvalidateDispatcherCacheImpl.CacheInvalidationException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testExceptionMessageAndCause() {
        String message = "Test message";
        Throwable cause = new Throwable("Cause message");
        InvalidateDispatcherCacheImpl.CacheInvalidationException exception = new InvalidateDispatcherCacheImpl.CacheInvalidationException(message, cause);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }


    @Test
    public void testGetAllInvalidPaths() throws Exception {
        String storePath = "/store/path";
        Map<String, String[]> dynamicProperties = new HashMap<>();
        dynamicProperties.put("productSkus", new String[]{"sku1", "sku2"});
        dynamicProperties.put("categoryUids", new String[]{"uid1", "uid2"});

        setUpMocksForQuery();
        setUpMocksForGraphqlClient();

        String[] expectedPaths = new String[]{"/content/path"};

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getAllInvalidPaths", Session.class, ResourceResolver.class, GraphqlClient.class, String.class, Map.class);
        method.setAccessible(true);

        String[] result = (String[]) method.invoke(invalidateDispatcherCacheImpl, session, resourceResolver, graphqlClient, storePath, dynamicProperties);

        assertNotNull(result);


    }

    private void setUpMocksForQuery() throws Exception {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(session.getWorkspace()).thenReturn(mock(Workspace.class));
        when(session.getWorkspace().getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath()).thenReturn("/content/path");
    }

    private void setUpMocksForGraphqlClient() {
        // Mock GraphqlClient response for productSkus
        GraphqlResponse<Map<String, Object>, Map<String, Object>> graphqlResponseProducts = mock(GraphqlResponse.class);
        when(graphqlResponseProducts.getData()).thenReturn(Map.of("products", Map.of("items", List.of(
                Map.of("sku", "sku1", "url_key", "url1", "url_rewrites", List.of(Map.of("url", "/path1"))),
                Map.of("sku", "sku2", "url_key", "url2", "url_rewrites", List.of(Map.of("url", "/path2")))
        ))));
        doAnswer(invocation -> graphqlResponseProducts).when(graphqlClient).execute(any(GraphqlRequest.class), any(Type.class), any(Type.class));

        // Mock GraphqlClient response for categoryUids
        GraphqlResponse<Map<String, Object>, Map<String, Object>> graphqlResponseCategories = mock(GraphqlResponse.class);
        when(graphqlResponseCategories.getData()).thenReturn(Map.of("categoryList", List.of(
                Map.of("uid", "uid1", "url_key", "category1", "url_path", "/category1"),
                Map.of("uid", "uid2", "url_key", "category2", "url_path", "/category2")
        )));
        doAnswer(invocation -> graphqlResponseCategories).when(graphqlClient).execute(any(GraphqlRequest.class), any(Type.class), any(Type.class));
    }


    @Test
    public void testGetSession() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getSession", ResourceResolver.class);
        method.setAccessible(true);

        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        Session result = (Session) method.invoke(invalidateDispatcherCacheImpl, resourceResolver);
        assertNotNull(result);
        assertEquals(session, result);

        when(resourceResolver.adaptTo(Session.class)).thenReturn(null);
        result = (Session) method.invoke(invalidateDispatcherCacheImpl, resourceResolver);
        assertNull(result);
    }




    @Test
    public void testGetSkuBasedSql2Query() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getSkuBasedSql2Query", Session.class, String.class, String.class);
        method.setAccessible(true);

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(query);

        Query result = (Query) method.invoke(invalidateDispatcherCacheImpl, session, "/storePath", "'sku1', 'sku2'");
        assertNotNull(result);
        assertEquals(query, result);
    }

    @Test
    public void testGetCategoryBasedSql2Query() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getCategoryBasedSql2Query", Session.class, String.class, String.class);
        method.setAccessible(true);

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(query);

        Query result = (Query) method.invoke(invalidateDispatcherCacheImpl, session, "/storePath", "'uid1', 'uid2'");
        assertNotNull(result);
        assertEquals(query, result);
    }

    @Test
    public void testGetCorrespondingPageBasedOnEntries() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getCorrespondingPageBasedOnEntries", Session.class, String.class, String[].class, String.class);
        method.setAccessible(true);

        String[] entries = {"entry1", "entry2"};
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath("content")).thenReturn("/content/path");

        String[] result = (String[]) method.invoke(invalidateDispatcherCacheImpl, session, "/storePath", entries, "productSkus");
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("/content/path.html", result[0]);
    }

    @Test
    public void testGetAllInvalidPaths1() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getAllInvalidPaths", Session.class, ResourceResolver.class, GraphqlClient.class, String.class, Map.class);
        method.setAccessible(true);

        Map<String, String[]> dynamicProperties = new HashMap<>();
        dynamicProperties.put("productSkus", new String[]{"sku1", "sku2"});
        dynamicProperties.put("categoryUids", new String[]{"uid1", "uid2"});

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath("content")).thenReturn("/content/path");

        // Mock GraphqlClient and its response
        GraphqlResponse<Map<String, Object>, Map<String, Object>> graphqlResponse = mock(GraphqlResponse.class);
        when(graphqlResponse.getData()).thenReturn(new HashMap<>());
        when(graphqlClient.execute(any(GraphqlRequest.class), any(Type.class), any(Type.class))).thenReturn((GraphqlResponse) graphqlResponse);

        String[] result = (String[]) method.invoke(invalidateDispatcherCacheImpl, session, resourceResolver, graphqlClient, "/storePath", dynamicProperties);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("/content/path.html", result[0]);
    }

    @Test
    public void testRemoveUpToDelimiter() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("removeUpToDelimiter", String.class, String.class, boolean.class);
        method.setAccessible(true);

        String input = "example/path/to/resource";
        String delimiter = "/to/";

        String result = (String) method.invoke(invalidateDispatcherCacheImpl, input, delimiter, false);
        assertEquals("example/path", result);

        result = (String) method.invoke(invalidateDispatcherCacheImpl, input, delimiter, true);
        assertEquals("example/path", result);
    }

    @Test
    public void testCreateProductParams() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("createProductParams", Map.class);
        method.setAccessible(true);

        Map<String, Object> item = Map.of(
                "uid", "uid1",
                "url_key", "urlKey1",
                "url_path", "urlPath1"
        );

        ProductUrlFormat.Params params = (ProductUrlFormat.Params) method.invoke(invalidateDispatcherCacheImpl, item);

        assertEquals("XXXXXX", params.getUrlKey());
        assertEquals("uid1", params.getCategoryUrlParams().getUid());
        assertEquals("urlKey1", params.getCategoryUrlParams().getUrlKey());
        assertEquals("urlPath1", params.getCategoryUrlParams().getUrlPath());
    }

    @Test
    public void testProcessItem() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("processItem", Set.class, Map.class, Page.class);
        method.setAccessible(true);

        Set<String> uniquePagePaths = new HashSet<>();
        Map<String, Object> item = Map.of(
                "uid", "uid1",
                "url_key", "urlKey1",
                "url_path", "urlPath1"
        );

        Page page = mock(Page.class);
        when(urlProvider.toProductUrl(any(), eq(page), any(ProductUrlFormat.Params.class)))
                .thenReturn("/path1/product-page.html");

        method.invoke(invalidateDispatcherCacheImpl, uniquePagePaths, item, page);

        assertTrue(uniquePagePaths.contains("/path1"));
    }


    @Test(expected = RuntimeException.class)
    public void testFlushCache_IOException() throws Exception {
        String handle = "/content/path";
        when(httpClient.executeMethod(postMethod)).thenThrow(new IOException("IO error"));

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("flushCache", String.class);
        method.setAccessible(true);
        try {
            method.invoke(invalidateDispatcherCacheImpl, handle);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }



    @Test
    public void testInvalidateCache_ResourceNotFound4() throws Exception {
        when(slingSettingsService.getRunModes()).thenReturn(Set.of("author"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(null);

        invalidateDispatcherCacheImpl.invalidateCache("/content/path");

        verify(resourceResolver).close();
    }




    @Test
    public void testInvalidateCache_SessionNotFound() throws Exception {
        // Simulate session not found
        when(slingSettingsService.getRunModes()).thenReturn(Set.of("author"));
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);

        Method getSessionMethod = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getSession", ResourceResolver.class);
        getSessionMethod.setAccessible(true);
        when(getSessionMethod.invoke(invalidateDispatcherCacheImpl, resourceResolver)).thenReturn(null);

        invalidateDispatcherCacheImpl.invalidateCache("/content/path");

        verify(invalidateCacheSupport, never()).getCommerceProperties(any(), any());
    }


}