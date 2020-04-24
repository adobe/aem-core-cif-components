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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SearchOptionsImplTest {

    SearchOptionsImpl modelUnderTest;

    Map<String, String> testAttributeFilters;

    private final static String SEARCH_ATTRIBUTE_KEY_1 = "key1";
    private final static String SEARCH_ATTRIBUTE_KEY_2 = "key2";
    private final static String SEARCH_ATTRIBUTE_KEY_3 = "key3";

    private final static String SEARCH_ATTRIBUTE_VALUE_1 = "value1";
    private final static String SEARCH_ATTRIBUTE_VALUE_2 = "value2";
    private final static String SEARCH_ATTRIBUTE_VALUE_3 = "value3";

    @Before
    public void setup() {
        testAttributeFilters = new HashMap<>();
        testAttributeFilters.put(SEARCH_ATTRIBUTE_KEY_1, SEARCH_ATTRIBUTE_VALUE_1);
        testAttributeFilters.put(SEARCH_ATTRIBUTE_KEY_2, SEARCH_ATTRIBUTE_VALUE_2);
        testAttributeFilters.put(SEARCH_ATTRIBUTE_KEY_3, SEARCH_ATTRIBUTE_VALUE_3);

    }

    @Test
    public void testGetsAllFilters() {
        modelUnderTest = new SearchOptionsImpl();
        modelUnderTest.setAttributeFilters(testAttributeFilters);

        assertThat(modelUnderTest.getAllFilters()).isNotNull();
        assertThat(modelUnderTest.getAllFilters().size()).isEqualTo(3);
    }

    @Test
    public void testIncludesCategoryIdIfSet() {
        modelUnderTest = new SearchOptionsImpl();

        final String categoryId = "123";

        modelUnderTest.setAttributeFilters(testAttributeFilters);
        modelUnderTest.setCategoryId(categoryId);

        assertThat(modelUnderTest.getAllFilters()).isNotNull();
        assertThat(modelUnderTest.getAllFilters().size()).isEqualTo(4);
        assertThat(modelUnderTest.getCategoryId()).hasValue(categoryId);
    }

    @Test
    public void testIncludesSearchQueryIfSet() {
        modelUnderTest = new SearchOptionsImpl();

        final String searchQuery = "123";

        modelUnderTest.setAttributeFilters(testAttributeFilters);
        modelUnderTest.setSearchQuery(searchQuery);

        assertThat(modelUnderTest.getAllFilters()).isNotNull();
        assertThat(modelUnderTest.getAllFilters().size()).isEqualTo(4);
        assertThat(modelUnderTest.getSearchQuery()).hasValue(searchQuery);
    }

    @Test
    public void testNormalSearchTermsAllowedThrough() {
        modelUnderTest = new SearchOptionsImpl();

        String searchQuery = "DROPTHETHINGS";
        modelUnderTest.setAttributeFilters(testAttributeFilters);
        modelUnderTest.setSearchQuery(searchQuery);
        assertThat(modelUnderTest.getSearchQuery()).hasValue(searchQuery);

        searchQuery = "some words";
        modelUnderTest.setAttributeFilters(testAttributeFilters);
        modelUnderTest.setSearchQuery(searchQuery);
        assertThat(modelUnderTest.getSearchQuery()).hasValue(searchQuery);

        searchQuery = "size xl";
        modelUnderTest.setAttributeFilters(testAttributeFilters);
        modelUnderTest.setSearchQuery(searchQuery);
        assertThat(modelUnderTest.getSearchQuery()).hasValue(searchQuery);

    }

}
