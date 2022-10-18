/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.models.productcollection;

import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;

public interface ProductCollection {

    /**
     * Name of the String resource property indicating number of products to render on front-end.
     */
    String PN_PAGE_SIZE = "pageSize";

    /**
     * Name of the boolean resource property indicating if the product collection should load prices on the client-side.
     */
    @Deprecated
    String PN_LOAD_CLIENT_PRICE = "loadClientPrice";

    /**
     * Name of the boolean resource property indicating if the product collection should display the 'Add to Cart' button on the product
     * collection items.
     */
    String PN_ENABLE_ADD_TO_CART = "enableAddToCart";

    /**
     * Name of the boolean resource property indicating if the product collection should display the 'Add to Wish List' button on the
     * product collection items.
     */
    String PN_ENABLE_ADD_TO_WISH_LIST = "enableAddToWishList";

    /**
     * Name of the String resource property indicating the type of pagination that should be displayed.
     */
    String PN_PAGINATION_TYPE = "paginationType";

    /**
     * Name of the String resource property for the default product sort field.
     */
    String PN_DEFAULT_SORT_FIELD = "defaultSortField";

    /**
     * Name of the String resource property for the default product sort order.
     */
    String PN_DEFAULT_SORT_ORDER = "defaultSortOrder";

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
     * @deprecated Per component client-side price loading is deprecated. This information is exposed in the
     *             {@link com.adobe.cq.commerce.core.components.models.storeconfigexporter.StoreConfigExporter} and enabled site wide.
     * @return true if prices should be loaded client side
     */
    @Deprecated
    boolean loadClientPrice();

    /**
     * Returns the type of pagination that should be displayed.
     * 
     * @return The pagination type.
     */
    String getPaginationType();

    /**
     * Indicates whether the 'Add to Cart' button should be displayed on the product collection item.
     *
     * @return {@code true} if the button should be displayed, {@code false} otherwise
     */
    default boolean isAddToCartEnabled() {
        return false;
    }

    /**
     * Indicates whether the 'Add to Wish List' button should be displayed on the product collection item.
     *
     * @return {@code true} if the button should be displayed, {@code false} otherwise
     */
    default boolean isAddToWishListEnabled() {
        return false;
    }

    /**
     * Extends or replaces the product attribute filter with a custom instance defined by a lambda hook.
     *
     * Example 1 (Extend):
     *
     * <pre>
     * {@code
     * collection.extendProductFilterWith(f -> f
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * Example 2 (Replace):
     *
     * <pre>
     * {@code
     * collection.extendProductFilterWith(f -> new ProductAttributeFilterInput()
     *     .setSku(new FilterEqualTypeInput()
     *         .setEq("custom-sku"))
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * @param productAttributeFilterHook Lambda that extends or replaces the product attribute filter.
     */
    default void extendProductFilterWith(Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook) {}
}
