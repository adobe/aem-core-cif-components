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

package com.adobe.cq.commerce.core.search.models;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;

/**
 * Represents a set of search results from a backend service. This would generally contain the actual {@link ProductListItem}s
 * as well as {@link SearchAggregation}s.
 */
@ConsumerType
public interface SearchResultsSet {

    @Nonnull
    SearchOptions getSearchOptions();

    @Nonnull
    Integer getTotalResults();

    @Nonnull
    Optional<String> getSearchQuery();

    @Nonnull
    Map<String, String> getAppliedQueryParameters();

    @Nonnull
    List<ProductListItem> getProductListItems();

    @Nonnull
    List<SearchAggregation> getSearchAggregations();

    @Nonnull
    List<SearchAggregation> getAppliedAggregations();

    @Nonnull
    List<SearchAggregation> getAvailableAggregations();

    @Nonnull
    List<Map<String, String>> getPaginationParameters();

    @Nonnull
    Map<String, String> getPreviousPageParameters();

    @Nonnull
    Map<String, String> getNextPageParameters();

}
