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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateCacheSupportTest {

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private GraphqlClient graphqlClient;

    @InjectMocks
    private InvalidateCacheSupport invalidateCacheSupport;

    private ComponentsConfiguration componentsConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        componentsConfiguration = new ComponentsConfiguration(new ValueMapDecorator(new HashMap<>()));
    }

    @Test
    public void testActivate() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("enableDispatcherCacheInvalidation", true);
        invalidateCacheSupport.activate(properties);
        assertTrue(invalidateCacheSupport.getEnableDispatcherCacheInvalidation());
    }

    @Test
    public void testDeactivate() {
        invalidateCacheSupport.deactivate();
        assertFalse(invalidateCacheSupport.getEnableDispatcherCacheInvalidation());
    }

    @Test
    public void testGetClient() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("identifier", "graphqlClientId");
        invalidateCacheSupport.bindGraphqlClient(graphqlClient, properties);
        assertEquals(graphqlClient, invalidateCacheSupport.getClient("graphqlClientId"));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClientNotFound() {
        invalidateCacheSupport.getClient("nonExistentClientId");
    }

    @Test
    public void testUnbindGraphqlClient() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("identifier", "graphqlClientId");
        invalidateCacheSupport.bindGraphqlClient(graphqlClient, properties);
        invalidateCacheSupport.unbindGraphqlClient(graphqlClient);
        assertThrows(IllegalStateException.class, () -> invalidateCacheSupport.getClient("graphqlClientId"));
    }

    @Test
    public void testGetCommerceProperties() {
        when(resourceResolver.getResource("storePath")).thenReturn(resource);
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(componentsConfiguration);
        assertEquals(componentsConfiguration, invalidateCacheSupport.getCommerceProperties(resourceResolver, "storePath"));
    }

    @Test
    public void testGetCommercePropertiesResourceNotFound() {
        when(resourceResolver.getResource("storePath")).thenReturn(null);
        assertNull(invalidateCacheSupport.getCommerceProperties(resourceResolver, "storePath"));
    }

    @Test
    public void testGetResource() {
        when(resourceResolver.getResource("path")).thenReturn(resource);
        assertEquals(resource, invalidateCacheSupport.getResource(resourceResolver, "path"));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetResourceException() {
        when(resourceResolver.getResource("path")).thenThrow(new RuntimeException());
        invalidateCacheSupport.getResource(resourceResolver, "path");
    }

    @Test
    public void testGetServiceUserResourceResolver() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        assertEquals(resourceResolver, invalidateCacheSupport.getServiceUserResourceResolver());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetServiceUserResourceResolverException() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException());
        invalidateCacheSupport.getServiceUserResourceResolver();
    }
}
