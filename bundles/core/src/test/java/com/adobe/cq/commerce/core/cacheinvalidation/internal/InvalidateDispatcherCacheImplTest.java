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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.slf4j.Logger;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

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

    private ComponentsConfiguration componentsConfiguration, commerceProperties;

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

        String[] expectedPaths = new String[] { "/content/path.html" };
        String[] result = (String[]) method.invoke(invalidateDispatcherCacheImpl, query);

        assertArrayEquals(expectedPaths, result);
    }

    @Test
    public void testExceptionMessage() {
        String message = "Test message";
        InvalidateDispatcherCacheImpl.CacheInvalidationException exception = new InvalidateDispatcherCacheImpl.CacheInvalidationException(
            message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testExceptionMessageAndCause() {
        String message = "Test message";
        Throwable cause = new Throwable("Cause message");
        InvalidateDispatcherCacheImpl.CacheInvalidationException exception = new InvalidateDispatcherCacheImpl.CacheInvalidationException(
            message, cause);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
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
            Map.of("sku", "sku2", "url_key", "url2", "url_rewrites", List.of(Map.of("url", "/path2")))))));
        doAnswer(invocation -> graphqlResponseProducts).when(graphqlClient).execute(any(GraphqlRequest.class), any(Type.class), any(
            Type.class));

        // Mock GraphqlClient response for categoryUids
        GraphqlResponse<Map<String, Object>, Map<String, Object>> graphqlResponseCategories = mock(GraphqlResponse.class);
        when(graphqlResponseCategories.getData()).thenReturn(Map.of("categoryList", List.of(
            Map.of("uid", "uid1", "url_key", "category1", "url_path", "/category1"),
            Map.of("uid", "uid2", "url_key", "category2", "url_path", "/category2"))));
        doAnswer(invocation -> graphqlResponseCategories).when(graphqlClient).execute(any(GraphqlRequest.class), any(Type.class), any(
            Type.class));
    }

    @Test
    public void testGetCorrespondingPageProperties() throws Exception {
        String storePath = "/store/path";
        String propertyName = "propertyName";
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getCorrespondingPageProperties", ResourceResolver.class,
            String.class, String.class);
        method.setAccessible(true);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(mock(PageManager.class));
        when(resourceResolver.adaptTo(PageManager.class).getPage(storePath)).thenReturn(mock(Page.class));
        when(resourceResolver.adaptTo(PageManager.class).getPage(storePath).getProperties()).thenReturn(valueMap);
        when(valueMap.get(propertyName, String.class)).thenReturn("propertyValue");

        String result = (String) method.invoke(invalidateDispatcherCacheImpl, resourceResolver, storePath, propertyName);
        assertEquals("propertyValue", result);
    }

}
