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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.search.internal.models.FilterAttributeMetadataImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterRangeTypeInput;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchResultsServiceImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Mock
    SearchFilterService searchFilterService;
    @Mock
    SlingHttpServletRequest request;
    @Mock
    MagentoGraphqlClient magentoGraphqlClient;
    @Mock
    Page productPage;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Query query;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Products products;

    Resource resource;
    SearchResultsServiceImpl serviceUnderTest;
    SearchOptionsImpl searchOptions;

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

        resource = context.resourceResolver().getResource("/content/pageA");

        when(searchFilterService.retrieveCurrentlyAvailableCommerceFilters(any())).thenReturn(Arrays.asList(
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
        when(products.getItems()).thenReturn(new ArrayList<>());
        when(products.getAggregations()).thenReturn(new ArrayList<>());
        when(query.getProducts()).thenReturn(products);

        GraphqlResponse<Query, Error> response = new GraphqlResponse<Query, Error>();
        response.setData(query);

        when(magentoGraphqlClient.execute(any())).thenReturn(response);

        context.registerService(SearchFilterService.class, searchFilterService);

        UrlProviderImpl urlProvider = new UrlProviderImpl();
        urlProvider.activate(new MockUrlProviderConfiguration());
        context.registerService(UrlProvider.class, urlProvider);

        prepareSearchOptions();
        serviceUnderTest = context.registerInjectActivateService(new SearchResultsServiceImpl(magentoGraphqlClient));
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

        verify(searchFilterService, times(1)).retrieveCurrentlyAvailableCommerceFilters(any());
        assertThat(searchResultsSet).isNotNull();
        assertThat(searchResultsSet.getTotalResults()).isEqualTo(0);
        assertThat(searchResultsSet.getAppliedQueryParameters()).containsKeys("search_query");
        assertThat(searchResultsSet.getProductListItems()).isEmpty();
        assertThat(searchResultsSet.getSorter().getKeys()).isNull();
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
    public void testSearchWithInvalidSortKey() {

        searchOptions.addSorterKey("invalid", "Invalid", null);
        searchOptions.getAttributeFilters().put(Sorter.PARAMETER_SORT_KEY, "invalid");
        searchOptions.getAttributeFilters().put(Sorter.PARAMETER_SORT_ORDER, Sorter.Order.ASC.name());

        SearchResultsSet searchResultsSet = serviceUnderTest.performSearch(
            searchOptions,
            resource,
            productPage,
            request);

        assertThat(searchResultsSet.getSorter().getKeys()).hasSize(1);
        assertThat(searchResultsSet.getSorter().getCurrentKey().getName()).isEqualTo("invalid");
        assertThat(searchResultsSet.getSorter().getCurrentKey().getOrder()).isEqualTo(Sorter.Order.ASC);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(magentoGraphqlClient, times(1)).execute(captor.capture());
        String query = captor.getValue();
        assertThat(query).doesNotContain("sort:{invalid:ASC}");
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

    private FilterAttributeMetadata createMatchFilterAttributeMetadata(String attributeCode) {
        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(attributeCode);
        newFilterAttributeMetadata.setFilterInputType(FilterMatchTypeInput.class.getSimpleName());
        newFilterAttributeMetadata.setAttributeType("String");
        newFilterAttributeMetadata.setAttributeInputType("text");
        return newFilterAttributeMetadata;
    }

    private FilterAttributeMetadata createStringEqualFilterAttributeMetadata(String attributeCode) {
        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(attributeCode);
        newFilterAttributeMetadata.setFilterInputType(FilterEqualTypeInput.class.getSimpleName());
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
}
