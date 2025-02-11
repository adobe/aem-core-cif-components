
/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2025 Adobe
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

import static org.mockito.Mockito.*;

public class InvalidateCacheImplTest {

    @InjectMocks
    private InvalidateCacheImpl invalidateCacheImpl;

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private GraphqlClient graphqlClient;

    @Mock
    private Logger logger;

    private ComponentsConfiguration commerceProperties;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(mock(ValueMap.class));
        commerceProperties = new ComponentsConfiguration(new ValueMapDecorator(new HashMap<>()));
        when(invalidateCacheSupport.getCommerceProperties(any(), anyString())).thenReturn(commerceProperties);
        when(invalidateCacheSupport.getClient(anyString())).thenReturn(graphqlClient);
    }

    @Test
    public void testInvalidateCache_resourceFound() {
        String path = "/content/path";
        when(resourceResolver.getResource(path)).thenReturn(resource);

        invalidateCacheImpl.invalidateCache(path);

        verify(resourceResolver).getResource(path);
        verify(invalidateCacheSupport).getServiceUserResourceResolver();
    }

}
