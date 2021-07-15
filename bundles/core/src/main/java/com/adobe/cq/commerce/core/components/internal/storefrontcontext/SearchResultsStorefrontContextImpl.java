/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.storefrontcontext;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultCategory;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultProduct;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultSuggestion;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultsStorefrontContext;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;

public class SearchResultsStorefrontContextImpl extends AbstractCommerceStorefrontContext implements SearchResultsStorefrontContext {

    private final SearchResultsSet searchResultsSet;

    public SearchResultsStorefrontContextImpl(SearchResultsSet searchResultsSet) {
        this.searchResultsSet = searchResultsSet;
    }

    @Override
    public String getSearchUnitId() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getSearchRequestId() {
        return StringUtils.EMPTY;
    }

    @Override
    public List<SearchResultProduct> getProducts() {
        return searchResultsSet.getProductListItems().stream()
            .map(p -> new SearchResultProduct() {

                @Override
                public String getName() {
                    return p.getTitle();
                }

                @Override
                public String getSku() {
                    return p.getSKU();
                }

                @Override
                public String getUrl() {
                    return p.getURL();
                }

                @Override
                public String getImageUrl() {
                    return p.getImageURL();
                }

                @Override
                public Double getPrice() {
                    return p.getPriceRange().getFinalPrice();
                }

                @Override
                public int getRank() {
                    return 0;
                }
            }).collect(Collectors.toList());
    }

    @Override
    public List<SearchResultCategory> getCategories() {
        SearchAggregation searchAggregationOptions = searchResultsSet
            .getSearchAggregations().stream()
            .filter(a -> a.getIdentifier().equals("category_id"))
            .findFirst()
            .orElse(null);

        if (searchAggregationOptions != null) {
            return searchAggregationOptions.getOptions().stream()
                .map(o -> new SearchResultCategory() {

                    @Override
                    public String getName() {
                        return o.getDisplayLabel();
                    }

                    @Override
                    public String getUrl() {
                        return StringUtils.EMPTY;
                    }

                    @Override
                    public int getRank() {
                        return 0;
                    }
                }).collect(Collectors.toList());
        }

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

}
