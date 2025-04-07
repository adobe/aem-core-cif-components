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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The CacheInvalidationStrategy interface defines the contract for cache invalidation strategies
 * in the Commerce Core components. This interface provides methods to determine how the cache entries
 * should be invalidated based on specific patterns and request types.
 * 
 * Implementations of this interface can provide different strategies for cache invalidation,
 * such as request-types based invalidation.
 */
@ConsumerType
public interface CacheInvalidationStrategy {
    /**
     * Returns a list of patterns used for cache invalidation. These patterns define which cache entries
     * should be invalidated based on the provided parameters.
     *
     * @param invalidationParameters an array of strings that are used to generate the cache invalidation patterns
     * @return a list of cache invalidation patterns that determine which cache entries to invalidate
     */
    List<String> getPatterns(String[] invalidationParameters);

    /**
     * Returns the type of cache invalidation request. This type specifies how the cache should be invalidated
     *
     * @return the type of cache invalidation request that specifies how the cache should be invalidated
     */
    String getInvalidationRequestType();
}
