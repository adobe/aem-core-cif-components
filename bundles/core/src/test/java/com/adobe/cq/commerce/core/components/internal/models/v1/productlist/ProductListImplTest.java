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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
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

import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.core.search.internal.services.FilterAttributeMetadataCacheImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
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
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductListImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);

                context.registerInjectActivateService(new FilterAttributeMetadataCacheImpl());
                context.registerInjectActivateService(new SearchFilterServiceImpl());
                context.registerInjectActivateService(new SearchResultsServiceImpl());
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PAGE = "/content/pageA";
    private static final String PRODUCTLIST = "/content/pageA/jcr:content/root/responsivegrid/productlist";

    private Resource productListResource;
    private ProductListImpl productListModel;
    private CategoryTree category;
    private Products products;

    @Mock
    HttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(PAGE);
        context.currentResource(PRODUCTLIST);
        productListResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTLIST));

        category = Utils.getQueryFromResource("graphql/magento-graphql-category-result.json").getCategory();
        products = Utils.getQueryFromResource("graphql/magento-graphql-search-result.json").getProducts();

        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "httpMethod", HttpMethod.POST);

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-category-result.json", httpClient, HttpStatus.SC_OK, "{category");
        Utils.setupHttpResponse("graphql/magento-graphql-search-result.json", httpClient, HttpStatus.SC_OK, "{products");
        Mockito.when(productListResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("6");

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
    public void getTitle() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertEquals(category.getName(), productListModel.getTitle());
    }

    @Test
    public void getImage() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertEquals(category.getImage(), productListModel.getImage());
    }

    @Test
    public void getImageWhenMissingInResponse() {
        productListModel = context.request().adaptTo(ProductListImpl.class);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getImage()).thenReturn("");
        Whitebox.setInternalState(productListModel.getCategoryRetriever(), "category", category);

        String image = productListModel.getImage();
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
    }

    @Test
    public void testCreateFilterMap() {
        productListModel = context.request().adaptTo(ProductListImpl.class);

        Map<String, String[]> queryParameters;
        Map<String, String> filterMap;

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] {});
        filterMap = productListModel.createFilterMap(queryParameters);
        Assert.assertEquals("filters out query parameters without values", 0, filterMap.size());

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] { "123" });
        filterMap = productListModel.createFilterMap(queryParameters);
        Assert.assertEquals("retails valid query filters", 1, filterMap.size());
    }

    @Test
    public void testCalculateCurrentPageCursor() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertEquals("negative page indexes are not allowed", 1, productListModel.calculateCurrentPageCursor("-1").intValue());
        Assert.assertEquals("null value is dealt with", 1, productListModel.calculateCurrentPageCursor(null).intValue());
    }

    @Test
    public void testFilterQueriesReturnNull() {
        // We want to make sure that components will not fail if the __type and/or customAttributeMetadata fields are null
        // For example, 3rd-party integrations might not support this immediately

        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(productListResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        Query query = new Query().setProducts(products).setCategory(category);
        GraphqlResponse<Object, Object> response = new GraphqlResponse<Object, Object>();
        response.setData(query);

        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

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
    public void testProductListNoGraphqlClient() throws IOException {
        Mockito.when(productListResource.adaptTo(GraphqlClient.class)).thenReturn(null);
        productListModel = context.request().adaptTo(ProductListImpl.class);

        Assert.assertTrue(productListModel.getTitle().isEmpty());
        Assert.assertTrue(productListModel.getImage().isEmpty());
        Assert.assertTrue(productListModel.getProducts().isEmpty());
    }

    @Test
    public void testExtendProductQuery() {
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(productListResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        Query query = new Query().setProducts(products).setCategory(category);
        GraphqlResponse<Object, Object> response = new GraphqlResponse<Object, Object>();
        response.setData(query);

        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

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
}
