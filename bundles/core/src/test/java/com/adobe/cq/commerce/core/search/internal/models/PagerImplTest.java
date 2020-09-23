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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import com.adobe.cq.commerce.core.search.models.PagerPage;

import static org.assertj.core.api.Assertions.assertThat;

public class PagerImplTest {

    private static final String PARAMETER_KEY = "param-key";
    private static final String PARAMETER_VALUE = "param-value";
    private static final int TOTAL_PAGES = 39;
    private static final int CURRENT_PAGE = 4;

    @Test
    public void testGetPaginationParameters() {
        PagerImpl pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, CURRENT_PAGE);
        final List<PagerPage> pagerPages = pager.getPages();

        assertThat(pager.getCurrentPage()).isEqualTo(CURRENT_PAGE);
        assertThat(pager.getTotalPages()).isEqualTo(TOTAL_PAGES);
        assertThat(pagerPages.get(0).getParameters().size()).isEqualTo(2);
        assertThat(pagerPages.get(0).getParameters()).containsKey("page");
        assertThat(pagerPages.get(0).getParameters()).containsKey(PARAMETER_KEY);
    }

    @Test
    public void testGetPreviousPageParameters() {
        PagerImpl pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, CURRENT_PAGE);
        final Map<String, String> previousPageParameters = pager.getPreviousPageParameters();
        assertThat(previousPageParameters.size()).isEqualTo(2);
    }

    @Test
    public void testGetNextPageParameters() {
        PagerImpl pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, CURRENT_PAGE);
        final Map<String, String> previousPageParameters = pager.getNextPageParameters();
        assertThat(previousPageParameters.size()).isEqualTo(2);
    }

    @Test
    public void testPageEdgeConditionsProvidePage() {
        PagerImpl pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), 1, 1);
        assertThat(pager.getPages()).hasSize(1);

        pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), 2, 1);
        assertThat(pager.getPages()).hasSize(2);

        pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), 4, 1);
        assertThat(pager.getPages().stream().map(page -> page.getPageNumber()).collect(Collectors.toList())).contains(1, 2, 3, 4);

        pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), PagerImpl.MAXIMUM_PAGE_DISPLAY_COUNT, 1);
        assertThat(pager.getPages()).hasSize(PagerImpl.MAXIMUM_PAGE_DISPLAY_COUNT);

    }

    @Test
    public void testUsesPageSubsetWhenOverMaxPageResults() {
        PagerImpl pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), PagerImpl.MAXIMUM_PAGE_DISPLAY_COUNT + 1,
            5);
        assertThat(pager.getPages()).hasSize(PagerImpl.PAGINATION_RANGE_SIZE + 2);
    }

    @Test
    public void testAlwaysReturnPageResultsWithCorrectSize() {
        PagerImpl pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, 1);
        assertThat(pager.getPages()).hasSize(PagerImpl.PAGINATION_RANGE_SIZE + 1);

        pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, TOTAL_PAGES);
        assertThat(pager.getPages()).hasSize(PagerImpl.PAGINATION_RANGE_SIZE + 1);
    }

    @Test
    public void testAlwaysReturnsFirstAndLastPage() {
        PagerImpl pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, 1);
        assertThat(pager.getPages().stream().map(page -> page.getPageNumber()).collect(Collectors.toList()))
            .contains(1, TOTAL_PAGES);

        pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, TOTAL_PAGES);
        assertThat(pager.getPages().stream().map(page -> page.getPageNumber()).collect(Collectors.toList()))
            .contains(1, TOTAL_PAGES);

        pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, 10);
        assertThat(pager.getPages().stream().map(page -> page.getPageNumber()).collect(Collectors.toList()))
            .contains(1, TOTAL_PAGES);
    }

    @Test
    public void testHasMorePagesAtBoundary() {
        PagerImpl pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, 1);
        assertThat(pager.getMorePagesBefore()).isFalse();
        assertThat(pager.getMorePagesAfter()).isTrue();

        pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, TOTAL_PAGES);
        assertThat(pager.getMorePagesBefore()).isTrue();
        assertThat(pager.getMorePagesAfter()).isFalse();

        pager = new PagerImpl(Collections.singletonMap(PARAMETER_KEY, PARAMETER_VALUE), TOTAL_PAGES, 10);
        assertThat(pager.getMorePagesBefore()).isTrue();
        assertThat(pager.getMorePagesAfter()).isTrue();

    }

}
