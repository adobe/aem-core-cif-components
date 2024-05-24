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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.CategoryUrlParameterRetriever;
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
import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterRangeTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeSortInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPriceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.SortEnum;
import com.adobe.cq.commerce.magento.graphql.SortField;
import com.adobe.cq.commerce.magento.graphql.SortFields;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(service = SearchResultsService.class)
public class SearchResultsServiceImpl implements SearchResultsService {

    private static final String CATEGORY_ID_FILTER = "category_id";
    private static final String CATEGORY_UID_FILTER = "category_uid";

    @Reference
    private SearchFilterServiceImpl searchFilterService;
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
        return performSearch(searchOptions, resource, productPage, request, productQueryHook, null, null).getRight();
    }

    @NotNull
    @Override
    public SearchResultsSet performSearch(SearchOptions searchOptions, Resource resource, Page productPage,
        SlingHttpServletRequest request, Consumer<ProductInterfaceQuery> productQueryHook,
        Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook) {
        return performSearch(searchOptions, resource, productPage, request, productQueryHook, productAttributeFilterHook, null).getRight();
    }

    @Nonnull
    @Override
    public Pair<CategoryInterface, SearchResultsSet> performSearch(
        final SearchOptions searchOptions,
        final Resource resource,
        final Page productPage,
        final SlingHttpServletRequest request,
        final Consumer<ProductInterfaceQuery> productQueryHook,
        AbstractCategoryRetriever categoryRetriever) {
        return performSearch(searchOptions, resource, productPage, request, productQueryHook, null, categoryRetriever);
    }

    @Nonnull
    @Override
    public Pair<CategoryInterface, SearchResultsSet> performSearch(
        final SearchOptions searchOptions,
        final Resource resource,
        final Page productPage,
        final SlingHttpServletRequest request,
        final Consumer<ProductInterfaceQuery> productQueryHook,
        Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook,
        AbstractCategoryRetriever categoryRetriever) {

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
        CategoryInterface category = null;

        if (categoryRetriever != null || mutableSearchOptions.getCategoryUid().isPresent()) {
            // if a categoryRetriever is given or if a category uid filter is given, fetch the category and (re)set the uid filter
            if (categoryRetriever == null) {
                categoryRetriever = new CategoryUrlParameterRetriever(magentoGraphqlClient);
                categoryRetriever.setIdentifier(mutableSearchOptions.getCategoryUid().get());
            }
            category = categoryRetriever.fetchCategory();
            if (category == null) {
                // category not found
                LOGGER.debug("Category not found.");
                return new ImmutablePair<>(null, searchResultsSet);
            }
            if (category.getUid() != null) {
                mutableSearchOptions.setCategoryUid(category.getUid().toString());
            }
        }

        if (categoryRetriever == null && StringUtils.isNotEmpty(mutableSearchOptions.getAllFilters().get(CATEGORY_ID_FILTER))) {
            // if no categoryRetriever is given but an id filter, fetch the category only but don't set the uid filter as id and uid filter
            // cannot be used together in the same query
            categoryRetriever = new CategoryUrlParameterRetriever(magentoGraphqlClient) {
                @Override
                public Pair<QueryQuery.CategoryListArgumentsDefinition, CategoryTreeQueryDefinition> generateCategoryQueryArgs(
                    String identifier) {
                    Pair<QueryQuery.CategoryListArgumentsDefinition, CategoryTreeQueryDefinition> original = super.generateCategoryQueryArgs(
                        identifier);
                    FilterEqualTypeInput identifierFilter = new FilterEqualTypeInput().setEq(identifier);
                    CategoryFilterInput filter = new CategoryFilterInput().setIds(identifierFilter);
                    return new ImmutablePair<>(q -> q.filters(filter), original.getRight());
                }
            };
            categoryRetriever.setIdentifier(mutableSearchOptions.getAllFilters().get(CATEGORY_ID_FILTER));
            category = categoryRetriever.fetchCategory();
        }

        List<Error> errors = new ArrayList<>();
        // We will use the search filter service to retrieve all of the potential available filters the commerce system
        // has available for querying against
        Pair<List<FilterAttributeMetadata>, List<Error>> filterAttributeInfo = searchFilterService
            .retrieveCurrentlyAvailableCommerceFiltersInfo(request, page);
        List<FilterAttributeMetadata> availableFilters = filterAttributeInfo.getLeft();
        SorterKey currentSorterKey = findSortKey(mutableSearchOptions);
        if (filterAttributeInfo.getRight() != null) {
            errors.addAll(filterAttributeInfo.getRight());
        }

        String productsQueryString = generateProductsQueryString(mutableSearchOptions, availableFilters, productQueryHook,
            productAttributeFilterHook,
            currentSorterKey);
        LOGGER.debug("Generated products query string {}", productsQueryString);
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(productsQueryString);

        // remove the category_uid filter from the search options after the query to not included it in all places
        removeCategoryUidFilterEntriesIfPossible(mutableSearchOptions, request);

        // If we have any errors returned we'll log them and return an empty search result
        if (CollectionUtils.isNotEmpty(response.getErrors())) {
            errors.addAll(response.getErrors());
            response.getErrors()
                .forEach(err -> LOGGER.error("An error has occurred: {} ({})", err.getMessage(), err.getCategory()));

            return new ImmutablePair<>(category, searchResultsSet);
        }

        // Finally we transform the results to something useful and expected by other the Sling Models and wider display layer
        Products products = response.getData().getProducts();
        final List<ProductListItem> productListItems = extractProductsFromResponse(products.getItems(), productPage, request, resource,
            category);

        prepareSortKeys(currentSorterKey, products.getSortFields(), mutableSearchOptions, searchResultsSet);

        List<SearchAggregation> searchAggregations = extractSearchAggregationsFromResponse(products.getAggregations(),
            mutableSearchOptions.getAllFilters(), availableFilters);

        searchResultsSet.setTotalResults(products.getTotalCount());
        searchResultsSet.setProductListItems(productListItems);
        searchResultsSet.setSearchAggregations(searchAggregations);
        searchResultsSet.setErrors(errors);

        return new ImmutablePair<>(category, searchResultsSet);
    }

    private SorterKey findSortKey(SearchOptionsImpl searchOptions) {
        SorterKey defaultSorterKey = searchOptions.getDefaultSorter();
        String sortKeyParam = searchOptions.getAllFilters().get(Sorter.PARAMETER_SORT_KEY);
        if (sortKeyParam == null) {
            if (defaultSorterKey != null) {
                sortKeyParam = defaultSorterKey.getName();
            } else {
                return null;
            }
        }

        Sorter.Order defaultSortOrder;
        if (defaultSorterKey != null) {
            defaultSortOrder = defaultSorterKey.getOrder();
            if (defaultSortOrder == null) {
                defaultSortOrder = Sorter.Order.ASC;
            }
        } else {
            defaultSortOrder = Sorter.Order.ASC;
        }

        String sortOrderParam = searchOptions.getAllFilters().get(Sorter.PARAMETER_SORT_ORDER);
        Sorter.Order sortOrder = Sorter.Order.fromString(sortOrderParam, defaultSortOrder);

        SorterKeyImpl resultSorterKey = new SorterKeyImpl(sortKeyParam, sortKeyParam);
        resultSorterKey.setOrder(sortOrder);
        resultSorterKey.setSelected(true);

        return resultSorterKey;
    }

    private void prepareSortKeys(SorterKey currentSorterKey, SortFields sortFields, SearchOptions searchOptions,
        SearchResultsSetImpl searchResultsSet) {

        String defaultSortField = null;
        if (sortFields != null) {
            for (SortField sortField : sortFields.getOptions()) {
                if (searchOptions.getSorterKeys().stream().noneMatch(sk -> sk.getName().equals(sortField.getValue()))) {
                    searchOptions.addSorterKey(sortField.getValue(), sortField.getLabel(), Sorter.Order.ASC);
                }
            }

            defaultSortField = sortFields.getDefault();
        }

        List<SorterKey> availableSorterKeys = searchOptions.getSorterKeys();

        SorterImpl sorter = searchResultsSet.getSorter();
        List<SorterKey> keys = new ArrayList<>(availableSorterKeys);
        keys.sort(Comparator.comparing(SorterKey::getLabel));
        sorter.setKeys(keys);

        for (SorterKey key : keys) {
            SorterKeyImpl keyImpl = (SorterKeyImpl) key;

            Map<String, String> cParams = new HashMap<>(searchOptions.getAllFilters());
            cParams.put(Sorter.PARAMETER_SORT_KEY, key.getName());
            Sorter.Order keyOrder = keyImpl.getOrder();
            if (currentSorterKey == null) {
                if (defaultSortField != null && defaultSortField.equals(key.getName())) {
                    keyImpl.setSelected(true);
                    sorter.setCurrentKey(key);
                }
            } else {
                if (currentSorterKey.getName().equals(key.getName())) {
                    keyImpl.setSelected(true);
                    sorter.setCurrentKey(key);
                    keyOrder = currentSorterKey.getOrder();
                } else if (keyOrder == null) {
                    keyOrder = currentSorterKey.getOrder();
                }
            }
            keyImpl.setOrder(keyOrder);
            cParams.put(Sorter.PARAMETER_SORT_ORDER, keyOrder.name().toLowerCase());
            keyImpl.setCurrentOrderParameters(cParams);

            Map<String, String> oParams = new HashMap<>(searchOptions.getAllFilters());
            oParams.put(Sorter.PARAMETER_SORT_KEY, key.getName());
            oParams.put(Sorter.PARAMETER_SORT_ORDER, keyOrder.opposite().name().toLowerCase());
            keyImpl.setOppositeOrderParameters(oParams);
        }
    }

    private String generateProductsQueryString(
        final SearchOptions searchOptions,
        final List<FilterAttributeMetadata> availableFilters,
        final Consumer<ProductInterfaceQuery> productQueryHook,
        Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook,
        final SorterKey sorterKey) {
        ProductAttributeFilterInput filterInputs = new ProductAttributeFilterInput();

        // Apply product attribute filter hook if set
        if (productAttributeFilterHook != null) {
            filterInputs = productAttributeFilterHook.apply(filterInputs);
        }
        final ProductAttributeFilterInput filterInputs2 = filterInputs;

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
                    filterInputs2.setCustomFilter(code, filter);
                } else if ("FilterMatchTypeInput".equals(filterAttributeMetadata.getFilterInputType())) {
                    FilterMatchTypeInput filter = new FilterMatchTypeInput();
                    filter.setMatch(value);
                    filterInputs2.setCustomFilter(code, filter);
                } else if ("FilterRangeTypeInput".equals(filterAttributeMetadata.getFilterInputType())) {
                    FilterRangeTypeInput filter = new FilterRangeTypeInput();
                    final String[] rangeValues = value.split("_");
                    if (rangeValues.length == 1 && StringUtils.isNumeric(rangeValues[0])) {
                        // The range has a single value like '60'
                        filter.setFrom(rangeValues[0]);
                        filter.setTo(rangeValues[0]);
                        filterInputs2.setCustomFilter(code, filter);
                    } else if (rangeValues.length > 1) {
                        // For values such as '*_60', the from range should be left empty
                        if (StringUtils.isNumeric(rangeValues[0])) {
                            filter.setFrom(rangeValues[0]);
                        }
                        // For values such as '60_*', the to range should be left empty
                        if (StringUtils.isNumeric(rangeValues[1])) {
                            filter.setTo(rangeValues[1]);
                        }
                        filterInputs2.setCustomFilter(code, filter);
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
            productArguments.filter(filterInputs2);
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
                    LOGGER.debug("Unrecognized sort key: " + sortKey);
                }
                if (validSortKey) {
                    productArguments.sort(sort);
                } else {
                    // handle sort keys not supported in the current magento-graphql library
                    productArguments.sort(new ProductAttributeSortInput() {
                        @Override
                        public void appendTo(StringBuilder _queryBuilder) {
                            _queryBuilder.append('{');
                            _queryBuilder.append(sortKey + ":");
                            _queryBuilder.append(sortEnum.toString());
                            _queryBuilder.append('}');
                        }
                    });
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
                .label())
            .sortFields(s -> s.options(sf -> sf.value().label()).defaultValue());

        return Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
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
                .urlKey()
                .urlPath()
                .urlRewrites(uq -> uq.url())
                .name()
                .smallImage(i -> i.url())
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
        final SlingHttpServletRequest request, Resource resource, CategoryInterface categoryContext) {

        LOGGER.debug("Found {} products for search term", products.size());

        String resourceType = resource.getResourceType();
        String prefix = StringUtils.substringAfterLast(resourceType, "/");
        String parentId = ComponentUtils.generateId(prefix, resource.getPath());
        ProductToProductListItemConverter converter = new ProductToProductListItemConverter(productPage, request, urlProvider, parentId,
            categoryContext);

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
     * Removes the category_uid filter from the search options if it can be obtained from the request.
     *
     * @param mutableSearchOptions
     * @param request
     */
    private void removeCategoryUidFilterEntriesIfPossible(SearchOptionsImpl mutableSearchOptions, SlingHttpServletRequest request) {
        String categoryUid = urlProvider.getCategoryIdentifier(request);
        if (StringUtils.isNotBlank(categoryUid) &&
            mutableSearchOptions.getCategoryUid().filter(categoryUid::equals).isPresent()) {
            mutableSearchOptions.setCategoryUid(null);
        }
    }
}
