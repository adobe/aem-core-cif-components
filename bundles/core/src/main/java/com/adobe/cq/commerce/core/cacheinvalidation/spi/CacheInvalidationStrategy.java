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
     * Returns the pattern used for cache invalidation. This pattern defines which cache entries
     * should be invalidated based on specific criteria.
     *
     * @return the cache invalidation pattern that determines which cache entries to invalidate
     */
    String getPattern();

    /**
     * Returns the type of cache invalidation request. This type specifies how the cache should be invalidated
     *
     * @return the type of cache invalidation request that specifies how the cache should be invalidated
     */
    String getInvalidationRequestType();
}
