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

import static org.mockito.Mockito.*;

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
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ValueMap valueMap = new ValueMapDecorator(Collections.singletonMap("attribute1", "value1"));
        commerceProperties = new ComponentsConfiguration(valueMap);
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
        when(valueMap.get(InvalidateCacheSupport.PROPERTIES_CACHE_NAME, String[].class)).thenReturn(new String[]{"cache1"});
        when(valueMap.get("attribute1", String[].class)).thenReturn(new String[]{"value1"});

        invalidateCacheImpl.invalidateCache(path);

        verify(graphqlClient).invalidateCache(anyString(), any(String[].class), any(String[].class));
    }
}