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

package com.adobe.cq.commerce.core.search.internal.converters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.adobe.cq.commerce.core.search.internal.models.FilterAttributeMetadataImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchAggregationImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;

public class AggregationToSearchAggregationConverter implements Function<Aggregation, SearchAggregation> {

    private final Map<String, String> appliedFilters;

    private final List<FilterAttributeMetadata> availableFilters;

    private static final String PRICE_IDENTIFIER = "price";

    public AggregationToSearchAggregationConverter(final Map<String, String> appliedFilters,
                                                   final List<FilterAttributeMetadata> availableFilters) {
        this.appliedFilters = appliedFilters == null ? new HashMap<>() : appliedFilters;
        this.availableFilters = availableFilters == null ? new ArrayList<>() : availableFilters;
    }

    @Override
    public SearchAggregation apply(final Aggregation aggregation) {

        Optional<String> setFilterValue = Optional.empty();
        if (appliedFilters.get(aggregation.getAttributeCode()) != null) {
            setFilterValue = Optional.ofNullable(appliedFilters.get(aggregation.getAttributeCode()));
        }
        // filterable will be true or false depending on whether or not the attribute appears in the list of available filters
        // provided by the introspection query
        final String identifier = aggregation.getAttributeCode();

        final Optional<FilterAttributeMetadata> attributeMetadata = availableFilters.stream()
            .filter(filterCandidate -> filterCandidate.getAttributeCode().equals(identifier)).findFirst();
        final boolean filterable = attributeMetadata.isPresent();

        SearchAggregationImpl searchAggregation = new SearchAggregationImpl();
        searchAggregation.setFilterable(filterable);
        searchAggregation.setCount(aggregation == null ? 0 : aggregation.getCount());
        searchAggregation.setOptions(getOptions(aggregation, appliedFilters));
        searchAggregation.setDisplayLabel(aggregation.getLabel());
        searchAggregation.setIdentifier(identifier);
        searchAggregation.setRemoveFilters(getRemoveFilters(aggregation, appliedFilters));

        setFilterValue.ifPresent(appliedValue -> {
            searchAggregation.setAppliedFilterValue(appliedValue);
            searchAggregation.setAppliedFilterDisplayLabel(getAppliedFilterDisplayLabel(aggregation, appliedValue, identifier,
                attributeMetadata.get()));
        });

        return searchAggregation;
    }

    private List<SearchAggregationOption> getOptions(final Aggregation aggregation,
        final Map<String, String> appliedFilters) {

        final FilterAttributeMetadata filterAttributeMetadata = availableFilters.stream()
            .filter(item -> item.getAttributeCode().equals(aggregation.getAttributeCode())).findFirst().orElse(null);

        AggregationOptionToSearchAggregationOptionConverter converter = new AggregationOptionToSearchAggregationOptionConverter(
            aggregation.getAttributeCode(), filterAttributeMetadata, appliedFilters);

        return aggregation.getOptions()
            .stream()
            .map(converter)
            .collect(Collectors.toList());
    }

    private String getAppliedFilterDisplayLabel(Aggregation aggregation, String setFilterValue, String identifier,
        FilterAttributeMetadata attributeMetadata) {
        if (setFilterValue == null) {
            return null;
        }
        final Optional<AggregationOption> matchedAggregate = aggregation.getOptions().stream()
            .filter(item -> setFilterValue.equals(item.getValue()))
            .findFirst();
        String candidate = matchedAggregate.isPresent() ? matchedAggregate.get().getLabel() : null;

        if (candidate != null) {
            // Special case to handle boolean / "bucket" values in Magento's GraphQL API
            if (attributeMetadata != null && FilterAttributeMetadataImpl.INPUT_TYPE_BOOLEAN.equals(attributeMetadata
                .getAttributeInputType())) {
                return "0".equalsIgnoreCase(setFilterValue) ? "No" : "Yes";
            }
            return candidate;
        }

        // There are currently a few special cases that need to be considered, the first being that the `price` filter may return
        // filters that are different / don't match the actual filter being applied, so for that case we will manually set the label value
        // to match the filter that is applied
        if (PRICE_IDENTIFIER.equalsIgnoreCase(identifier)) {
            return setFilterValue.replace('_', '-');
        }

        return null;
    }

    public Map<String, String> getRemoveFilters(Aggregation aggregation, Map<String, String> appliedFilters) {
        return appliedFilters.entrySet()
            .stream()
            .filter(stringStringEntry -> !stringStringEntry.getKey().equalsIgnoreCase(aggregation.getAttributeCode()))
            .collect(Collectors.toMap(o -> o.getKey(), o -> o.getValue()));
    }
}
