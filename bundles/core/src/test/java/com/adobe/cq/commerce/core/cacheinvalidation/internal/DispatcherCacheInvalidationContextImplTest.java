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

import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DispatcherCacheInvalidationContextImplTest {

    private Page page;
    private ResourceResolver resourceResolver;
    private List<String> invalidationParameters;
    private String storePath;
    private MagentoGraphqlClient graphqlClient;
    private CacheInvalidationContextImpl context;

    @Before
    public void setUp() {
        page = mock(Page.class);
        resourceResolver = mock(ResourceResolver.class);
        invalidationParameters = Arrays.asList("attr1", "attr2");
        storePath = "/content/store";
        graphqlClient = mock(MagentoGraphqlClient.class);

        context = new CacheInvalidationContextImpl(page, resourceResolver, invalidationParameters, storePath, graphqlClient);
    }

    @Test
    public void testGetPage() {
        assertEquals(page, context.getPage());
    }

    @Test
    public void testGetResourceResolver() {
        assertEquals(resourceResolver, context.getResourceResolver());
    }

    @Test
    public void testGetInvalidationParameters() {
        assertEquals(invalidationParameters, context.getInvalidationParameters());
    }

    @Test
    public void testGetStorePath() {
        assertEquals(storePath, context.getStorePath());
    }

    @Test
    public void testGetGraphqlClient() {
        assertEquals(graphqlClient, context.getGraphqlClient());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullPage() {
        new CacheInvalidationContextImpl(null, resourceResolver, invalidationParameters, storePath, graphqlClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullResourceResolver() {
        new CacheInvalidationContextImpl(page, null, invalidationParameters, storePath, graphqlClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullInvalidationParameters() {
        new CacheInvalidationContextImpl(page, resourceResolver, null, storePath, graphqlClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullStorePath() {
        new CacheInvalidationContextImpl(page, resourceResolver, invalidationParameters, null, graphqlClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullGraphqlClient() {
        new CacheInvalidationContextImpl(page, resourceResolver, invalidationParameters, storePath, null);
    }
}
