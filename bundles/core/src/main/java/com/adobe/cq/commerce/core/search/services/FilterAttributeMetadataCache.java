/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.search.services;

import java.util.List;
import java.util.Optional;

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;

/**
 * This cache provides the filter attribute metadata to be used by search service(s). This interface allows for a caching layer to be
 * implemented in
 * anyway the implementer sees fit given available caching mechanisms in a particular system.
 */
public interface FilterAttributeMetadataCache {

    /**
     * Retrieve {@link List<FilterAttributeMetadataCache>} from cache.
     *
     * @return the optionally cached {@link List<FilterAttributeMetadataCache>}
     */
    Optional<List<FilterAttributeMetadata>> getFilterAttributeMetadata();

    /**
     * Set the filter attribute metadata in cache.
     *
     * @param filterAttributeMetadata the filter metadata to be stored in cache
     */
    void setFilterAttributeMetadata(List<FilterAttributeMetadata> filterAttributeMetadata);
}
