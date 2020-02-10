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

package com.adobe.cq.commerce.core.search;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;

import com.day.cq.wcm.api.Page;

/**
 * This service hides the interaction between the GraphQL backend and the Sling component models. It's job is to performa a search, given a
 * particular search query and filters.
 */
public interface SearchResultsService {

    /**
     * Perform a search against the commerce backend and return a {@link SearchResultsSet} for consumption by the frontend. When the search
     * is performed the implementing concrete classes are responsible for correctly applying the provided filters.
     *
     * @param searchOptions the search options for thigns like filters, query, etc
     * @param resource resource for adaption
     * @param productPage product page to provide context to the search service
     * @return a {@link SearchResultsSet} with search results and metadata
     */
    @Nonnull
    default SearchResultsSet performSearch(
        SearchOptions searchOptions,
        Resource resource,
        Page productPage) {
        throw new UnsupportedOperationException();
    }
}
