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
package com.adobe.cq.commerce.core.components.internal.models.v1.searchresults;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchResultsImplTest {

    private static final ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);

    private static final String PAGE = "/content/pageA";
    private static final String SEARCHRESULTS = "/content/pageA/jcr:content/root/responsivegrid/searchresults";

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            context.registerInjectActivateService(new SearchFilterServiceImpl());
            context.registerInjectActivateService(new SearchResultsServiceImpl());
            context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                (Function<Resource, ComponentsConfiguration>) input -> MOCK_CONFIGURATION_OBJECT);

            Utils.addDataLayerConfig(mockConfigBuilder, true);
            Utils.addStorefrontContextConfig(mockConfigBuilder, true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
        })
        .build();

    private SearchResultsImpl searchResultsModel;
    private Resource pageResource;
    private GraphqlClient graphqlClient;

    @Mock
    CloseableHttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(SEARCHRESULTS);
        Resource searchResultsResource = context.resourceResolver().getResource(SEARCHRESULTS);

        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        graphqlClient = Mockito.spy(new GraphqlClientImpl());
        context.registerInjectActivateService(graphqlClient, "httpMethod", "POST");

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-result.json", httpClient, HttpStatus.SC_OK, "{products");

        // This is needed by the SearchResultsService used by the productlist component
        pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(searchResultsResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, searchResultsResource.getValueMap());

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);
    }

    @Test
    public void testProducts() {
        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();
        Assert.assertEquals("Return the correct number of products", 4, products.size());

        SearchResultsSet searchResultsSet = searchResultsModel.getSearchResultsSet();
        Assert.assertEquals(products, searchResultsSet.getProductListItems());
        Assert.assertEquals(0, searchResultsSet.getAppliedAggregations().size());
        Assert.assertEquals(8, searchResultsSet.getSearchAggregations().size());
        Assert.assertEquals(8, searchResultsSet.getAvailableAggregations().size());
        Assert.assertEquals(1, searchResultsSet.getAppliedQueryParameters().size()); // only search_query
        Assert.assertEquals(4, searchResultsSet.getTotalResults().intValue());

        List<SearchAggregation> searchAggregations = searchResultsSet.getSearchAggregations();
        Assert.assertEquals(8, searchAggregations.size());

        // We want to make sure all price ranges are properly processed
        SearchAggregation priceAggregation = searchAggregations.stream().filter(a -> a.getIdentifier().equals("price")).findFirst().get();
        Assert.assertEquals(3, priceAggregation.getOptions().size());
        Assert.assertEquals(3, priceAggregation.getOptionCount());
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("30-40")));
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("40-*")));
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("14")));
    }

    @Test
    public void testMissingSearchTerm() {
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();

        // Check that we get an empty list of products and the GraphQL client is never called
        Assert.assertTrue("Products list is empty", products.isEmpty());
        Mockito.verify(graphqlClient, never()).execute(any(), any(), any());
        Mockito.verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testNoMagentoGraphqlClient() {
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Mockito.when(pageResource.adaptTo(GraphqlClient.class)).thenReturn(null);
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();
        Assert.assertTrue("Products list is empty", products.isEmpty());
    }

    @Test
    public void testCreateFilterMap() {
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Map<String, String[]> queryParameters = new HashMap<>();
        queryParameters.put("search_query", new String[] { "ok" });
        Map<String, String> filterMap = searchResultsModel.createFilterMap(queryParameters);

        Assert.assertEquals("filters query string parameter out correctly", 0, filterMap.size());
    }

    @Test
    public void testFilterQueriesReturnNull() throws IOException {
        // We want to make sure that components will not fail if the __type and/or customAttributeMetadata fields are null
        // For example, 3rd-party integrations might not support these queries

        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Mockito.reset(httpClient);
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-result.json", httpClient, HttpStatus.SC_OK, "{products");

        Collection<ProductListItem> productList = searchResultsModel.getProducts();
        Assert.assertEquals("Return the correct number of products", 4, productList.size());

        SearchResultsSet searchResultsSet = searchResultsModel.getSearchResultsSet();
        Assert.assertEquals(0, searchResultsSet.getAvailableAggregations().size());
    }

    @Test
    public void testSorting() {
        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);
        SearchResultsSet resultSet = searchResultsModel.getSearchResultsSet();
        Assert.assertNotNull(resultSet);
        Assert.assertTrue(resultSet.hasSorting());
        Sorter sorter = resultSet.getSorter();
        Assert.assertNotNull(sorter);

        SorterKey currentKey = sorter.getCurrentKey();
        Assert.assertNotNull(currentKey);
        Assert.assertEquals("relevance", currentKey.getName());
        Assert.assertEquals("Relevance", currentKey.getLabel());
        Assert.assertEquals(Sorter.Order.DESC, currentKey.getOrder());
        Assert.assertTrue(currentKey.isSelected());

        Map<String, String> currentOrderParameters = currentKey.getCurrentOrderParameters();
        Assert.assertNotNull(currentOrderParameters);
        Assert.assertEquals(resultSet.getAppliedQueryParameters().size() + 2, currentOrderParameters.size());
        resultSet.getAppliedQueryParameters().forEach((key, value) -> Assert.assertEquals(value, currentOrderParameters.get(key)));
        Assert.assertEquals("relevance", currentOrderParameters.get(Sorter.PARAMETER_SORT_KEY));
        Assert.assertEquals("desc", currentOrderParameters.get(Sorter.PARAMETER_SORT_ORDER));

        Map<String, String> oppositeOrderParameters = currentKey.getOppositeOrderParameters();
        Assert.assertNotNull(oppositeOrderParameters);
        Assert.assertEquals(resultSet.getAppliedQueryParameters().size() + 2, oppositeOrderParameters.size());
        resultSet.getAppliedQueryParameters().forEach((key, value) -> Assert.assertEquals(value, oppositeOrderParameters.get(key)));
        Assert.assertEquals("relevance", oppositeOrderParameters.get(Sorter.PARAMETER_SORT_KEY));
        Assert.assertEquals("asc", oppositeOrderParameters.get(Sorter.PARAMETER_SORT_ORDER));

        List<SorterKey> keys = sorter.getKeys();
        Assert.assertNotNull(keys);
        Assert.assertEquals(3, keys.size());
        SorterKey defaultKey = keys.get(2);
        Assert.assertEquals(currentKey.getName(), defaultKey.getName());

        SorterKey otherKey = keys.get(0);
        Assert.assertEquals("price", otherKey.getName());
        Assert.assertEquals("Price", otherKey.getLabel());
        Assert.assertEquals(Sorter.Order.ASC, otherKey.getOrder());

        otherKey = keys.get(1);
        Assert.assertEquals("name", otherKey.getName());
        Assert.assertEquals("Product Name", otherKey.getLabel());
        Assert.assertEquals(Sorter.Order.ASC, otherKey.getOrder());
    }

    @Test
    public void testJsonRender() throws IOException {
        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        ObjectMapper mapper = new ObjectMapper();

        String expected = Utils.getResource("results/result-datalayer-searchresults-component.json");
        String jsonResult = searchResultsModel.getData().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));

        String itemsJsonExpected = Utils.getResource("results/result-datalayer-searchresults-component-items.json");
        String itemsJsonResult = searchResultsModel.getProducts()
            .stream()
            .map(i -> i.getData().getJson())
            .collect(Collectors.joining(",", "[", "]"));
        Assert.assertEquals(mapper.readTree(itemsJsonExpected), mapper.readTree(itemsJsonResult));
    }

    @Test
    public void testStorefrontContextRender() throws IOException {
        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        ObjectMapper mapper = new ObjectMapper();

        String expected = Utils.getResource("storefront-context/result-storefront-context-search-component.json");
        String jsonResult = searchResultsModel.getSearchStorefrontContext().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));

        expected = Utils.getResource("storefront-context/result-storefront-context-search-results-component.json");
        jsonResult = searchResultsModel.getSearchResultsStorefrontContext().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }

    @Test
    public void testStorefrontContextRenderWithAttrsAndSorting() throws IOException {
        context.request().setParameterMap(
            ImmutableMap.of("search_query", "glove", "category_id", "21", "sort_key", "price", "sort_order", "asc"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        ObjectMapper mapper = new ObjectMapper();

        String expected = Utils.getResource("storefront-context/result-storefront-context-search-sorting-component.json");
        String jsonResult = searchResultsModel.getSearchStorefrontContext().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));

        expected = Utils.getResource("storefront-context/result-storefront-context-search-results-sorting-component.json");
        jsonResult = searchResultsModel.getSearchResultsStorefrontContext().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }

    @Test
    public void testStorefrontContextRenderDisabled() throws IOException {
        Utils.addStorefrontContextConfig(mockConfigBuilder, false);
        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Assert.assertNull(searchResultsModel.getSearchResultsStorefrontContext().getJson());
    }

    @Test
    public void testExtendProductQuery() {
        ArgumentCaptor<GraphqlRequest> argumentCaptor = ArgumentCaptor.forClass(GraphqlRequest.class);

        // Customize query
        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);
        Assert.assertNotNull(searchResultsModel);
        searchResultsModel.extendProductQueryWith(ProductInterfaceQuery::metaTitle);

        // Execute query
        SearchResultsSet resultSet = searchResultsModel.getSearchResultsSet();

        // Verify that query contains customized value
        verify(graphqlClient, times(3)).execute(argumentCaptor.capture(), any(), any(), any());
        String query = argumentCaptor.getAllValues().get(2).getQuery();
        Assert.assertTrue(query.contains("meta_title"));
    }
}
