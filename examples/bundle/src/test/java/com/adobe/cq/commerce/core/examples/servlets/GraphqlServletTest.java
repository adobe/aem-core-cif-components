/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.examples.servlets;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.CategoryArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.ProductsArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.StoreConfig;
import com.adobe.cq.commerce.magento.graphql.StoreConfigQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.google.gson.reflect.TypeToken;

public class GraphqlServletTest {

    private GraphqlServlet graphqlServlet;
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @Before
    public void setUp() throws ServletException {
        graphqlServlet = new GraphqlServlet();
        graphqlServlet.init();
        request = new MockSlingHttpServletRequest(null);
        response = new MockSlingHttpServletResponse();
    }

    @Test
    public void testGetRequest() throws ServletException, IOException {
        // Search parameters
        CategoryArgumentsDefinition searchArgs = q -> q.id(2);

        // Create "recursive" query with depth 5 to fetch category data and children
        // There isn't any better way to build such a query with GraphQL
        CategoryTreeQueryDefinition queryArgs = q -> TestGraphqlQueries.CATEGORY_TREE_LAMBDA
            .apply(q)
            .children(r -> TestGraphqlQueries.CATEGORY_TREE_LAMBDA
                .apply(r)
                .children(s -> TestGraphqlQueries.CATEGORY_TREE_LAMBDA
                    .apply(s)
                    .children(t -> TestGraphqlQueries.CATEGORY_TREE_LAMBDA
                        .apply(t)
                        .children(u -> TestGraphqlQueries.CATEGORY_TREE_LAMBDA
                            .apply(u)))));

        String query = Operations.query(q -> q.category(searchArgs, queryArgs)).toString();

        request.setParameterMap(Collections.singletonMap("query", query));

        graphqlServlet.doGet(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        CategoryTree category = graphqlResponse.getData().getCategory();

        Assert.assertEquals(Integer.valueOf(2), category.getId());
    }

    @Test
    public void testPostRequest() throws ServletException, IOException {
        // Search parameters
        ProductsArgumentsDefinition searchArgs = s -> s.search("whatever").currentPage(1).pageSize(3);

        // Main queries
        ProductsQueryDefinition queryArgs = q -> q.items(TestGraphqlQueries.CONFIGURABLE_PRODUCT_QUERY);
        StoreConfigQueryDefinition storeQuery = q -> q.secureBaseMediaUrl();

        String query = Operations.query(q -> q
            .products(searchArgs, queryArgs)
            .storeConfig(storeQuery)
            .countries(c -> c.id())).toString(); // Not supported, should return null

        GraphqlRequest graphqlRequest = new GraphqlRequest(query);
        String body = QueryDeserializer.getGson().toJson(graphqlRequest);
        request.setContent(body.getBytes());

        graphqlServlet.doPost(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);

        Products products = graphqlResponse.getData().getProducts();
        Assert.assertEquals(1, products.getItems().size());
        Assert.assertEquals("MJ01", products.getItems().get(0).getSku());

        // This is an important check, do NOT remove, see the comment in the servlet code
        Assert.assertEquals("beaumont-summit-kit", products.getItems().get(0).getUrlKey());

        StoreConfig storeConfig = graphqlResponse.getData().getStoreConfig();
        Assert.assertTrue(storeConfig.getSecureBaseMediaUrl().startsWith("https://"));

        Assert.assertNull(graphqlResponse.getData().getCountries());
    }

    @Test
    public void testGetRequestWithVariables() throws ServletException, IOException {
        String query = "query rootCategory($catId: Int!) {category(id: $catId){id,name,url_path}}";

        Map<String, Object> map = new HashMap<>();
        map.put("query", query);
        map.put("variables", Collections.singletonMap("catId", 2));
        map.put("operationName", "rootCategory");
        request.setParameterMap(map);

        graphqlServlet.doGet(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        CategoryTree category = graphqlResponse.getData().getCategory();

        Assert.assertEquals(Integer.valueOf(2), category.getId());
    }

    @Test
    public void testPostRequestWithVariables() throws ServletException, IOException {
        String query = "query rootCategory($catId: Int!) {category(id: $catId){id,name,url_path}}";

        GraphqlRequest graphqlRequest = new GraphqlRequest(query);
        graphqlRequest.setVariables(Collections.singletonMap("catId", 2));
        graphqlRequest.setOperationName("rootCategory");
        String body = QueryDeserializer.getGson().toJson(graphqlRequest);
        request.setContent(body.getBytes());

        graphqlServlet.doPost(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        CategoryTree category = graphqlResponse.getData().getCategory();

        Assert.assertEquals(Integer.valueOf(2), category.getId());
    }
}
