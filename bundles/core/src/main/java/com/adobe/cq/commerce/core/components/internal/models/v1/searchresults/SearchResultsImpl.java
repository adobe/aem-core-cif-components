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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.productcollection.ProductCollectionImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;

/**
 * Concrete implementation of the {@link SearchResults} Sling Model API
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = SearchResults.class, resourceType = SearchResultsImpl.RESOURCE_TYPE)
public class SearchResultsImpl extends ProductCollectionImpl implements SearchResults {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResultsImpl.class);
    static final String RESOURCE_TYPE = "core/cif/components/commerce/searchresults";

    private String searchTerm;

    @PostConstruct
    protected void initModel() {
        searchTerm = request.getParameter(SearchOptionsImpl.SEARCH_QUERY_PARAMETER_ID);

        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        Integer currentPageIndex = calculateCurrentPageCursor(
            request.getParameter(SearchOptionsImpl.CURRENT_PAGE_PARAMETER_ID));
        Map<String, String[]> params = request.getParameterMap();
        if (params.containsKey("fashion_color")) {
            String[] fashion_color = params.get("fashion_color");

            LOGGER.debug("fashion_color size {}", fashion_color.length);
        }

        Map<String, String[]> searchFilters = createFilterMap(params);

        LOGGER.debug("Detected search parameter {}", searchTerm);

        searchOptions = new SearchOptionsImpl();
        searchOptions.setCurrentPage(currentPageIndex);
        searchOptions.setPageSize(navPageSize);
        searchOptions.setAttributeFilters(searchFilters);
        searchOptions.setSearchQuery(searchTerm);
    }

    protected Map<String, String[]> createFilterMap(final Map<String, String[]> parameterMap) {
        return super.createFilterMap(parameterMap).entrySet().stream()
            .filter(entry -> !entry.getKey().equals(SearchOptionsImpl.SEARCH_QUERY_PARAMETER_ID))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * {@see SearchResults#getProducts()}
     */
    @Nonnull
    @Override
    public Collection<ProductListItem> getProducts() {
        if (StringUtils.isBlank(searchTerm)) {
            return Collections.emptyList();
        }

        return getSearchResultsSet().getProductListItems();
    }

    @Nonnull
    @Override
    public SearchResultsSet getSearchResultsSet() {
        if (searchResultsSet == null) {
            searchResultsSet = searchResultsService.performSearch(searchOptions, resource, productPage, request);
        }
        return searchResultsSet;
    }
}
