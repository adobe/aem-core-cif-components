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

package com.adobe.cq.commerce.core.components.testing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Utils {

    public static final String DATALAYER_CONFIG_NAME = "com.adobe.cq.wcm.core.components.internal.DataLayerConfig";

    /**
     * This method prepares the mock http response with either the content of the <code>filename</code>
     * or the provided <code>content</code> String.<br>
     * <br>
     * <b>Important</b>: because of the way the content of an HTTP response is consumed, this method MUST be called each time
     * the client is called.
     *
     * @param filename The file to use for the json response.
     * @param httpClient The HTTP client for which we want to mock responses.
     * @param httpCode The http code that the mocked response will return.
     *
     * @return The JSON content of that file.
     *
     * @throws IOException
     */
    public static String setupHttpResponse(String filename, HttpClient httpClient, int httpCode) throws IOException {
        return setupHttpResponse(filename, httpClient, httpCode, null);
    }

    /**
     * This method prepares the mock http response with either the content of the <code>filename</code>
     * or the provided <code>content</code> String.<br>
     * <br>
     * <b>Important</b>: because of the way the content of an HTTP response is consumed, this method MUST be called each time
     * the client is called.
     *
     * @param filename The file to use for the json response.
     * @param httpClient The HTTP client for which we want to mock responses.
     * @param httpCode The http code that the mocked response will return.
     * @param startsWith When set, the body of the GraphQL POST request must start with that String.
     *
     * @return The JSON content of that file.
     *
     * @throws IOException
     */
    public static String setupHttpResponse(String filename, HttpClient httpClient, int httpCode, String startsWith) throws IOException {
        String json = IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);

        HttpEntity mockedHttpEntity = Mockito.mock(HttpEntity.class);
        HttpResponse mockedHttpResponse = Mockito.mock(HttpResponse.class);
        StatusLine mockedStatusLine = Mockito.mock(StatusLine.class);

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Mockito.when(mockedHttpEntity.getContent()).thenReturn(new ByteArrayInputStream(bytes));
        Mockito.when(mockedHttpEntity.getContentLength()).thenReturn(new Long(bytes.length));

        Mockito.when(mockedHttpResponse.getEntity()).thenReturn(mockedHttpEntity);

        if (startsWith != null) {
            GraphqlQueryMatcher matcher = new GraphqlQueryMatcher(startsWith);
            Mockito.when(httpClient.execute(Mockito.argThat(matcher))).thenReturn(mockedHttpResponse);
        } else {
            Mockito.when(httpClient.execute((HttpUriRequest) Mockito.any())).thenReturn(mockedHttpResponse);
        }

        Mockito.when(mockedStatusLine.getStatusCode()).thenReturn(httpCode);
        Mockito.when(mockedHttpResponse.getStatusLine()).thenReturn(mockedStatusLine);

        return json;
    }

    /**
     * Returns a GraphqlClient instance configured to return the JSON response from the <code>filename</code> resource.
     *
     * @param filename The filename of the resource containing the JSON response.
     * @return The GraphqlClient instance.
     * @throws IOException
     */
    public static GraphqlClient setupGraphqlClientWithHttpResponseFrom(String filename) throws IOException {
        return setupGraphqlClientWithHttpResponseFrom(filename, null);
    }

    /**
     * Returns a GraphqlClient instance configured to return the JSON response from the <code>filename</code> resource.
     *
     * @param filename The filename of the resource containing the JSON response.
     * @param queryStartsWith An optional String that must match the start of the GraphQL query.
     * @return The GraphqlClient instance.
     * @throws IOException
     */
    public static GraphqlClient setupGraphqlClientWithHttpResponseFrom(String filename, String queryStartsWith) throws IOException {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "httpMethod", HttpMethod.POST);
        setupHttpResponse(filename, httpClient, HttpStatus.SC_OK, queryStartsWith);
        return graphqlClient;
    }

    /**
     * Returns a Magento Query response based on the given filename resource containing a JSON GraphQL response.
     *
     * @param filename The filename of the resource.
     * @return The parsed Magento Query object.
     * @throws IOException
     */
    public static Query getQueryFromResource(String filename) throws IOException {
        String json = getResource(filename);
        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> response = QueryDeserializer.getGson().fromJson(json, type);
        return response.getData();
    }

    public static String getResource(String filename) throws IOException {
        return IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }

    /**
     * Matcher class used to match a GraphQL query. This is used to properly mock GraphQL responses.
     */
    private static class GraphqlQueryMatcher extends ArgumentMatcher<HttpUriRequest> {

        private String startsWith;

        public GraphqlQueryMatcher(String startsWith) {
            this.startsWith = startsWith;
        }

        @Override
        public boolean matches(Object obj) {
            if (!(obj instanceof HttpUriRequest)) {
                return false;
            }

            if (obj instanceof HttpEntityEnclosingRequest) {
                // GraphQL query is in POST body
                HttpEntityEnclosingRequest req = (HttpEntityEnclosingRequest) obj;
                try {
                    String body = IOUtils.toString(req.getEntity().getContent(), StandardCharsets.UTF_8);
                    Gson gson = new Gson();
                    GraphqlRequest graphqlRequest = gson.fromJson(body, GraphqlRequest.class);
                    return graphqlRequest.getQuery().startsWith(startsWith);
                } catch (Exception e) {
                    return false;
                }
            } else {
                // GraphQL query is in the URL 'query' parameter
                HttpUriRequest req = (HttpUriRequest) obj;
                String uri = null;
                try {
                    uri = URLDecoder.decode(req.getURI().toString(), StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    return false;
                }
                String graphqlQuery = uri.substring(uri.indexOf("?query=") + 7);
                return graphqlQuery.startsWith(startsWith);
            }
        }

    }

    static public ConfigurationBuilder getDataLayerConfig(boolean enabled) {
        ValueMap datalayerConfig = new ValueMapDecorator(ImmutableMap.of("enabled", enabled));
        ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
        Mockito.when(mockConfigBuilder.name(DATALAYER_CONFIG_NAME)).thenReturn(mockConfigBuilder);
        Mockito.when(mockConfigBuilder.asValueMap()).thenReturn(datalayerConfig);

        return mockConfigBuilder;
    }
}