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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.services.FilterAttributeMetadataCache;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchFilterServiceImplTest {

    @Rule
    public final AemContext context = new AemContext();

    @Mock
    FilterAttributeMetadataCache filterAttributeMetadataCache;

    @Mock
    Resource resource;

    SearchFilterServiceImpl searchFilterServiceUnderTest;

    @Before
    public void setup() {

        when(filterAttributeMetadataCache.getFilterAttributeMetadata())
            .thenReturn(Optional.of(new ArrayList<>()));

        context.registerService(FilterAttributeMetadataCache.class, filterAttributeMetadataCache);
        searchFilterServiceUnderTest = context.registerInjectActivateService(new SearchFilterServiceImpl());
    }

    @Test
    public void testRetrieveMetadata() {

        final List<FilterAttributeMetadata> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFilters(resource);

        assertThat(filterAttributeMetadata).isNotNull();
        assertThat(filterAttributeMetadata).isEmpty();
    }

}
