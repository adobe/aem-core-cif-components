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

import java.util.Map;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;

/**
 * Basic implementation of {@link SearchAggregationOption}.
 */
public class SearchAggregationOptionImpl implements SearchAggregationOption {

    private String filterValue;
    private String displayLabel;
    private int count;
    private Map<String, String> addFilterMap;
    private String pageUrl;

    @Nonnull
    @Override
    public String getFilterValue() {
        return filterValue;
    }

    @Nonnull
    @Override
    public String getDisplayLabel() {
        return displayLabel;
    }

    @Nonnull
    @Override
    public int getCount() {
        return count;
    }

    @Nonnull
    @Override
    public Map<String, String> getAddFilterMap() {
        return addFilterMap;
    }

    @Override
    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public void setFilterValue(final String filterValue) {
        this.filterValue = filterValue;
    }

    public void setDisplayLabel(final String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public void setAddFilterMap(final Map<String, String> addFilterMap) {
        this.addFilterMap = addFilterMap;
    }

}
