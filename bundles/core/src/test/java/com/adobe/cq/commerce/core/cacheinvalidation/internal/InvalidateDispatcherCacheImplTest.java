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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateDispatcherCacheImplTest {

    private InvalidateDispatcherCacheImpl invalidateDispatcherCacheImpl;
    private SlingSettingsService slingSettingsService;
    private InvalidateCacheSupport invalidateCacheSupport;
    private InvalidateCacheRegistry invalidateCacheRegistry;
    private ResourceResolver resourceResolver;
    private Logger logger;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        invalidateDispatcherCacheImpl = new InvalidateDispatcherCacheImpl();
        slingSettingsService = mock(SlingSettingsService.class);
        invalidateCacheSupport = mock(InvalidateCacheSupport.class);
        invalidateCacheRegistry = mock(InvalidateCacheRegistry.class);
        resourceResolver = mock(ResourceResolver.class);
        logger = mock(Logger.class);

        // Set the invalidateCacheRegistry field
        Field invalidateCacheRegistryField = InvalidateDispatcherCacheImpl.class.getDeclaredField("invalidateCacheRegistry");
        invalidateCacheRegistryField.setAccessible(true);
        invalidateCacheRegistryField.set(invalidateDispatcherCacheImpl, invalidateCacheRegistry);
    }

    private Object invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(invalidateDispatcherCacheImpl, args);
    }

    @Test
    public void testInvalidateCacheOnAuthor() throws NoSuchFieldException, IllegalAccessException {
        Field slingSettingsServiceField = InvalidateDispatcherCacheImpl.class.getDeclaredField("slingSettingsService");
        slingSettingsServiceField.setAccessible(true);
        slingSettingsServiceField.set(invalidateDispatcherCacheImpl, slingSettingsService);

        Field invalidateCacheSupportField = InvalidateDispatcherCacheImpl.class.getDeclaredField("invalidateCacheSupport");
        invalidateCacheSupportField.setAccessible(true);
        invalidateCacheSupportField.set(invalidateDispatcherCacheImpl, invalidateCacheSupport);

        Field invalidateCacheRegistryField = InvalidateDispatcherCacheImpl.class.getDeclaredField("invalidateCacheRegistry");
        invalidateCacheRegistryField.setAccessible(true);
        invalidateCacheRegistryField.set(invalidateDispatcherCacheImpl, invalidateCacheRegistry);

        when(slingSettingsService.getRunModes()).thenReturn(new HashSet<>(Arrays.asList("author")));

        invalidateDispatcherCacheImpl.invalidateCache("path");

        verify(slingSettingsService).getRunModes();
        verifyNoMoreInteractions(invalidateCacheSupport, invalidateCacheRegistry, resourceResolver);
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
    public void testFlushCache() throws Exception {
        // Get the LOGGER field
        Field loggerField = InvalidateDispatcherCacheImpl.class.getDeclaredField("LOGGER");
        loggerField.setAccessible(true);

        // Remove the final modifier from the LOGGER field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(loggerField, loggerField.getModifiers() & ~Modifier.FINAL);

        // Set the mocked logger in the InvalidateDispatcherCacheImpl instance
        loggerField.set(null, logger);

        // Use the invokePrivateMethod helper to access the private method
        invokePrivateMethod("flushCache", new Class<?>[] { String.class }, "handle");

        // Verify that the logger was called with the expected message
        verify(logger, times(1)).info(anyString(), anyString());
    }

    // Corrected testGetPage
    @Test
    public void testGetPage() throws Exception {
        // Ensure all mocks are correctly stubbed
        PageManager pageManager = mock(PageManager.class);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage(anyString())).thenReturn(mock(Page.class));

        // Invoke the method and assert the result
        Page result = (Page) invokePrivateMethod("getPage", new Class<?>[] { ResourceResolver.class, String.class }, resourceResolver,
            "path");
        assertNotNull(result);
    }
}
