/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.core.components.internal.models.v1.searchresults;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.productcollection.ProductCollectionImpl;
import com.adobe.cq.commerce.core.components.internal.storefrontcontext.SearchResultsStorefrontContextImpl;
import com.adobe.cq.commerce.core.components.internal.storefrontcontext.SearchStorefrontContextImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultsStorefrontContext;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchStorefrontContext;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;

/**
 * Concrete implementation of the {@link SearchResults} Sling Model API
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = SearchResults.class,
    resourceType = { SearchResultsImpl.RESOURCE_TYPE, SearchResultsImpl.RESOURCE_TYPE_V2 })
public class SearchResultsImpl extends ProductCollectionImpl implements SearchResults {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResultsImpl.class);
    static final String RESOURCE_TYPE = "core/cif/components/commerce/searchresults/v1/searchresults";
    static final String RESOURCE_TYPE_V2 = "core/cif/components/commerce/searchresults/v2/searchresults";

    private String searchTerm;
    String searchRequestId = UUID.randomUUID().toString();

    private Consumer<ProductInterfaceQuery> productQueryHook;

    private Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook;

    @PostConstruct
    protected void initModel() {
        searchTerm = request.getParameter(SearchOptionsImpl.SEARCH_QUERY_PARAMETER_ID);
        if (StringUtils.isBlank(searchTerm)) {
            searchResultsSet = new SearchResultsSetImpl();
            return;
        }

        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        Integer currentPageIndex = calculateCurrentPageCursor(request.getParameter(SearchOptionsImpl.CURRENT_PAGE_PARAMETER_ID));

        Map<String, String> searchFilters = createFilterMap(request.getParameterMap());

        LOGGER.debug("Detected search parameter {}", searchTerm);

        searchOptions = new SearchOptionsImpl();
        searchOptions.setCurrentPage(currentPageIndex);
        searchOptions.setPageSize(navPageSize);
        searchOptions.setAttributeFilters(searchFilters);
        searchOptions.setSearchQuery(searchTerm);

        // configure sorting
        String defaultSortField = properties.get(PN_DEFAULT_SORT_FIELD, String.class);
        String defaultSortOrder = properties.get(PN_DEFAULT_SORT_ORDER, Sorter.Order.ASC.name());

        if (StringUtils.isNotBlank(defaultSortField)) {
            Sorter.Order value = Sorter.Order.fromString(defaultSortOrder, Sorter.Order.ASC);
            searchOptions.setDefaultSorter(defaultSortField, value);
        }
        // relevance is not provided in the products search results, we add it manually
        searchOptions.addSorterKey("relevance", "Relevance", Sorter.Order.DESC);
    }

    protected Map<String, String> createFilterMap(final Map<String, String[]> parameterMap) {
        Map<String, String> filters = super.createFilterMap(parameterMap);
        filters.remove(SearchOptionsImpl.SEARCH_QUERY_PARAMETER_ID);
        return filters;
    }

    @Nonnull
    @Override
    public Collection<ProductListItem> getProducts() {
        return getSearchResultsSet().getProductListItems();
    }

    @Nonnull
    @Override
    public SearchResultsSet getSearchResultsSet() {
        if (searchResultsSet == null) {
            searchResultsSet = searchResultsService.performSearch(searchOptions, resource, currentPage, request, productQueryHook,
                productAttributeFilterHook);
        }
        return searchResultsSet;
    }

    @Override
    public SearchStorefrontContext getSearchStorefrontContext() {
        return new SearchStorefrontContextImpl(getSearchResultsSet().getSearchOptions(), searchRequestId, getId(), resource);
    }

    @Override
    public SearchResultsStorefrontContext getSearchResultsStorefrontContext() {
        return new SearchResultsStorefrontContextImpl(getSearchResultsSet(), searchRequestId, getId(), resource);
    }

    @Override
    public void extendProductQueryWith(Consumer<ProductInterfaceQuery> productQueryHook) {
        if (this.productQueryHook == null) {
            this.productQueryHook = productQueryHook;
        } else {
            this.productQueryHook = this.productQueryHook.andThen(productQueryHook);
        }
    }

    @Override
    public void extendProductFilterWith(
        Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook) {
        if (this.productAttributeFilterHook == null) {
            this.productAttributeFilterHook = productAttributeFilterHook;
        } else {
            this.productAttributeFilterHook = this.productAttributeFilterHook.andThen(productAttributeFilterHook);
        }
    }
}
