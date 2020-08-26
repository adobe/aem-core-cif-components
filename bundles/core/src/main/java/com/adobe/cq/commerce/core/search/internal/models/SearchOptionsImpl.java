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

package com.adobe.cq.commerce.core.search.internal.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;

public class SearchOptionsImpl implements SearchOptions {

    public static final Integer PAGE_SIZE_DEFAULT = 6;
    public static final String CATEGORY_ID_PARAMETER_ID = "category_id";
    public static final String SEARCH_QUERY_PARAMETER_ID = "search_query";
    public static final String CURRENT_PAGE_PARAMETER_ID = "page";

    Map<String, String> attributeFilters;

    String categoryId;

    String searchQuery;

    Integer currentPage = 1;

    Integer pageSize = PAGE_SIZE_DEFAULT;

    List<SorterKey> sorterKeys = new ArrayList<>();

    public SearchOptionsImpl() {
        attributeFilters = new HashMap<>();
    }

    @Override
    public Map<String, String> getAllFilters() {
        Map<String, String> allFilters = new HashMap<>(getAttributeFilters());

        if (getCategoryId().isPresent()) {
            allFilters.put(CATEGORY_ID_PARAMETER_ID, getCategoryId().get());
        }
        if (getSearchQuery().isPresent()) {
            allFilters.put(SEARCH_QUERY_PARAMETER_ID, getSearchQuery().get());
        }

        return allFilters;
    }

    @Override
    public Map<String, String> getAttributeFilters() {
        return attributeFilters;
    }

    public void setAttributeFilters(final Map<String, String> attributeFilters) {
        this.attributeFilters = attributeFilters;
    }

    public Optional<String> getCategoryId() {
        return Optional.ofNullable(categoryId);
    }

    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public Optional<String> getSearchQuery() {
        return Optional.ofNullable(searchQuery);
    }

    public void setSearchQuery(final String searchQuery) {
        this.searchQuery = searchQuery;
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(final Integer currentPage) {
        this.currentPage = currentPage;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public void addSorterKey(String name, String label, Sorter.Order preferredOrder) {
        SorterKeyImpl key = new SorterKeyImpl(name, label);
        if (preferredOrder != null) {
            key.setOrder(preferredOrder);
        }
        sorterKeys.add(key);
    }

    @Override
    public List<SorterKey> getSorterKeys() {
        return sorterKeys;
    }
}
