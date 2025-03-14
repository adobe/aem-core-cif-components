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
import java.lang.reflect.Modifier;
import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateCacheImplTest {

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private InvalidateCacheRegistry invalidateCacheRegistry;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    private ComponentsConfiguration commerceProperties;

    @Mock
    private GraphqlClient graphqlClient;

    @Mock
    private Logger logger;

    @InjectMocks
    private InvalidateCacheImpl invalidateCacheImpl;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ValueMap valueMap = new ValueMapDecorator(Collections.singletonMap("attribute1", "value1"));
        commerceProperties = new ComponentsConfiguration(valueMap);
        setLoggerField();
    }

    private void setLoggerField() throws Exception {
        Field loggerField = InvalidateCacheImpl.class.getDeclaredField("LOGGER");
        loggerField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(loggerField, loggerField.getModifiers() & ~Modifier.FINAL);

        loggerField.set(null, logger);
    }

    @Test
    public void testInvalidateCache() throws Exception {
        String path = "/content/test";
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(path)).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(mock(ValueMap.class));
        when(invalidateCacheSupport.getCommerceProperties(resourceResolver, null)).thenReturn(commerceProperties);
        when(invalidateCacheSupport.getClient(anyString())).thenReturn(graphqlClient);
        when(invalidateCacheRegistry.getAttributes()).thenReturn(Collections.singleton("attribute1"));

        ValueMap valueMap = resource.getValueMap();
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class)).thenReturn(null);
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_CACHE_NAME, String[].class)).thenReturn(new String[] { "cache1" });
        when(valueMap.get("attribute1", String[].class)).thenReturn(new String[] { "value1" });

        invalidateCacheImpl.invalidateCache(path);

        verify(graphqlClient).invalidateCache(anyString(), any(String[].class), any(String[].class));
    }

    @Test
    public void testInvalidateCacheWithNullResource() throws Exception {
        String path = "/content/test";
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(path)).thenReturn(null);

        invalidateCacheImpl.invalidateCache(path);

        verify(logger).debug("Resource not found at path: {}", path);
        verifyZeroInteractions(graphqlClient);
    }

    @Test
    public void testInvalidateCacheWithNullCommerceProperties() throws Exception {
        String path = "/content/test";
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(path)).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(mock(ValueMap.class));
        when(invalidateCacheSupport.getCommerceProperties(resourceResolver, null)).thenReturn(null);

        invalidateCacheImpl.invalidateCache(path);

        verify(logger).debug("Commerce data not found at path: {}", resource.getPath());
        verifyZeroInteractions(graphqlClient);
    }

    @Test
    public void testInvalidateCacheWithException() throws Exception {
        String path = "/content/test";
        RuntimeException exception = new RuntimeException("Test exception");
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenThrow(exception);

        invalidateCacheImpl.invalidateCache(path);

        verify(logger).error("Error processing JCR event: {}", "Test exception", exception);
        verifyZeroInteractions(graphqlClient);
    }

    @Test
    public void testInvalidateCacheWithNullGraphqlClient() throws Exception {
        String path = "/content/test";
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(path)).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(mock(ValueMap.class));
        when(invalidateCacheSupport.getCommerceProperties(resourceResolver, null)).thenReturn(commerceProperties);
        when(invalidateCacheSupport.getClient(anyString())).thenReturn(null);

        invalidateCacheImpl.invalidateCache(path);

        verifyZeroInteractions(graphqlClient);
    }

    private String[] invokeGetAttributePatterns(String[] patterns, String attribute) throws Exception {
        Method method = InvalidateCacheImpl.class.getDeclaredMethod("getAttributePatterns", String[].class, String.class);
        method.setAccessible(true);
        return (String[]) method.invoke(invalidateCacheImpl, patterns, attribute);
    }

    @Test
    public void testGetAttributePatterns_WithPattern() throws Exception {
        String[] patterns = { "pattern1", "pattern2" };
        String attribute = "attribute";
        String expectedPattern = "expectedPattern";
        when(invalidateCacheRegistry.getPattern(attribute)).thenReturn(expectedPattern);
        String[] result = invokeGetAttributePatterns(patterns, attribute);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("expectedPattern(pattern1|pattern2)", result[0]);
    }

    @Test
    public void testGetAttributePatterns_WithoutPattern() throws Exception {
        String[] patterns = { "pattern1", "pattern2" };
        String attribute = "attribute";
        when(invalidateCacheRegistry.getPattern(attribute)).thenReturn(null);
        String[] result = invokeGetAttributePatterns(patterns, attribute);
        assertNotNull(result);
        assertArrayEquals(patterns, result);
    }
}
