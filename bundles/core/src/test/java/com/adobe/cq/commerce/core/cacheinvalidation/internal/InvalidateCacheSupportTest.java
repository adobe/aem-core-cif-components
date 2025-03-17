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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateCacheSupportTest {

    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private GraphqlClient graphqlClient;

    private static final String TEST_STORE_PATH = "/content/venia/us/en";
    private static final String TEST_GRAPHQL_CLIENT = "default";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        invalidateCacheSupport = new InvalidateCacheSupport();

        // Set up ResourceResolverFactory mock
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, InvalidateCacheSupport.SERVICE_USER);
        when(resourceResolverFactory.getServiceResourceResolver(authInfo)).thenReturn(resourceResolver);

        // Inject dependencies
        setField(invalidateCacheSupport, "resourceResolverFactory", resourceResolverFactory);
    }

    @Test
    public void testActivateWithoutDispatcherConfig() {
        // Setup
        Map<String, Object> properties = new HashMap<>();

        // Execute
        invalidateCacheSupport.activate(properties);

        // Verify
        assertFalse(invalidateCacheSupport.getEnableDispatcherCacheInvalidation());
        assertNull(invalidateCacheSupport.getDispatcherBaseUrl());

        // Test URL path configurations with empty config
        List<PatternConfig> configs = invalidateCacheSupport.getDispatcherUrlConfigurationForType("product");
        assertTrue(configs.isEmpty());
    }

    @Test
    public void testDeactivate() {
        // Setup - activate first
        Map<String, Object> properties = new HashMap<>();
        properties.put("enableDispatcherCacheInvalidation", true);
        properties.put("dispatcherBaseUrl", "http://localhost:4503");
        invalidateCacheSupport.activate(properties);

        // Execute
        invalidateCacheSupport.deactivate();

        // Verify
        assertFalse(invalidateCacheSupport.getEnableDispatcherCacheInvalidation());
        assertNull(invalidateCacheSupport.getDispatcherBaseUrl());
    }

    @Test
    public void testGetServiceUserResourceResolver() throws LoginException {
        // Execute
        ResourceResolver result = invalidateCacheSupport.getServiceUserResourceResolver();

        // Verify
        assertNotNull(result);
        assertEquals(resourceResolver, result);
    }

    @Test
    public void testGetResource() {
        // Setup
        when(resourceResolver.getResource(TEST_STORE_PATH)).thenReturn(resource);

        // Execute
        Resource result = invalidateCacheSupport.getResource(resourceResolver, TEST_STORE_PATH);

        // Verify
        assertNotNull(result);
        assertEquals(resource, result);
    }

    @Test
    public void testGetResourceWithNullResource() {
        // Setup
        when(resourceResolver.getResource(TEST_STORE_PATH)).thenReturn(null);

        // Execute
        Resource result = invalidateCacheSupport.getResource(resourceResolver, TEST_STORE_PATH);

        // Verify
        assertNull(result);
    }

    @Test
    public void testConvertUrlPath() {
        // Setup
        Map<String, Object> properties = new HashMap<>();
        properties.put("dispatcherUrlPathConfiguration", new String[] {
            "product:/products/(.*):$1",
            "category:/categories/(.*):$1"
        });
        invalidateCacheSupport.activate(properties);

        // Test cases
        String productUrl = "/products/test-product";
        String categoryUrl = "/categories/test-category";
        String otherUrl = "/other/path";

        // Execute & Verify
        assertEquals("test-product", invalidateCacheSupport.convertUrlPath(productUrl));
        assertEquals("test-category", invalidateCacheSupport.convertUrlPath(categoryUrl));
        assertEquals(otherUrl, invalidateCacheSupport.convertUrlPath(otherUrl));
        assertEquals(null, invalidateCacheSupport.convertUrlPath(null));
    }

    @Test
    public void testBindAndUnbindGraphqlClient() {
        // Setup
        Map<String, Object> properties = new HashMap<>();
        properties.put("identifier", TEST_GRAPHQL_CLIENT);

        // Execute bind
        invalidateCacheSupport.bindGraphqlClient(graphqlClient, properties);

        // Verify bind
        GraphqlClient result = invalidateCacheSupport.getClient(TEST_GRAPHQL_CLIENT);
        assertNotNull(result);
        assertEquals(graphqlClient, result);

        // Execute unbind
        invalidateCacheSupport.unbindGraphqlClient(graphqlClient);

        // Verify unbind - should throw exception
        try {
            invalidateCacheSupport.getClient(TEST_GRAPHQL_CLIENT);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(TEST_GRAPHQL_CLIENT));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClientWithInvalidId() {
        invalidateCacheSupport.getClient("invalid-client");
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
}
