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

package com.adobe.cq.commerce.core.search.internal.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.search.internal.models.FilterAttributeMetadataImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class FilterAttributeMetadataCacheImplTest {

    FilterAttributeMetadataCacheImpl classUnderTest;

    @Test
    public void testNewInstanceReturnsEmpty() {
        classUnderTest = new FilterAttributeMetadataCacheImpl();

        assertThat(classUnderTest.getFilterAttributeMetadata()).isEmpty();
    }

    @Test
    public void testNewInstanceReturnsCachedValueAfterSet() {
        classUnderTest = new FilterAttributeMetadataCacheImpl();

        classUnderTest.setFilterAttributeMetadata(new ArrayList<>());

        assertThat(classUnderTest.getFilterAttributeMetadata()).isNotEmpty();
    }

    @Test
    public void testTimeoutResultsInEmptyResult() {
        long aLongTimeAgo = 1000L;
        long aVeryShortTTL = 1L;

        classUnderTest = new FilterAttributeMetadataCacheImpl(new ArrayList<>(), aLongTimeAgo, aVeryShortTTL);
        assertThat(classUnderTest.getFilterAttributeMetadata()).isEmpty();

    }

    @Test
    public void testNoLastFetchedResultsInEmpty() {

        long randomTTL = 1L;

        classUnderTest = new FilterAttributeMetadataCacheImpl(new ArrayList<>(), null, randomTTL);
        assertThat(classUnderTest.getFilterAttributeMetadata()).isEmpty();

    }

    @Test
    public void testRequsetWithinTTLProvidesResult() {
        long currentTime = Instant.now().toEpochMilli();
        long aVeryLongTTL = 99999999999L;

        FilterAttributeMetadataImpl filterAttributeMetadata = new FilterAttributeMetadataImpl();
        final List<FilterAttributeMetadata> testMetadata = Arrays.asList(filterAttributeMetadata);

        classUnderTest = new FilterAttributeMetadataCacheImpl(testMetadata, currentTime, aVeryLongTTL);
        assertThat(classUnderTest.getFilterAttributeMetadata()).contains(testMetadata);

    }

}
