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

package com.adobe.cq.commerce.core.components.internal.models.v1.productlist;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.client.MockExternalizer;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductListImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");
    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
            "my-store"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);

                context.registerInjectActivateService(new SearchFilterServiceImpl());
                context.registerInjectActivateService(new SearchResultsServiceImpl());

                context.registerService(Externalizer.class, new MockExternalizer());

                ConfigurationBuilder mockConfigBuilder = Utils.getDataLayerConfig(true);
                context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PAGE = "/content/pageA";
    private static final String PRODUCTLIST = "/content/pageA/jcr:content/root/responsivegrid/productlist";

    private Resource productListResource;
    private Resource pageResource;
    private ProductListImpl productListModel;
    private CategoryTree category;
    private Products products;
    private GraphqlClient graphqlClient;

    @Mock
    HttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(PRODUCTLIST);
        productListResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTLIST));

        category = Utils.getQueryFromResource("graphql/magento-graphql-search-result-with-category.json").getCategory();
        products = Utils.getQueryFromResource("graphql/magento-graphql-search-result-with-category.json").getProducts();

        graphqlClient = Mockito.spy(new GraphqlClientImpl());
        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "httpMethod", HttpMethod.POST);

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-result-with-category.json", httpClient, HttpStatus.SC_OK, "{products");

        when(productListResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        // This is needed by the SearchResultsService used by the productlist component
        pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(productListResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        Function<Resource, ComponentsConfiguration> adapter = r -> r.getPath().equals(PAGE) ? MOCK_CONFIGURATION_OBJECT
            : ComponentsConfiguration.EMPTY;
        context.registerAdapter(Resource.class, ComponentsConfiguration.class, adapter);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("6");
        context.request().setServletPath(PAGE + ".6.html"); // used by context.request().getRequestURI();

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productListResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productListResource.getValueMap());

        XSSAPI xssApi = mock(XSSAPI.class);
        when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
        slingBindings.put("xssApi", xssApi);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);

        // context.request().adaptTo(ProductListImpl.class); is moved to each test because it uses an internal cache
        // and we want to override the "slug" in testEditModePlaceholderData()
    }

    @Test
    public void testTitleAndMetadata() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertEquals(category.getName(), productListModel.getTitle());
        Assert.assertEquals(category.getMetaDescription(), productListModel.getMetaDescription());
        Assert.assertEquals(category.getMetaKeywords(), productListModel.getMetaKeywords());
        Assert.assertEquals(category.getMetaTitle(), productListModel.getMetaTitle());
        Assert.assertEquals("https://author" + PAGE + ".6.html", productListModel.getCanonicalUrl());
    }

    @Test
    public void getImage() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertEquals(category.getImage(), productListModel.getImage());
    }

    @Test
    public void getImageWhenMissingInResponse() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        ProductListImpl spyProductListModel = Mockito.spy(productListModel);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getImage()).thenReturn("");
        Mockito.doReturn(category).when(spyProductListModel).getCategory();

        String image = spyProductListModel.getImage();
        Assert.assertEquals("", image);
    }

    @Test
    public void getProducts() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Collection<ProductListItem> products = productListModel.getProducts();
        Assert.assertNotNull(products);

        // We introduce one "faulty" product data in the response, it should be skipped
        Assert.assertEquals(4, products.size());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        List<ProductListItem> results = products.stream().collect(Collectors.toList());
        for (int i = 0; i < results.size(); i++) {
            // get raw GraphQL object
            ProductInterface productInterface = this.products.getItems().get(i);
            // get mapped product list item
            ProductListItem item = results.get(i);

            Assert.assertEquals(productInterface.getName(), item.getTitle());
            Assert.assertEquals(productInterface.getSku(), item.getSKU());
            Assert.assertEquals(productInterface.getUrlKey(), item.getSlug());
            Assert.assertEquals(String.format(PRODUCT_PAGE + ".%s.html", productInterface.getUrlKey()), item.getURL());

            // Make sure deprecated methods still work
            Assert.assertEquals(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getValue(), item.getPrice(), 0);
            Assert.assertEquals(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getCurrency().toString(), item
                .getCurrency());
            priceFormatter.setCurrency(Currency.getInstance(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getCurrency()
                .toString()));
            Assert.assertEquals(priceFormatter.format(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getValue()), item
                .getFormattedPrice());

            ProductImage smallImage = productInterface.getSmallImage();
            if (smallImage == null) {
                // if small image is missing for a product in GraphQL response then image URL is null for the related item
                Assert.assertNull(item.getImageURL());
            } else {
                Assert.assertEquals(smallImage.getUrl(), item.getImageURL());
            }

            Assert.assertEquals(productInterface instanceof GroupedProduct, item.getPriceRange().isStartPrice());
        }

        SearchResultsSet searchResultsSet = productListModel.getSearchResultsSet();
        List<SearchAggregation> searchAggregations = searchResultsSet.getSearchAggregations();
        Assert.assertEquals(7, searchAggregations.size()); // category_id is excluded

        // We want to make sure all price ranges are properly processed
        SearchAggregation priceAggregation = searchAggregations.stream().filter(a -> a.getIdentifier().equals("price")).findFirst().get();
        Assert.assertEquals(3, priceAggregation.getOptions().size());
        Assert.assertEquals(3, priceAggregation.getOptionCount());
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("30-40")));
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("40-*")));
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("14")));
    }

    @Test
    public void testFilterQueriesReturnNull() throws IOException {
        // We want to make sure that components will not fail if the __type and/or customAttributeMetadata fields are null
        // For example, 3rd-party integrations might not support this immediately

        Mockito.reset(httpClient);
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-result-with-category.json", httpClient, HttpStatus.SC_OK, "{products");

        productListModel = context.request().adaptTo(ProductListImpl.class);
        Collection<ProductListItem> productList = productListModel.getProducts();
        Assert.assertEquals("Return the correct number of products", 4, productList.size());

        SearchResultsSet searchResultsSet = productListModel.getSearchResultsSet();
        Assert.assertEquals(0, searchResultsSet.getAvailableAggregations().size());
    }

    @Test
    public void testEditModePlaceholderData() throws IOException {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString(null);
        productListModel = context.request().adaptTo(ProductListImpl.class);

        String json = Utils.getResource(ProductListImpl.PLACEHOLDER_DATA);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        category = rootQuery.getCategory();

        Assert.assertEquals(category.getName(), productListModel.getTitle());
        Assert.assertEquals(category.getProducts().getItems().size(), productListModel.getProducts().size());
    }

    @Test
    public void testMissingSelectorOnPublish() throws IOException {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(true);
        slingBindings.put("wcmmode", wcmMode);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString(null);
        context.request().setServletPath(PAGE + ".html"); // used by context.request().getRequestURI();
        productListModel = context.request().adaptTo(ProductListImpl.class);

        // Check that we get an empty list of products and the GraphQL client is never called
        Assert.assertTrue(productListModel.getProducts().isEmpty());
        Mockito.verify(graphqlClient, never()).execute(any(), any(), any());
        Mockito.verify(graphqlClient, never()).execute(any(), any(), any(), any());

        // Test canonical url on publish
        Assert.assertEquals("https://publish" + PAGE + ".html", productListModel.getCanonicalUrl());
    }

    @Test
    public void testProductListNoGraphqlClient() throws IOException {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(context.resourceResolver().getResource("/content/pageB"));
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, context.pageManager().getPage("/content/pageB"));

        productListModel = context.request().adaptTo(ProductListImpl.class);

        Assert.assertTrue(productListModel.getTitle().isEmpty());
        Assert.assertTrue(productListModel.getImage().isEmpty());
        Assert.assertTrue(productListModel.getProducts().isEmpty());
        Assert.assertTrue(productListModel.getMetaTitle().isEmpty());
        Assert.assertNull(productListModel.getMetaDescription());
        Assert.assertNull(productListModel.getMetaKeywords());
    }

    @Test
    public void testExtendProductQuery() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        productListModel.getCategoryRetriever().extendProductQueryWith(p -> p.createdAt().stockStatus());
        productListModel.getProducts();

        ArgumentCaptor<GraphqlRequest> captor = ArgumentCaptor.forClass(GraphqlRequest.class);
        verify(graphqlClient, atLeastOnce()).execute(captor.capture(), any(), any(), any());

        // Check the "products" query
        List<GraphqlRequest> requests = captor.getAllValues();
        String productsQuery = null;
        for (GraphqlRequest request : requests) {
            if (request.getQuery().startsWith("{products")) {
                productsQuery = request.getQuery();
                break;
            }
        }
        Assert.assertTrue(productsQuery.contains("created_at,stock_status"));
    }

    @Test
    public void testSorting() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        SearchResultsSet resultSet = productListModel.getSearchResultsSet();
        Assert.assertNotNull(resultSet);
        Assert.assertTrue(resultSet.hasSorting());
        Sorter sorter = resultSet.getSorter();
        Assert.assertNotNull(sorter);

        SorterKey currentKey = sorter.getCurrentKey();
        Assert.assertNotNull(currentKey);
        Assert.assertEquals("price", currentKey.getName());
        Assert.assertEquals("Price", currentKey.getLabel());
        Assert.assertEquals(Sorter.Order.ASC, currentKey.getOrder());
        Assert.assertTrue(currentKey.isSelected());

        Map<String, String> currentOrderParameters = currentKey.getCurrentOrderParameters();
        Assert.assertNotNull(currentOrderParameters);
        Assert.assertEquals(resultSet.getAppliedQueryParameters().size() + 2, currentOrderParameters.size());
        resultSet.getAppliedQueryParameters().forEach((key, value) -> Assert.assertEquals(value, currentOrderParameters.get(key)));
        Assert.assertEquals("price", currentOrderParameters.get(Sorter.PARAMETER_SORT_KEY));
        Assert.assertEquals("asc", currentOrderParameters.get(Sorter.PARAMETER_SORT_ORDER));

        Map<String, String> oppositeOrderParameters = currentKey.getOppositeOrderParameters();
        Assert.assertNotNull(oppositeOrderParameters);
        Assert.assertEquals(resultSet.getAppliedQueryParameters().size() + 2, oppositeOrderParameters.size());
        resultSet.getAppliedQueryParameters().forEach((key, value) -> Assert.assertEquals(value, oppositeOrderParameters.get(key)));
        Assert.assertEquals("price", oppositeOrderParameters.get(Sorter.PARAMETER_SORT_KEY));
        Assert.assertEquals("desc", oppositeOrderParameters.get(Sorter.PARAMETER_SORT_ORDER));

        List<SorterKey> keys = sorter.getKeys();
        Assert.assertNotNull(keys);
        Assert.assertEquals(2, keys.size());
        SorterKey defaultKey = keys.get(0);
        Assert.assertEquals(currentKey.getName(), defaultKey.getName());

        SorterKey otherKey = keys.get(1);
        Assert.assertEquals("name", otherKey.getName());
        Assert.assertEquals("Product Name", otherKey.getLabel());
        Assert.assertEquals(Sorter.Order.ASC, otherKey.getOrder());
    }

    @Test
    public void testClientLoadingIsDisabledOnLaunchPage() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertTrue(productListModel.loadClientPrice());
        Page launch = context.pageManager().getPage("/content/launches/2020/09/14/mylaunch" + PAGE);
        Whitebox.setInternalState(productListModel, "currentPage", launch);
        Assert.assertFalse(productListModel.loadClientPrice());
    }

    @Test
    public void testJsonRender() throws IOException {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        ObjectMapper mapper = new ObjectMapper();

        String expected = Utils.getResource("results/result-datalayer-productlist-component.json");
        String jsonResult = productListModel.getData().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));

        String itemsJsonExpected = Utils.getResource("results/result-datalayer-productlistitem-component.json");
        String itemsJsonResult = productListModel.getProducts()
            .stream()
            .map(i -> i.getData().getJson())
            .collect(Collectors.joining(",", "[", "]"));
        Assert.assertEquals(mapper.readTree(itemsJsonExpected), mapper.readTree(itemsJsonResult));
    }
}
