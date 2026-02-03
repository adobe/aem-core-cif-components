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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.storefrontcontext.QueryType;
import com.adobe.cq.commerce.core.components.storefrontcontext.Range;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchFilter;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchSort;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchStorefrontContext;
import com.adobe.cq.commerce.core.components.storefrontcontext.SortDirection;
import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;

public class SearchStorefrontContextImpl extends AbstractCommerceStorefrontContext implements SearchStorefrontContext {

    private final SearchOptions searchOptions;
    private final String unitId;
    private final String requestId;

    public SearchStorefrontContextImpl(SearchOptions searchOptions, String requestId, String unitId, Resource resource) {
        super(resource);
        this.searchOptions = searchOptions;
        this.requestId = requestId;
        this.unitId = unitId;
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
    public List<QueryType> getQueryTypes() {
        return Collections.singletonList(QueryType.products);
    }

    @Override
    public String getPhrase() {
        Optional<String> searchQuery = searchOptions.getSearchQuery();
        return searchQuery.orElse(StringUtils.EMPTY);
    }

    @Override
    public int getPageSize() {
        return searchOptions.getPageSize();
    }

    @Override
    public int getCurrentPage() {
        return searchOptions.getCurrentPage();
    }

    @Override
    public List<SearchFilter> getFilter() {
        return searchOptions.getAttributeFilters().entrySet().stream()
            .filter(e -> !e.getKey().startsWith("sort_"))
            .map(e -> new SearchFilter() {
                @Override
                public String getAttribute() {
                    return e.getKey();
                }

                @Override
                public String getEq() {
                    return e.getValue();
                }

                @Override
                public List<String> getIn() {
                    return null;
                }

                // TODO: support ranges here, as we support them in the implementation. Currently a range will be treated as eq
                @Override
                public Range getRange() {
                    return null;
                }
            }).collect(Collectors.toList());

    }

    @Override
    public List<SearchSort> getSort() {
        List<SorterKey> sorterKeys = searchOptions.getSorterKeys();

        return sorterKeys.stream().filter(SorterKey::isSelected).map(sorterKey -> new SearchSort() {
            @Override
            public String getAttribute() {
                return sorterKey.getName();
            }

            @Override
            public SortDirection getDirection() {
                return sorterKey.getOrder().equals(Sorter.Order.ASC) ? SortDirection.ASC : SortDirection.DESC;
            }
        }).collect(Collectors.toList());
    }
}
