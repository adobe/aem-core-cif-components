/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

import java.util.Map;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.search.models.SearchFilter;

public class SearchFilterImpl implements SearchFilter {
    private String value;
    private String displayLabel;
    private Map<String, String[]> removeFilterMap;

    public SearchFilterImpl(String value, String displayLabel, Map<String, String[]> removeFilterMap) {
        this.value = value;
        this.displayLabel = displayLabel;
        this.removeFilterMap = removeFilterMap;
    }

    @Nonnull
    @Override
    public String getValue() {
        return value;
    }

    @Nonnull
    @Override
    public String getDisplayLabel() {
        return displayLabel;
    }

    @Nonnull
    @Override
    public Map<String, String[]> getRemoveFilterMap() {
        return removeFilterMap;
    }
}
