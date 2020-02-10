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

package com.adobe.cq.commerce.core.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;

import org.osgi.annotation.versioning.ConsumerType;


/**
 * Represents a set of search results from a backend service. This would generally contain the actual {@link ProductListItem}s
 * as well as {@link SearchAggregation}s.
 */
@ConsumerType
public interface SearchResultsSet {

    @Nonnull
    default SearchOptions getSearchOptions() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default Integer getTotalResults() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default Optional<String> getSearchQuery() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default Map<String, String> getAppliedQueryParameters() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default List<ProductListItem> getProductListItems() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default List<SearchAggregation> getSearchAggregations() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default List<SearchAggregation> getAppliedAggregations() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default List<SearchAggregation> getAvailableAggregations() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default List<Map<String, String>> getPaginationParameters() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default Map<String, String> getPreviousPageParameters(Map<String, String> appliedParameters) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default Map<String, String> getNextPageParameters(Map<String, String> appliedParameters) {
        throw new UnsupportedOperationException();
    }

}
