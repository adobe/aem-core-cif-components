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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.search.internal.models.FilterAttributeMetadataImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchResultsServiceImplTest {

    @Rule
    public final AemContext context = new AemContext();

    @Mock
    SearchFilterService searchFilterService;
    @Mock
    SlingHttpServletRequest request;
    @Mock
    MagentoGraphqlClient magentoGraphqlClient;
    @Mock
    Page productPage;
    @Mock
    Resource resource;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Query query;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Products products;

    SearchResultsServiceImpl serviceUnderTest;

    private static final String FILTER_ATTRIBUTE_NAME_CODE = "name";
    private static final String FILTER_ATTRIBUTE_BOOLEAN_CODE = "super_cool_product";

    private static final String SEARCH_QUERY = "pants";

    @Before
    public void setup() {

        when(searchFilterService.retrieveCurrentlyAvailableCommerceFilters(any())).thenReturn(Arrays.asList(
            createMatchFilterAttributeMetadata(FILTER_ATTRIBUTE_NAME_CODE),
            createEqualFilterAttributeMetadata(FILTER_ATTRIBUTE_BOOLEAN_CODE)));

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

        serviceUnderTest = context.registerInjectActivateService(new SearchResultsServiceImpl(magentoGraphqlClient));
    }

    @Test
    public void testPerformSearch() {

        SearchOptionsImpl searchOptions = new SearchOptionsImpl();
        searchOptions.setPageSize(6);
        searchOptions.setSearchQuery(SEARCH_QUERY);
        searchOptions.setCurrentPage(1);

        final SearchResultsSet searchResultsSet = serviceUnderTest.performSearch(
            searchOptions,
            resource,
            productPage,
            request);

        verify(searchFilterService, times(1)).retrieveCurrentlyAvailableCommerceFilters(any());
        assertThat(searchResultsSet).isNotNull();
        assertThat(searchResultsSet.getTotalResults()).isEqualTo(0);
        assertThat(searchResultsSet.getAppliedQueryParameters()).containsKeys("search_query");
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

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(magentoGraphqlClient, times(1)).execute(captor.capture());

        String expectedQuery = "{products(search:\"pants\",currentPage:1,pageSize:6,filter:{}){total_count,items{__typename,id,sku,name,"
            + "small_image{url},url_key,price_range{minimum_price{regular_price{value,currency},final_price{value,currency},"
            + "discount{amount_off,percent_off}}},... on ConfigurableProduct{price_range{maximum_price{regular_price{value,currency},"
            + "final_price{value,currency},discount{amount_off,percent_off}}}},created_at,is_returnable_custom_:is_returnable},"
            + "aggregations{options{count,label,value},attribute_code,count,label}}}";
        assertThat(expectedQuery).isEqualTo(captor.getValue());
    }

    private FilterAttributeMetadata createMatchFilterAttributeMetadata(final String attributeCode) {

        final String FILTER_ATTRIBUTE_NAME_INPUT_TYPE = "text";
        final String FILTER_ATTRIBUTE_NAME_ATTRIBUTE_TYPE = "String";
        final String FILTER_ATTRIBUTE_NAME_FILTER_TYPE = "FilterMatchTypeInput";

        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(attributeCode);
        newFilterAttributeMetadata.setFilterInputType(FILTER_ATTRIBUTE_NAME_FILTER_TYPE);
        newFilterAttributeMetadata.setAttributeType(FILTER_ATTRIBUTE_NAME_ATTRIBUTE_TYPE);
        newFilterAttributeMetadata.setAttributeInputType(FILTER_ATTRIBUTE_NAME_INPUT_TYPE);
        return newFilterAttributeMetadata;
    }

    private FilterAttributeMetadata createEqualFilterAttributeMetadata(final String attributeCode) {

        final String FILTER_ATTRIBUTE_BOOLEAN_FILTER_TYPE = "FilterEqualTypeInput";
        final String FILTER_ATTRIBUTE_BOOLEAN_INPUT_TYPE = "boolean";
        final String FILTER_ATTRIBUTE_BOOLEAN_ATTRIBUTE_TYPE = "Int";

        FilterAttributeMetadataImpl newFilterAttributeMetadata = new FilterAttributeMetadataImpl();
        newFilterAttributeMetadata.setAttributeCode(attributeCode);
        newFilterAttributeMetadata.setFilterInputType(FILTER_ATTRIBUTE_BOOLEAN_FILTER_TYPE);
        newFilterAttributeMetadata.setAttributeType(FILTER_ATTRIBUTE_BOOLEAN_ATTRIBUTE_TYPE);
        newFilterAttributeMetadata.setAttributeInputType(FILTER_ATTRIBUTE_BOOLEAN_INPUT_TYPE);
        return newFilterAttributeMetadata;
    }

}
