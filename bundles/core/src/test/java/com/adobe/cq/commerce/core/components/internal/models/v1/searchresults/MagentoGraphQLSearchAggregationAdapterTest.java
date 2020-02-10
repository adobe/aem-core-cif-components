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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adobe.cq.commerce.core.search.internal.MagentoGraphQLSearchAggregationAdapter;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MagentoGraphQLSearchAggregationAdapterTest {

    private final static String ATTRIBUTE_CODE = "color";
    private final static String ATTRIBUTE_LABEL = "Color";

    private final static Integer BLACK_COUNT = 10;
    private final static String BLACK_LABEL = "Black";
    private final static String BLACK_VALUE = "50";

    private final static Integer RED_COUNT = 1;
    private final static String RED_LABEL = "Red";
    private final static String RED_VALUE = "42";

    Aggregation emptyAggregation;
    Aggregation multipleAggregation;
    Aggregation bucketAggregation;
    Map<String, String> existingFilters;

    @Before
    public void setUp() throws Exception {
        emptyAggregation = mock(Aggregation.class);
        when(emptyAggregation.getAttributeCode()).thenReturn(ATTRIBUTE_CODE);
        when(emptyAggregation.getLabel()).thenReturn(ATTRIBUTE_LABEL);
        when(emptyAggregation.getCount()).thenReturn(1);
        when(emptyAggregation.getOptions()).thenReturn(new ArrayList<>());

        multipleAggregation = mock(Aggregation.class);

        AggregationOption blackAggregationOption = mock(AggregationOption.class);
        when(blackAggregationOption.getCount()).thenReturn(BLACK_COUNT);
        when(blackAggregationOption.getLabel()).thenReturn(BLACK_LABEL);
        when(blackAggregationOption.getValue()).thenReturn(BLACK_VALUE);
        AggregationOption redAggregationOption = mock(AggregationOption.class);
        when(redAggregationOption.getCount()).thenReturn(RED_COUNT);
        when(redAggregationOption.getLabel()).thenReturn(RED_LABEL);
        when(redAggregationOption.getValue()).thenReturn(RED_VALUE);

        when(multipleAggregation.getOptions()).thenReturn(Arrays.asList(blackAggregationOption, redAggregationOption));
        when(multipleAggregation.getCount()).thenReturn(2);
        when(multipleAggregation.getLabel()).thenReturn(ATTRIBUTE_LABEL);
        when(multipleAggregation.getAttributeCode()).thenReturn(ATTRIBUTE_CODE);

        bucketAggregation = mock(Aggregation.class);

        AggregationOption bucketYesAggregationOption = mock(AggregationOption.class);
        when(bucketYesAggregationOption.getCount()).thenReturn(2);
        when(bucketYesAggregationOption.getLabel()).thenReturn("1");
        when(bucketYesAggregationOption.getValue()).thenReturn("1");

        when(bucketAggregation.getOptions()).thenReturn(Arrays.asList(bucketYesAggregationOption));
        when(bucketAggregation.getCount()).thenReturn(1);
        when(bucketAggregation.getLabel()).thenReturn("color_thing_bucket");
        when(bucketAggregation.getAttributeCode()).thenReturn("color_thing_bucket");

        existingFilters = new HashMap<>();
        existingFilters.put("size", "49");
        existingFilters.put("color", "42");
        existingFilters.put("color_thing_bucket", "1");

    }

    @Test
    public void testGetDisplayLabel() {
        MagentoGraphQLSearchAggregationAdapter adapter = new MagentoGraphQLSearchAggregationAdapter(emptyAggregation, null, null,
            existingFilters);
        Assert.assertEquals(ATTRIBUTE_LABEL, adapter.getDisplayLabel());
        Assert.assertEquals(ATTRIBUTE_CODE, adapter.getIdentifier());

        adapter = new MagentoGraphQLSearchAggregationAdapter(emptyAggregation, null, null,
            existingFilters);
        Assert.assertEquals(Optional.empty(), adapter.getAppliedFilterDisplayLabel());
        Assert.assertEquals(Optional.empty(), adapter.getAppliedFilterValue());
        Assert.assertEquals(false, adapter.getFilterable());
    }

    @Test
    public void testGetAppliedFilterDisplayLabel() {
        MagentoGraphQLSearchAggregationAdapter adapter = new MagentoGraphQLSearchAggregationAdapter(multipleAggregation, RED_VALUE, true,
            existingFilters);
        Assert.assertEquals(Optional.of(RED_LABEL), adapter.getAppliedFilterDisplayLabel());
    }

    @Test
    public void testGetAppliedFilterDisplayLabelForBuckets() {
        MagentoGraphQLSearchAggregationAdapter adapter = new MagentoGraphQLSearchAggregationAdapter(bucketAggregation, "1", true,
            existingFilters);
        Assert.assertEquals("Color Thing", adapter.getDisplayLabel());
        Assert.assertEquals(Optional.of("Yes"), adapter.getAppliedFilterDisplayLabel());
    }

}
