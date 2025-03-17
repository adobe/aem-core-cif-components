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

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

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
    private ResourceResolver resourceResolver;

    @Mock
    private SlingSettingsService slingSettingsService;

    @Mock
    private UrlProviderImpl urlProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

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
        verify(invalidateCacheSupport, never()).getServiceUserResourceResolver();
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

}
