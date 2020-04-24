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

package com.adobe.cq.commerce.core.components.internal.models.v1.searchresults;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchResultsImplTest {

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

    private static final String PAGE = "/content/pageA";
    private static final String SEARCHRESULTS = "/content/pageA/jcr:content/root/responsivegrid/searchresults";

    private SearchResultsImpl searchResultsModel;
    private Resource searchResultsResource;

    @Mock
    HttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(PAGE);
        context.currentResource(SEARCHRESULTS);
        searchResultsResource = Mockito.spy(context.resourceResolver().getResource(SEARCHRESULTS));

        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "httpMethod", HttpMethod.POST);

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-result.json", httpClient, HttpStatus.SC_OK, "{products");
        Mockito.when(searchResultsResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(searchResultsResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, searchResultsResource.getValueMap());

        XSSAPI xssApi = mock(XSSAPI.class);
        when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
        slingBindings.put("xssApi", xssApi);

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
        Assert.assertEquals(7, searchResultsSet.getAvailableAggregations().size()); // category_id is removed
        Assert.assertEquals(1, searchResultsSet.getAppliedQueryParameters().size()); // only search_query
        Assert.assertEquals(4, searchResultsSet.getTotalResults().intValue());
    }

    @Test
    public void testMissingSearchTerm() {
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();
        Assert.assertTrue("Products list is empty", products.isEmpty());
    }

    @Test
    public void testNoMagentoGraphqlClient() {
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Mockito.when(searchResultsResource.adaptTo(GraphqlClient.class)).thenReturn(null);
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();
        Assert.assertTrue("Products list is empty", products.isEmpty());
    }

    @Test
    public void testCreateFilterMap() {
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);
        Map<String, String[]> queryParameters;
        Map<String, String> filterMap;

        queryParameters = new HashMap<>();
        queryParameters.put("search_query", new String[] { "ok" });
        filterMap = searchResultsModel.createFilterMap(queryParameters);
        Assert.assertEquals("filters query string parameter out correctly", 0, filterMap.size());

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] {});
        filterMap = searchResultsModel.createFilterMap(queryParameters);
        Assert.assertEquals("filters out query parameters without values", 0, filterMap.size());

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] { "123" });
        filterMap = searchResultsModel.createFilterMap(queryParameters);
        Assert.assertEquals("retails valid query filters", 1, filterMap.size());
    }

    @Test
    public void testCalculateCurrentPageCursor() {
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);
        Assert.assertEquals("negative page indexes are not allowed", 1, searchResultsModel.calculateCurrentPageCursor("-1").intValue());
        Assert.assertEquals("null value is dealt with", 1, searchResultsModel.calculateCurrentPageCursor(null).intValue());
    }

    @Test
    public void testFilterQueriesReturnNull() throws IOException {
        // We want to make sure that components will not fail if the __type and/or customAttributeMetadata fields are null
        // For example, 3rd-party integrations might not support this immediately

        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(searchResultsResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        Products products = Utils.getQueryFromResource("graphql/magento-graphql-search-result.json").getProducts();
        Query query = new Query().setProducts(products);
        GraphqlResponse<Object, Object> response = new GraphqlResponse<Object, Object>();
        response.setData(query);
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        Collection<ProductListItem> productList = searchResultsModel.getProducts();
        Assert.assertEquals("Return the correct number of products", 4, productList.size());

        SearchResultsSet searchResultsSet = searchResultsModel.getSearchResultsSet();
        Assert.assertEquals(0, searchResultsSet.getAvailableAggregations().size());
    }
}
