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
import java.util.function.Function;
import java.util.stream.Collectors;

import com.adobe.cq.commerce.core.search.SearchAggregationOption;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;

/**
 * This class is responsible for converting a Magento GraphQL {@link AggregationOption} domain object to a Sling Model / POJO friendly
 * concrete
 * class {@link SearchAggregationOption}.
 */
public class AggregationOptionToSearchAggregationOptionConverter implements Function<AggregationOption, SearchAggregationOption> {

    private final String attributeCode;

    private final Map<String, String> filters;

    private boolean isBooleanAttribute;

    public AggregationOptionToSearchAggregationOptionConverter(final String attributeCode, final Map<String, String> filters) {

        // This is a "special case" for the time, which is that some attributes end in "_bucket" even though
        // _bucket is not actually part of the attribute code in Magento. We'll fix that for the time being with a
        // replacement, and we'll note that this is a boolean attribute so we can return more "friendly" yes/no
        // option labels
        if (attributeCode.endsWith("_bucket")) {
            this.attributeCode = attributeCode.replace("_bucket", "");
            this.isBooleanAttribute = true;
        } else {
            this.attributeCode = attributeCode;
            this.isBooleanAttribute = false;
        }

        this.filters = filters;
    }

    @Override
    public SearchAggregationOption apply(final AggregationOption aggregationOption) {
        SearchAggregationOptionImpl searchAggregationOption = new SearchAggregationOptionImpl();

        // Special case handling for boolean values to return a friendlier "yes/no" response
        if (isBooleanAttribute) {
            searchAggregationOption.setDisplayLabel(("0".equalsIgnoreCase(aggregationOption.getLabel()) ? "No" : "Yes"));
        } else {
            searchAggregationOption.setDisplayLabel(aggregationOption.getLabel());
        }

        searchAggregationOption.setCount(aggregationOption.getCount());
        searchAggregationOption.setFilterValue(aggregationOption.getValue());

        // this is done for convenience sake so we have all filters available
        Map<String, String> newFilters = new HashMap<>(filters);
        newFilters.put(attributeCode, aggregationOption.getValue());
        searchAggregationOption.setAddFilterMap(newFilters.entrySet().stream()
            .filter(item -> !item.getKey().equals("page"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        return searchAggregationOption;
    }

}
