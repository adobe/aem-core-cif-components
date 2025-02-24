/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.cacheinvalidation.internal.spi.CacheInvalidationStrategy;
import com.adobe.cq.commerce.core.cacheinvalidation.internal.spi.DispatcherCacheInvalidationStrategy;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateCacheRegistryTest {

    @Mock
    private CacheInvalidationStrategy cacheInvalidationStrategy;

    @Mock
    private DispatcherCacheInvalidationStrategy dispatcherCacheInvalidationStrategy;

    @InjectMocks
    private InvalidateCacheRegistry invalidateCacheRegistry;

    @Mock
    private Page page;

    @Mock
    private ResourceResolver resourceResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBindInvalidateCache() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute1");

        invalidateCacheRegistry.bindInvalidateCache(cacheInvalidationStrategy, properties);
        assertEquals(cacheInvalidationStrategy, invalidateCacheRegistry.get("attribute1"));
    }

    @Test
    public void testUnbindInvalidateCache() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute1");

        invalidateCacheRegistry.bindInvalidateCache(cacheInvalidationStrategy, properties);
        invalidateCacheRegistry.unbindInvalidateCache(properties);
        assertNull(invalidateCacheRegistry.get("attribute1"));
    }

    @Test
    public void testBindInvalidateDispatcherCache() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute2");

        invalidateCacheRegistry.bindInvalidateDispatcherCache(dispatcherCacheInvalidationStrategy, properties);
        assertEquals(dispatcherCacheInvalidationStrategy, invalidateCacheRegistry.get("attribute2"));
    }

    @Test
    public void testUnbindInvalidateDispatcherCache() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute2");

        invalidateCacheRegistry.bindInvalidateDispatcherCache(dispatcherCacheInvalidationStrategy, properties);
        invalidateCacheRegistry.unbindInvalidateDispatcherCache(properties);
        assertNull(invalidateCacheRegistry.get("attribute2"));
    }

    @Test
    public void testGetPattern() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute1");

        when(cacheInvalidationStrategy.getPattern()).thenReturn("pattern");
        invalidateCacheRegistry.bindInvalidateCache(cacheInvalidationStrategy, properties);

        assertEquals("pattern", invalidateCacheRegistry.getPattern("attribute1"));
    }

    @Test
    public void testGetGraphqlQuery() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute2");

        when(dispatcherCacheInvalidationStrategy.getGraphqlQuery(new String[] { "data" })).thenReturn("graphqlQuery");
        invalidateCacheRegistry.bindInvalidateDispatcherCache(dispatcherCacheInvalidationStrategy, properties);

        assertEquals("graphqlQuery", invalidateCacheRegistry.getGraphqlQuery("attribute2", new String[] { "data" }));
    }

    @Test
    public void testGetPathsToInvalidate() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute2");

        Map<String, Object> data = new HashMap<>();
        String[] paths = new String[] { "path1", "path2" };

        when(dispatcherCacheInvalidationStrategy.getPathsToInvalidate(page, resourceResolver, data, "storePath")).thenReturn(paths);
        invalidateCacheRegistry.bindInvalidateDispatcherCache(dispatcherCacheInvalidationStrategy, properties);

        assertArrayEquals(paths, invalidateCacheRegistry.getPathsToInvalidate("attribute2", page, resourceResolver, data, "storePath"));
    }

    @Test
    public void testGetAttributes() {
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute1");

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER, "attribute2");

        invalidateCacheRegistry.bindInvalidateCache(cacheInvalidationStrategy, properties1);
        invalidateCacheRegistry.bindInvalidateDispatcherCache(dispatcherCacheInvalidationStrategy, properties2);

        Set<String> attributes = invalidateCacheRegistry.getAttributes();
        assertTrue(attributes.contains("attribute1"));
        assertTrue(attributes.contains("attribute2"));
    }

    @Test
    public void testGetGraphqlQuery_NonDispatcherCacheInvalidationStrategy() {
        String result = invalidateCacheRegistry.getGraphqlQuery("attribute1", new String[] { "data" });
        assertNull(result);
    }

    @Test
    public void testGetPathsToInvalidate_NonDispatcherCacheInvalidationStrategy() {
        String[] result = invalidateCacheRegistry.getPathsToInvalidate("attribute1", page, resourceResolver, new HashMap<>(), "storePath");
        assertArrayEquals(new String[0], result);
    }
}
