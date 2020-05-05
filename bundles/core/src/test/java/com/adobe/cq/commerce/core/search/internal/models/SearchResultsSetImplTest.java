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

package com.adobe.cq.commerce.core.search.internal.models;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adobe.cq.commerce.core.search.models.SearchAggregation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResultsSetImplTest {

    private SearchResultsSetImpl modelUnderTest;
    private SearchOptionsImpl searchOptions = new SearchOptionsImpl();

    @Before
    public void setUp() throws IOException {

        // set up the search filters
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("color", "42");

        // setup some example search options
        searchOptions.setSearchQuery("test");
        searchOptions.setCategoryId("23");
        searchOptions.setAttributeFilters(parameterMap);

        SearchAggregation appliedColorAggregation = mock(SearchAggregation.class);
        when(appliedColorAggregation.getDisplayLabel()).thenReturn("Color");
        when(appliedColorAggregation.getIdentifier()).thenReturn("color");
        when(appliedColorAggregation.getFilterable()).thenReturn(true);
        when(appliedColorAggregation.getAppliedFilterValue()).thenReturn(Optional.of("42"));
        when(appliedColorAggregation.getAppliedFilterDisplayLabel()).thenReturn(Optional.of("blue"));

        SearchAggregation availableMaterialAggregation = mock(SearchAggregation.class);
        when(availableMaterialAggregation.getDisplayLabel()).thenReturn("Material");
        when(availableMaterialAggregation.getIdentifier()).thenReturn("material");
        when(availableMaterialAggregation.getFilterable()).thenReturn(true);
        when(availableMaterialAggregation.getAppliedFilterValue()).thenReturn(Optional.ofNullable(null));
        when(availableMaterialAggregation.getAppliedFilterDisplayLabel()).thenReturn(Optional.empty());

        modelUnderTest = new SearchResultsSetImpl();
        modelUnderTest.setSearchOptions(searchOptions);
        modelUnderTest.setTotalResults(9);
        modelUnderTest.setSearchAggregations(Arrays.asList(appliedColorAggregation, availableMaterialAggregation));

    }

    @Test
    public void testGetAppliedAggregations() {
        final List<SearchAggregation> appliedAggregations = modelUnderTest.getAppliedAggregations();
        Assert.assertEquals("identifies applied aggregations", 1, modelUnderTest.getAppliedAggregations().size());
    }

    @Test
    public void testGetAvailableAggregations() {
        final List<SearchAggregation> availableAggregations = modelUnderTest.getAppliedAggregations();
        Assert.assertEquals("Identifies available aggregations", 1, modelUnderTest.getAvailableAggregations().size());

        SearchResultsSetImpl emptySearchResults = new SearchResultsSetImpl();
        Assert.assertEquals("returns zero available aggregations for empty result set", 0, emptySearchResults.getAvailableAggregations()
            .size());
        Assert.assertEquals("returns zero applied aggregations for empty result set", 0, emptySearchResults.getAppliedAggregations()
            .size());
    }

}
