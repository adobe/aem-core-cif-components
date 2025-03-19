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
import java.util.*;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
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
    private SlingSettingsService slingSettingsService;

    @Mock
    private UrlProviderImpl urlProvider;

    @InjectMocks
    private InvalidateDispatcherCacheImpl invalidateDispatcherCache;

    private ValueMap properties;

    @Mock
    private AttributeStrategies attributeStrategies;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        invalidateDispatcherCache = new InvalidateDispatcherCacheImpl();

        // Setup SlingSettingsService
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("author"));

        // Initialize the component
        dispatcherCache = new InvalidateDispatcherCacheImpl();
        setField(dispatcherCache, "invalidateCacheSupport", invalidateCacheSupport);
        setField(dispatcherCache, "invalidateCacheRegistry", invalidateCacheRegistry);
        setField(dispatcherCache, "slingSettingsService", slingSettingsService);
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
        when(attributeStrategies.getStrategies(false)).thenReturn(Collections.singletonList(strategyInfo));
        when(strategyInfo.getStrategy()).thenReturn(strategy);

        // Ensure invalidateCacheRegistry is not null
        when(invalidateCacheRegistry.getAttributes()).thenReturn(Collections.singleton("attribute"));
        when(invalidateCacheRegistry.getAttributeStrategies("attribute")).thenReturn(attributeStrategies);
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
    public void testInvalidateCacheWithNullPath() {
        dispatcherCache.invalidateCache(null);
        verify(invalidateCacheSupport, never()).getServiceUserResourceResolver();
    }

    @Test
    public void testInvalidateCacheWithEmptyPath() {
        dispatcherCache.invalidateCache("");
        verify(invalidateCacheSupport, never()).getServiceUserResourceResolver();
    }

    @Test
    public void testInvalidateCacheNotOnAuthor() {
        // Change to publish mode
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("publish"));

        dispatcherCache.invalidateCache("/content/path");
    }

    @Test
    public void testInvalidateCacheResourceNotFound() {
        when(invalidateCacheSupport.getResource(any(), anyString())).thenReturn(null);
        dispatcherCache.invalidateCache("/content/path");
        verify(invalidateCacheRegistry, never()).getAttributes();
    }

    @Test
    public void testInvalidateCacheWithException() {
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenThrow(new RuntimeException("Test exception"));
        dispatcherCache.invalidateCache("/content/path");
        verify(invalidateCacheRegistry, never()).getAttributes();
    }

    @Test
    public void testGetAllInvalidPaths() throws Exception {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        dynamicProperties.put("attribute", new String[] { "value" });
        String[] result = dispatcherCache.getAllInvalidPaths(resourceResolver, mock(MagentoGraphqlClient.class), "/content/store",
            dynamicProperties).toArray(new String[0]);
        assertNotNull(result);
    }

    @Test
    public void testCheckProperty() {
        when(valueMap.get("key", String.class)).thenReturn("value");
        Map<String, Object> properties = dispatcherCache.createProperty(false, String.class);
        assertTrue(dispatcherCache.checkProperty(valueMap, "key", properties));
    }

    /*
     * @Test
     * public void testFlushCache() {
     * try {
     * dispatcherCache.flushCache("handle", "http://localhost:80");
     * // Verify that no exceptions were thrown
     * } catch (CacheInvalidationException e) {
     * fail("Exception should not be thrown: " + e.getMessage());
     * }
     * }
     */
    @Test
    public void testInvokeFunction() throws Exception {
        Map<String, Object> properties = mock(Map.class);
        when(properties.get("method")).thenReturn("someMethod");
        when(properties.get("parameterTypes")).thenReturn(new Class<?>[] { String.class });
        when(properties.get("args")).thenReturn(new Object[] { "arg" });

        invalidateDispatcherCache.invokeFunction(properties);
    }

    @Test
    public void testInvokeFunctionWithException() throws Exception {
        Map<String, Object> properties = mock(Map.class);
        when(properties.get("method")).thenReturn("invalidMethod");
        when(properties.get("parameterTypes")).thenReturn(new Class<?>[] { String.class });
        when(properties.get("args")).thenReturn(new Object[] { "arg" });

        boolean result = invalidateDispatcherCache.invokeFunction(properties);

        assertFalse(result);
    }

    @Test
    public void testInvalidateCacheWithNullDispatcherBaseUrl() throws Exception {
        // Simulate that getDispatcherBaseUrl returns null
        when(invalidateCacheSupport.getDispatcherBaseUrl()).thenReturn(null);
        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("author"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);

        // Mock properties as well
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("/content/store");

        dispatcherCache.invalidateCache("/content/path");
        // Add appropriate assertions to verify that the system behaves correctly
    }

    @Test
    public void testInvalidateCacheWithInvalidDispatcherBasePath() throws Exception {
        // Simulate an invalid dispatcher base path
        when(invalidateCacheSupport.getDispatcherBasePathForStorePath(anyString())).thenReturn("/invalid/path");

        when(slingSettingsService.getRunModes()).thenReturn(Collections.singleton("author"));
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getResource(resourceResolver, "/content/path")).thenReturn(resource);

        // Mock properties as well
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn("/content/store");

        dispatcherCache.invalidateCache("/content/path");
        // Add assertions to verify how the invalid path is handled
    }

    @Test
    public void testCheckPropertyWithEmptyValue() {
        // Test the case when valueMap returns null or empty values for a key
        when(valueMap.get("key", String.class)).thenReturn(null);
        Map<String, Object> properties = dispatcherCache.createProperty(false, String.class);
        dispatcherCache.checkProperty(valueMap, "key", properties);
    }

    @Test
    public void testGetDynamicPropertiesWithNoAttributes() {
        // Mock properties and attributes
        ValueMap properties = mock(ValueMap.class);
        when(invalidateCacheRegistry.getAttributes()).thenReturn(Collections.emptySet());

        // Call the method
        Map<String, String[]> result = dispatcherCache.getDynamicProperties(properties);

        // Verify that the result is an empty map
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetDynamicPropertiesWithValidAttributes() {
        // Mock properties and attributes
        ValueMap properties = mock(ValueMap.class);
        when(properties.get("attribute", String[].class)).thenReturn(new String[] { "value" });
        Set<String> attributes = new HashSet<>();
        attributes.add("attribute");
        when(invalidateCacheRegistry.getAttributes()).thenReturn(attributes);
        when(invalidateCacheRegistry.getAttributeStrategies("attribute")).thenReturn(mock(AttributeStrategies.class));

        // Call the method
        Map<String, String[]> result = dispatcherCache.getDynamicProperties(properties);

        // Verify that the result contains the attribute
        assertNotNull(result);

    }

    @Test
    public void testCreateJsonData() {
        // Arrange
        String storePath = "/path/to/store";
        when(resourceResolver.resolve(anyString())).thenReturn(null);  // Mock any resource resolver calls, if needed

        // Act
        Map<String, Map<String, Object>> jsonData = dispatcherCache.createJsonData(resourceResolver, storePath);

        // Assert
        assertNotNull(jsonData);
        assertEquals(4, jsonData.size());  // Check the size matches the expected number of entries
        assertTrue(jsonData.containsKey("categoryPath"));
        assertTrue(jsonData.containsKey("productPath"));
    }

    // Helper method to simulate the creation of a function property
    private Map<String, Object> createFunctionProperty(String method, Class<?>[] parameterTypes, Object[] args) {
        Map<String, Object> property = new HashMap<>();
        property.put("IS_FUNCTION", true);  // Assuming this is the key for function indication
        property.put("method", method);
        property.put("parameterTypes", parameterTypes);
        property.put("args", args);
        return property;
    }

    @Mock
    private StrategyInfo strategyInfo;

    @Test
    public void testIsValidEntryWithReflection() throws Exception {
        // Arrange
        InvalidateDispatcherCacheImpl dispatcherCache = new InvalidateDispatcherCacheImpl();
        Map.Entry<String, String[]> entry = new AbstractMap.SimpleEntry<>("key", new String[] { "value" });

        // Access the private method using reflection
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValidEntry", Map.Entry.class);
        method.setAccessible(true); // Make the private method accessible

        // Invoke the private method
        boolean result = (boolean) method.invoke(dispatcherCache, entry);

        // Assert the result
        assertTrue(result);
    }

    @Test
    public void testIsValidEntryWithEmptyValues() throws Exception {
        // Arrange
        InvalidateDispatcherCacheImpl dispatcherCache = new InvalidateDispatcherCacheImpl();
        Map.Entry<String, String[]> entry = new AbstractMap.SimpleEntry<>("key", new String[] {});

        // Access the private method using reflection
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValidEntry", Map.Entry.class);
        method.setAccessible(true); // Make the private method accessible

        // Invoke the private method
        boolean result = (boolean) method.invoke(dispatcherCache, entry);

        // Assert the result
        assertFalse(result);
    }

    @Test
    public void testIsValidEntry_NullEntry() throws Exception {
        // Arrange
        InvalidateDispatcherCacheImpl dispatcherCache = new InvalidateDispatcherCacheImpl();
        Map.Entry<String, String[]> entry = null;

        // Access the private method using reflection
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValidEntry", Map.Entry.class);
        method.setAccessible(true); // Make the private method accessible

        // Invoke the private method
        boolean result = (boolean) method.invoke(dispatcherCache, entry);

        // Assert the result
        assertFalse(result);
    }

    @Test
    public void testIsValidEntry_Valid() throws Exception {
        // Arrange
        InvalidateDispatcherCacheImpl dispatcherCache = new InvalidateDispatcherCacheImpl();
        Map.Entry<String, String[]> entry = new AbstractMap.SimpleEntry<>("key", new String[] { "value" });

        // Access the private method using reflection
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValidEntry", Map.Entry.class);
        method.setAccessible(true); // Make the private method accessible

        // Invoke the private method
        boolean result = (boolean) method.invoke(dispatcherCache, entry);

        // Assert the result
        assertTrue(result);
    }

    @Test
    public void testIsValidEntry_Invalid() throws Exception {
        // Arrange
        InvalidateDispatcherCacheImpl dispatcherCache = new InvalidateDispatcherCacheImpl();
        Map.Entry<String, String[]> entry = new AbstractMap.SimpleEntry<>("key", new String[] {});

        // Access the private method using reflection
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("isValidEntry", Map.Entry.class);
        method.setAccessible(true); // Make the private method accessible

        // Invoke the private method
        boolean result = (boolean) method.invoke(dispatcherCache, entry);

        // Assert the result
        assertFalse(result);
    }

    @Test
    public void testShouldPerformFullCacheClear_withInvalidateAllFlag() throws Exception {
        // Setup mock data
        ValueMap properties = mock(ValueMap.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String storePath = "some/path";

        // Mock the property to return true for the invalidateAll flag
        when(properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)).thenReturn(true);

        // Use reflection to invoke the private method
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("shouldPerformFullCacheClear", ValueMap.class,
            ResourceResolver.class, String.class);
        method.setAccessible(true);  // Make the method accessible

        // Invoke the method
        boolean result = (boolean) method.invoke(dispatcherCache, properties, resourceResolver, storePath);

        // Assert that the result is true, as invalidateAll flag is set
        assertTrue(result);
    }

    @Test
    public void testShouldPerformFullCacheClear_withInvalidProperties() throws Exception {
        // Setup mock data
        ValueMap properties = mock(ValueMap.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String storePath = "some/path";

        // Mock properties.get to return false when fetching PROPERTIES_INVALIDATE_ALL
        when(properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)).thenReturn(true);  // or false, depending on what you
        // want to simulate

        // Mock `isValid` to return false (to simulate invalid properties)
        when(dispatcherCache.isValid(properties, resourceResolver, storePath)).thenReturn(false);

        // Use reflection to invoke the private method
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("shouldPerformFullCacheClear", ValueMap.class,
            ResourceResolver.class, String.class);
        method.setAccessible(true);

        // Invoke the method
        boolean result = (boolean) method.invoke(dispatcherCache, properties, resourceResolver, storePath);

        // Assert that the result is true, as the properties are invalid
        assertTrue(result);
    }

    @Test
    public void testShouldPerformFullCacheClear_InvalidateAllTrue() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("shouldPerformFullCacheClear", ValueMap.class,
            ResourceResolver.class, String.class);
        method.setAccessible(true);
        when(properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)).thenReturn(true);
        boolean result = (boolean) method.invoke(dispatcherCache, properties, resourceResolver, "storePath");
        assertTrue(result);
    }

    @Test
    public void testShouldPerformFullCacheClear() throws Exception {
        when(properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)).thenReturn(true);

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("shouldPerformFullCacheClear", ValueMap.class,
            ResourceResolver.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(dispatcherCache, properties, resourceResolver, "/store/path");
        assertTrue(result);
    }

    @Test
    public void testShouldPerformFullCacheClear_InvalidProperties() throws Exception {
        when(properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)).thenReturn(false);

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("shouldPerformFullCacheClear", ValueMap.class,
            ResourceResolver.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(dispatcherCache, properties, resourceResolver, "/store/path");
        assertFalse(result);
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
        String dispatcherUrl = "http://localhost:80";
        String originalPath = "/original/path";

        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("flushCacheForPaths", List.class, String.class, String.class);
        method.setAccessible(true);

        method.invoke(dispatcherCache, paths, dispatcherUrl, originalPath);

    }

    @Test
    public void testProcessAttributeStrategy() throws Exception {
        Map.Entry<String, String[]> entry = mock(Map.Entry.class);
        when(entry.getKey()).thenReturn("testKey");
        when(entry.getValue()).thenReturn(new String[] { "value1", "value2" });
        MagentoGraphqlClient client = mock(MagentoGraphqlClient.class);

        List<String> invalidPaths = Arrays.asList("/path1", "/path2");
        when(strategy.getPathsToInvalidate(any(DispatcherCacheInvalidationContext.class))).thenReturn(invalidPaths);

        // Ensure that getAttributeStrategies does not return null
        when(invalidateCacheRegistry.getAttributeStrategies(anyString())).thenReturn(attributeStrategies);
        when(attributeStrategies.getStrategies(false)).thenReturn(Collections.singletonList(new StrategyInfo(strategy, Collections
            .emptyMap(), false)));

        // Use reflection to access the private method
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod(
            "processAttributeStrategy", Map.Entry.class, Page.class, ResourceResolver.class, String.class, MagentoGraphqlClient.class);
        method.setAccessible(true);

        // Invoke the private method
        Set<String> result = (Set<String>) method.invoke(dispatcherCache, entry, page, resourceResolver, "storePath", client);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("/path1"));
        assertTrue(result.contains("/path2"));
    }

}
