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

package com.adobe.cq.commerce.core.components.internal.models.v1.searchresults;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.ProductsArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;

/**
 * Concrete implementation of the {@link SearchResults} Sling Model API
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = SearchResults.class,
    resourceType = SearchResultsImpl.RESOURCE_TYPE)
public class SearchResultsImpl implements SearchResults {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResultsImpl.class);
    static final String RESOURCE_TYPE = "core/cif/components/commerce/searchresults";

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    private String searchTerm;
    private MagentoGraphqlClient magentoGraphqlClient;
    private boolean deepLink;

    Page productPage;

    @PostConstruct
    protected void initModel() {
        deepLink = !WCMMode.DISABLED.equals(WCMMode.fromRequest(request));
        searchTerm = request.getParameter("search_query");
        LOGGER.debug("Detected search parameter {}", searchTerm);

        // Get MagentoGraphqlClient from the resource.
        magentoGraphqlClient = MagentoGraphqlClient.create(resource);
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
    }

    /**
     * {@see SearchResults#getProducts()}
     */
    @Nonnull
    @Override
    public Collection<ProductListItem> getProducts() {
        if (magentoGraphqlClient == null || StringUtils.isEmpty(searchTerm)) {
            return Collections.emptyList();
        }

        String queryString = generateQueryString(searchTerm);

        LOGGER.debug("Generated query string {}", queryString);
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString);
        return extractProductsFromResponse(response);
    }

    /**
     * Generates a query string for the specified search term. This query string condition is 'like'.
     * 
     * @param searchTerm the search term used for filtering
     * @return the query string
     */
    @Nonnull
    protected String generateQueryString(String searchTerm) {
        ProductsArgumentsDefinition searchArgs = s -> s.search(searchTerm);
        ProductsQueryDefinition queryArgs = productsQuery -> productsQuery.items(generateProductQuery());
        return Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
    }

    /**
     * Generates a query object for a product. The generated query contains the following fields: id, name, slug (url_key), image url,
     * regular price, regular price currency
     *
     * @return a {@link ProductInterfaceQueryDefinition} object
     */
    @Nonnull
    protected ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> q.id()
            .urlKey()
            .name()
            .smallImage(i -> i.label()
                .url())
            .price(price -> price.regularPrice(
                regularPrice -> regularPrice.amount(
                    moneyQuery -> moneyQuery.value().currency())));
    }

    /**
     * Extracts a list of products from the graphql response. This method uses
     * {@link SearchResultsImpl#generateItemFromProductInterface(ProductInterface)} to tranform the objects from the Graphql response to
     * {@link ProductListItem} objects
     * 
     * @param response a {@link GraphqlResponse} object
     * @return a list of {@link ProductListItem} objects
     */
    @Nonnull
    protected List<ProductListItem> extractProductsFromResponse(GraphqlResponse<Query, Error> response) {
        Query rootQuery = response.getData();
        List<ProductInterface> products = rootQuery.getProducts().getItems();

        LOGGER.debug("Found {} products for search term {}", products.size(), searchTerm);

        return products.stream()
            .map(product -> generateItemFromProductInterface(product))
            .collect(Collectors.toList());
    }

    /**
     * Transforms a {@link ProductInterface} object into a {@link ProductListItem}
     *
     * @param product the {@link ProductInterface} object to transform
     * @return a new {@link ProductListItem} object
     */
    @Nonnull
    protected ProductListItem generateItemFromProductInterface(ProductInterface product) {

        ProductListItem productListItem = new ProductListItemImpl(product.getSku(),
            product.getUrlKey(),
            product.getName(),
            product.getPrice()
                .getRegularPrice()
                .getAmount()
                .getValue(),
            product.getPrice()
                .getRegularPrice()
                .getAmount()
                .getCurrency()
                .toString(),
            product.getSmallImage()
                .getUrl(),
            productPage,
            null,
            deepLink);

        return productListItem;
    }
}
