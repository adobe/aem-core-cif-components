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
 * The DispatcherCacheInvalidationStrategy interface defines the methods used to invalidate the dispatcher cache.
 */
@ConsumerType
public interface DispatcherCacheInvalidationStrategy extends CacheInvalidationStrategy {
    /**
     * Returns the paths to invalidate based on the provided context.
     *
     * @param context the context containing all necessary information for cache invalidation
     * @return a {@code List<String>} of paths to invalidate
     */
    List<String> getPathsToInvalidate(DispatcherCacheInvalidationContext context);
}
