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

package com.adobe.cq.commerce.core.components.internal.models.v1.searchresults;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adobe.cq.commerce.core.search.internal.MagentoGraphQLSearchAggregationOptionAdapter;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MagentoGraphQLSearchAggregationOptionAdapterTest {

    private final static Integer BLACK_COUNT = 10;
    private final static String BLACK_ATTRIBUTE = "color";
    private final static String BLACK_LABEL = "Black";
    private final static String BLACK_VALUE = "50";

    AggregationOption aggregationOption;
    Map<String, String> existingFilters;

    @Before
    public void setUp() throws Exception {

        aggregationOption = mock(AggregationOption.class);
        when(aggregationOption.getCount()).thenReturn(BLACK_COUNT);
        when(aggregationOption.getLabel()).thenReturn(BLACK_LABEL);
        when(aggregationOption.getValue()).thenReturn(BLACK_VALUE);

        existingFilters = new HashMap<>();
        existingFilters.put("size", "49");
        existingFilters.put("color", "42");

    }

    @Test
    public void Should_Adapt_Count() {
        MagentoGraphQLSearchAggregationOptionAdapter adapter = new MagentoGraphQLSearchAggregationOptionAdapter(aggregationOption,
            BLACK_ATTRIBUTE,
            existingFilters);
        Assert.assertEquals(BLACK_COUNT, adapter.getCount());
    }

    @Test
    public void Should_Adapt_Display_Label() {
        MagentoGraphQLSearchAggregationOptionAdapter adapter = new MagentoGraphQLSearchAggregationOptionAdapter(aggregationOption,
            BLACK_ATTRIBUTE,
            existingFilters);
        Assert.assertEquals(BLACK_LABEL, adapter.getDisplayLabel());
    }

    @Test
    public void Should_Adapt_Filter_Value() {
        MagentoGraphQLSearchAggregationOptionAdapter adapter = new MagentoGraphQLSearchAggregationOptionAdapter(aggregationOption,
            BLACK_ATTRIBUTE,
            existingFilters);
        Assert.assertEquals(BLACK_VALUE, adapter.getFilterValue());
    }

}
