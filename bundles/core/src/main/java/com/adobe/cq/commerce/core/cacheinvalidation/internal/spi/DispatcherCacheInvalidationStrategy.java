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

package com.adobe.cq.commerce.core.cacheinvalidation.internal.spi;

import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ConsumerType;

import com.day.cq.wcm.api.Page;

/**
 * The DispatcherCacheInvalidationStrategy interface defines the methods used to invalidate the dispatcher cache.
 */
@ConsumerType
public interface DispatcherCacheInvalidationStrategy extends CacheInvalidationStrategy {

    /**
     * Returns the query used for cache invalidation based on the store path and data list.
     *
     * @param storePath the store path
     * @param dataList the data list containing the data to be invalidated
     * @return the cache invalidation query
     */
    String getQuery(String storePath, String dataList);

    /**
     * Returns the GraphQL query used for cache invalidation based on the provided data.
     *
     * @param data the data array contains the data to be invalidated
     * @return the GraphQL query based on the data array
     */
    String getGraphqlQuery(String[] data);

    /**
     * Returns the paths to invalidate based on the provided page, resource resolver, data, and store path.
     *
     * @param page the page
     * @param resourceResolver the resource resolver
     * @param data based on the data it will get the invalidation paths
     * @param storePath the store path
     * @return the array of paths to invalidate
     */
    String[] getPathsToInvalidate(Page page, ResourceResolver resourceResolver, Map<String, Object> data, String storePath);
}
