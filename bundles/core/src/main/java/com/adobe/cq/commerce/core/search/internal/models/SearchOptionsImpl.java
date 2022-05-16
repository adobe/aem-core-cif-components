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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;

public class SearchOptionsImpl implements SearchOptions {

    public static final Integer PAGE_SIZE_DEFAULT = 6;
    public static final String CATEGORY_UID_PARAMETER_ID = "category_uid";
    public static final String SEARCH_QUERY_PARAMETER_ID = "search_query";
    public static final String CURRENT_PAGE_PARAMETER_ID = "page";

    Map<String, String> attributeFilters;

    String categoryUid;

    String searchQuery;

    Integer currentPage = 1;

    Integer pageSize = PAGE_SIZE_DEFAULT;

    List<SorterKey> sorterKeys = new ArrayList<>();

    SorterKey defaultSorter;

    public SearchOptionsImpl() {
        attributeFilters = new HashMap<>();
    }

    public SearchOptionsImpl(SearchOptions searchOptions) {
        this.currentPage = searchOptions.getCurrentPage();
        this.pageSize = searchOptions.getPageSize();
        this.attributeFilters = searchOptions.getAttributeFilters();
        this.sorterKeys = searchOptions.getSorterKeys();

        Map<String, String> allFilters = searchOptions.getAllFilters();

        if (allFilters.containsKey(CATEGORY_UID_PARAMETER_ID)) {
            categoryUid = allFilters.get(CATEGORY_UID_PARAMETER_ID);
        }

        if (searchOptions.getSearchQuery().isPresent()) {
            searchQuery = searchOptions.getSearchQuery().get();
        }

        this.defaultSorter = searchOptions.getDefaultSorter();
    }

    @Override
    public Map<String, String> getAllFilters() {
        Map<String, String> allFilters = new HashMap<>(getAttributeFilters());

        if (getCategoryUid().isPresent()) {
            allFilters.put(CATEGORY_UID_PARAMETER_ID, getCategoryUid().get());
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

    public Optional<String> getCategoryUid() {
        return Optional.ofNullable(categoryUid);
    }

    public void setCategoryUid(final String categoryUid) {
        this.categoryUid = categoryUid;
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

    @Override
    public void setDefaultSorter(String sortField, Sorter.Order sortOrder) {
        SorterKeyImpl defaultSorter = new SorterKeyImpl(sortField, sortField);
        defaultSorter.setOrder(sortOrder);
        this.defaultSorter = defaultSorter;
    }

    @Override
    public SorterKey getDefaultSorter() {
        return defaultSorter;
    }
}
