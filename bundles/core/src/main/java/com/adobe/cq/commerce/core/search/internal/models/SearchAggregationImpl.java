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
import java.util.List;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;
import com.adobe.cq.commerce.core.search.models.SearchFilter;

/**
 * Basic implementation of {@link SearchAggregation}.
 */
public class SearchAggregationImpl implements SearchAggregation {

    String identifier;
    String displayLabel;
    List<SearchFilter> appliedFilters = new ArrayList<>();
    boolean filterable = false;
    int count = 0;
    List<SearchAggregationOption> options = new ArrayList<>();

    @Nonnull
    @Override
    public List<SearchFilter> getAppliedFilters() {
        return appliedFilters;
    }

    @Nonnull
    @Override
    public boolean getFilterable() {
        return filterable;
    }

    @Nonnull
    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Nonnull
    @Override
    public String getDisplayLabel() {
        return displayLabel;
    }

    @Nonnull
    @Override
    public int getOptionCount() {
        return count;
    }

    @Nonnull
    @Override
    public List<SearchAggregationOption> getOptions() {
        return options;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public void setDisplayLabel(final String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public void addFilter(final SearchFilter filter) {
        this.appliedFilters.add(filter);
    }

    public void setFilterable(final boolean filterable) {
        this.filterable = filterable;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public void setOptions(final List<SearchAggregationOption> options) {
        this.options = options;
    }
}
