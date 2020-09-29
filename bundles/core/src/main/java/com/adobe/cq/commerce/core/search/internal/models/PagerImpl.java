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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.search.models.Pager;
import com.adobe.cq.commerce.core.search.models.PagerPage;

/**
 * Implementation of {@link Pager}.
 */
public class PagerImpl implements Pager {

    /**
     * The number of pages in results before we cut off display with ellipsis.
     */
    public static final int MAXIMUM_PAGE_DISPLAY_COUNT = 7;

    /**
     * The number of possible pages to show around current page before showing ellipsis.
     */
    public static final int PAGINATION_RANGE_SIZE = 3;

    Map<String, String> existingQueryParameters;
    int totalPages;
    int currentPageIndex;

    public PagerImpl(final Map<String, String> existingQueryParameters, final int totalPages, final int currentPageIndex) {
        this.existingQueryParameters = existingQueryParameters;
        this.totalPages = totalPages;
        this.currentPageIndex = currentPageIndex;
    }

    @Override
    public int getCurrentPage() {
        return currentPageIndex;
    }

    @Override
    public int getTotalPages() {
        return totalPages;
    }

    @Nonnull
    @Override
    public List<PagerPage> getPages() {

        List<PagerPage> pages = new ArrayList<>();

        for (int currentIndex = 1; currentIndex <= totalPages; currentIndex++) {
            Map<String, String> pageParameters = new HashMap<>(existingQueryParameters);
            pageParameters.put("page", Integer.toString(currentIndex));
            if (inDisplayRange(totalPages, currentIndex) || currentIndex == 1 || currentIndex == totalPages) {
                pages.add(new PagerPageImpl(currentIndex, pageParameters, true));
            }
        }

        return pages;
    }

    @Nonnull
    @Override
    public Map<String, String> getPreviousPageParameters() {
        Integer previousPage = currentPageIndex <= 1 ? 1 : currentPageIndex - 1;
        Map<String, String> parameters = new HashMap<>(existingQueryParameters);
        parameters.put("page", previousPage.toString());
        return parameters;
    }

    @Nonnull
    @Override
    public Map<String, String> getNextPageParameters() {
        Integer nextPage = currentPageIndex >= totalPages ? totalPages : currentPageIndex + 1;
        Map<String, String> parameters = new HashMap<>(existingQueryParameters);
        parameters.put("page", nextPage.toString());
        return parameters;
    }

    @Override
    public boolean getMorePagesBefore() {
        return !inDisplayRange(totalPages, 2);
    }

    @Override
    public boolean getMorePagesAfter() {
        return !inDisplayRange(totalPages, totalPages - 1);
    }

    private boolean inDisplayRange(int totalPages, int currentIndex) {

        // if the total pages is less than or equal the page max display value then all are displayable
        if (totalPages <= MAXIMUM_PAGE_DISPLAY_COUNT) {
            return true;
        }

        // if we have an even number we need to remove one as we don't want exactly the display amount before
        final int displayBefore = PAGINATION_RANGE_SIZE % 2 == 0 ? PAGINATION_RANGE_SIZE / 2 - 1 : PAGINATION_RANGE_SIZE / 2;
        final int displayAfter = PAGINATION_RANGE_SIZE / 2;

        int leftStartDisplay = Math.max(currentPageIndex - displayBefore, 1);
        int rightEndDisplay = Math.min(currentPageIndex + displayAfter, totalPages);

        // if our index is close to the end then we need to use the end as the basis for our display range
        if ((currentPageIndex + displayAfter) > totalPages) {
            leftStartDisplay = Math.max(totalPages - PAGINATION_RANGE_SIZE + 1, 1);
            rightEndDisplay = totalPages;
        } else if ((currentPageIndex - displayBefore) < 1) {
            leftStartDisplay = 1;
            rightEndDisplay = Math.min(PAGINATION_RANGE_SIZE, totalPages);
        }

        return (leftStartDisplay <= currentIndex) && (currentIndex <= rightEndDisplay);
    }

}
