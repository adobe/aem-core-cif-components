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

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.search.SearchAggregationOption;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;

public class MagentoGraphQLSearchAggregationOptionAdapter implements SearchAggregationOption {

    private final AggregationOption aggregationOption;

    private final String attributeCode;

    private final Map<String, String> filters;

    private Boolean isBooleanAttribute;

    public MagentoGraphQLSearchAggregationOptionAdapter(AggregationOption aggregationOption, final String attributeCode,
                                                        final Map<String, String> filters) {
        this.aggregationOption = aggregationOption;

        // This is a "special case" for the time, which is that some attributes end in "_bucket" even though
        // _bucket is not actually part of the attribute code in Magento. We'll fix that for the time being with a
        // replacement, and we'll note that this is a boolean attribute so we can return more "friendly" yes/no
        // option labels
        if (attributeCode.endsWith("_bucket")) {
            this.attributeCode = attributeCode.replace("_bucket", "");
            isBooleanAttribute = true;
        } else {
            this.attributeCode = attributeCode;
            isBooleanAttribute = false;
        }

        this.filters = new HashMap(filters);
        this.filters.put(attributeCode, aggregationOption.getValue());
    }

    @Nonnull
    @Override
    public String getFilterValue() {
        return aggregationOption.getValue();
    }

    @Nonnull
    @Override
    public String getDisplayLabel() {
        // Special case handling for boolean values to return a friendlier "yes/no" response
        if (isBooleanAttribute) {
            return "0".equalsIgnoreCase(aggregationOption.getLabel()) ? "No" : "Yes";
        }
        return aggregationOption.getLabel();
    }

    @Nonnull
    @Override
    public Integer getCount() {
        return aggregationOption.getCount();
    }

    @Nonnull
    @Override
    public Map<String, String> getAddFilterMap() {
        return filters;
    }
}
