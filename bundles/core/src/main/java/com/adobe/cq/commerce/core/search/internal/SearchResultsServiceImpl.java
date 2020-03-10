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

package com.adobe.cq.commerce.core.search.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.SearchAggregation;
import com.adobe.cq.commerce.core.search.SearchFilterService;
import com.adobe.cq.commerce.core.search.SearchOptions;
import com.adobe.cq.commerce.core.search.SearchResultsService;
import com.adobe.cq.commerce.core.search.SearchResultsSet;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterRangeTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPriceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.introspection.GenericProductAttributeFilterInput;
import com.day.cq.wcm.api.Page;

@Component(service = SearchResultsService.class)
public class SearchResultsServiceImpl implements SearchResultsService {

    @Reference
    SearchFilterService searchFilterService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResultsServiceImpl.class);

    @Nonnull
    @Override
    public SearchResultsSet performSearch(
        final SearchOptions searchOptions,
        final Resource resource,
        final Page productPage) {

        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource);

        Locale locale = productPage.getLanguage(false);

        Map<String, String> availableFilters = searchFilterService.retrieveCurrentlyAvailableCommerceFilters(resource);

        SearchResultsSetImpl searchResultsSet = new SearchResultsSetImpl();
        searchResultsSet.setSearchOptions(searchOptions);

        if (magentoGraphqlClient == null) {
            return searchResultsSet;
        }

        String queryString = generateQueryString(searchOptions, availableFilters);

        LOGGER.debug("Generated query string {}", queryString);
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString);

        if (!checkAndLogErrors(response.getErrors())) {
            final List<ProductListItem> productListItems = extractProductsFromResponse(
                response.getData().getProducts().getItems(),
                productPage,
                locale);
            final List<SearchAggregation> searchAggregations = extractSearchAggregationsFromResponse(response.getData().getProducts()
                .getAggregations(),
                searchOptions.getAllFilters(), availableFilters);
            searchResultsSet.setTotalResults(response.getData().getProducts().getTotalCount());
            searchResultsSet.setProductListItems(productListItems);
            searchResultsSet.setSearchAggregations(searchAggregations);
        }

        return searchResultsSet;

    }

    /**
     * Generates a query string for the specified search term. This query string condition is 'like'.
     *
     * @param searchOptions options for searching
     * @param availableFilters available filters
     * @return the query string
     */
    @Nonnull
    private String generateQueryString(
        final SearchOptions searchOptions,
        final Map<String, String> availableFilters) {
        GenericProductAttributeFilterInput filterInputs = new GenericProductAttributeFilterInput();

        searchOptions.getAllFilters().entrySet()
            .stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().replace("_bucket", "")))
            .entrySet().stream()
            .filter(field -> availableFilters.containsKey(field.getKey()))
            .forEach(filterCandidate -> {
                String code = filterCandidate.getKey();
                String value = filterCandidate.getValue();
                String filterType = availableFilters.get(code);

                if ("FilterEqualTypeInput".equals(filterType)) {
                    FilterEqualTypeInput filter = new FilterEqualTypeInput();
                    filter.setEq(value);
                    filterInputs.addEqualTypeInput(code, filter);
                } else if ("FilterMatchTypeInput".equals(filterType)) {
                    FilterMatchTypeInput filter = new FilterMatchTypeInput();
                    filter.setMatch(value);
                    filterInputs.addMatchTypeInput(code, filter);
                } else if ("FilterRangeTypeInput".equals(filterType)) {
                    FilterRangeTypeInput filter = new FilterRangeTypeInput();
                    final String[] rangeValues = value.split("_");
                    filter.setFrom(rangeValues[0]);
                    // For values such as `60_*`, the to range should be left empty
                    if (StringUtils.isNumeric(rangeValues[1])) {
                        filter.setTo(rangeValues[1]);
                    }
                    filterInputs.addRangeTypeInput(code, filter);
                }

            });

        QueryQuery.ProductsArgumentsDefinition searchArgs;

        searchArgs = productArguments -> {
            if (searchOptions.getSearchQuery().isPresent()) {
                productArguments.search(searchOptions.getSearchQuery().get());
            }
            productArguments.currentPage(searchOptions.getCurrentPage());
            productArguments.pageSize(searchOptions.getPageSize());
            productArguments.filter(filterInputs);
        };

        ProductsQueryDefinition queryArgs = productsQuery -> productsQuery
            .totalCount()
            .items(generateProductQuery())
            .aggregations(a -> a
                .options(ao -> ao
                    .count()
                    .label()
                    .value())
                .attributeCode()
                .count()
                .label());

        return Operations.query(query -> query.products(searchArgs, queryArgs))
            .toString();

    }

    /**
     * Checks the graphql response for errors and logs out to the error console if any are found
     *
     * @param errors the {@link List<Error>} if any
     * @return {@link true} if any errors were found, {@link false} otherwise
     */
    private boolean checkAndLogErrors(List<Error> errors) {
        if (errors != null && errors.size() > 0) {
            errors.stream()
                .forEach(err -> LOGGER.error("An error has occurred: {} ({})", err.getMessage(), err.getCategory()));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Generates a query object for a product. The generated query contains the following fields: id, name, slug (url_key), image url,
     * regular price, regular price currency
     *
     * @return a {@link ProductInterfaceQueryDefinition} object
     */
    @Nonnull
    private ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> q.id()
            .urlKey()
            .name()
            .smallImage(i -> i
                .label()
                .url())
            .onConfigurableProduct(cp -> cp
                .priceRange(r -> r
                    .maximumPrice(generatePriceQuery())
                    .minimumPrice(generatePriceQuery())))
            .onSimpleProduct(sp -> sp
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery())));
    }

    /**
     * todo: this is used in a number of classes, should likely be moved to a shared class
     * 
     * @return
     */
    private ProductPriceQueryDefinition generatePriceQuery() {
        return q -> q
            .regularPrice(r -> r
                .value()
                .currency())
            .finalPrice(f -> f
                .value()
                .currency())
            .discount(d -> d
                .amountOff()
                .percentOff());
    }

    /**
     * Extracts a list of products from the graphql response. This method uses
     * {@link SearchResultsServiceImpl#generateItemFromProductInterface(ProductInterface, Page, Locale)} to tranform the objects from the
     * Graphql response to {@link ProductListItem} objects
     *
     * @param products a {@link List<ProductInterface>} object
     * @return a list of {@link ProductListItem} objects
     */
    @Nonnull
    private List<ProductListItem> extractProductsFromResponse(List<ProductInterface> products, Page productPage, Locale locale) {

        LOGGER.debug("Found {} products for search term", products.size());

        return products.stream()
            .map(product -> generateItemFromProductInterface(product, productPage, locale))
            .collect(Collectors.toList());
    }

    /**
     * Extracts {@link List<SearchAggregation>} from the response object returned from the GraphQL query. This method enriches the response
     * data from the search query with the information about which filters are actually available as well as which filters are actually
     * applied.
     *
     * @param aggregations the response aggregation data
     * @param appliedFilters the currently applied filters
     * @param availableFilters the filters that are available
     * @return enriched {@link SearchAggregation} objects
     */
    private List<SearchAggregation> extractSearchAggregationsFromResponse(
        final List<Aggregation> aggregations,
        final Map<String, String> appliedFilters,
        final Map<String, String> availableFilters) {
        return aggregations.stream()
            .map(aggregation -> {
                String filter = null;
                if (appliedFilters.get(aggregation.getAttributeCode()) != null) {
                    filter = appliedFilters.get(aggregation.getAttributeCode());
                }
                // filterable will be true or false depending on whether or not the attribute appears in the list of available filters
                // provided by the introspection query
                final boolean filterable = availableFilters.entrySet().stream()
                    .anyMatch(filterCandidate -> filterCandidate.getKey().equals(aggregation.getAttributeCode().replace("_bucket", "")));
                return new MagentoGraphQLSearchAggregationAdapter(aggregation, filter, filterable, appliedFilters);
            })
            .collect(Collectors.toList());
    }

    /**
     * Transforms a {@link ProductInterface} object into a {@link ProductListItem}
     *
     * @param product the {@link ProductInterface} object to transform
     * @return a new {@link ProductListItem} object
     */
    @Nonnull
    private ProductListItem generateItemFromProductInterface(ProductInterface product, Page productPage, Locale locale) {

        Price price = new PriceImpl(product.getPriceRange(), locale);

        ProductListItem productListItem = new ProductListItemImpl(product.getSku(),
            product.getUrlKey(),
            product.getName(),
            price,
            product.getSmallImage()
                .getUrl(),
            productPage,
            null,
            null); // todo: should pass in request object

        return productListItem;
    }
}
