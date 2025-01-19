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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvalidateCacheSupportTest {

    @InjectMocks
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private GraphqlClient graphqlClient;




    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetServiceUserResourceResolver() throws LoginException {
        // Mock dependencies
        ResourceResolver mockResolver = mock(ResourceResolver.class);

        // Mock the behavior of the getServiceResourceResolver method
        Map<String, Object> params = new HashMap<>();
        params.put(ResourceResolverFactory.SUBSERVICE, "cif-cache-service");
        when(resourceResolverFactory.getServiceResourceResolver(params)).thenReturn(mockResolver);

        // Test the method
        ResourceResolver result = invalidateCacheSupport.getServiceUserResourceResolver();

        // Verify the result
        assertNotNull("ResourceResolver should not be null", result);
    }

    @Test
    public void testGetServiceUserResourceResolver_loginException() throws LoginException {
        // Mock the behavior to throw a LoginException
        when(resourceResolverFactory.getServiceResourceResolver(any())).thenThrow(new LoginException("Login failed"));

        // Test the method and assert the expected exception
        try {
            invalidateCacheSupport.getServiceUserResourceResolver();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected exception
        }
    }

    @Test
    public void testGetResource() {
        Resource resource = mock(Resource.class);
        when(resourceResolver.getResource("path")).thenReturn(resource);

        Resource result = invalidateCacheSupport.getResource(resourceResolver, "path");

        assertNotNull(result);
        assertEquals(resource, result);
    }

    @Test(expected = RuntimeException.class)
    public void testGetResource_exception() {
        when(resourceResolver.getResource("path")).thenThrow(new RuntimeException());

        invalidateCacheSupport.getResource(resourceResolver, "path");
    }

    @Test
    public void testBindGraphqlClient() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("identifier", "newClientId");

        invalidateCacheSupport.bindGraphqlClient(graphqlClient, properties);

        Field clientsField = InvalidateCacheSupport.class.getDeclaredField("clients");
        clientsField.setAccessible(true);
        Collection<Object> clients = (Collection<Object>) clientsField.get(invalidateCacheSupport);

        assertEquals(1, clients.size());

        // Use reflection to access the private graphqlClient field in ClientHolder
        Object clientHolder = clients.iterator().next();
        Field graphqlClientField = clientHolder.getClass().getDeclaredField("graphqlClient");
        graphqlClientField.setAccessible(true);
        assertEquals(graphqlClient, graphqlClientField.get(clientHolder));
    }

    @Test
    public void testGetClient_validClientId() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("identifier", "validClientId");

        // Use reflection to access the private constructor of ClientHolder
        Constructor<?> constructor = InvalidateCacheSupport.class.getDeclaredClasses()[0].getDeclaredConstructor(GraphqlClient.class,
            Map.class);
        constructor.setAccessible(true);
        Object clientHolder = constructor.newInstance(graphqlClient, properties);

        Field clientsField = InvalidateCacheSupport.class.getDeclaredField("clients");
        clientsField.setAccessible(true);
        Collection<Object> clients = (Collection<Object>) clientsField.get(invalidateCacheSupport);
        clients.add(clientHolder);

        GraphqlClient result = invalidateCacheSupport.getClient("validClientId");
        assertNotNull(result);
        assertEquals(graphqlClient, result);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClient_invalidClientId() {
        invalidateCacheSupport.getClient("nonExistentClientId");
    }




    @Test
    public void testUnbindGraphqlClient() throws Exception {
        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        Map<String, Object> properties = new HashMap<>();
        properties.put("identifier", "clientId");

        // Bind the client first
        invalidateCacheSupport.bindGraphqlClient(graphqlClient, properties);

        // Use reflection to access the private clients field
        Field clientsField = InvalidateCacheSupport.class.getDeclaredField("clients");
        clientsField.setAccessible(true);
        Collection<?> clients = (Collection<?>) clientsField.get(invalidateCacheSupport);

        // Ensure the client is added
        assertEquals(1, clients.size());

        // Unbind the client
        invalidateCacheSupport.unbindGraphqlClient(graphqlClient);

        // Ensure the client is removed
        assertEquals(0, clients.size());
    }


    @Test
    public void testGetCommercePropertiesResourceNotFound() {
        when(resourceResolver.getResource("/store/path")).thenReturn(null);

        ComponentsConfiguration result = invalidateCacheSupport.getCommerceProperties(resourceResolver, "/store/path");

        assertNull(result);
    }


}
