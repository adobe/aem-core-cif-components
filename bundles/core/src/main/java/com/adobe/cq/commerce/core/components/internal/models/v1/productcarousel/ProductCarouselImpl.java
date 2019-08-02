/*
 * Copyright 2019 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPricesQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductCarousel.class, resourceType = ProductCarouselImpl.RESOURCE_TYPE)
public class ProductCarouselImpl implements ProductCarousel {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productcarousel/v1/productcarousel";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCarouselImpl.class);

    @Inject
    private Resource resource;

    @Inject
    private String[] productSkuList;

    @Inject
    private Page currentPage;

    private List<ProductInterface> productList;

    private Page productPage;

    private MagentoGraphqlClient magentoGraphqlClient;

    @PostConstruct
    private void initModel() {
        final List<String> productSkus = Arrays.asList(this.productSkuList);
        magentoGraphqlClient = MagentoGraphqlClient.create(resource);
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
        if (magentoGraphqlClient == null) {
            LOGGER.error("Cannot get a GraphqlClient using the resource at {}",
                resource.getPath());
        }
        this.productList = this.fetchProductFromGraphql(magentoGraphqlClient, productSkus);
        Collections.sort(this.productList, Comparator.comparing(item -> productSkus.indexOf(item.getSku())));
    }

    public ProductPricesQueryDefinition generatePriceQuery() {
        return q -> q
            .regularPrice(rp -> rp
                .amount(a -> a
                    .currency()
                    .value()));
    }

    public ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> q
            .id()
            .sku()
            .name()
            .thumbnail(t -> t.label().url())
            .urlKey()
            .price(this.generatePriceQuery());
    }

    private List<ProductInterface> fetchProductFromGraphql(MagentoGraphqlClient client,
        final List<String> productSkus) {
        FilterTypeInput input = new FilterTypeInput().setIn(productSkus);
        ProductFilterInput filter = new ProductFilterInput().setSku(input);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        ProductsQueryDefinition queryArgs = q -> q.items(this.generateProductQuery());
        final String queryString = Operations.query(query -> query
            .products(searchArgs, queryArgs)).toString();

        GraphqlResponse<Query, com.adobe.cq.commerce.magento.graphql.gson.Error> response = magentoGraphqlClient.execute(queryString);
        Query rootQuery = response.getData();
        List<ProductInterface> products = rootQuery.getProducts().getItems();
        if (products.size() > 0) {
            return products;
        }
        return null;
    }

    @Override
    public List<ProductListItem> getProducts() {
        List<ProductListItem> carouselProductList = new ArrayList<>();
        if (!this.productList.isEmpty()) {
            for (ProductInterface product : this.productList) {
                carouselProductList.add(new ProductListItemImpl(
                    product.getSku(),
                    product.getUrlKey(),
                    product.getName(),
                    product.getPrice().getRegularPrice().getAmount().getValue(),
                    product.getPrice().getRegularPrice().getAmount().getCurrency().toString(),
                    product.getThumbnail().getUrl(),
                    productPage));
            }
        }
        return carouselProductList;
    }
}
