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
package com.adobe.cq.commerce.core.components.models.searchresults;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;

/**
 * Don't forget the comment
 */
@ProviderType
public interface SearchResults {

    /**
     * Name of the String resource property indicating number of products to render on front-end.
     */
    String PN_PAGE_SIZE = "pageSize";

    /**
     * Name of the boolean resource property indicating if the product list should load prices on the client-side.
     */
    String PN_LOAD_CLIENT_PRICE = "loadClientPrice";

    /**
     * Returns the product list's items collection, as {@link ProductListItem}s elements.
     *
     * @return {@link Collection} of {@link ProductListItem}s
     */
    @Nonnull
    Collection<ProductListItem> getProducts();

    /**
     * Returns the aggregations resulting from the search, as {@link SearchAggregation}s elements.
     *
     * @return {@link List} of {@link SearchAggregation}s
     */
    @Nonnull
    List<SearchAggregation> getAggregations();

    /**
     * Get the search result set. This is the actual search result data.
     *
     * @return the result of the search
     */
    @Nonnull
    SearchResultsSet getSearchResultsSet();

    /**
     * This is the path to the search page.
     *
     * @return the search page page
     */
    @Nonnull
    String getSearchResultsPagePath();

    /**
     * Should prices be re-loaded client-side.
     *
     * @return true if prices should be loaded client side
     */
    boolean loadClientPrice();

}
