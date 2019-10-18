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

package com.adobe.cq.commerce.core.components.internal.models.v1.retriever;

import java.util.List;
import java.util.function.Consumer;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.ProductRetriever;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPricesQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQuery;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.StoreConfigQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.shopify.graphql.support.AbstractQuery;

public class ProductRetrieverImpl implements ProductRetriever {

    private String query;

    private String slug;

    private MagentoGraphqlClient client;

    private ProductInterface product;

    private String mediaBaseUrl;

    private Consumer<AbstractQuery<?>> productQueryHook;

    private Consumer<AbstractQuery<?>> variantQueryHook;

    public ProductRetrieverImpl(MagentoGraphqlClient client) {
        if (client == null) {
            throw new java.lang.Error("No GraphQL client provided");
        }

        this.client = client;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void setSlug(String slug) {
        this.slug = slug;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends AbstractQuery<?>> void setProductQueryHook(Consumer<U> productQueryHook) {
        this.productQueryHook = (Consumer<AbstractQuery<?>>) productQueryHook;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends AbstractQuery<?>> void setVariantQueryHook(Consumer<U> variantQueryHook) {
        this.variantQueryHook = (Consumer<AbstractQuery<?>>) variantQueryHook;
    }

    @Override
    public ProductInterface getProduct() {
        if (product == null) {
            populate();
        }

        return product;
    }

    @Override
    public String getMediaBaseUrl() {
        if (mediaBaseUrl == null) {
            populate();
        }

        return mediaBaseUrl;
    }

    private GraphqlResponse<Query, Error> getData() {
        if (query == null) {
            query = generateQuery(slug);
        }

        return client.execute(query);
    }

    private void populate() {
        // Get product list from response
        GraphqlResponse<Query, Error> response = getData();
        Query rootQuery = response.getData();
        List<ProductInterface> products = rootQuery.getProducts().getItems();

        // TODO WORKAROUND
        // we need a temporary detour and use storeconfig to get the base media url since the product media gallery only returns the images
        // file names but no full URLs
        mediaBaseUrl = rootQuery.getStoreConfig().getSecureBaseMediaUrl();

        // Return first product in list
        if (products.size() > 0) {
            product = products.get(0);
        }
    }

    /* --- GraphQL queries --- */

    private String generateQuery(String slug) {
        // Create query and pass it to the product retriever
        // Search parameters
        FilterTypeInput input = new FilterTypeInput().setEq(slug);
        ProductFilterInput filter = new ProductFilterInput().setUrlKey(input);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        // GraphQL query
        ProductsQueryDefinition queryArgs = q -> q.items(generateProductQuery());
        return Operations.query(query -> query
            .products(searchArgs, queryArgs)
            .storeConfig(generateStoreConfigQuery())).toString();
    }

    private ProductPricesQueryDefinition generatePriceQuery() {
        return q -> q
            .regularPrice(rp -> rp
                .amount(a -> a
                    .currency()
                    .value()));
    }

    private SimpleProductQueryDefinition generateSimpleProductQuery() {
        return (SimpleProductQuery q) -> {
            q.sku()
                .name()
                .description(d -> d.html())
                .image(i -> i.label().url())
                .thumbnail(t -> t.label().url())
                .urlKey()
                .stockStatus()
                .color()
                .price(generatePriceQuery())
                .mediaGalleryEntries(g -> g
                    .disabled()
                    .file()
                    .label()
                    .position()
                    .mediaType());

            // Apply product variant query hook
            if (variantQueryHook != null) {
                variantQueryHook.accept(q);
            }
        };
    }

    private ProductInterfaceQueryDefinition generateProductQuery() {
        return (ProductInterfaceQuery q) -> {
            q.sku()
                .name()
                .description(d -> d.html())
                .image(i -> i.label().url())
                .thumbnail(t -> t.label().url())
                .urlKey()
                .stockStatus()
                .price(generatePriceQuery())
                .mediaGalleryEntries(g -> g
                    .disabled()
                    .file()
                    .label()
                    .position()
                    .mediaType())
                .onConfigurableProduct(cp -> cp
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
                        .product(generateSimpleProductQuery())));

            // Apply product query hook
            if (productQueryHook != null) {
                productQueryHook.accept(q);
            }
        };
    }

    private StoreConfigQueryDefinition generateStoreConfigQuery() {
        return q -> q.secureBaseMediaUrl();
    }

}
