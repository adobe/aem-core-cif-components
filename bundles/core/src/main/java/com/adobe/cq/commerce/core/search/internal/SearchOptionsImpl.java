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

package com.adobe.cq.commerce.core.search.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.commerce.core.search.SearchOptions;

public class SearchOptionsImpl implements SearchOptions {

    private static final Integer PAGE_SIZE_DEFAULT = 6;
    private static final String CATEGORY_ID_PAREMETER_ID = "category_id";
    private static final String SEARCH_QUERY_PAREMETER_ID = "search_query";

    Map<String, String> attributeFilters;

    String categoryId;

    String searchQuery;

    Integer currentPage = 1;

    Integer pageSize = PAGE_SIZE_DEFAULT;

    public SearchOptionsImpl() {
        attributeFilters = new HashMap<>();
    }

    @Override
    public Map<String, String> getAllFilters() {
        Map<String, String> allFilters = new HashMap<>(getAttributeFilters());

        if (getCategoryId().isPresent()) {
            allFilters.put(CATEGORY_ID_PAREMETER_ID, getCategoryId().get());
        }
        if (getSearchQuery().isPresent()) {
            allFilters.put(SEARCH_QUERY_PAREMETER_ID, getSearchQuery().get());
        }

        return allFilters;
    }

    @Override
    public Map<String, String> getAttributeFilters() {
        return attributeFilters;
    }

    @Override
    public void setAttributeFilters(final Map<String, String> attributeFilters) {
        this.attributeFilters = attributeFilters;
    }

    @Override
    public Optional<String> getCategoryId() {
        return Optional.ofNullable(categoryId);
    }

    @Override
    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public Optional<String> getSearchQuery() {
        return Optional.ofNullable(searchQuery);
    }

    @Override
    public void setSearchQuery(final String searchQuery) {
        this.searchQuery = processSearchTerm(searchQuery);
    }

    @Override
    public Integer getCurrentPage() {
        return currentPage;
    }

    @Override
    public void setCurrentPage(final Integer currentPage) {
        this.currentPage = currentPage;
    }

    @Override
    public Integer getPageSize() {
        return pageSize;
    }

    @Override
    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Processes the search term to prepare it for the actual query.
     * This method just prepends/appends the default wildcard character % to the search term. Overriding methods can implement their own
     * processing.
     *
     * @return the processed search term, by default {@code "%searchTerm%"}
     */
    @Nonnull
    protected String processSearchTerm(String searchTerm) {
        if (!StringUtils.isAlphanumericSpace(searchTerm)) {
            return "";
        }
        return searchTerm;
    }

}
