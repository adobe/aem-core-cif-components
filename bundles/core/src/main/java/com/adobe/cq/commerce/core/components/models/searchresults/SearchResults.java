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
package com.adobe.cq.commerce.core.components.models.searchresults;

import java.util.function.Consumer;
import java.util.function.Function;

import com.adobe.cq.commerce.core.components.models.productcollection.ProductCollection;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultsStorefrontContext;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchStorefrontContext;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;

/**
 * Don't forget the comment
 */
public interface SearchResults extends ProductCollection {

    /**
     * Returns the storefront context related to search input
     *
     * @return search input context
     */
    SearchStorefrontContext getSearchStorefrontContext();

    /**
     * Returns the storefront context related to search results
     *
     * @return search results context
     */
    SearchResultsStorefrontContext getSearchResultsStorefrontContext();

    /**
     * Extend the product query part of the search GraphQL query with a partial query provided by a lambda hook that sets additional
     * fields.
     *
     * Example:
     *
     * <pre>
     * {@code
     * searchResults.extendProductQueryWith(p -> p
     *     .createdAt()
     *     .addCustomSimpleField("is_returnable"));
     * }
     * </pre>
     *
     * If called multiple times, each hook will be "appended" to the previously registered hook(s).
     *
     * @param productQueryHook Lambda that extends the product query
     */
    void extendProductQueryWith(Consumer<ProductInterfaceQuery> productQueryHook);

    /**
     * Extends or replaces the product attribute filter with a custom instance defined by a lambda hook.
     *
     * Example 1 (Extend):
     *
     * <pre>
     * {@code
     * searchResults.extendProductFilterWith(f -> f
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * Example 2 (Replace):
     *
     * <pre>
     * {@code
     * searchResults.extendProductFilterWith(f -> new ProductAttributeFilterInput()
     *     .setSku(new FilterEqualTypeInput()
     *         .setEq("custom-sku"))
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * @param productAttributeFilterHook Lambda that extends or replaces the product attribute filter.
     */
    void extendProductFilterWith(Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook);
}
