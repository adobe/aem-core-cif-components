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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang.WordUtils;

import com.adobe.cq.commerce.core.search.SearchAggregation;
import com.adobe.cq.commerce.core.search.SearchAggregationOption;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;

/**
 * This adapter class allows us to use MagentoGraphQL search aggregations as the data source for a component friendly SearchAggregation.
 * This
 * class "adapts" the data from the MagentoGraphQL response into something that hides the details of GraphQL and Magento from the frontend
 * components.
 */
public class MagentoGraphQLSearchAggregationAdapter implements SearchAggregation {

    private final Aggregation aggregation;

    private final String setValue;

    private final Boolean filterable;

    private final Map<String, String> filters;

    private static final String PRICE_IDENTIFIER = "price";

    /**
     * Constructor takes in a GraphQL Aggregation object as well as some additional metadata required to build a complete SearchAggregation.
     *
     * @param aggregation graphql data object with aggregation information
     * @param setFilterValue the filter value currently set
     * @param filterable whether or not the aggregation option can be applied as a filter
     */
    public MagentoGraphQLSearchAggregationAdapter(
                                                  final Aggregation aggregation,
                                                  final String setFilterValue,
                                                  final Boolean filterable,
                                                  final Map<String, String> filters) {
        this.aggregation = aggregation;
        this.setValue = setFilterValue;
        this.filterable = filterable;
        this.filters = filters;
    }

    @Nonnull
    @Override
    public Optional<String> getAppliedFilterValue() {
        return Optional.ofNullable(setValue);
    }

    @Nonnull
    @Override
    public Optional<String> getAppliedFilterDisplayLabel() {
        if (setValue == null) {
            return Optional.empty();
        }
        final Optional<AggregationOption> matchedAggregate = aggregation.getOptions().stream()
            .filter(item -> setValue.equals(item.getValue()))
            .findFirst();
        Optional<String> candidate = matchedAggregate.isPresent() ? Optional.of(matchedAggregate.get().getLabel()) : Optional.empty();

        if (candidate.isPresent()) {
            // Special case to handle boolean / "bucket" values in Magento's GraphQL API
            if (aggregation.getAttributeCode().endsWith("_bucket")) {
                return "0".equalsIgnoreCase(setValue) ? Optional.of("No") : Optional.of("Yes");
            }
            return candidate;
        }

        // There are currently a few special cases that need to be considered, the first being that the `price` filter may return
        // filters that are different / don't match the actual filter being applied, so for that case we will manually set the label value
        // to match the filter that is applied
        if (PRICE_IDENTIFIER.equalsIgnoreCase(getIdentifier())) {
            return Optional.of(setValue.replace('_', '-'));
        }

        return Optional.empty();

    }

    @Nonnull
    @Override
    public boolean getFilterable() {
        if (filterable == null) {
            return false;
        }
        return filterable;
    }

    @Nonnull
    @Override
    public String getIdentifier() {
        return aggregation.getAttributeCode().replace("_bucket", "");
    }

    @Nonnull
    @Override
    public String getDisplayLabel() {

        // this is an unfortunate special case, boolean values in Magento do not come across with
        // the proper display label, so for the time being we're going to do a simple string manipulation
        // until this is resolved with the Magento GraphQL API.
        if (aggregation.getLabel().endsWith("_bucket")) {
            return WordUtils.capitalize(aggregation.getLabel().replace("_bucket", "").replace("_", " "));
        }

        return aggregation.getLabel();
    }

    @Nonnull
    @Override
    public Integer getOptionCount() {
        if (aggregation == null) {
            return 0;
        }
        return aggregation.getCount();
    }

    @Nonnull
    @Override
    public List<SearchAggregationOption> getOptions() {
        return aggregation.getOptions()
            .stream()
            .map(option -> new MagentoGraphQLSearchAggregationOptionAdapter(option, aggregation.getAttributeCode(), filters))
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public Map<String, String> getRemoveFilterMap() {
        return filters.entrySet()
            .stream()
            .filter(stringStringEntry -> !stringStringEntry.getKey().equalsIgnoreCase(aggregation.getAttributeCode()))
            .collect(Collectors.toMap(o -> o.getKey(), o -> o.getValue()));
    }
}
