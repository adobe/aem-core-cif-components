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
package com.adobe.cq.commerce.core.components.testing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.junit.Assert;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.wcm.core.components.internal.DataLayerConfig;
import com.adobe.cq.wcm.core.components.internal.jackson.DefaultMethodSkippingModuleProvider;
import com.adobe.cq.wcm.core.components.internal.jackson.PageModuleProvider;
import com.adobe.cq.wcm.core.components.services.link.PathProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Utils {

    public static final String DATALAYER_CONFIG_NAME = "com.adobe.cq.wcm.core.components.internal.DataLayerConfig";
    public static final String STOREFRONT_CONTEXT_CONFIG_NAME = "com.adobe.cq.commerce.core.components.internal.storefrontcontext.CommerceStorefrontContextConfig";

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
     * @param contains When set, the body of the GraphQL POST request must start with that String.
     *
     * @return The JSON content of that file.
     *
     * @throws IOException
     */
    public static String setupHttpResponse(String filename, HttpClient httpClient, int httpCode, String contains) throws IOException {
        String json = IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);

        HttpEntity mockedHttpEntity = mock(HttpEntity.class);
        HttpResponse mockedHttpResponse = mock(HttpResponse.class);
        StatusLine mockedStatusLine = mock(StatusLine.class);

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        when(mockedHttpEntity.getContent()).thenAnswer(inv -> new ByteArrayInputStream(bytes));
        when(mockedHttpEntity.getContentLength()).thenReturn(new Long(bytes.length));

        when(mockedHttpResponse.getEntity()).thenReturn(mockedHttpEntity);

        if (contains != null) {
            GraphqlQueryMatcher matcher = new GraphqlQueryMatcher(contains);
            when(httpClient.execute(Mockito.argThat(matcher))).thenReturn(mockedHttpResponse);
        } else {
            when(httpClient.execute((HttpUriRequest) Mockito.any())).thenReturn(mockedHttpResponse);
        }

        when(mockedStatusLine.getStatusCode()).thenReturn(httpCode);
        when(mockedHttpResponse.getStatusLine()).thenReturn(mockedStatusLine);

        return json;
    }

    /**
     * Returns a plain GraphqlClient instance mock.
     */
    public static GraphqlClient setupGraphqlClient() {
        HttpClient httpClient = mock(HttpClient.class);
        GraphqlClient graphqlClient = new GraphqlClientImpl();

        GraphqlClientConfiguration graphqlClientConfiguration = mock(GraphqlClientConfiguration.class);
        when(graphqlClientConfiguration.httpMethod()).thenReturn(HttpMethod.POST);

        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "configuration", graphqlClientConfiguration);
        return graphqlClient;
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
        GraphqlClient graphqlClient = setupGraphqlClient();
        addHttpResponseFrom(graphqlClient, filename, queryStartsWith);
        return graphqlClient;
    }

    /**
     * Adds another response to the already mocked graphql client.
     *
     * @param graphqlClient
     * @param filename
     * @param queryStartsWith
     * @throws IOException
     */
    public static void addHttpResponseFrom(GraphqlClient graphqlClient, String filename, String... queryStartsWith) throws IOException {
        HttpClient httpClient = (HttpClient) Whitebox.getInternalState(graphqlClient, "client");
        for (String query : queryStartsWith) {
            setupHttpResponse(filename, httpClient, HttpStatus.SC_OK, query);
        }
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

        private String contains;

        public GraphqlQueryMatcher(String contains) {
            this.contains = contains;
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
                    return graphqlRequest.getQuery().contains(contains);
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
                return graphqlQuery.contains(contains);
            }
        }

    }

    static public void addDataLayerConfig(ConfigurationBuilder mockConfigBuilder, boolean enabled) {
        ValueMap datalayerVm = new ValueMapDecorator(ImmutableMap.of("enabled", enabled));

        DataLayerConfig dataLayerConfig = Mockito.mock(DataLayerConfig.class);
        Mockito.when(dataLayerConfig.enabled()).thenReturn(enabled);

        Mockito.when(mockConfigBuilder.name(DATALAYER_CONFIG_NAME)).thenReturn(mockConfigBuilder);
        Mockito.when(mockConfigBuilder.asValueMap()).thenReturn(datalayerVm);
        Mockito.when(mockConfigBuilder.as(DataLayerConfig.class)).thenReturn(dataLayerConfig);
    }

    static public void addStorefrontContextConfig(ConfigurationBuilder mockConfigBuilder, boolean enabled) {
        ValueMap storefrontConfigVm = new ValueMapDecorator(ImmutableMap.of("enabled", enabled));
        Mockito.when(mockConfigBuilder.name(STOREFRONT_CONTEXT_CONFIG_NAME)).thenReturn(mockConfigBuilder);
        Mockito.when(mockConfigBuilder.asValueMap()).thenReturn(storefrontConfigVm);
    }

    /**
     * Provided a {@code model} object and an {@code expectedJsonResource} identifying a JSON file in the class path, this method will
     * test the JSON export of the model and compare it to the JSON object provided by the {@code expectedJsonResource}.
     *
     * @param model the Sling Model
     * @param expectedJsonResource the class path resource providing the expected JSON object
     */
    public static void testJSONExport(Object model, String expectedJsonResource) {
        Writer writer = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        PageModuleProvider pageModuleProvider = new PageModuleProvider();
        mapper.registerModule(pageModuleProvider.getModule());
        DefaultMethodSkippingModuleProvider defaultMethodSkippingModuleProvider = new DefaultMethodSkippingModuleProvider();
        mapper.registerModule(defaultMethodSkippingModuleProvider.getModule());
        try {
            mapper.writer().writeValue(writer, model);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to generate JSON export for model %s: %s", model.getClass().getName(), e
                .getMessage()));
        }
        JsonReader outputReader = Json.createReader(IOUtils.toInputStream(writer.toString(), StandardCharsets.UTF_8));
        InputStream is = Utils.class.getResourceAsStream(expectedJsonResource);
        if (is != null) {
            JsonReader expectedReader = Json.createReader(is);
            Assert.assertEquals(expectedReader.read(), outputReader.read());
        } else {
            throw new RuntimeException("Unable to find test file " + expectedJsonResource + ".");
        }
        IOUtils.closeQuietly(is);
    }

    public static class MockPathProcessor implements PathProcessor {
        @Override
        public boolean accepts(String s, SlingHttpServletRequest slingHttpServletRequest) {
            return true;
        }

        @Override
        public String sanitize(String s, SlingHttpServletRequest slingHttpServletRequest) {
            return s;
        }

        @Override
        public String map(String s, SlingHttpServletRequest slingHttpServletRequest) {
            return s;
        }

        @Override
        public String externalize(String s, SlingHttpServletRequest slingHttpServletRequest) {
            return s;
        }
    }
}
