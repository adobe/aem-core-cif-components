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
package com.adobe.cq.commerce.core.search.internal.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.search.internal.converters.AggregationToSearchAggregationConverter;
import com.adobe.cq.commerce.core.search.internal.converters.ProductToProductListItemConverter;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.internal.models.SorterImpl;
import com.adobe.cq.commerce.core.search.internal.models.SorterKeyImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;
import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterRangeTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeSortInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPriceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.CategoryListArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.SortEnum;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(service = SearchResultsService.class)
public class SearchResultsServiceImpl implements SearchResultsService {

    private static final String CATEGORY_ID_FILTER = "category_id";
    private static final String CATEGORY_UID_FILTER = "category_uid";

    @Reference
    private SearchFilterService searchFilterService;
    @Reference
    private UrlProvider urlProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResultsServiceImpl.class);

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
        return performSearch(searchOptions, resource, productPage, request, productQueryHook, null).getRight();
    }

    @Nonnull
    @Override
    public Pair<CategoryInterface, SearchResultsSet> performSearch(
        final SearchOptions searchOptions,
        final Resource resource,
        final Page productPage,
        final SlingHttpServletRequest request,
        final Consumer<ProductInterfaceQuery> productQueryHook,
        final AbstractCategoryRetriever categoryRetriever) {

        SearchResultsSetImpl searchResultsSet = new SearchResultsSetImpl();
        SearchOptionsImpl mutableSearchOptions = new SearchOptionsImpl(searchOptions);
        searchResultsSet.setSearchOptions(mutableSearchOptions);
        MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
        Page page = resource.adaptTo(Page.class);
        if (page == null) {
            PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
            if (pageManager != null) {
                page = pageManager.getContainingPage(resource);
            }
        }

        if (magentoGraphqlClient == null || page == null) {
            LOGGER.error("The search result service was unable to create a new MagentoGraphqlClient.");
            return new ImmutablePair<>(null, searchResultsSet);
        }

        // Next we generate the graphql category query and actually query the commerce system
        CategoryTree category = null;
        if (generateCategoryQueryString(categoryRetriever).isPresent()) {
            String categoryQueryString = generateCategoryQueryString(categoryRetriever).get();
            LOGGER.debug("Generated category query string {}", categoryQueryString);
            GraphqlResponse<Query, Error> categoryResponse = magentoGraphqlClient.execute(categoryQueryString);
            if (CollectionUtils.isEmpty(categoryResponse.getErrors()) && categoryResponse.getData() != null) {
                Query categoryData = categoryResponse.getData();
                List<CategoryTree> categories = categoryData.getCategoryList();
                if (CollectionUtils.isNotEmpty(categories)) {
                    category = categories.get(0);
                    mutableSearchOptions.setCategoryUid(category.getUid().toString());
                }
            }
        }

        // We will use the search filter service to retrieve all of the potential available filters the commerce system
        // has available for querying against
        List<FilterAttributeMetadata> availableFilters = searchFilterService.retrieveCurrentlyAvailableCommerceFilters(page);
        SorterKey currentSorterKey = prepareSorting(mutableSearchOptions, searchResultsSet);

        String productsQueryString = generateProductsQueryString(mutableSearchOptions, availableFilters, productQueryHook,
            currentSorterKey);
        LOGGER.debug("Generated products query string {}", productsQueryString);
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(productsQueryString);

        // If we have any errors returned we'll log them and return an empty search result
        if (CollectionUtils.isNotEmpty(response.getErrors())) {
            response.getErrors()
                .forEach(err -> LOGGER.error("An error has occurred: {} ({})", err.getMessage(), err.getCategory()));

            return new ImmutablePair<>(category, searchResultsSet);
        }

        // Finally we transform the results to something useful and expected by other the Sling Models and wider display layer
        Products products = response.getData().getProducts();
        final List<ProductListItem> productListItems = extractProductsFromResponse(
            products.getItems(),
            productPage,
            request,
            resource);

        List<SearchAggregation> searchAggregations = extractSearchAggregationsFromResponse(products.getAggregations(),
            mutableSearchOptions.getAllFilters(), availableFilters);

        // special handling of category identifier(s)
        removeCategoryIdAggregationIfNecessary(searchAggregations, mutableSearchOptions);
        removeCategoryUidFilterEntriesIfPossible(searchAggregations, request);

        searchResultsSet.setTotalResults(products.getTotalCount());
        searchResultsSet.setProductListItems(productListItems);
        searchResultsSet.setSearchAggregations(searchAggregations);

        return new ImmutablePair<>(category, searchResultsSet);
    }

    private SorterKey prepareSorting(SearchOptions searchOptions, SearchResultsSetImpl searchResultsSet) {
        List<SorterKey> availableSorterKeys = searchOptions.getSorterKeys();
        if (CollectionUtils.isEmpty(availableSorterKeys)) {
            return null;
        }

        SorterKey resultSorterKey = null;

        SorterKey defaultSorterKey = availableSorterKeys.get(0);
        String sortKeyParam = searchOptions.getAllFilters().get(Sorter.PARAMETER_SORT_KEY);
        if (sortKeyParam == null) {
            sortKeyParam = defaultSorterKey.getName();
        }
        String sortOrderParam = searchOptions.getAllFilters().get(Sorter.PARAMETER_SORT_ORDER);
        Sorter.Order sortOrder;
        try {
            if (sortOrderParam != null) {
                sortOrderParam = Sorter.Order.valueOf(sortOrderParam.toUpperCase()).name();
            }
        } catch (RuntimeException x) {
            sortOrderParam = null;
        }
        if (sortOrderParam == null) {
            sortOrder = defaultSorterKey.getOrder();
            if (sortOrder == null) {
                sortOrder = Sorter.Order.ASC;
            }
        } else {
            sortOrder = Sorter.Order.valueOf(sortOrderParam.toUpperCase());
        }

        SorterImpl sorter = searchResultsSet.getSorter();
        List<SorterKey> keys = new ArrayList<>();
        keys.addAll(availableSorterKeys);
        sorter.setKeys(keys);

        for (SorterKey key : keys) {
            SorterKeyImpl keyImpl = (SorterKeyImpl) key;

            Map<String, String> cParams = new HashMap<>(searchOptions.getAllFilters());
            cParams.put(Sorter.PARAMETER_SORT_KEY, key.getName());
            Sorter.Order keyOrder = keyImpl.getOrder();
            if (sortKeyParam.equals(key.getName())) {
                keyImpl.setSelected(true);
                sorter.setCurrentKey(key);
                keyOrder = sortOrder;
                resultSorterKey = keyImpl;
            } else if (keyOrder == null) {
                keyOrder = sortOrder;
            }
            keyImpl.setOrder(keyOrder);
            cParams.put(Sorter.PARAMETER_SORT_ORDER, keyOrder.name().toLowerCase());
            keyImpl.setCurrentOrderParameters(cParams);

            Map<String, String> oParams = new HashMap<>(searchOptions.getAllFilters());
            oParams.put(Sorter.PARAMETER_SORT_KEY, key.getName());
            oParams.put(Sorter.PARAMETER_SORT_ORDER, keyOrder.opposite().name().toLowerCase());
            keyImpl.setOppositeOrderParameters(oParams);
        }

        return resultSorterKey;
    }

    private String generateProductsQueryString(
        final SearchOptions searchOptions,
        final List<FilterAttributeMetadata> availableFilters,
        final Consumer<ProductInterfaceQuery> productQueryHook,
        final SorterKey sorterKey) {
        GenericProductAttributeFilterInput filterInputs = new GenericProductAttributeFilterInput();

        searchOptions.getAllFilters().entrySet()
            .stream()
            .filter(field -> availableFilters.stream()
                .anyMatch(item -> item.getAttributeCode().equals(field.getKey())))
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
                    if (rangeValues.length == 1 && StringUtils.isNumeric(rangeValues[0])) {
                        // The range has a single value like '60'
                        filter.setFrom(rangeValues[0]);
                        filter.setTo(rangeValues[0]);
                        filterInputs.addRangeTypeInput(code, filter);
                    } else if (rangeValues.length > 1) {
                        // For values such as '*_60', the from range should be left empty
                        if (StringUtils.isNumeric(rangeValues[0])) {
                            filter.setFrom(rangeValues[0]);
                        }
                        // For values such as '60_*', the to range should be left empty
                        if (StringUtils.isNumeric(rangeValues[1])) {
                            filter.setTo(rangeValues[1]);
                        }
                        filterInputs.addRangeTypeInput(code, filter);
                    }
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
            if (sorterKey != null) {
                String sortKey = sorterKey.getName();
                String sortOrder = sorterKey.getOrder().name();
                ProductAttributeSortInput sort = new ProductAttributeSortInput();
                SortEnum sortEnum = SortEnum.valueOf(sortOrder);
                boolean validSortKey = true;
                if ("relevance".equals(sortKey)) {
                    sort.setRelevance(sortEnum);
                } else if ("name".equals(sortKey)) {
                    sort.setName(sortEnum);
                } else if ("price".equals(sortKey)) {
                    sort.setPrice(sortEnum);
                } else if ("position".equals(sortKey)) {
                    sort.setPosition(sortEnum);
                } else {
                    validSortKey = false;
                    LOGGER.warn("Unknown sort key: " + sortKey);
                }
                if (validSortKey) {
                    productArguments.sort(sort);
                }
            }
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
     * Generates a query string for the category specified in the retriever.
     *
     * @param categoryRetriever
     * @return the query string
     */
    @Nonnull
    private Optional<String> generateCategoryQueryString(final AbstractCategoryRetriever categoryRetriever) {
        if (categoryRetriever != null) {
            Pair<CategoryListArgumentsDefinition, CategoryTreeQueryDefinition> categoryArgs = categoryRetriever.generateCategoryQueryArgs();
            return Optional.of(Operations.query(query -> query
                .categoryList(categoryArgs.getLeft(), categoryArgs.getRight())).toString());
        }

        return Optional.empty();
    }

    /**
     * Generates a query object for a product. The generated query contains the following fields: id, name, slug (url_key), image url,
     * regular price, regular price currency
     *
     * @param productQueryHook
     * @return a {@link ProductInterfaceQueryDefinition} object
     */
    @Nonnull
    private ProductInterfaceQueryDefinition generateProductQuery(
        final Consumer<ProductInterfaceQuery> productQueryHook) {
        return (ProductInterfaceQuery q) -> {
            q.sku()
                .name()
                .smallImage(i -> i.url())
                .urlKey()
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery()))
                .onConfigurableProduct(cp -> cp
                    .priceRange(r -> r
                        .maximumPrice(generatePriceQuery())))
                .onBundleProduct(bp -> bp
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
        final SlingHttpServletRequest request, Resource resource) {

        LOGGER.debug("Found {} products for search term", products.size());

        ProductToProductListItemConverter converter = new ProductToProductListItemConverter(productPage, request, urlProvider, resource);

        return products.stream()
            .map(converter)
            .filter(Objects::nonNull) // the converter returns null if the conversion fails
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

    /**
     * Removes the category_id filter from the search aggregations when category_uid filter is present because either cannot be combined
     * with the other.
     *
     * @param aggs
     * @param searchOptions
     */
    private void removeCategoryIdAggregationIfNecessary(List<SearchAggregation> aggs, SearchOptionsImpl searchOptions) {
        // Special treatment for category_id filter as this is always present and collides with category_uid filter (CIF-2206)
        if (searchOptions.getCategoryUid().isPresent()) {
            aggs.removeIf(agg -> CATEGORY_ID_FILTER.equals(agg.getIdentifier()));
        }
    }

    /**
     * Removes the category_uid filter from all filter options' filter maps when the category uid can be retrieved from the request already.
     *
     * @param aggs
     * @param request
     */
    private void removeCategoryUidFilterEntriesIfPossible(List<SearchAggregation> aggs, SlingHttpServletRequest request) {
        String categoryUid = urlProvider.getCategoryIdentifier(request);
        if (StringUtils.isNotBlank(categoryUid)) {
            aggs.stream()
                .map(SearchAggregation::getOptions)
                .flatMap(Collection::stream)
                .map(SearchAggregationOption::getAddFilterMap)
                .forEach(filterMap -> filterMap.remove(CATEGORY_UID_FILTER, categoryUid));
        }
    }
}
