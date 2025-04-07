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

package com.adobe.cq.commerce.core.cacheinvalidation.spi;

import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.day.cq.wcm.api.Page;

/**
 * Context object containing all parameters needed for dispatcher cache invalidation.
 * This interface provides access to various components required for managing dispatcher cache invalidation
 * in the Commerce integration with AEM, including the page context, resource resolver,
 * invalidate type data, store path, and GraphQL client.
 */
@ProviderType
public interface CacheInvalidationContext {
    /**
     * Returns the AEM page object associated with the cache invalidation context.
     * This page object contains metadata and content structure information needed for cache management.
     *
     * @return the AEM page object
     */
    Page getPage();

    /**
     * Returns the Sling resource resolver instance used for accessing and manipulating resources.
     * This resolver is essential for performing resource-level operations during cache invalidation.
     *
     * @return the Sling resource resolver
     */
    ResourceResolver getResourceResolver();

    /**
     * Returns a list of invalidate type data strings that provide additional details
     * necessary for processing cache invalidation.
     *
     * @return a list of invalidate type data strings
     */
    List<String> getInvalidateTypeData();

    /**
     * Returns the store path associated with the cache invalidation context.
     * This path typically represents the location of the store configuration in the AEM repository.
     *
     * @return the store path as a string
     */
    String getStorePath();

    /**
     * Returns the Magento GraphQL client instance used for making GraphQL queries
     * to the Commerce backend. This client is necessary for fetching data
     * that may affect cache invalidation decisions.
     *
     * @return the Magento GraphQL client
     */
    MagentoGraphqlClient getGraphqlClient();
}
