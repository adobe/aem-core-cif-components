/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.storefrontcontext;

import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultCategory;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;

public class SearchResultCategoryImpl implements SearchResultCategory {
    private final SearchAggregationOption searchAggregationOption;

    public SearchResultCategoryImpl(SearchAggregationOption searchAggregationOption) {
        this.searchAggregationOption = searchAggregationOption;
    }

    @Override
    public String getName() {
        return searchAggregationOption.getDisplayLabel();
    }

    @Override
    public String getUrl() {
        return StringUtils.EMPTY;
    }

    @Override
    public int getRank() {
        return 0;
    }
}
