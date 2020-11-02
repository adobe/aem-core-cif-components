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

package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.magento.graphql.BundleProductQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.GroupedProductQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;

class ProductRetriever extends AbstractProductRetriever {

    ProductRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    /* --- GraphQL queries --- */
    @Override
    protected String generateQuery(String identifier) {
        // Adds the store config query to the generic query of AbstractProductRetriever

        FilterEqualTypeInput identifierFilter = new FilterEqualTypeInput().setEq(identifier);
        ProductAttributeFilterInput filter;
        if (ProductIdentifierType.URL_KEY.equals(productIdentifierType)) {
            filter = new ProductAttributeFilterInput().setUrlKey(identifierFilter);
        } else {
            filter = new ProductAttributeFilterInput().setSku(identifierFilter);
        }

        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        // GraphQL query
        ProductsQueryDefinition queryArgs = q -> q.items(generateProductQuery());
        return Operations.query(query -> query
            .products(searchArgs, queryArgs)).toString();
    }

    private SimpleProductQueryDefinition generateSimpleProductQuery() {
        return q -> {
            q.sku()
                .name()
                .description(d -> d.html())
                .image(i -> i.label().url())
                .thumbnail(t -> t.label().url())
                .urlKey()
                .stockStatus()
                .color()
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery()))
                .mediaGallery(g -> g
                    .disabled()
                    .url()
                    .label()
                    .position());

            // Apply product variant query hook
            if (variantQueryHook != null) {
                variantQueryHook.accept(q);
            }
        };
    }

    @Override
    protected ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> {
            q.sku()
                .name()
                .description(d -> d.html())
                .image(i -> i.label().url())
                .thumbnail(t -> t.label().url())
                .urlKey()
                .stockStatus()
                .metaDescription()
                .metaKeyword()
                .metaTitle()
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery()))
                .mediaGallery(g -> g
                    .disabled()
                    .url()
                    .label()
                    .position())
                .onConfigurableProduct(cp -> cp
                    .priceRange(r -> r
                        .maximumPrice(generatePriceQuery()))
                    .configurableOptions(o -> o
                        .label()
                        .attributeCode()
                        .values(v -> v
                            .valueIndex()
                            .label()))
                    .variants(v -> v
                        .attributes(a -> a
                            .code()
                            .valueIndex())
                        .product(generateSimpleProductQuery())))
                .onGroupedProduct(generateGroupedProductQuery())
                .onBundleProduct(generateBundleProductQuery());

            // Apply product query hook
            if (productQueryHook != null) {
                productQueryHook.accept(q);
            }
        };
    }

    private GroupedProductQueryDefinition generateGroupedProductQuery() {
        return gp -> gp
            .items(i -> i
                .position()
                .qty()
                .product(p -> p
                    .sku()
                    .name()
                    .priceRange(r -> r
                        .minimumPrice(generatePriceQuery()))));
    }

    private BundleProductQueryDefinition generateBundleProductQuery() {
        return bp -> bp
            .priceRange(r -> r
                .maximumPrice(generatePriceQuery()));
    }
}
