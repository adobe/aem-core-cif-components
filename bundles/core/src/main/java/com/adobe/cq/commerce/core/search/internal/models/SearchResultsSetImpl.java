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
package com.adobe.cq.commerce.core.search.internal.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.models.Pager;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.SorterKey;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public class SearchResultsSetImpl implements SearchResultsSet {

    private SearchOptions searchOptions;
    private Integer totalResults;
    private List<ProductListItem> productListItems = new ArrayList<>();
    private List<SearchAggregation> searchAggregations = new ArrayList<>();
    private Pager pager;
    private SorterImpl sorter = new SorterImpl();
    private List<Error> errors;

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
    public Pager getPager() {
        if (pager == null) {
            pager = new PagerImpl(getAppliedQueryParameters(), getTotalPages(), getSearchOptions().getCurrentPage());
        }
        return pager;
    }

    @Nonnull
    @Override
    public SorterImpl getSorter() {
        return sorter;
    }

    private int getTotalPages() {
        return getTotalResults() % getSearchOptions().getPageSize() == 0
            ? getTotalResults() / getSearchOptions().getPageSize()
            : getTotalResults() / getSearchOptions().getPageSize() + 1;
    }

    @Nonnull
    @Override
    public Map<String, String> getAppliedQueryParameters() {
        if (searchOptions == null) {
            return new HashMap<>();
        }

        return searchOptions.getAllFilters();
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
            .filter(searchAggregation -> !searchAggregation.getAppliedFilterValue().isPresent()
                && searchAggregation.getFilterable())
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public List<SearchAggregation> getAppliedAggregations() {
        return searchAggregations
            .stream()
            .filter(searchAggregation -> searchAggregation.getAppliedFilterValue().isPresent())
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasAggregations() {
        return !searchAggregations.isEmpty();
    }

    @Override
    public boolean hasPagination() {
        return getTotalPages() > 1;
    }

    @Override
    public boolean hasSorting() {
        List<SorterKey> keys = getSorter().getKeys();
        return keys != null && !keys.isEmpty();
    }

    @Override
    public List<Error> getErrors() {
        return errors == null ? Collections.emptyList() : errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }
}
