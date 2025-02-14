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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

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
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        invalidateDispatcherCacheImpl = new InvalidateDispatcherCacheImpl();
        slingSettingsService = mock(SlingSettingsService.class);
        invalidateCacheSupport = mock(InvalidateCacheSupport.class);
        invalidateCacheRegistry = mock(InvalidateCacheRegistry.class);
        resourceResolver = mock(ResourceResolver.class);
        logger = mock(Logger.class);
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

        // Set up the mock behavior
        when(slingSettingsService.getRunModes()).thenReturn(new HashSet<>(Arrays.asList("author")));

        // Call the method under test
        invalidateDispatcherCacheImpl.invalidateCache("path");

        // Verify interactions
        verify(slingSettingsService).getRunModes();
        verifyNoMoreInteractions(invalidateCacheSupport, invalidateCacheRegistry, resourceResolver);
    }

    @Test
    public void testCreateProperty() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("createProperty", boolean.class, Class.class);
        method.setAccessible(true);

        Map<String, Object> result = (Map<String, Object>) method.invoke(invalidateDispatcherCacheImpl, false, String.class);

        assertNotNull(result);
        assertEquals(false, result.get("isFunction"));
        assertEquals(String.class, result.get("class"));
    }

    @Test
    public void testCreateFunctionProperty() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("createFunctionProperty", String.class, Class[].class,
            Object[].class);
        method.setAccessible(true);

        String methodName = "testMethod";
        Class<?>[] parameterTypes = { String.class };
        Object[] args = { "testArg" };

        Map<String, Object> result = (Map<String, Object>) method.invoke(invalidateDispatcherCacheImpl, methodName, parameterTypes, args);

        assertNotNull(result);
        assertEquals(true, result.get("isFunction"));
        assertEquals(methodName, result.get("method"));
        assertArrayEquals(parameterTypes, (Class<?>[]) result.get("parameterTypes"));
        assertArrayEquals(args, (Object[]) result.get("args"));
    }

    @Test
    public void testGetPropertiesValue() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getPropertiesValue", ValueMap.class, String.class,
            Class.class);
        method.setAccessible(true);

        ValueMap properties = new ValueMapDecorator(new HashMap<>());
        properties.put("key", "value");

        String result = (String) method.invoke(invalidateDispatcherCacheImpl, properties, "key", String.class);

        assertEquals("value", result);
    }

    @Test
    public void testGetPage() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getPage", ResourceResolver.class, String.class);
        method.setAccessible(true);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        PageManager pageManager = mock(PageManager.class);
        Page page = mock(Page.class);

        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage("storePath")).thenReturn(page);

        Page result = (Page) method.invoke(invalidateDispatcherCacheImpl, resourceResolver, "storePath");

        assertEquals(page, result);
    }

    @Test
    public void testGetCorrespondingPageProperties() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("getCorrespondingPageProperties", ResourceResolver.class,
            String.class, String.class);
        method.setAccessible(true);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Page page = mock(Page.class);
        ValueMap properties = new ValueMapDecorator(new HashMap<>());
        properties.put("propertyName", "propertyValue");

        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(mock(PageManager.class));
        when(resourceResolver.adaptTo(PageManager.class).getPage("storePath")).thenReturn(page);
        when(page.getProperties()).thenReturn(properties);

        String result = (String) method.invoke(invalidateDispatcherCacheImpl, resourceResolver, "storePath", "propertyName");

        assertEquals("propertyValue", result);
    }

    @Test
    public void testFormatList() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("formatList", String[].class, String.class, String.class);
        method.setAccessible(true);

        String[] invalidCacheEntries = { "entry1", "entry2" };
        String delimiter = ",";
        String pattern = "[%s]";

        String result = (String) method.invoke(invalidateDispatcherCacheImpl, invalidCacheEntries, delimiter, pattern);

        assertEquals("[entry1],[entry2]", result);
    }

    @Test
    public void testExtractPagePath() throws Exception {
        Method method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod("extractPagePath", String.class);
        method.setAccessible(true);
        String fullPath = "/content/page/jcr:content";
        String result = (String) method.invoke(invalidateDispatcherCacheImpl, fullPath);
        assertEquals("/content/page", result);
    }
}
