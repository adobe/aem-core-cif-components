/*
 *   Copyright 2019 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.adobe.cq.commerce.core.search.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.SearchAggregation;
import com.adobe.cq.commerce.core.search.SearchOptions;
import com.adobe.cq.commerce.core.search.SearchResultsSet;

public class SearchResultsSetImpl implements SearchResultsSet {

    private SearchOptions searchOptions;
    private Integer totalResults;
    private List<ProductListItem> productListItems = new ArrayList<>();
    private List<SearchAggregation> searchAggregations = new ArrayList<>();
    private static final String SEARCH_FILTER_QUERY_STRING = "search_query";

    @Nonnull
    @Override
    public SearchOptions getSearchOptions() {
        return searchOptions == null ? new SearchOptionsImpl() : searchOptions;
    }

    @Nonnull
    @Override
    public Integer getTotalResults() {
        return totalResults == null ? 0 : totalResults;
    }

    @Nonnull
    @Override
    public List<ProductListItem> getProductListItems() {
        return productListItems;
    }

    @Nonnull
    @Override
    public List<SearchAggregation> getSearchAggregations() {
        return searchAggregations;
    }

    @Nonnull
    @Override
    public List<Map<String, String>> getPaginationParameters() {
        final Integer totalPages = getTotalResults() % getSearchOptions().getPageSize() == 0
            ? getTotalResults() / getSearchOptions().getPageSize()
            : getTotalResults() / getSearchOptions().getPageSize() + 1;

        List<Map<String, String>> pages = new ArrayList<>();

        Map<String, String> existingQueryParameters = getAppliedQueryParameters();

        for (Integer currentPage = 1; currentPage < totalPages; currentPage++) {
            Map<String, String> pageParameters = new HashMap<>();
            pageParameters.putAll(existingQueryParameters);
            pageParameters.put("page", currentPage.toString());
            pages.add(pageParameters);
        }

        return pages;
    }

    @Nonnull
    @Override
    public Map<String, String> getPreviousPageParameters(final Map<String, String> appliedParameters) {
        Integer previousPage = getSearchOptions().getCurrentPage() <= 1 ? 1 : getSearchOptions().getCurrentPage() - 1;
        Map<String, String> parameters = new HashMap<>(appliedParameters);
        parameters.put("page", previousPage.toString());
        return parameters;
    }

    @Nonnull
    @Override
    public Map<String, String> getNextPageParameters(final Map<String, String> appliedParameters) {
        final Integer totalPages = getTotalResults() % getSearchOptions().getPageSize() == 0
            ? getTotalResults() / getSearchOptions().getPageSize()
            : getTotalResults() / getSearchOptions().getPageSize() + 1;
        Integer nextPage = getSearchOptions().getCurrentPage() >= totalPages ? totalPages : getSearchOptions().getCurrentPage() + 1;
        Map<String, String> parameters = new HashMap<>(appliedParameters);
        parameters.put("page", nextPage.toString());
        return parameters;
    }

    @Nonnull
    @Override
    public Map<String, String> getAppliedQueryParameters() {
        if (searchOptions == null) {
            return new HashMap<>();
        }
        Map<String, String> appliedParameters = new HashMap<>(searchOptions.getAllFilters());
        searchOptions.getSearchQuery().ifPresent(query -> appliedParameters.put(SEARCH_FILTER_QUERY_STRING, query));

        return appliedParameters;
    }

    public void setTotalResults(final Integer totalResults) {
        this.totalResults = totalResults;
    }

    public void setSearchOptions(final SearchOptions searchOptions) {
        this.searchOptions = searchOptions;
    }

    public void setProductListItems(final List<ProductListItem> productListItems) {
        this.productListItems = productListItems;
    }

    public void setSearchAggregations(final List<SearchAggregation> searchAggregations) {
        this.searchAggregations = searchAggregations;
    }

    @Nonnull
    @Override
    public List<SearchAggregation> getAvailableAggregations() {
        return searchAggregations
            .stream()
            .filter(searchAggregation -> {
                return !searchAggregation.getAppliedFilterValue().isPresent()
                    && searchAggregation.getFilterable()
                    && !"category_id".equals(searchAggregation.getIdentifier());
            })
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public List<SearchAggregation> getAppliedAggregations() {
        return searchAggregations
            .stream()
            .filter(searchAggregation -> searchAggregation.getAppliedFilterValue().isPresent()
                && !"category_id".equals(searchAggregation.getIdentifier()))
            .collect(Collectors.toList());
    }

}
