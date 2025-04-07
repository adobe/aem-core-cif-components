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

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;
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
    private CacheInvalidationContext mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        registry = new InvalidateCacheRegistry();
    }

    @Test
    public void testGetInvalidationTypes() {
        // Setup
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_TYPE_PARAMETER, "attr1");
        Mockito.when(mockStrategy.getInvalidationRequestType()).thenReturn("attr1");

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_TYPE_PARAMETER, "attr2");
        Mockito.when(mockStrategy2.getInvalidationRequestType()).thenReturn("attr2");

        // Test
        registry.bindInvalidateCache(mockStrategy, properties1);
        registry.bindInvalidateCache(mockStrategy2, properties2);

        // Verify
        Set<String> invalidateTypes = registry.getInvalidationTypes();
        assertEquals(2, invalidateTypes.size());
        assertTrue(invalidateTypes.contains("attr1"));
        assertTrue(invalidateTypes.contains("attr2"));
    }

    @Test
    public void testGetInvalidationTypeStrategies() {
        // Setup
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_TYPE_PARAMETER, "invalidType");
        Mockito.when(mockStrategy.getInvalidationRequestType()).thenReturn("invalidType");

        // Test
        registry.bindInvalidateCache(mockStrategy, properties);

        // Verify
        InvalidationStrategies strategies = registry.getInvalidationStrategies("invalidType");
        assertTrue(strategies != null);
    }
}
