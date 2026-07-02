/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.mcp.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;

/**
 * Minimal {@link SearchOptions} implementation used by MCP tools to build search parameters from tool call
 * arguments.
 */
public class McpSearchOptions implements SearchOptions {

    private String query;
    private int currentPage = 1;
    private int pageSize = 20;
    private final Map<String, String> attributeFilters = new HashMap<>();
    private final List<SorterKey> sorterKeys = new ArrayList<>();

    public void setSearchQuery(String query) {
        this.query = query;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void putFilter(String key, String value) {
        attributeFilters.put(key, value);
    }

    @Override
    public Optional<String> getSearchQuery() {
        return Optional.ofNullable(query);
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public Map<String, String> getAttributeFilters() {
        return attributeFilters;
    }

    @Override
    public Map<String, String> getAllFilters() {
        return attributeFilters;
    }

    @Override
    public void addSorterKey(String name, String label, Sorter.Order preferredOrder) {
        // v1: sorting is not exposed to MCP tools
    }

    @Override
    public List<SorterKey> getSorterKeys() {
        return sorterKeys;
    }
}
