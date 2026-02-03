/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.core.search.models;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

/**
 * Represents a set of search results from a backend service. This would generally contain the actual {@link ProductListItem}s
 * as well as {@link SearchAggregation}s.
 */
@ConsumerType
public interface SearchResultsSet {

    /**
     * Get the search options used to provide this search result set.
     *
     * @return the {@link SearchOptions} used in the search
     */
    @Nonnull
    SearchOptions getSearchOptions();

    /**
     * Get the total number of results.
     *
     * @return the total number of results
     */
    @Nonnull
    Integer getTotalResults();

    /**
     * Get a map of the applied search query string parameters.
     *
     * @return the query string parameters (key value pairs)
     */
    @Nonnull
    Map<String, String> getAppliedQueryParameters();

    /**
     * Get the result product list items. These are the actual result of the search.
     *
     * @return the resulting products
     */
    @Nonnull
    List<ProductListItem> getProductListItems();

    /**
     * Get the available search aggregations for the given result set.
     *
     * @return the aggregations or filters available for the search result set.
     */
    @Nonnull
    List<SearchAggregation> getSearchAggregations();

    /**
     * Get the list of aggregations or filters that were applied in this search.
     *
     * @return the applied filter aggregations
     */
    @Nonnull
    List<SearchAggregation> getAppliedAggregations();

    /**
     * Get the available search aggregations for the given result set
     *
     * @return the available aggregations
     */
    @Nonnull
    List<SearchAggregation> getAvailableAggregations();

    /**
     * @return The pager model, responsible for handling the pagination of search results.
     */
    @Nonnull
    Pager getPager();

    /**
     * @return The sorter model, responsible for handling the sorting of search results.
     */
    @Nonnull
    Sorter getSorter();

    /**
     * @return {@code true} if the result set provides search aggregations for faceted search support, {@code false} otherwise
     */
    boolean hasAggregations();

    /**
     * @return {@code true} if the result set provides pagination, {@code false} otherwise
     */
    boolean hasPagination();

    /**
     * @return {@code true} if the result set provides support for sorting of the results, {@code false} otherwise
     */
    boolean hasSorting();

    /**
     * Get the list of errors that occurred during the search.
     *
     * @return the list of errors
     */
    default List<Error> getErrors() {
        return Collections.emptyList();
    }

    /**
     * Check if there were errors during the search.
     *
     * @return {@code true} if the result set contains errors, {@code false} otherwise
     */
    default boolean hasErrors() {
        return getErrors() != null && !getErrors().isEmpty();
    }
}
