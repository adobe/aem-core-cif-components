/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.storefrontcontext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.storefrontcontext.SearchFacet;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultCategory;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultProduct;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultSuggestion;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultsStorefrontContext;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;

public class SearchResultsStorefrontContextImpl extends AbstractCommerceStorefrontContext implements SearchResultsStorefrontContext {

    private final SearchResultsSet searchResultsSet;
    private final String unitId;
    private final String requestId;

    public SearchResultsStorefrontContextImpl(SearchResultsSet searchResultsSet, String requestId, String unitId, Resource resource) {
        super(resource);
        this.searchResultsSet = searchResultsSet;
        this.unitId = unitId;
        this.requestId = requestId;
    }

    @Override
    public String getSearchUnitId() {
        return unitId;
    }

    @Override
    public String getSearchRequestId() {
        return requestId;
    }

    @Override
    public List<SearchResultProduct> getProducts() {
        return searchResultsSet.getProductListItems().stream()
            .map(SearchResultProductImpl::new).collect(Collectors.toList());
    }

    @Override
    public List<SearchResultCategory> getCategories() {
        return Collections.emptyList();
    }

    @Override
    public List<SearchResultSuggestion> getSuggestions() {
        return Collections.emptyList();
    }

    @Override
    public int getPage() {
        return searchResultsSet.getSearchOptions().getCurrentPage();
    }

    @Override
    public int getPerPage() {
        return searchResultsSet.getSearchOptions().getPageSize();
    }

    @Override
    public List<SearchFacet> getFacets() {
        // TODO: return the facets as we have them available in our result set
        return new ArrayList<SearchFacet>();
    }

}
