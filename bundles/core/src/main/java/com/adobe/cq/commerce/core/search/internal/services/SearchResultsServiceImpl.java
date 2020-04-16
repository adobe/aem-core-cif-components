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

package com.adobe.cq.commerce.core.search.internal.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.search.internal.converters.AggregationToSearchAggregationConverter;
import com.adobe.cq.commerce.core.search.internal.converters.ProductToProductListItemConverter;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterRangeTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPriceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

@Component(service = SearchResultsService.class)
public class SearchResultsServiceImpl implements SearchResultsService {

    @Reference
    SearchFilterService searchFilterService;

    @Reference
    private UrlProvider urlProvider;

    private MagentoGraphqlClient magentoGraphqlClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResultsServiceImpl.class);

    public SearchResultsServiceImpl() {}

    public SearchResultsServiceImpl(final MagentoGraphqlClient magentoGraphqlClient) {
        this.magentoGraphqlClient = magentoGraphqlClient;
    }

    @Nonnull
    @Override
    public SearchResultsSet performSearch(
        final SearchOptions searchOptions,
        final Resource resource,
        final Page productPage,
        final SlingHttpServletRequest request) {
        return performSearch(searchOptions, resource, productPage, request, null);
    }

    @Nonnull
    @Override
    public SearchResultsSet performSearch(
        final SearchOptions searchOptions,
        final Resource resource,
        final Page productPage,
        final SlingHttpServletRequest request,
        final Consumer<ProductInterfaceQuery> productQueryHook) {

        SearchResultsSetImpl searchResultsSet = new SearchResultsSetImpl();
        searchResultsSet.setSearchOptions(searchOptions);

        if (magentoGraphqlClient == null) {
            magentoGraphqlClient = MagentoGraphqlClient.create(resource);
        }

        if (magentoGraphqlClient == null) {
            LOGGER.error("The search result service was unable to create a new MagentoGraphqlClient.");
            return searchResultsSet;
        }

        // We will use the search filter service to retrieve all of the potential available filters the commerce system
        // has available for querying against
        List<FilterAttributeMetadata> availableFilters = searchFilterService.retrieveCurrentlyAvailableCommerceFilters(resource);

        // Next we generate the graphql query and actually query the commerce system
        String queryString = generateQueryString(searchOptions, availableFilters, productQueryHook);
        LOGGER.debug("Generated query string {}", queryString);
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString);

        // If we have any errors returned we'll log them and return an empty search result
        if (response.getErrors() != null && response.getErrors().size() > 0) {
            response.getErrors().stream()
                .forEach(err -> LOGGER.error("An error has occurred: {} ({})", err.getMessage(), err.getCategory()));
            return searchResultsSet;
        }

        // Finally we transform the results to something useful and expected by other the Sling Models and wider display layer
        final List<ProductListItem> productListItems = extractProductsFromResponse(
            response.getData().getProducts().getItems(),
            productPage,
            request);
        final List<SearchAggregation> searchAggregations = extractSearchAggregationsFromResponse(response.getData().getProducts()
            .getAggregations(),
            searchOptions.getAllFilters(), availableFilters);
        searchResultsSet.setTotalResults(response.getData().getProducts().getTotalCount());
        searchResultsSet.setProductListItems(productListItems);
        searchResultsSet.setSearchAggregations(searchAggregations);

        return searchResultsSet;
    }

    /**
     * Generates a query string for the specified search term. This query string condition is 'like'.
     *
     * @param searchOptions options for searching
     * @param availableFilters available filters
     * @param productQueryHook
     * @return the query string
     */
    @Nonnull
    private String generateQueryString(
        final SearchOptions searchOptions,
        final List<FilterAttributeMetadata> availableFilters,
        final Consumer<ProductInterfaceQuery> productQueryHook) {
        GenericProductAttributeFilterInput filterInputs = new GenericProductAttributeFilterInput();

        searchOptions.getAllFilters().entrySet()
            .stream()
            .filter(field -> availableFilters.stream()
                .filter(item -> item.getAttributeCode().equals(field.getKey())).findFirst().isPresent())
            .forEach(filterCandidate -> {
                String code = filterCandidate.getKey();
                String value = filterCandidate.getValue();
                // this should be safe as we've filtered out search options already for those only with filter attributes
                final FilterAttributeMetadata filterAttributeMetadata = availableFilters.stream()
                    .filter(item -> item.getAttributeCode().equals(code)).findFirst().get();

                if ("FilterEqualTypeInput".equals(filterAttributeMetadata.getFilterInputType())) {
                    FilterEqualTypeInput filter = new FilterEqualTypeInput();
                    filter.setEq(value);
                    filterInputs.addEqualTypeInput(code, filter);
                } else if ("FilterMatchTypeInput".equals(filterAttributeMetadata.getFilterInputType())) {
                    FilterMatchTypeInput filter = new FilterMatchTypeInput();
                    filter.setMatch(value);
                    filterInputs.addMatchTypeInput(code, filter);
                } else if ("FilterRangeTypeInput".equals(filterAttributeMetadata.getFilterInputType())) {
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
            .items(generateProductQuery(productQueryHook))
            .aggregations(a -> a
                .options(ao -> ao
                    .count()
                    .label()
                    .value())
                .attributeCode()
                .count()
                .label());

        return Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
    }

    /**
     * Generates a query object for a product. The generated query contains the following fields: id, name, slug (url_key), image url,
     * regular price, regular price currency
     *
     * @return a {@link ProductInterfaceQueryDefinition} object
     * @param productQueryHook
     */
    @Nonnull
    private ProductInterfaceQueryDefinition generateProductQuery(
        final Consumer<ProductInterfaceQuery> productQueryHook) {
        return (ProductInterfaceQuery q) -> {
            q.id()
                .sku()
                .name()
                .smallImage(i -> i.url())
                .urlKey()
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery()))
                .onConfigurableProduct(cp -> cp
                    .priceRange(r -> r
                        .maximumPrice(generatePriceQuery())));
            if (productQueryHook != null) {
                productQueryHook.accept(q);
            }
        };
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
     * Extracts a list of products suitable for Sling Model consumption from the graphql response.
     *
     * @param products a {@link List<ProductInterface>} object
     * @param request
     * @return a list of {@link ProductListItem} objects
     */
    @Nonnull
    private List<ProductListItem> extractProductsFromResponse(List<ProductInterface> products, Page productPage,
        final SlingHttpServletRequest request) {

        LOGGER.debug("Found {} products for search term", products.size());

        ProductToProductListItemConverter converter = new ProductToProductListItemConverter(productPage, request, urlProvider);

        return products.stream()
            .map(converter)
            .filter(p -> p != null) // the converter returns null if the conversion fails
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
        final List<FilterAttributeMetadata> availableFilters) {

        if (CollectionUtils.isEmpty(aggregations) || CollectionUtils.isEmpty(availableFilters)) {
            return Collections.emptyList();
        }

        AggregationToSearchAggregationConverter converter = new AggregationToSearchAggregationConverter(appliedFilters, availableFilters);

        return aggregations.stream()
            .map(converter)
            .collect(Collectors.toList());
    }

}
