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

package com.adobe.cq.commerce.core.search.models;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * This class is responsible for providing pagination support for search results. This class does all of the logic required to provide any
 * information HTL templates may need to display the pagination interface element.
 */
public interface Pager {

    /**
     * Get all pagination parameters available for the current result set.
     *
     * @return the pagination parameters
     */
    @Nonnull
    List<PagerPage> getPages();

    /**
     * Get the previous page parameters for supporting linking to the previous page.
     *
     * @return the previous page parameters
     */
    @Nonnull
    Map<String, String> getPreviousPageParameters();

    /**
     * Get the mext page parameters for supporting linking to the mext page.
     *
     * @return the mext page parameters
     */
    @Nonnull
    Map<String, String> getNextPageParameters();

    /**
     * Get the current page.
     *
     * @return the current page
     */
    int getCurrentPage();

    /**
     * Get the total number of pages.
     *
     * @return the total number of pages
     */
    int getTotalPages();

    /**
     * Whether there are additional pages in pagination set than are displayable currently at the start of the page list.
     *
     * @return true if more pages before current starting page
     */
    boolean getMorePagesBefore();

    /**
     * Whether there are additional pages in pagination set than are displayable currently at the end of the page list.
     *
     * @return true if more pages after current end page
     */
    boolean getMorePagesAfter();

}
