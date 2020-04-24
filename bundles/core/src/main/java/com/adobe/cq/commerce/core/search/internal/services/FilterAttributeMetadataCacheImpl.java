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

package com.adobe.cq.commerce.core.search.internal.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.services.FilterAttributeMetadataCache;

/**
 * This "caching" implementation takes advantage of the OOTB singleton behavior for OSGi component services. A more robust or full featured
 * true
 * caching system could be used in place of this implementation if available by implementing {@link FilterAttributeMetadataCache} interface
 * and
 * exposing the service to OSGi.
 */
@Component(service = FilterAttributeMetadataCache.class)
public class FilterAttributeMetadataCacheImpl implements FilterAttributeMetadataCache {

    // The "cache" life of the custom attributes for filter queries
    private final Long attributeCacheTimeoutMS;
    private List<FilterAttributeMetadata> filterAttributeMetadata = null;
    private Long lastFetched = null;

    /**
     * Default constructor for class.
     */
    public FilterAttributeMetadataCacheImpl() {
        attributeCacheTimeoutMS = 600000L;
    }

    /**
     * Constructor with parameters, useful for testing or other cases requiring initial state.
     *
     * @param filterAttributeMetadata initial filter attribute metadata
     * @param lastFetched time the metadata was last fetched
     */
    public FilterAttributeMetadataCacheImpl(final List<FilterAttributeMetadata> filterAttributeMetadata, final Long lastFetched,
                                            final Long attributeCacheTimeoutMS) {
        this.filterAttributeMetadata = filterAttributeMetadata;
        this.lastFetched = lastFetched;
        this.attributeCacheTimeoutMS = attributeCacheTimeoutMS;
    }

    @Override
    public Optional<List<FilterAttributeMetadata>> getFilterAttributeMetadata() {

        if (shouldRefreshData()) {
            return Optional.empty();
        }

        return Optional.of(filterAttributeMetadata);
    }

    @Override
    public void setFilterAttributeMetadata(final List<FilterAttributeMetadata> filterAttributeMetadata) {
        this.filterAttributeMetadata = filterAttributeMetadata;
        lastFetched = Instant.now().toEpochMilli();
    }

    /**
     * Determines whether or not a refresh of data is required.
     *
     * @return true if data should be refreshed.
     */
    private boolean shouldRefreshData() {

        Long now = Instant.now().toEpochMilli();
        if (filterAttributeMetadata == null || lastFetched == null || (now - lastFetched) > attributeCacheTimeoutMS) {
            return true;
        }

        return false;
    }

}
