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

import java.lang.reflect.Type;
import java.util.Collections;

import javax.servlet.ServletException;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.google.gson.reflect.TypeToken;

public class MockGraphqlClient implements GraphqlClient {

    private GraphqlServlet graphqlServlet;

    public MockGraphqlClient() throws ServletException {
        graphqlServlet = new GraphqlServlet();
        graphqlServlet.init();
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public <T, U> GraphqlResponse<T, U> execute(GraphqlRequest graphqlRequest, Type typeOfT, Type typeOfU) {
        return execute(graphqlRequest, typeOfT, typeOfU, null);
    }

    @Override
    public <T, U> GraphqlResponse<T, U> execute(GraphqlRequest graphqlRequest, Type typeOfT, Type typeOfU, RequestOptions options) {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(null);
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        try {
            if (options != null && HttpMethod.GET.equals(options.getHttpMethod())) {
                request.setParameterMap(Collections.singletonMap("query", graphqlRequest.getQuery()));
                graphqlServlet.doGet(request, response);
            } else {
                String body = QueryDeserializer.getGson().toJson(graphqlRequest);
                request.setContent(body.getBytes());
                graphqlServlet.doPost(request, response);
            }
        } catch (Exception e) {
            return null;
        }
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, typeOfT, typeOfU).getType();
        return QueryDeserializer.getGson().fromJson(output, type);
    }

    @Override
    public String getGraphQLEndpoint() {
        return null;
    }

}
