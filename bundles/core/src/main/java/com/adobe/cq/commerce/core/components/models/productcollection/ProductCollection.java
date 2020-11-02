/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.models.productcollection;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;

public interface ProductCollection {

    /**
     * Name of the String resource property indicating number of products to render on front-end.
     */
    String PN_PAGE_SIZE = "pageSize";

    /**
     * Name of the boolean resource property indicating if the product list should load prices on the client-side.
     */
    String PN_LOAD_CLIENT_PRICE = "loadClientPrice";

    /**
     * Name of the String resource property indicating the type of pagination that should be displayed.
     */
    String PN_PAGINATION_TYPE = "paginationType";

    /**
     * Returns the product list's items collection, as {@link ProductListItem}s elements.
     *
     * @return {@link Collection} of {@link ProductListItem}s
     */
    @Nonnull
    Collection<ProductListItem> getProducts();

    /**
     * Get the search result set. This is the actual search result data.
     *
     * @return the result of the search
     */
    @Nonnull
    SearchResultsSet getSearchResultsSet();

    /**
     * Should prices be re-loaded client-side.
     *
     * @return true if prices should be loaded client side
     */
    boolean loadClientPrice();

    /**
     * Returns the type of pagination that should be displayed.
     * 
     * @return The pagination type.
     */
    String getPaginationType();
}
