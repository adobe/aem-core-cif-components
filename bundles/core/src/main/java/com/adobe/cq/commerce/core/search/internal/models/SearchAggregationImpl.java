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
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;

/**
 * Basic implementation of {@link SearchAggregation}.
 */
public class SearchAggregationImpl implements SearchAggregation {

    String identifier;
    String displayLabel;
    String appliedFilterValue = null;
    String appliedFilterDisplayLabel = null;
    boolean filterable = false;
    int count = 0;
    List<SearchAggregationOption> options = new ArrayList<>();
    Map<String, String> removeFilters;

    @Nonnull
    @Override
    public Optional<String> getAppliedFilterValue() {
        return Optional.ofNullable(appliedFilterValue);
    }

    @Nonnull
    @Override
    public Optional<String> getAppliedFilterDisplayLabel() {
        return Optional.ofNullable(appliedFilterDisplayLabel);
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

    @Nonnull
    @Override
    public Map<String, String> getRemoveFilterMap() {
        return removeFilters;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public void setDisplayLabel(final String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public void setAppliedFilterValue(final String appliedFilterValue) {
        this.appliedFilterValue = appliedFilterValue;
    }

    public void setAppliedFilterDisplayLabel(final String appliedFilterDisplayLabel) {
        this.appliedFilterDisplayLabel = appliedFilterDisplayLabel;
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

    public void setRemoveFilters(final Map<String, String> removeFilters) {
        this.removeFilters = removeFilters;
    }
}
