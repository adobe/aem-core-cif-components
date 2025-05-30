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

import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationContext;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.day.cq.wcm.api.Page;

/**
 * Implementation of {@link CacheInvalidationContext}.
 */
public class CacheInvalidationContextImpl implements CacheInvalidationContext {
    private final Page page;
    private final ResourceResolver resourceResolver;
    private final List<String> invalidationParameters;
    private final String storePath;
    private final MagentoGraphqlClient graphqlClient;

    private static void requireNonNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " parameter cannot be null");
        }
    }

    /**
     * Creates a new CacheInvalidationContextImpl with the given invalidationParameters.
     *
     * @param page the page
     * @param resourceResolver the resource resolver
     * @param invalidationParameters the parameters which needs to invalidated
     * @param storePath the store path
     * @param graphqlClient the Magento GraphQL client
     */
    public CacheInvalidationContextImpl(Page page, ResourceResolver resourceResolver, List<String> invalidationParameters,
                                        String storePath, MagentoGraphqlClient graphqlClient) {
        requireNonNull(page, "Page");
        requireNonNull(resourceResolver, "ResourceResolver");
        requireNonNull(invalidationParameters, "InvalidationParameters");
        requireNonNull(storePath, "StorePath");
        requireNonNull(graphqlClient, "GraphqlClient");

        this.page = page;
        this.resourceResolver = resourceResolver;
        this.invalidationParameters = invalidationParameters;
        this.storePath = storePath;
        this.graphqlClient = graphqlClient;
    }

    @Override
    public Page getPage() {
        return page;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public List<String> getInvalidationParameters() {
        return invalidationParameters;
    }

    @Override
    public String getStorePath() {
        return storePath;
    }

    @Override
    public MagentoGraphqlClient getGraphqlClient() {
        return graphqlClient;
    }
}
