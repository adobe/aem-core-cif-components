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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.search.internal.models.FilterAttributeMetadataImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AggregationToSearchAggregationConverterTest {

    @Mock
    Aggregation testAggregation;

    @Mock
    AggregationOption testAggregationOption1;
    @Mock
    AggregationOption testAggregationOption2;

    Map<String, String> appliedFilters;
    List<FilterAttributeMetadata> availableAttributes;

    AggregationToSearchAggregationConverter converterUnderTest;

    private static final String AGGREGATION_CODE = "color";
    private static final String AGGREGATION_LABEL = "Color";
    private static final int AGGREGATION_COUNT = 1;

    private static final String AGGREGATION_OPTION_VALUE = "1232";
    private static final String AGGREGATION_OPTION_LABEL = "Green";
    private static final int AGGREGATION_OPTION_COUNT = 9;

    @Before
    public void setUp() {

        when(testAggregationOption1.getCount()).thenReturn(AGGREGATION_OPTION_COUNT);
        when(testAggregationOption1.getLabel()).thenReturn(AGGREGATION_OPTION_LABEL);
        when(testAggregationOption1.getValue()).thenReturn(AGGREGATION_OPTION_VALUE);

        when(testAggregation.getAttributeCode()).thenReturn(AGGREGATION_CODE);
        when(testAggregation.getCount()).thenReturn(AGGREGATION_COUNT);
        when(testAggregation.getLabel()).thenReturn(AGGREGATION_LABEL);
        when(testAggregation.getOptions()).thenReturn(Arrays.asList(
            testAggregationOption1));
    }

    @Test
    public void testConvertsExpectedAggregationIfNotFilterable() {
        appliedFilters = new HashMap<>();
        availableAttributes = new ArrayList<>();

        converterUnderTest = new AggregationToSearchAggregationConverter(appliedFilters, availableAttributes);

        final SearchAggregation result = converterUnderTest.apply(testAggregation);

        assertThat(result).isNotNull();
        assertThat(result.getIdentifier()).isEqualTo(AGGREGATION_CODE);
        assertThat(result.getFilterable()).isFalse();
        assertThat(result.getDisplayLabel()).isEqualTo(AGGREGATION_LABEL);
        assertThat(result.getOptionCount()).isEqualTo(AGGREGATION_COUNT);
        assertThat(result.getAppliedFilterDisplayLabel()).isEmpty();
        assertThat(result.getAppliedFilterValue()).isEmpty();
        assertThat(result.getOptions()).hasSize(1);
    }

    @Test
    public void testConvertsExpectedAggregationIfFilterable() {
        appliedFilters = new HashMap<>();
        appliedFilters.put(AGGREGATION_CODE, AGGREGATION_OPTION_VALUE);
        availableAttributes = new ArrayList<>();
        FilterAttributeMetadataImpl filterAttributeMetadata = new FilterAttributeMetadataImpl();
        filterAttributeMetadata.setAttributeCode(AGGREGATION_CODE);
        filterAttributeMetadata.setAttributeInputType(FilterAttributeMetadataImpl.INPUT_TYPE_SELECT);
        availableAttributes.add(filterAttributeMetadata);

        converterUnderTest = new AggregationToSearchAggregationConverter(appliedFilters, availableAttributes);

        final SearchAggregation result = converterUnderTest.apply(testAggregation);

        assertThat(result.getFilterable()).isTrue();
        assertThat(result.getAppliedFilterValue()).hasValue(AGGREGATION_OPTION_VALUE);
        assertThat(result.getAppliedFilterDisplayLabel()).hasValue(AGGREGATION_OPTION_LABEL);
    }

    @Test
    public void testConvertsExpectedAggregationIfSetAndBoolean() {

        when(testAggregationOption1.getCount()).thenReturn(2);
        when(testAggregationOption1.getLabel()).thenReturn("0");
        when(testAggregationOption1.getValue()).thenReturn("0");
        when(testAggregationOption2.getCount()).thenReturn(5);
        when(testAggregationOption2.getLabel()).thenReturn("1");
        when(testAggregationOption2.getValue()).thenReturn("1");

        when(testAggregation.getAttributeCode()).thenReturn(AGGREGATION_CODE);
        when(testAggregation.getCount()).thenReturn(2);
        when(testAggregation.getLabel()).thenReturn(AGGREGATION_LABEL);
        when(testAggregation.getOptions()).thenReturn(Arrays.asList(
            testAggregationOption1,
            testAggregationOption2));

        appliedFilters = new HashMap<>();
        appliedFilters.put(AGGREGATION_CODE, "0");
        availableAttributes = new ArrayList<>();

        FilterAttributeMetadataImpl filterAttributeMetadata = new FilterAttributeMetadataImpl();
        filterAttributeMetadata.setAttributeCode(AGGREGATION_CODE);
        filterAttributeMetadata.setAttributeInputType(FilterAttributeMetadataImpl.INPUT_TYPE_BOOLEAN);
        availableAttributes.add(filterAttributeMetadata);

        converterUnderTest = new AggregationToSearchAggregationConverter(appliedFilters, availableAttributes);

        final SearchAggregation result = converterUnderTest.apply(testAggregation);

        assertThat(result.getFilterable()).isNotNull();
        assertThat(result.getAppliedFilterValue()).hasValue("0");
        assertThat(result.getAppliedFilterDisplayLabel()).hasValue("No");
    }
}
