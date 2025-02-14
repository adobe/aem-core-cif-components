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

import java.lang.reflect.*;
import java.util.*;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.*;

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

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateDispatcherCacheImplTest {

    @Mock
    private SlingSettingsService slingSettingsService;

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private InvalidateCacheRegistry invalidateCacheRegistry;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private GraphqlClient graphqlClient;

    @Mock
    private Logger logger;

    @InjectMocks
    private InvalidateDispatcherCacheImpl invalidateDispatcherCacheImpl;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
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

    private Object invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(invalidateDispatcherCacheImpl, args);
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
    public void testInvalidateCache_ResourceIsNull_ShouldExitEarly() throws NoSuchMethodException {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(null);

        invalidateDispatcherCacheImpl.invalidateCache("/content/path");

        verify(invalidateCacheSupport).getServiceUserResourceResolver();
        verify(invalidateCacheSupport).getResource(resourceResolver, "/content/path");
        verify(resource, never()).getValueMap();

    }

    @Test
    public void testCreateProperty() throws Exception {
        Map<String, Object> result = (Map<String, Object>) invokePrivateMethod("createProperty", new Class<?>[] { boolean.class,
            Class.class }, false, String.class);
        assertNotNull(result);
        assertEquals(false, result.get("isFunction"));
        assertEquals(String.class, result.get("class"));
    }

    @Test
    public void testCreateFunctionProperty() throws Exception {
        Map<String, Object> result = (Map<String, Object>) invokePrivateMethod("createFunctionProperty", new Class<?>[] { String.class,
            Class[].class, Object[].class }, "testMethod", new Class<?>[] { String.class }, new Object[] { "testArg" });
        assertNotNull(result);
        assertEquals(true, result.get("isFunction"));
        assertEquals("testMethod", result.get("method"));
        assertArrayEquals(new Class<?>[] { String.class }, (Class<?>[]) result.get("parameterTypes"));
        assertArrayEquals(new Object[] { "testArg" }, (Object[]) result.get("args"));
    }

    @Test
    public void testGetPropertiesValue() throws Exception {
        ValueMap properties = new ValueMapDecorator(new HashMap<>());
        properties.put("key", "value");
        String result = (String) invokePrivateMethod("getPropertiesValue", new Class<?>[] { ValueMap.class, String.class, Class.class },
            properties, "key", String.class);
        assertEquals("value", result);
    }

    @Test
    public void testGetCorrespondingPageProperties() throws Exception {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Page page = mock(Page.class);
        ValueMap properties = new ValueMapDecorator(new HashMap<>());
        properties.put("propertyName", "propertyValue");
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(mock(PageManager.class));
        when(resourceResolver.adaptTo(PageManager.class).getPage("storePath")).thenReturn(page);
        when(page.getProperties()).thenReturn(properties);
        String result = (String) invokePrivateMethod("getCorrespondingPageProperties", new Class<?>[] { ResourceResolver.class,
            String.class, String.class }, resourceResolver, "storePath", "propertyName");
        assertEquals("propertyValue", result);
    }

    @Test
    public void testFormatList() throws Exception {
        String[] invalidCacheEntries = { "entry1", "entry2" };
        String result = (String) invokePrivateMethod("formatList", new Class<?>[] { String[].class, String.class, String.class },
            invalidCacheEntries, ",", "[%s]");
        assertEquals("[entry1],[entry2]", result);
    }

    @Test
    public void testExtractPagePath() throws Exception {
        String result = (String) invokePrivateMethod("extractPagePath", new Class<?>[] { String.class }, "/content/page/jcr:content");
        assertEquals("/content/page", result);
    }

    @Test
    public void testGetSession() throws Exception {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(mock(Session.class));
        Session result = (Session) invokePrivateMethod("getSession", new Class<?>[] { ResourceResolver.class }, resourceResolver);
        assertNotNull(result);
    }

    @Test
    public void testGetCorrespondingPageBasedOnEntries() throws Exception {
        // Mock dependencies
        Session session = mock(Session.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        PageManager pageManager = mock(PageManager.class);
        Page page = mock(Page.class);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage(anyString())).thenReturn(page);

        // Set up the entries and key
        String storePath = "storePath";
        String[] entries = { "entry1", "entry2" };
        String key = "key";

        // Invoke the private method
        String[] result = (String[]) invokePrivateMethod("getCorrespondingPageBasedOnEntries", new Class<?>[] { Session.class, String.class,
            String[].class, String.class }, session, storePath, entries, key);

        // Assert the result
        assertNotNull(result);
    }

    @Test
    public void testGetGraphqlResponseData() throws Exception {
        GraphqlClient client = mock(GraphqlClient.class);
        when(client.execute(any(), any(), any())).thenReturn(mock(GraphqlResponse.class));
        Map<String, Object> result = (Map<String, Object>) invokePrivateMethod("getGraphqlResponseData", new Class<?>[] {
            GraphqlClient.class, String.class }, client, "query");
        assertNotNull(result);
    }

    @Test
    public void testInvokeFunction() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("method", "testMethod");
        boolean result = (boolean) invokePrivateMethod("invokeFunction", new Class<?>[] { Map.class }, properties);
        assertFalse(result);
    }

    @Test
    public void testCheckProperty() throws Exception {
        ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
        Map<String, Object> properties = new HashMap<>();
        properties.put("class", String.class);
        boolean result = (boolean) invokePrivateMethod("checkProperty", new Class<?>[] { ValueMap.class, String.class, Map.class },
            valueMap, "key", properties);
        assertTrue(result);
    }

    @Test
    public void testCreateJsonData() throws Exception {
        Map<String, Map<String, Object>> result = (Map<String, Map<String, Object>>) invokePrivateMethod("createJsonData", new Class<?>[] {
            ResourceResolver.class, String.class }, resourceResolver, "storePath");
        assertNotNull(result);
    }

    @Test
    public void testInvalidateCacheWithException() {
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenThrow(new RuntimeException("Test exception"));

        invalidateDispatcherCacheImpl.invalidateCache("/content/test");

        verify(logger).error(eq("Error invalidating cache: {}"), eq("Test exception"), any(RuntimeException.class));
    }

    @Test
    public void testGetQueryResult() throws Exception {
        // Mock dependencies
        Query mockQuery = mock(Query.class);
        QueryResult mockQueryResult = mock(QueryResult.class);
        RowIterator mockRowIterator = mock(RowIterator.class);
        Row mockRow1 = mock(Row.class);
        Row mockRow2 = mock(Row.class);

        // Set up the expected results
        String[] expectedResult = new String[] {
            "/content/page1" + InvalidateCacheSupport.HTML_SUFFIX,
            "/content/page2" + InvalidateCacheSupport.HTML_SUFFIX
        };

        // Set up behavior for mocks
        when(mockQuery.execute()).thenReturn(mockQueryResult);
        when(mockQueryResult.getRows()).thenReturn(mockRowIterator);
        when(mockRowIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowIterator.nextRow()).thenReturn(mockRow1, mockRow2);
        when(mockRow1.getPath("content")).thenReturn("/content/page1/jcr:content");
        when(mockRow2.getPath("content")).thenReturn("/content/page2/jcr:content");

        // Use reflection to access the private method
        String[] result = (String[]) invokePrivateMethod("getQueryResult", new Class<?>[] { Query.class }, mockQuery);

        // Verify the expected results
        assertNotNull(result);
        assertArrayEquals(expectedResult, result);
    }

    @Test
    public void testGetSqlQuery_ValidQuery() throws Exception {
        // Mock dependencies
        Session mockSession = mock(Session.class);
        QueryManager mockQueryManager = mock(QueryManager.class);
        Query mockQuery = mock(Query.class);
        Workspace mockWorkspace = mock(Workspace.class);

        // Set up behavior for mocks
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(mockQueryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(mockQuery);

        // Invoke the private method
        Query result = (Query) invokePrivateMethod("getSqlQuery", new Class<?>[] { Session.class, String.class }, mockSession,
            "SELECT * FROM [nt:base]");

        // Verify the expected results
        assertNotNull(result);
        assertEquals(mockQuery, result);
    }

    @Test
    public void testGetSqlQuery_InvalidQuery() throws Exception {
        // Mock dependencies
        Session mockSession = mock(Session.class);
        QueryManager mockQueryManager = mock(QueryManager.class);
        Workspace mockWorkspace = mock(Workspace.class);

        // Set up behavior for mocks
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(mockQueryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenThrow(new RuntimeException("Invalid query"));

        // Invoke the private method and verify exception
        try {
            invokePrivateMethod("getSqlQuery", new Class<?>[] { Session.class, String.class }, mockSession, "INVALID QUERY");
            fail("Expected CacheInvalidationException to be thrown");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof CacheInvalidationException);
            assertEquals("Error creating SKU-based SQL2 query", cause.getMessage());
        }
    }
}
