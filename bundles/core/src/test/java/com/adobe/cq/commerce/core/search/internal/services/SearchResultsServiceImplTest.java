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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithCategoryAndUrlKey;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.search.internal.models.FilterAttributeMetadataImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Aggregation;
import com.adobe.cq.commerce.magento.graphql.AggregationOption;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterRangeTypeInput;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.PriceRange;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.shopify.graphql.support.ID;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchResultsServiceImplTest {

    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");

    @Mock
    SearchFilterServiceImpl searchFilterService;
    @Mock
    MagentoGraphqlClient magentoGraphqlClient;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Query query;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Products products;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProductInterface product;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    PriceRange priceRange;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Money money;

    CategoryTree categoryTree = new CategoryTree();
    List<ProductInterface> productHits = new ArrayList<>();
    MockSlingHttpServletRequest request;
    Resource resource;
    SearchResultsServiceImpl serviceUnderTest;
    SearchOptionsImpl searchOptions;
    Page productPage;

    private static final String FILTER_ATTRIBUTE_NAME_CODE = "name";
    private static final String FILTER_ATTRIBUTE_COLOR_CODE = "color";
    private static final String FILTER_ATTRIBUTE_PRICE1_CODE = "price1";
    private static final String FILTER_ATTRIBUTE_PRICE2_CODE = "price2";
    private static final String FILTER_ATTRIBUTE_PRICE3_CODE = "price3";
    private static final String FILTER_ATTRIBUTE_PRICE4_CODE = "price4";
    private static final String FILTER_ATTRIBUTE_PRICE5_CODE = "price5";
    private static final String FILTER_ATTRIBUTE_BOOLEAN_CODE = "is_new";
    private static final String FILTER_ATTRIBUTE_UNKNOWN = "unknown";

    private static final String SEARCH_QUERY = "pants";

    @Before
    public void setup() {
        productPage = context.currentPage("/content/product-page");
        resource = context.resourceResolver().getResource("/content/pageA");
        request = context.request();

        when(product.getSku()).thenReturn("sku");
        when(product.getName()).thenReturn("name");
        when(money.getCurrency()).thenReturn(CurrencyEnum.USD);
        when(money.getValue()).thenReturn(12.34);
        when(priceRange.getMinimumPrice().getFinalPrice()).thenReturn(money);
        when(priceRange.getMinimumPrice().getRegularPrice()).thenReturn(money);
        when(product.getPriceRange()).thenReturn(priceRange);
        when(product.getUrlKey()).thenReturn("product");
        when(product.getUrlRewrites()).thenReturn(Arrays.asList(
            new UrlRewrite().setUrl("product"),
            new UrlRewrite().setUrl("url-path/product"),
            new UrlRewrite().setUrl("url-path/url-key/product"),
            new UrlRewrite().setUrl("anther-url-path/product"),
            new UrlRewrite().setUrl("anther-url-path/with/product"),
            new UrlRewrite().setUrl("anther-url-path/with/more/product"),
            new UrlRewrite().setUrl("anther-url-path/with/more/path/product"),
            new UrlRewrite().setUrl("anther-url-path/with/more/path/segments/product"),
            new UrlRewrite().setUrl("just-another/product"),
            new UrlRewrite().setUrl("just-another/category/product")));
        productHits.add(product);

        when(searchFilterService.retrieveCurrentlyAvailableCommerceFilters(any(), any())).thenReturn(Arrays.asList(
            createMatchFilterAttributeMetadata(FILTER_ATTRIBUTE_NAME_CODE),
            createStringEqualFilterAttributeMetadata(FILTER_ATTRIBUTE_COLOR_CODE),
            createRangeFilterAttributeMetadata(FILTER_ATTRIBUTE_PRICE1_CODE),
            createRangeFilterAttributeMetadata(FILTER_ATTRIBUTE_PRICE2_CODE),
            createRangeFilterAttributeMetadata(FILTER_ATTRIBUTE_PRICE3_CODE),
            createRangeFilterAttributeMetadata(FILTER_ATTRIBUTE_PRICE4_CODE),
            createRangeFilterAttributeMetadata(FILTER_ATTRIBUTE_PRICE5_CODE),
            createBooleanEqualFilterAttributeMetadata(FILTER_ATTRIBUTE_BOOLEAN_CODE),
            createUnknownAttributeMetadata()));

        when(products.getTotalCount()).thenReturn(0);
        when(products.getItems()).thenReturn(productHits);
        when(products.getAggregations()).thenReturn(Collections.emptyList());
        when(query.getProducts()).thenReturn(products);
        when(query.getCategoryList()).thenReturn(Collections.singletonList(categoryTree));

        GraphqlResponse<Query, Error> response = new GraphqlResponse<>();
        response.setData(query);
        when(magentoGraphqlClient.execute(any())).thenReturn(response);

        categoryTree.setUid(new ID("foobar"));

        context.registerService(SearchFilterServiceImpl.class, searchFilterService);
        context.registerAdapter(SlingHttpServletRequest.class, MagentoGraphqlClient.class, magentoGraphqlClient);

        prepareSearchOptions();
        serviceUnderTest = context.registerInjectActivateService(new SearchResultsServiceImpl());

        // TODO: CIF-2469
        // With a newer version of OSGI mock we could re-register UrlProviderImpl with a different configuration
        UrlProvider urlProvider = context.getService(UrlProvider.class);
        Whitebox.setInternalState(urlProvider, "systemDefaultProductUrlFormat",
            ProductPageWithCategoryAndUrlKey.INSTANCE);
        Whitebox.setInternalState(urlProvider, "enableContextAwareProductUrls", true);
    }

    private static FilterAttributeMetadata createMatchFilterAttributeMetadata(String attributeCode) {
        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(attributeCode);
        newFilterAttributeMetadata.setFilterInputType(FilterMatchTypeInput.class.getSimpleName());
        newFilterAttributeMetadata.setAttributeType("String");
        newFilterAttributeMetadata.setAttributeInputType("text");
        return newFilterAttributeMetadata;
    }

    private FilterAttributeMetadata createBooleanEqualFilterAttributeMetadata(String attributeCode) {
        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(attributeCode);
        newFilterAttributeMetadata.setFilterInputType(FilterEqualTypeInput.class.getSimpleName());
        newFilterAttributeMetadata.setAttributeType("Int");
        newFilterAttributeMetadata.setAttributeInputType("boolean");
        return newFilterAttributeMetadata;
    }

    private FilterAttributeMetadata createRangeFilterAttributeMetadata(String attributeCode) {
        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(attributeCode);
        newFilterAttributeMetadata.setFilterInputType(FilterRangeTypeInput.class.getSimpleName());
        newFilterAttributeMetadata.setAttributeType("Float");
        newFilterAttributeMetadata.setAttributeInputType("price");
        return newFilterAttributeMetadata;
    }

    private FilterAttributeMetadata createUnknownAttributeMetadata() {
        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(FILTER_ATTRIBUTE_UNKNOWN);
        newFilterAttributeMetadata.setFilterInputType(FILTER_ATTRIBUTE_UNKNOWN);
        return newFilterAttributeMetadata;
    }

    private void prepareSearchOptions() {
        searchOptions = new SearchOptionsImpl();
        searchOptions.setPageSize(6);
        searchOptions.setSearchQuery(SEARCH_QUERY);
        searchOptions.setCurrentPage(1);

        Map<String, String> filters = new HashMap<>();
        filters.put(FILTER_ATTRIBUTE_NAME_CODE, "sport");
        filters.put(FILTER_ATTRIBUTE_COLOR_CODE, "red");

        // To avoid having 5 tests for the price range, we introduce 5 price parameters
        filters.put(FILTER_ATTRIBUTE_PRICE1_CODE, "20_30");
        filters.put(FILTER_ATTRIBUTE_PRICE2_CODE, "40_*");
        filters.put(FILTER_ATTRIBUTE_PRICE3_CODE, "*_50");
        filters.put(FILTER_ATTRIBUTE_PRICE4_CODE, "60");
        filters.put(FILTER_ATTRIBUTE_PRICE5_CODE, "*"); // invalid

        filters.put(FILTER_ATTRIBUTE_BOOLEAN_CODE, "1");
        filters.put(FILTER_ATTRIBUTE_UNKNOWN, FILTER_ATTRIBUTE_UNKNOWN);
        searchOptions.setAttributeFilters(filters);
    }

    @Test
    public void testPerformSearch() {
        SearchResultsSet searchResultsSet = serviceUnderTest.performSearch(
            searchOptions,
            resource,
            productPage,
            request);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(magentoGraphqlClient, times(1)).execute(captor.capture());

        verify(searchFilterService, times(1)).retrieveCurrentlyAvailableCommerceFilters(any(), any());
        assertThat(searchResultsSet).isNotNull();
        assertThat(searchResultsSet.getTotalResults()).isEqualTo(0);
        assertThat(searchResultsSet.getAppliedQueryParameters()).containsKeys("search_query");
        assertThat(searchResultsSet.getProductListItems()).hasSize(1);
        assertThat(searchResultsSet.getSorter().getKeys()).isEmpty();
        assertThat(searchResultsSet.getSorter().getCurrentKey()).isNull();

        String query = captor.getValue();
        assertThat(query).contains("search:\"pants\"");
        assertThat(query).contains("name:{match:\"sport\"}");
        assertThat(query).contains("color:{eq:\"red\"}");
        assertThat(query).contains("price1:{from:\"20\",to:\"30\"}");
        assertThat(query).contains("price2:{from:\"40\"}");
        assertThat(query).contains("price3:{to:\"50\"}");
        assertThat(query).contains("price4:{from:\"60\",to:\"60\"}");
        assertThat(query).doesNotContain("price5:");
        assertThat(query).contains("is_new:{eq:\"1\"}");
    }

    @Test
    public void testSearchAggregations() {
        when(products.getAggregations()).thenReturn(Collections.singletonList(new Aggregation()
            .setLabel("Name")
            .setAttributeCode("name")
            .setOptions(Arrays.asList(
                new AggregationOption().setLabel("Sport").setValue("sport").setCount(2),
                new AggregationOption().setLabel("Lifestyle").setValue("lifestyle").setCount(3)))
            .setCount(5)));

        SearchResultsSet resultsSet = serviceUnderTest.performSearch(searchOptions, resource, productPage, request);

        List<SearchAggregation> aggregations = resultsSet.getSearchAggregations();
        assertThat(aggregations).isNotNull();
        assertThat(aggregations.size()).isEqualTo(1);
        SearchAggregation aggregation = aggregations.get(0);
        assertThat(aggregation.getDisplayLabel()).isEqualTo("Name");
        assertThat(aggregation.getIdentifier()).isEqualTo("name");
        assertThat(aggregation.getOptionCount()).isEqualTo(5);

        List<SearchAggregationOption> options = aggregation.getOptions();
        assertThat(options).isNotNull();
        assertThat(options.size()).isEqualTo(2);

        SearchAggregationOption sportOption = options.get(0);
        assertThat(sportOption.getDisplayLabel()).isEqualTo("Sport");
        assertThat(sportOption.getFilterValue()).isEqualTo("sport");
        assertThat(sportOption.getCount()).isEqualTo(2);

        SearchAggregationOption lifestyleOption = options.get(1);
        assertThat(lifestyleOption.getDisplayLabel()).isEqualTo("Lifestyle");
        assertThat(lifestyleOption.getFilterValue()).isEqualTo("lifestyle");
        assertThat(lifestyleOption.getCount()).isEqualTo(3);
    }

    @Test
    public void testCategoryUidFilterEntriesRemoved() {
        final Predicate<Map<String, String>> filterMapFilter = map -> map.containsKey("category_uid");

        List<Aggregation> aggregationsList = new ArrayList<>();
        aggregationsList.add(new Aggregation()
            .setAttributeCode("name")
            .setOptions(Arrays.asList(
                new AggregationOption().setValue("1"),
                new AggregationOption().setValue("2"))));

        when(products.getAggregations()).thenReturn(aggregationsList);
        when(searchFilterService.retrieveCurrentlyAvailableCommerceFilters(any())).thenReturn(Arrays.asList(
            createStringEqualFilterAttributeMetadata("category_uid"),
            createStringEqualFilterAttributeMetadata("name")));

        searchOptions.setAttributeFilters(Collections.emptyMap());

        // test without category_uid
        SearchResultsSet resultsSet = serviceUnderTest.performSearch(searchOptions, resource, productPage, request);
        List<SearchAggregation> aggregations = resultsSet.getSearchAggregations();
        assertThat(getFilterMapsOfAllOptions(aggregations, filterMapFilter)).isEmpty();

        // test with category_uid filter
        searchOptions.setCategoryUid("foobar");
        resultsSet = serviceUnderTest.performSearch(searchOptions, resource, productPage, request);
        aggregations = resultsSet.getSearchAggregations();
        assertThat(getFilterMapsOfAllOptions(aggregations, filterMapFilter).count()).isEqualTo(2);

        // test with category_uid filter from request
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/foobar");
        resultsSet = serviceUnderTest.performSearch(searchOptions, resource, productPage, request);
        aggregations = resultsSet.getSearchAggregations();
        assertThat(getFilterMapsOfAllOptions(aggregations, filterMapFilter)).isEmpty();
    }

    private static FilterAttributeMetadata createStringEqualFilterAttributeMetadata(String attributeCode) {
        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(attributeCode);
        newFilterAttributeMetadata.setFilterInputType(FilterEqualTypeInput.class.getSimpleName());
        newFilterAttributeMetadata.setAttributeType("String");
        newFilterAttributeMetadata.setAttributeInputType("text");
        return newFilterAttributeMetadata;
    }

    private static Stream<Map<String, String>> getFilterMapsOfAllOptions(List<SearchAggregation> aggregations,
        Predicate<Map<String, String>> filter) {
        return aggregations.stream()
            .map(SearchAggregation::getOptions).flatMap(Collection::stream).map(SearchAggregationOption::getAddFilterMap)
            .filter(filter);
    }

    @Test
    public void testSearchWithSortOrderParam() {
        searchOptions.addSorterKey("name", "Name", Sorter.Order.DESC);
        searchOptions.getAttributeFilters().put(Sorter.PARAMETER_SORT_KEY, "name");
        searchOptions.getAttributeFilters().put(Sorter.PARAMETER_SORT_ORDER, Sorter.Order.ASC.name());

        SearchResultsSet searchResultsSet = serviceUnderTest.performSearch(
            searchOptions,
            resource,
            productPage,
            request);

        assertThat(searchResultsSet.getSorter().getKeys()).hasSize(1);
        assertThat(searchResultsSet.getSorter().getCurrentKey().getName()).isEqualTo("name");
        assertThat(searchResultsSet.getSorter().getCurrentKey().getOrder()).isEqualTo(Sorter.Order.ASC);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(magentoGraphqlClient, times(1)).execute(captor.capture());
        String query = captor.getValue();
        assertThat(query).contains("sort:{name:ASC}");
    }

    @Test
    public void testSearchWithInvalidSortOrderParam() {
        searchOptions.addSorterKey("position", "Position", null);
        searchOptions.getAttributeFilters().put(Sorter.PARAMETER_SORT_KEY, "position");
        searchOptions.getAttributeFilters().put(Sorter.PARAMETER_SORT_ORDER, "invalid");

        SearchResultsSet searchResultsSet = serviceUnderTest.performSearch(
            searchOptions,
            resource,
            productPage,
            request);

        assertThat(searchResultsSet.getSorter().getKeys()).hasSize(1);
        assertThat(searchResultsSet.getSorter().getCurrentKey().getName()).isEqualTo("position");
        assertThat(searchResultsSet.getSorter().getCurrentKey().getOrder()).isEqualTo(Sorter.Order.ASC);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(magentoGraphqlClient, times(1)).execute(captor.capture());
        String query = captor.getValue();
        assertThat(query).contains("sort:{position:ASC}");
    }

    @Test
    public void testSearchWithUnknownSortFieldParam() {
        searchOptions.addSorterKey("brand", "Brand", Sorter.Order.DESC);
        searchOptions.getAttributeFilters().put(Sorter.PARAMETER_SORT_KEY, "brand");
        searchOptions.getAttributeFilters().put(Sorter.PARAMETER_SORT_ORDER, Sorter.Order.ASC.name());

        SearchResultsSet searchResultsSet = serviceUnderTest.performSearch(
            searchOptions,
            resource,
            productPage,
            request);

        assertThat(searchResultsSet.getSorter().getKeys()).hasSize(1);
        assertThat(searchResultsSet.getSorter().getCurrentKey().getName()).isEqualTo("brand");
        assertThat(searchResultsSet.getSorter().getCurrentKey().getOrder()).isEqualTo(Sorter.Order.ASC);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(magentoGraphqlClient, times(1)).execute(captor.capture());
        String query = captor.getValue();
        assertThat(query).contains("sort:{brand:ASC}");
    }

    @Test
    public void testSearchWithDefaultSortField() {
        searchOptions.addSorterKey("name", "Name", Sorter.Order.ASC);
        searchOptions.setDefaultSorter("name", Sorter.Order.ASC);

        SearchResultsSet searchResultsSet = serviceUnderTest.performSearch(
            searchOptions,
            resource,
            productPage,
            request);

        assertThat(searchResultsSet.getSorter().getKeys()).hasSize(1);
        assertThat(searchResultsSet.getSorter().getCurrentKey().getName()).isEqualTo("name");
        assertThat(searchResultsSet.getSorter().getCurrentKey().getOrder()).isEqualTo(Sorter.Order.ASC);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(magentoGraphqlClient, times(1)).execute(captor.capture());
        String query = captor.getValue();
        assertThat(query).contains("sort:{name:ASC}");
    }

    @Test
    public void testNullMagentoClient() {
        GraphqlResponse<Query, Error> response = new GraphqlResponse<Query, Error>();
        response.setData(query);
        Error error = new Error();
        response.setErrors(Collections.singletonList(error));

        Mockito.reset(magentoGraphqlClient);
        when(magentoGraphqlClient.execute(any())).thenReturn(response);

        SearchResultsSet searchResultsSet = serviceUnderTest.performSearch(
            new SearchOptionsImpl(),
            resource,
            productPage,
            request);

        assertThat(searchResultsSet.getTotalResults()).isEqualTo(0);
        assertThat(searchResultsSet.getAppliedQueryParameters()).isEmpty();
        assertThat(searchResultsSet.getProductListItems()).isEmpty();
    }

    @Test
    public void testExtendProductQuery() {
        SearchOptionsImpl searchOptions = new SearchOptionsImpl();
        searchOptions.setPageSize(6);
        searchOptions.setSearchQuery(SEARCH_QUERY);
        searchOptions.setCurrentPage(1);

        serviceUnderTest.performSearch(
            searchOptions,
            resource,
            productPage,
            request,
            p -> p.createdAt()
                .addCustomSimpleField("is_returnable"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(magentoGraphqlClient, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).contains("created_at,is_returnable_custom_:is_returnable");
    }

    @Test
    public void testProductItemsReturnedInContextOfGivenCategoryRetriever() {
        CategoryTree local = new CategoryTree();
        local.setUid(new ID("foobar"));
        local.setUrlKey("url-key");
        local.setUrlPath("url-path/url-key");
        AbstractCategoryRetriever retriever = new AbstractCategoryRetriever(magentoGraphqlClient) {
            @Override
            public CategoryInterface fetchCategory() {
                return local;
            }

            @Override
            protected CategoryTreeQueryDefinition generateCategoryQuery() {
                return q -> {};
            }
        };

        Pair<CategoryInterface, SearchResultsSet> result = serviceUnderTest.performSearch(searchOptions, resource, productPage, request,
            null, retriever);

        assertSame(local, result.getLeft());

        SearchResultsSet resultsSet = result.getRight();
        List<ProductListItem> items = resultsSet.getProductListItems();
        assertEquals(1, items.size());
        ProductListItem item = items.get(0);
        assertEquals("/content/product-page.html/url-key/product.html", item.getURL());
    }

    @Test
    public void testProductItemsReturnedInContextOfUidFilterParameter() {
        categoryTree.setUrlKey("category");
        categoryTree.setUrlPath("just-another/category");
        searchOptions.setCategoryUid("foobar");

        Pair<CategoryInterface, SearchResultsSet> result = serviceUnderTest.performSearch(searchOptions, resource, productPage, request,
            null, null);

        SearchResultsSet resultsSet = result.getRight();
        List<ProductListItem> items = resultsSet.getProductListItems();
        assertEquals(1, items.size());
        ProductListItem item = items.get(0);
        // category is the deepest url_key in the context of the given CategoryTree (descendant of another-url-path/with)
        String url = item.getURL();
        assertEquals("/content/product-page.html/category/product.html", url);
    }

    @Test
    public void testProductItemsReturnedWithCanonicalUrl() {
        Pair<CategoryInterface, SearchResultsSet> result = serviceUnderTest.performSearch(searchOptions, resource, productPage, request,
            null, null);

        SearchResultsSet resultsSet = result.getRight();
        List<ProductListItem> items = resultsSet.getProductListItems();
        assertEquals(1, items.size());
        ProductListItem item = items.get(0);
        // segments is the deepest url_key without any context given
        assertEquals("/content/product-page.html/segments/product.html", item.getURL());
    }
}
