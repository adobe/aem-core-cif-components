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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InvalidateCacheRegistryTest {

    private InvalidateCacheRegistry registry;

    @Mock
    private CacheInvalidationStrategy mockStrategy;

    @Mock
    private CacheInvalidationStrategy mockStrategy2;

    @Mock
    private DispatcherCacheInvalidationStrategy mockDispatcherStrategy;

    @Mock
    private DispatcherCacheInvalidationContext mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        registry = new InvalidateCacheRegistry();
    }

    @Test
    public void testBindInvalidateCache() {
        // Setup
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "testAttribute");
        properties.put("component.name", "testComponent");
        Mockito.when(mockStrategy.getPattern()).thenReturn("/content/test");
        Mockito.when(mockStrategy.getInvalidationRequestType()).thenReturn("testAttribute");

        // Test
        registry.bindInvalidateCache(mockStrategy, properties);

        // Verify
        Set<String> patterns = registry.getPattern("testAttribute");
        assertEquals(1, patterns.size());
        assertTrue(patterns.contains("/content/test"));
        Mockito.verify(mockStrategy, Mockito.times(1)).getPattern();
    }

    @Test
    public void testBindInvalidateDispatcherCache() {
        // Setup
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "testDispatcherAttribute");
        properties.put("component.name", "testDispatcherComponent");
        List<String> expectedPaths = Arrays.asList("/content/dispatcher/test");
        Mockito.when(mockDispatcherStrategy.getPathsToInvalidate(Mockito.any())).thenReturn(expectedPaths);
        Mockito.when(mockContext.getAttributeData()).thenReturn(Collections.singletonList("testDispatcherAttribute"));
        Mockito.when(mockDispatcherStrategy.getInvalidationRequestType()).thenReturn("testDispatcherAttribute");

        // Test
        registry.bindInvalidateDispatcherCache(mockDispatcherStrategy, properties);

        // Verify
        List<String> paths = registry.getPathsToInvalidate(mockContext);
        assertEquals(expectedPaths, paths);
        Mockito.verify(mockDispatcherStrategy, Mockito.times(1)).getPathsToInvalidate(mockContext);
    }

    @Test
    public void testUnbindCache() {
        // Setup
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "testAttribute");
        properties.put("component.name", "testComponent");

        registry.bindInvalidateCache(mockStrategy, properties);

        // Test
        registry.unbindInvalidateCache(mockStrategy, properties);

        // Verify
        Set<String> patterns = registry.getPattern("testAttribute");
        assertTrue(patterns.isEmpty());
    }

    @Test
    public void testGetAttributes() {
        // Setup
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attr1");
        Mockito.when(mockStrategy.getInvalidationRequestType()).thenReturn("attr1");

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attr2");
        Mockito.when(mockStrategy2.getInvalidationRequestType()).thenReturn("attr2");

        // Test
        registry.bindInvalidateCache(mockStrategy, properties1);
        registry.bindInvalidateCache(mockStrategy2, properties2);

        // Verify
        Set<String> attributes = registry.getAttributes();
        assertEquals(2, attributes.size());
        assertTrue(attributes.contains("attr1"));
        assertTrue(attributes.contains("attr2"));
    }

    @Test
    public void testGetEmptyPathsWhenNoStrategiesRegistered() {
        // Setup
        Mockito.when(mockContext.getAttributeData()).thenReturn(Collections.singletonList("nonExistentAttribute"));

        // Test & Verify
        List<String> paths = registry.getPathsToInvalidate(mockContext);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetAttributeStrategies() {
        // Setup
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "testAttribute");
        Mockito.when(mockStrategy.getInvalidationRequestType()).thenReturn("testAttribute");

        // Test
        registry.bindInvalidateCache(mockStrategy, properties);

        // Verify
        AttributeStrategies strategies = registry.getAttributeStrategies("testAttribute");
        assertTrue(strategies != null);
    }
}
