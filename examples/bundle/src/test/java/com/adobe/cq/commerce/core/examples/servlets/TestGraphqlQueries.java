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

package com.adobe.cq.commerce.core.examples.servlets;

import java.util.function.Function;

import com.adobe.cq.commerce.magento.graphql.CategoryTreeQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPriceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;

public class TestGraphqlQueries {

    /**
     * Test query for product price
     */
    public static final ProductPriceQueryDefinition PRODUCT_PRICE_QUERY = q -> q
        .regularPrice(r -> r
            .value()
            .currency())
        .finalPrice(f -> f
            .value()
            .currency())
        .discount(d -> d
            .amountOff()
            .percentOff());

    /**
     * Test query for simple product
     */
    public static final SimpleProductQueryDefinition SIMPLE_PRODUCT_QUERY = q -> q
        .id()
        .sku()
        .name()
        .description(d -> d.html())
        .image(i -> i.url())
        .thumbnail(t -> t.url())
        .urlKey()
        .updatedAt()
        .createdAt()
        .priceRange(r -> r
            .minimumPrice(PRODUCT_PRICE_QUERY));

    /**
     * Test query for configurable product including variants
     */
    public static final ProductInterfaceQueryDefinition CONFIGURABLE_PRODUCT_QUERY = q -> q
        .id()
        .sku()
        .name()
        .description(d -> d.html())
        .image(i -> i.url())
        .thumbnail(t -> t.url())
        .urlKey()
        .updatedAt()
        .createdAt()
        .priceRange(r -> r
            .minimumPrice(PRODUCT_PRICE_QUERY))
        .categories(c -> c.urlPath())
        .onConfigurableProduct(cp -> cp
            .priceRange(r -> r
                .maximumPrice(PRODUCT_PRICE_QUERY))
            .variants(v -> v
                .product(SIMPLE_PRODUCT_QUERY)));

    /**
     * Test "lambda" query for category tree WITHOUT "children" part.
     * The "children" part cannot be added because it would otherwise introduce an infinite recursion.
     */
    public static final Function<CategoryTreeQuery, CategoryTreeQuery> CATEGORY_TREE_LAMBDA = q -> q
        .id()
        .name()
        .urlPath()
        .productCount()
        .childrenCount();
}
