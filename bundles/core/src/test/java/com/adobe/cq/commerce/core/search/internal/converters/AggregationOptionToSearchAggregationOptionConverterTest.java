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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.search.internal.models.FilterAttributeMetadataImpl;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AggregationOptionToSearchAggregationOptionConverterTest {

    private final static Integer BLACK_COUNT = 10;
    private final static String BLACK_ATTRIBUTE = "color";
    private final static String BLACK_LABEL = "Black";
    private final static String BLACK_VALUE = "50";

    @Mock
    AggregationOption aggregationOption;

    Map<String, String> existingFilters;

    AggregationOptionToSearchAggregationOptionConverter converterUnderTest;

    @Before
    public void setUp() throws Exception {

        when(aggregationOption.getCount()).thenReturn(BLACK_COUNT);
        when(aggregationOption.getLabel()).thenReturn(BLACK_LABEL);
        when(aggregationOption.getValue()).thenReturn(BLACK_VALUE);

        existingFilters = new HashMap<>();
        existingFilters.put("size", "49");
        existingFilters.put("color", "42");

    }

    @Test
    public void testShouldSetCount() {

        FilterAttributeMetadataImpl filterAttributeMetadata = new FilterAttributeMetadataImpl();
        filterAttributeMetadata.setAttributeInputType(FilterAttributeMetadataImpl.INPUT_TYPE_SELECT);
        filterAttributeMetadata.setAttributeCode(BLACK_ATTRIBUTE);

        converterUnderTest = new AggregationOptionToSearchAggregationOptionConverter(BLACK_ATTRIBUTE, filterAttributeMetadata,
            existingFilters);
        final SearchAggregationOption resultOption = converterUnderTest.apply(aggregationOption);

        assertThat(resultOption.getCount()).isEqualTo(BLACK_COUNT);
    }

    @Test
    public void testShouldSetDisplayLabel() {

        FilterAttributeMetadataImpl filterAttributeMetadata = new FilterAttributeMetadataImpl();
        filterAttributeMetadata.setAttributeInputType(FilterAttributeMetadataImpl.INPUT_TYPE_SELECT);
        filterAttributeMetadata.setAttributeCode(BLACK_ATTRIBUTE);

        converterUnderTest = new AggregationOptionToSearchAggregationOptionConverter(BLACK_ATTRIBUTE, filterAttributeMetadata,
            existingFilters);
        final SearchAggregationOption resultOption = converterUnderTest.apply(aggregationOption);

        assertThat(resultOption.getDisplayLabel()).isEqualTo(BLACK_LABEL);
    }

    @Test
    public void testShouldSetDisplayLabelForBooleanValues() {

        FilterAttributeMetadataImpl filterAttributeMetadata = new FilterAttributeMetadataImpl();
        filterAttributeMetadata.setAttributeInputType(FilterAttributeMetadataImpl.INPUT_TYPE_BOOLEAN);
        filterAttributeMetadata.setAttributeCode("blah");

        converterUnderTest = new AggregationOptionToSearchAggregationOptionConverter(BLACK_ATTRIBUTE, filterAttributeMetadata,
            existingFilters);
        when(aggregationOption.getLabel()).thenReturn("0");
        final SearchAggregationOption resultOption = converterUnderTest.apply(aggregationOption);
        assertThat(resultOption.getDisplayLabel()).isEqualTo("No");
    }

    @Test
    public void testShouldSetFilterValue() {
        FilterAttributeMetadataImpl filterAttributeMetadata = new FilterAttributeMetadataImpl();
        filterAttributeMetadata.setAttributeInputType(FilterAttributeMetadataImpl.INPUT_TYPE_SELECT);
        filterAttributeMetadata.setAttributeCode(BLACK_ATTRIBUTE);

        converterUnderTest = new AggregationOptionToSearchAggregationOptionConverter(BLACK_ATTRIBUTE, filterAttributeMetadata,
            existingFilters);
        final SearchAggregationOption resultOption = converterUnderTest.apply(aggregationOption);

        assertThat(resultOption.getFilterValue()).isEqualTo(BLACK_VALUE);
    }

}
