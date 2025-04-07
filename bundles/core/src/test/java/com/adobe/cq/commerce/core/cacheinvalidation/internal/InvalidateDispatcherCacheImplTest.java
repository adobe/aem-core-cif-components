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
import java.util.*;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateDispatcherCacheImplTest {

    private InvalidateDispatcherCacheImpl dispatcherCache;

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private InvalidateCacheRegistry invalidateCacheRegistry;

    @Mock
    private Resource resource;

    @Mock
    private PageManager pageManager;

    @Mock
    private Page page;

    @Mock
    private ValueMap valueMap;

    @Mock
    private HttpClientProvider httpClientProvider;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private DispatcherCacheInvalidationStrategy strategy;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private UrlProviderImpl urlProvider;

    @InjectMocks
    private InvalidateDispatcherCacheImpl invalidateDispatcherCache;

    private ValueMap properties;

    @Mock
    private InvalidationStrategies invalidationStrategies;

    @Mock
    private StrategyInfo strategyInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        invalidateDispatcherCache = new InvalidateDispatcherCacheImpl();

        // Initialize the component
        dispatcherCache = new InvalidateDispatcherCacheImpl();
        setField(dispatcherCache, "invalidateCacheSupport", invalidateCacheSupport);
        setField(dispatcherCache, "invalidateCacheRegistry", invalidateCacheRegistry);
        setField(dispatcherCache, "urlProvider", urlProvider);

        // Setup basic mocks
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(any(), anyString())).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getContainingPage(any(Resource.class))).thenReturn(page);

        // Setup basic properties
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("/content/store");
        properties = mock(ValueMap.class);
        when(invalidationStrategies.getStrategies(false)).thenReturn(Collections.singletonList(strategyInfo));
        when(strategyInfo.getStrategy()).thenReturn(strategy);

        // Ensure invalidateCacheRegistry is not null
        when(invalidateCacheRegistry.getInvalidationTypes()).thenReturn(Collections.singleton("invalidType"));
        when(invalidateCacheRegistry.getInvalidationStrategies("invalidType")).thenReturn(invalidationStrategies);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInvalidateCacheResourceNotFound() {
        when(invalidateCacheSupport.getResource(any(), anyString())).thenReturn(null);
        dispatcherCache.invalidateCache("/content/path");
        verify(invalidateCacheRegistry, never()).getInvalidationTypes();
    }

    @Test
    public void testInvalidateCacheWithException() {
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenThrow(new RuntimeException("Test exception"));
        dispatcherCache.invalidateCache("/content/path");
        verify(invalidateCacheRegistry, never()).getInvalidationTypes();
    }

    @Test
    public void testGetAllInvalidPaths() throws Exception {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        dynamicProperties.put("invalidType", new String[] { "value" });
        String[] result = dispatcherCache.getAllInvalidPaths(mock(ResourceResolver.class), mock(MagentoGraphqlClient.class),
            "/content/store", dynamicProperties).toArray(new String[0]);
        assertNotNull(result);
    }

    @Test
    public void testCheckProperty() {
        ValueMap valueMap = mock(ValueMap.class);
        when(valueMap.get("key", String.class)).thenReturn("value");
        Map<String, Object> properties = dispatcherCache.createProperty(false, String.class);
        assertTrue(dispatcherCache.checkProperty(valueMap, "key", properties));
    }

    @Test
    public void testInvokeFunction() throws Exception {
        Map<String, Object> properties = createFunctionProperty("someMethod", new Class<?>[] { String.class }, new Object[] { "arg" });
        dispatcherCache.invokeFunction(properties);
    }

    @Test
    public void testInvokeFunctionWithException() throws Exception {
        Map<String, Object> properties = createFunctionProperty("invalidMethod", new Class<?>[] { String.class }, new Object[] { "arg" });
        boolean result = dispatcherCache.invokeFunction(properties);
        assertFalse(result);
    }

    @Test
    public void testInvalidateCacheWithNullDispatcherBaseUrl() throws Exception {
        InvalidateCacheSupport invalidateCacheSupport = mock(InvalidateCacheSupport.class);
        when(invalidateCacheSupport.getDispatcherBaseUrl()).thenReturn(null);
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(mock(ResourceResolver.class));
        when(invalidateCacheSupport.getResource(any(), anyString())).thenReturn(mock(Resource.class));
        dispatcherCache.invalidateCache("/content/path");
    }

    @Test
    public void testInvalidateCacheWithInvalidDispatcherBasePath() throws Exception {
        InvalidateCacheSupport invalidateCacheSupport = mock(InvalidateCacheSupport.class);
        when(invalidateCacheSupport.getDispatcherBasePathForStorePath(anyString())).thenReturn("/invalid/path");
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(mock(ResourceResolver.class));
        when(invalidateCacheSupport.getResource(any(), anyString())).thenReturn(mock(Resource.class));
        dispatcherCache.invalidateCache("/content/path");
    }

    @Test
    public void testCheckPropertyWithEmptyValue() {
        ValueMap valueMap = mock(ValueMap.class);
        when(valueMap.get("key", String.class)).thenReturn(null);
        Map<String, Object> properties = dispatcherCache.createProperty(false, String.class);
        dispatcherCache.checkProperty(valueMap, "key", properties);
    }

    @Test
    public void testGetDynamicPropertiesWithNoInvalidType() {
        ValueMap properties = mock(ValueMap.class);
        InvalidateCacheRegistry invalidateCacheRegistry = mock(InvalidateCacheRegistry.class);
        when(invalidateCacheRegistry.getInvalidationTypes()).thenReturn(Collections.emptySet());
        Map<String, String[]> result = dispatcherCache.getDynamicProperties(properties);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetDynamicPropertiesWithValidInvalidType() {
        ValueMap properties = mock(ValueMap.class);
        when(properties.get("invalidType", String[].class)).thenReturn(new String[] { "value" });
        InvalidateCacheRegistry invalidateCacheRegistry = mock(InvalidateCacheRegistry.class);
        when(invalidateCacheRegistry.getInvalidationTypes()).thenReturn(Collections.singleton("invalidType"));
        when(invalidateCacheRegistry.getInvalidationStrategies("invalidType")).thenReturn(mock(InvalidationStrategies.class));
        Map<String, String[]> result = dispatcherCache.getDynamicProperties(properties);
        assertNotNull(result);
    }

    @Test
    public void testCreateJsonData() {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(resourceResolver.resolve(anyString())).thenReturn(null);
        Map<String, Map<String, Object>> jsonData = dispatcherCache.createJsonData(resourceResolver, "/path/to/store");
        assertNotNull(jsonData);
        assertEquals(4, jsonData.size());
        assertTrue(jsonData.containsKey("categoryPath"));
        assertTrue(jsonData.containsKey("productPath"));
    }

    @Test
    public void testIsValidEntryWithReflection() throws Exception {
        Map.Entry<String, String[]> entry = new AbstractMap.SimpleEntry<>("key", new String[] { "value" });
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValidEntry", Map.Entry.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(dispatcherCache, entry);
        assertTrue(result);
    }

    @Test
    public void testIsValidEntryWithEmptyValues() throws Exception {
        Map.Entry<String, String[]> entry = new AbstractMap.SimpleEntry<>("key", new String[] {});
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValidEntry", Map.Entry.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(dispatcherCache, entry);
        assertFalse(result);
    }

    @Test
    public void testIsValidEntry_NullEntry() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValidEntry", Map.Entry.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(dispatcherCache, (Map.Entry<String, String[]>) null);
        assertFalse(result);
    }

    @Test
    public void testShouldPerformFullCacheClear_withInvalidateAllFlag() throws Exception {
        ValueMap properties = mock(ValueMap.class);
        when(properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)).thenReturn(true);
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("shouldPerformFullCacheClear", ValueMap.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(dispatcherCache, properties);
        assertTrue(result);
    }

    @Test
    public void testShouldPerformFullCacheClear_withInvalidProperties() throws Exception {
        ValueMap properties = mock(ValueMap.class);
        when(properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)).thenReturn(true);
        when(dispatcherCache.isValid(properties, mock(ResourceResolver.class), "some/path")).thenReturn(false);
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("shouldPerformFullCacheClear", ValueMap.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(dispatcherCache, properties);
        assertTrue(result);
    }

    @Test
    public void testProcessAndConvertPaths() throws Exception {
        List<String> paths = Arrays.asList("/path1", "/path1/subpath", "/path2");
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("processAndConvertPaths", List.class);
        method.setAccessible(true);
        List<String> result = (List<String>) method.invoke(dispatcherCache, paths);
        assertNotNull(result);
    }

    @Test
    public void testFlushCacheForPaths() throws Exception {
        List<String> paths = Arrays.asList("/path1", "/path2");
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("flushCacheForPaths", List.class, String.class, String.class);
        method.setAccessible(true);
        method.invoke(dispatcherCache, paths, "http://localhost:80", "/original/path");
    }

    @Test
    public void testProcessInvalidationStrategy() throws Exception {
        Map.Entry<String, String[]> entry = mock(Map.Entry.class);
        when(entry.getKey()).thenReturn("testKey");
        when(entry.getValue()).thenReturn(new String[] { "value1", "value2" });
        MagentoGraphqlClient client = mock(MagentoGraphqlClient.class);

        List<String> invalidPaths = Arrays.asList("/path1", "/path2");
        when(strategy.getPathsToInvalidate(any(CacheInvalidationContext.class))).thenReturn(invalidPaths);

        // Ensure that getInvalidationStrategies does not return null
        when(invalidateCacheRegistry.getInvalidationStrategies(anyString())).thenReturn(invalidationStrategies);
        when(invalidationStrategies.getStrategies(false)).thenReturn(Collections.singletonList(new StrategyInfo(strategy, Collections
            .emptyMap(), false)));

        // Use reflection to access the private method
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod(
            "processInvalidationStrategy", Map.Entry.class, Page.class, ResourceResolver.class, String.class, MagentoGraphqlClient.class);
        method.setAccessible(true);

        // Invoke the private method
        Set<String> result = (Set<String>) method.invoke(dispatcherCache, entry, page, resourceResolver, "storePath", client);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("/path1"));
        assertTrue(result.contains("/path2"));
    }

    private Map<String, Object> createFunctionProperty(String method, Class<?>[] parameterTypes, Object[] args) {
        Map<String, Object> property = new HashMap<>();
        property.put("IS_FUNCTION", true);
        property.put("method", method);
        property.put("parameterTypes", parameterTypes);
        property.put("args", args);
        return property;
    }

    @Test
    public void testShouldPerformFullCacheClear() throws Exception {
        ValueMap properties = mock(ValueMap.class);
        when(properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)).thenReturn(true);
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("shouldPerformFullCacheClear", ValueMap.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(dispatcherCache, properties);
        assertTrue(result);
    }

    @Test
    public void testIsValid() throws Exception {
        ValueMap properties = mock(ValueMap.class);
        when(properties.get("key", String.class)).thenReturn(null);
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValid", ValueMap.class, ResourceResolver.class,
            String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(dispatcherCache, properties, mock(ResourceResolver.class), "some/path");
        assertFalse(result);
    }

    @Test
    public void testMagentoGraphqlClientNull() {
        when(invalidateCacheSupport.getResource(any(), anyString())).thenReturn(resource);
        when(resource.adaptTo(MagentoGraphqlClient.class)).thenReturn(null);
        dispatcherCache.invalidateCache("/content/path");
        verify(invalidateCacheRegistry, never()).getInvalidationTypes();
    }

    @Test
    public void testProcessAndConvertPath() throws Exception {
        List<String> paths = Arrays.asList("/path1", "/path1/subpath", "/path2");
        when(invalidateCacheSupport.convertUrlPath("/path1")).thenReturn("/path1");
        when(invalidateCacheSupport.convertUrlPath("/path1/subpath")).thenReturn("/path1/subpath");
        when(invalidateCacheSupport.convertUrlPath("/path2")).thenReturn("/path2");

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("processAndConvertPaths", List.class);
        method.setAccessible(true);
        List<String> result = (List<String>) method.invoke(dispatcherCache, paths);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("/path1"));
        assertTrue(result.contains("/path2"));
    }

}
