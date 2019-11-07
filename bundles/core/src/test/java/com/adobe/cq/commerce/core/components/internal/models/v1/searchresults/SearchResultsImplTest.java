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
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;

/**
 * JUnit test suite for {@link SearchResultsImpl}
 */
public class SearchResultsImplTest {

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

    private static final String PAGE = "/content/pageA";
    private static final String SEARCHRESULTS = "/content/pageA/jcr:content/root/responsivegrid/searchresults";

    private static final String SEARCH_TERM = "glove";
    private static final String QUERY_STRING = "{products(search:\"glove\"){items{__typename,id,url_key,name,small_image{label,url},price{regularPrice{amount{value,currency}}}}}}";

    private SearchResultsImpl searchResultsModel;
    private Resource searchResultsResource;
    private GraphqlResponse<Query, Error> response = new GraphqlResponse<>();

    @Before
    public void setUp() throws IOException {
        Page page = context.currentPage(PAGE);
        context.currentResource(SEARCHRESULTS);
        searchResultsResource = Mockito.spy(context.resourceResolver().getResource(SEARCHRESULTS));

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(searchResultsResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

        // Search results
        String json = IOUtils.toString(this.getClass().getResourceAsStream("/graphql/magento-graphql-search-result.json"),
            StandardCharsets.UTF_8);

        Query query = QueryDeserializer.getGson().fromJson(json, Query.class);
        GraphqlResponse<Object, Object> response = new GraphqlResponse<>();
        response.setData(query);

        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(searchResultsResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        Mockito.when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);
    }

    @Test
    public void testGenerateQueryString() {
        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        String actualQueryString = searchResultsModel.generateQueryString(SEARCH_TERM);
        Assert.assertEquals("The query string is generated", QUERY_STRING, actualQueryString);
    }

    @Test
    public void testProducts() {
        context.request().setParameterMap(Collections.singletonMap("search_query", "glove"));
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();
        Assert.assertEquals("Return the correct number of products", 2, products.size());
    }

    @Test
    public void testMissingSearchTerm() {
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();
        Assert.assertTrue("Products list is empty", products.isEmpty());
    }

    @Test
    public void testNoMagentoGraphqlClient() {
        Mockito.when(searchResultsResource.adaptTo(GraphqlClient.class)).thenReturn(null);
        searchResultsModel = context.request().adaptTo(SearchResultsImpl.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();
        Assert.assertTrue("Products list is empty", products.isEmpty());
    }
}
