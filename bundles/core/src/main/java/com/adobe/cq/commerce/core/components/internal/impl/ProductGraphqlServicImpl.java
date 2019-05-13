/*
 *   Copyright 2019 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.adobe.cq.commerce.core.components.internal.impl;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.AssetImpl;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.service.ProductGraphqlService;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import org.osgi.service.component.annotations.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *GraphQlService extracted from ProductModel
 * It could be one service to cater to all component , carrying all the service methods
 * ToDO: Refactoring
 *
 */

@Component(service = ProductGraphqlService.class)
public class ProductGraphqlServicImpl implements ProductGraphqlService {

    private static final String PRODUCT_IMAGE_FOLDER = "catalog/product";
    private String mediaBaseUrl;


    @Override
    public List<Asset> filterAndSortAssets(List<MediaGalleryEntry> assets) {
        return assets.parallelStream()
                .filter(e -> !e.getDisabled() && e.getMediaType().equals("image"))
                .map(this::mapAsset)
                .sorted(Comparator.comparing(Asset::getPosition))
                .collect(Collectors.toList());
    }
    @Override
    public ProductInterface getProduct(GraphqlClient client, String sku  ) {
        FilterTypeInput input = new FilterTypeInput().setEq(sku);
        ProductFilterInput filter = new ProductFilterInput().setSku(input);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);
        ProductsQueryDefinition queryArgs = q -> q.items(this.generateProductQuery());
        String queryString = Operations.query(query -> query
                .products(searchArgs, queryArgs)
                .storeConfig(this.generateStoreConfigQuery())).toString();
        GraphqlResponse<Query, Error> response = client.execute(new GraphqlRequest(queryString), Query.class, Error.class, QueryDeserializer.getGson());
        Query rootQuery = response.getData();
        List<ProductInterface> products = rootQuery.getProducts().getItems();
        mediaBaseUrl = rootQuery.getStoreConfig().getSecureBaseMediaUrl();
        if (products.size() > 0) {
            return products.get(0);
        }
        return null;
    }

    public ProductPricesQueryDefinition generatePriceQuery() {
        return q -> q
                .regularPrice(rp -> rp
                        .amount(a -> a
                                .currency()
                                .value()));
    }

    public SimpleProductQueryDefinition generateSimpleProductQuery() {
        return q -> q
                .id()
                .sku()
                .name()
                .description(d -> d.html())
                .image(i -> i.label().url())
                .thumbnail(t -> t.label().url())
                .urlKey()
                .stockStatus()
                .color()
                .price(this.generatePriceQuery())
                .mediaGalleryEntries(g -> g
                        .disabled()
                        .file()
                        .label()
                        .position()
                        .mediaType());
    }

    public ProductInterfaceQueryDefinition generateProductQuery() {
        // Custom attributes or attributes that are part of a non-standard
        // attribute set have to be added to the query manually. This also
        // requires the customer to use newly generated GraphQL classes.
        return q -> q
                .id()
                .sku()
                .name()
                .image(i -> i.label().url())
                .thumbnail(t -> t.label().url())
                .urlKey()
                .stockStatus()
                .price(this.generatePriceQuery())
                .categories(c -> c.urlPath())
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
                                        .valueIndex()
                                )
                                .product(this.generateSimpleProductQuery())));
    }

    private StoreConfigQueryDefinition generateStoreConfigQuery() {
        return q -> q.secureBaseMediaUrl();
    }


    private Asset mapAsset(MediaGalleryEntry entry) {
        AssetImpl asset = new AssetImpl();
        asset.setLabel(entry.getLabel());
        asset.setPosition(entry.getPosition());
        asset.setType(entry.getMediaType());
        asset.setPath(mediaBaseUrl + PRODUCT_IMAGE_FOLDER + entry.getFile());
        return asset;
    }
}
