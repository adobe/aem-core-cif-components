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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

/**
 * Concrete implementation of the {@link SearchResults} Sling Model API
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = SearchResults.class,
    resourceType = SearchResultsImpl.RESOURCE_TYPE)
public class SearchResultsImpl implements SearchResults {

    static final String RESOURCE_TYPE = "core/cif/components/commerce/searchresults";

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResultsImpl.class);
    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;

    private boolean loadClientPrice;
    private int navPageSize;
    private Page searchPage;
    private Page productPage;
    private String searchTerm;
    private SearchOptionsImpl searchOptions;
    private SearchResultsSet searchResultsSet;

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable
    private Style currentStyle;

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @Inject
    private SearchResultsService searchResultsService;

    @PostConstruct
    protected void initModel() {
        navPageSize = properties.get(PN_PAGE_SIZE, currentStyle.get(PN_PAGE_SIZE, SearchOptionsImpl.PAGE_SIZE_DEFAULT));
        loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE, currentStyle.get(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));

        searchTerm = request.getParameter(SearchOptionsImpl.SEARCH_QUERY_PARAMETER_ID);

        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        Integer currentPageIndex = calculateCurrentPageCursor(request.getParameter(SearchOptionsImpl.CURRENT_PAGE_PARAMETER_ID));

        productPage = SiteNavigation.getProductPage(currentPage);
        searchPage = SiteNavigation.getSearchResultsPage(currentPage);
        Map<String, String> searchFilters = createFilterMap(request.getParameterMap());

        LOGGER.debug("Detected search parameter {}", searchTerm);

        searchOptions = new SearchOptionsImpl();
        searchOptions.setCurrentPage(currentPageIndex);
        searchOptions.setPageSize(navPageSize);
        searchOptions.setAttributeFilters(searchFilters);
        searchOptions.setSearchQuery(searchTerm);
    }

    protected Integer calculateCurrentPageCursor(final String currentPageIndexCandidate) {
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        return StringUtils.isNumeric(currentPageIndexCandidate) && Integer.valueOf(currentPageIndexCandidate) > 0
            ? Integer.valueOf(currentPageIndexCandidate)
            : 1;
    }

    @Nonnull
    @Override
    public String getSearchResultsPagePath() {
        return searchPage.getPath();
    }

    protected Map<String, String> createFilterMap(final Map<String, String[]> parameterMap) {
        Map<String, String> filters = new HashMap<>();
        parameterMap.entrySet().forEach(filterCandidate -> {
            String code = filterCandidate.getKey();
            String[] value = filterCandidate.getValue();

            // we'll remove the search filter
            if (code.equalsIgnoreCase(SearchOptionsImpl.SEARCH_QUERY_PARAMETER_ID)) {
                return;
            }

            // we'll make sure there is a value defined for the key
            if (value.length != 1) {
                return;
            }

            filters.put(code, value[0]);
        });

        return filters;
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
    public List<SearchAggregation> getAggregations() {
        return getSearchResultsSet().getSearchAggregations();
    }

    @Nonnull
    @Override
    public SearchResultsSet getSearchResultsSet() {
        if (searchResultsSet == null) {
            searchResultsSet = searchResultsService.performSearch(searchOptions, resource, productPage, request);
        }
        return searchResultsSet;
    }

    @Override
    public boolean loadClientPrice() {
        return loadClientPrice;
    }
}
