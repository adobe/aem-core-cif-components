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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.adobe.cq.commerce.core.cacheinvalidation.internal.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateDispatcherCacheImplTest {

    @Mock
    private SlingSettingsService slingSettingsService;

    @Mock
    private ValueMap valueMap;

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private InvalidateCacheRegistry invalidateCacheRegistry;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private GraphqlClient graphqlClient;

    @Mock
    private Logger logger;

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
    private Row row;

    @Mock
    private PageManager pageManager;

    @Mock
    private Page page;

    @Mock
    private GraphqlClient client;

    @Mock
    private GraphqlResponse graphqlResponse;

    private ComponentsConfiguration componentsConfiguration;

    @InjectMocks
    private InvalidateDispatcherCacheImpl invalidateDispatcherCacheImpl;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        componentsConfiguration = new ComponentsConfiguration(new ValueMapDecorator(new HashMap<>()));
        setLoggerField();
    }

    private void setLoggerField() throws Exception {
        Field loggerField = InvalidateDispatcherCacheImpl.class.getDeclaredField("LOGGER");
        loggerField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(loggerField, loggerField.getModifiers() & ~Modifier.FINAL);

        loggerField.set(null, logger);
    }

    @Test
    public void testInvalidateCache_AuthorMode_ShouldExitEarly() {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("author"));
        invalidateDispatcherCacheImpl.invalidateCache("/content/path");
        verify(invalidateCacheSupport, never()).getServiceUserResourceResolver();
        verify(logger).error("Operation is only supported for author");
    }

    @Test
    public void testInvalidateCache_ResourceResolverThrowsException_ShouldExitEarly() {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenThrow(new RuntimeException("Test exception"));

        invalidateDispatcherCacheImpl.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport, never()).getResource(any(), anyString());
        verify(logger).error(eq("Error invalidating cache: {}"), eq("Test exception"), any(RuntimeException.class));
    }

    @Test
    public void testInvalidateCache_ResourceIsNull_ShouldExitEarly() {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(null);

        invalidateDispatcherCacheImpl.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport).getResource(resourceResolver, "/content/path");
        verify(resource, never()).getValueMap();
    }

    @Test
    public void testInvalidateCache_GetAllInvalidPathsReturnsEmptyArray_ShouldExitEarly() throws Exception {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);

        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("storePath");

        InvalidateDispatcherCacheImpl invalidateDispatcherCacheImplSpy = spy(invalidateDispatcherCacheImpl);
        doReturn(session).when(invalidateDispatcherCacheImplSpy).getSession(resourceResolver);
        doReturn(true).when(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        doReturn(new String[0]).when(invalidateDispatcherCacheImplSpy).getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());
        doReturn(componentsConfiguration).when(invalidateDispatcherCacheImplSpy).getCommerceProperties(resourceResolver, "storePath");

        invalidateDispatcherCacheImplSpy.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport).getResource(resourceResolver, "/content/path");
        verify(resource).getValueMap();
        verify(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        verify(invalidateDispatcherCacheImplSpy).getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());
    }

    @Test
    public void testInvalidateCache_FlushCacheCalledForEachPath() throws Exception {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("storePath");

        InvalidateDispatcherCacheImpl invalidateDispatcherCacheImplSpy = spy(invalidateDispatcherCacheImpl);
        doReturn(session).when(invalidateDispatcherCacheImplSpy).getSession(resourceResolver);
        doReturn(true).when(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        doReturn(new String[] { "/content/page1", "/content/page2" }).when(invalidateDispatcherCacheImplSpy)
            .getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());
        doReturn(componentsConfiguration).when(invalidateDispatcherCacheImplSpy).getCommerceProperties(resourceResolver, "storePath");
        // Mock the flushCache method to do nothing
        doNothing().when(invalidateDispatcherCacheImplSpy).flushCache(anyString());

        invalidateDispatcherCacheImplSpy.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport).getResource(resourceResolver, "/content/path");
        verify(resource).getValueMap();
        verify(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        verify(invalidateDispatcherCacheImplSpy).getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());
        verify(invalidateDispatcherCacheImplSpy).flushCache("/content/page1");
        verify(invalidateDispatcherCacheImplSpy).flushCache("/content/page2");
    }

    @Test
    public void testInvalidateCache_IsValidReturnsFalse_ShouldExitEarly() throws Exception {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("storePath");

        InvalidateDispatcherCacheImpl invalidateDispatcherCacheImplSpy = spy(invalidateDispatcherCacheImpl);
        doReturn(session).when(invalidateDispatcherCacheImplSpy).getSession(resourceResolver);
        doReturn(false).when(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");

        invalidateDispatcherCacheImplSpy.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport).getResource(resourceResolver, "/content/path");
        verify(resource).getValueMap();
        verify(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        verify(invalidateDispatcherCacheImplSpy, never()).getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());
    }

    @Test
    public void testInvalidateCache_GetCommercePropertiesReturnsNull_ShouldExitEarly() throws Exception {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("storePath");

        InvalidateDispatcherCacheImpl invalidateDispatcherCacheImplSpy = spy(invalidateDispatcherCacheImpl);
        doReturn(session).when(invalidateDispatcherCacheImplSpy).getSession(resourceResolver);
        doReturn(true).when(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        doReturn(null).when(invalidateDispatcherCacheImplSpy).getCommerceProperties(resourceResolver, "storePath");

        invalidateDispatcherCacheImplSpy.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport).getResource(resourceResolver, "/content/path");
        verify(resource).getValueMap();
        verify(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        verify(invalidateDispatcherCacheImplSpy).getCommerceProperties(resourceResolver, "storePath");
        verify(invalidateDispatcherCacheImplSpy, never()).getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());
    }

    @Test
    public void testInvalidateCache_GetAllInvalidPathsThrowsException_ShouldLogError() throws Exception {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("storePath");

        InvalidateDispatcherCacheImpl invalidateDispatcherCacheImplSpy = spy(invalidateDispatcherCacheImpl);
        doReturn(session).when(invalidateDispatcherCacheImplSpy).getSession(resourceResolver);
        doReturn(true).when(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        doReturn(componentsConfiguration).when(invalidateDispatcherCacheImplSpy).getCommerceProperties(resourceResolver, "storePath");
        doThrow(new CacheInvalidationException("Test exception")).when(invalidateDispatcherCacheImplSpy)
            .getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());

        invalidateDispatcherCacheImplSpy.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport).getResource(resourceResolver, "/content/path");
        verify(resource).getValueMap();
        verify(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        verify(invalidateDispatcherCacheImplSpy).getCommerceProperties(resourceResolver, "storePath");
        verify(logger).error(eq("Error invalidating cache: {}"), eq("Test exception"), any(CacheInvalidationException.class));
    }

    @Test
    public void testInvalidateCache_FlushCacheThrowsException_ShouldLogError() throws Exception {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("storePath");

        InvalidateDispatcherCacheImpl invalidateDispatcherCacheImplSpy = spy(invalidateDispatcherCacheImpl);
        doReturn(session).when(invalidateDispatcherCacheImplSpy).getSession(resourceResolver);
        doReturn(true).when(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        doReturn(new String[] { "/content/page1", "/content/page2" }).when(invalidateDispatcherCacheImplSpy)
            .getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());
        doReturn(componentsConfiguration).when(invalidateDispatcherCacheImplSpy).getCommerceProperties(resourceResolver, "storePath");
        doThrow(new CacheInvalidationException("Test exception")).when(invalidateDispatcherCacheImplSpy).flushCache(anyString());

        invalidateDispatcherCacheImplSpy.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport).getResource(resourceResolver, "/content/path");
        verify(resource).getValueMap();
        verify(invalidateDispatcherCacheImplSpy).isValid(valueMap, resourceResolver, "storePath");
        verify(invalidateDispatcherCacheImplSpy).getAllInvalidPaths(any(), any(), any(), anyString(), anyMap());
        verify(invalidateDispatcherCacheImplSpy).flushCache("/content/page1");
        verify(invalidateDispatcherCacheImplSpy).flushCache("/content/page2");
        verify(logger, times(2)).error(eq("Error flushing cache for path {}: {}"), anyString(), eq("Test exception"));
    }

    @Test
    public void testGetDynamicProperties_WithValidAttributes() {
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        valueMap.put("dynamic_property1", new String[] { "value1", "value2" });

        when(invalidateCacheRegistry.getAttributes()).thenReturn(Collections.singleton("dynamic_property1"));
        when(invalidateCacheRegistry.get("dynamic_property1")).thenReturn(mock(DispatcherCacheInvalidationStrategy.class));

        Map<String, String[]> result = invalidateDispatcherCacheImpl.getDynamicProperties(valueMap);

        assertEquals(1, result.size());
        assertArrayEquals(new String[] { "value1", "value2" }, result.get("dynamic_property1"));
    }

    @Test
    public void testGetSession_ValidSession() throws Exception {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        Session result = invalidateDispatcherCacheImpl.getSession(resourceResolver);
        assertNotNull(result);
    }

    @Test(expected = CacheInvalidationException.class)
    public void testGetSession_InvalidSession() throws Exception {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(null);
        invalidateDispatcherCacheImpl.getSession(resourceResolver);
    }

    @Test
    public void testGetCommerceProperties() {
        when(invalidateCacheSupport.getCommerceProperties(resourceResolver, "storePath")).thenReturn(componentsConfiguration);

        ComponentsConfiguration result = invalidateDispatcherCacheImpl.getCommerceProperties(resourceResolver, "storePath");

        assertNotNull(result);
    }

    @Test
    public void testGetAllInvalidPaths() throws Exception {
        // Set up the mocks
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath()).thenReturn("/content/page");
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getContainingPage(any(Resource.class))).thenReturn(page);
        when(page.getPath()).thenReturn("/content/page");

        // Create dynamic properties
        Map<String, String[]> dynamicProperties = new HashMap<>();
        dynamicProperties.put("property1", new String[] { "value1", "value2" });

        // Set up the GraphqlClient mock
        when(client.execute(any(GraphqlRequest.class), any(Type.class), any(Type.class))).thenReturn(graphqlResponse);
        when(graphqlResponse.getData()).thenReturn(Collections.singletonMap("key", "value"));

        // Ensure invalidateCacheRegistry.getPathsToInvalidate returns a valid array
        when(invalidateCacheRegistry.getPathsToInvalidate(anyString(), any(Page.class), any(ResourceResolver.class), anyMap(), anyString()))
            .thenReturn(new String[] { "/content/page" });

        // Call the method to test
        String[] invalidPaths = invalidateDispatcherCacheImpl.getAllInvalidPaths(
            session, resourceResolver, client, "storePath", dynamicProperties);

        // Verify the results
        assertNotNull(invalidPaths);
        assertEquals(1, invalidPaths.length);
        assertEquals("/content/page", invalidPaths[0]);
    }

    @Test
    public void testGetPathsToInvalidate() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        Page page = mock(Page.class);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(mock(PageManager.class));
        when(resourceResolver.adaptTo(PageManager.class).getPage("storePath")).thenReturn(page);
        when(invalidateCacheRegistry.getPathsToInvalidate(anyString(), any(Page.class), any(ResourceResolver.class), anyMap(), anyString()))
            .thenReturn(new String[] { "/content/path1", "/content/path2" });

        String[] result = invalidateDispatcherCacheImpl.getPathsToInvalidate(resourceResolver, data, "storePath", "key");
        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    public void testGetGraphqlResponseData() {
        GraphqlClient client = graphqlClient;
        when(client.execute(any(), any(), any())).thenReturn(graphqlResponse);
        Map<String, Object> result = invalidateDispatcherCacheImpl.getGraphqlResponseData(client, "query");
        assertNotNull(result);
    }

    @Test
    public void testCheckProperty_ValidProperty() {
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        valueMap.put("key", "value");
        Map<String, Object> properties = new HashMap<>();
        properties.put("class", String.class);

        boolean result = invalidateDispatcherCacheImpl.checkProperty(valueMap, "key", properties);
        assertTrue(result);
    }

    @Test
    public void testCreateJsonData() {
        Map<String, Map<String, Object>> result = invalidateDispatcherCacheImpl.createJsonData(resourceResolver, "storePath");
        assertNotNull(result);
    }

    @Test
    public void testGetQueryResult_ValidResult() throws Exception {

        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath("content")).thenReturn("/content/page/jcr:content");

        String[] result = invalidateDispatcherCacheImpl.getQueryResult(query);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("/content/page.html", result[0]);
    }

    @Test
    public void testExtractPagePath() {
        String result = invalidateDispatcherCacheImpl.extractPagePath("/content/page/jcr:content");
        assertEquals("/content/page", result);
    }

    @Test
    public void testIsValidWithFunctionProperties() {
        Map<String, Map<String, Object>> jsonData = new HashMap<>();
        Map<String, Object> property = new HashMap<>();
        property.put("isFunction", true);
        property.put("method", "testMethod");
        property.put("parameterTypes", new Class<?>[] {});
        property.put("args", new Object[] {});
        jsonData.put("testKey", property);

        InvalidateDispatcherCacheImpl invalidateDispatcherCacheImplSpy = spy(invalidateDispatcherCacheImpl);
        doReturn(jsonData).when(invalidateDispatcherCacheImplSpy).createJsonData(resourceResolver, "testStorePath");
        doReturn(true).when(invalidateDispatcherCacheImplSpy).invokeFunction(property);

        boolean result = invalidateDispatcherCacheImplSpy.isValid(valueMap, resourceResolver, "testStorePath");
        assertTrue(result);
    }

    @Test
    public void testGetCorrespondingPageProperties() {
        String storePath = "/content/store";
        String propertyName = "propertyName";

        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage(storePath)).thenReturn(page);
        when(page.getProperties()).thenReturn(valueMap);
        when(valueMap.get(propertyName, String.class)).thenReturn("propertyValue");

        String result = invalidateDispatcherCacheImpl.getCorrespondingPageProperties(resourceResolver, storePath, propertyName);
        assertEquals("propertyValue", result);
    }

    @Test
    public void testGetSqlQuery() throws Exception {
        String sql2Query = "SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE('/content')";

        // Ensure session is properly mocked
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(sql2Query, Query.JCR_SQL2)).thenReturn(query);

        Query result = invalidateDispatcherCacheImpl.getSqlQuery(session, sql2Query);
        assertEquals(query, result);
    }

    @Test
    public void testInvokeFunction() throws Exception {
        // Create a map to represent the properties
        Map<String, Object> properties = new HashMap<>();
        properties.put("method", "getCorrespondingPageProperties");
        properties.put("parameterTypes", new Class<?>[] { ResourceResolver.class, String.class, String.class });
        properties.put("args", new Object[] { resourceResolver, "/content/store", "propertyName" });

        // Create a spy of the InvalidateDispatcherCacheImpl class
        InvalidateDispatcherCacheImpl invalidateDispatcherCacheImplSpy = spy(invalidateDispatcherCacheImpl);

        // Mock the getCorrespondingPageProperties method to return a value
        doReturn("propertyValue").when(invalidateDispatcherCacheImplSpy).getCorrespondingPageProperties(any(ResourceResolver.class),
            anyString(), anyString());

        // Call the invokeFunction method
        boolean result = invalidateDispatcherCacheImplSpy.invokeFunction(properties);

        // Verify the result
        assertTrue(result);
        verify(invalidateDispatcherCacheImplSpy).getCorrespondingPageProperties(resourceResolver, "/content/store", "propertyName");
    }
}
