/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.core.components.internal.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Failure cache tests with the real GraphQL client and a mocked HTTP connection. They verify that the failures are
 * recognized the way the GraphQL client really reports them: with the default executor, with the fault tolerant executor
 * (circuit breaker) and through the GraphQL client cache.
 */
public class MagentoGraphqlClientImplFailureCacheTest {

    private static final String QUERY = "{products{items{sku}}}";
    private static final String RESOURCE_TYPE = "venia/components/commerce/product";
    private static final Map<String, Object> FAULT_TOLERANT = Collections.singletonMap("enableFaultTolerantFallback", true);
    private static final Map<String, Object> CLIENT_CACHE = Collections.singletonMap("cacheConfigurations",
        new String[] { RESOURCE_TYPE + ":true:100:60" });

    @Rule
    public final AemContext context = TestContext.newAemContext("/context/jcr-content.json");

    private CloseableHttpClient httpClient;

    @Before
    public void setUp() {
        httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));
    }

    @Test
    public void testReadTimeout() throws IOException {
        assertFailureCached(new SocketTimeoutException("Read timed out"), null);
    }

    @Test
    public void testReadTimeoutFaultTolerant() throws IOException {
        assertFailureCached(new SocketTimeoutException("Read timed out"), FAULT_TOLERANT);
    }

    @Test
    public void testReadTimeoutClientCache() throws IOException {
        assertFailureCached(new SocketTimeoutException("Read timed out"), CLIENT_CACHE);
    }

    @Test
    public void testConnectionRefused() throws IOException {
        assertFailureCached(new ConnectException("Connection refused"), null);
    }

    @Test
    public void testConnectionRefusedFaultTolerant() throws IOException {
        assertFailureCached(new ConnectException("Connection refused"), FAULT_TOLERANT);
    }

    @Test
    public void testHttpStatusErrorNotCached() throws IOException {
        MagentoGraphqlClient client = createClient(null);
        Utils.setupHttpResponse(null, httpClient, HttpStatus.SC_SERVICE_UNAVAILABLE);

        client.execute(QUERY);
        client.execute(QUERY);

        verify(httpClient, times(2)).execute(any(HttpUriRequest.class), any(ResponseHandler.class));
    }

    @Test
    public void testCircuitBreakerOpenNotCached() throws IOException {
        MagentoGraphqlClient client = createClient(FAULT_TOLERANT);
        when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenThrow(new SocketTimeoutException("Read timed out"));

        // send distinct failing queries until the circuit breaker opens
        String openQuery = null;
        for (int i = 0; i < 20 && openQuery == null; i++) {
            String query = "{q" + i + "}";
            if (client.execute(query).getErrors().get(0).getMessage().contains("circuit breaker open")) {
                openQuery = query;
            }
        }
        assertNotNull("The circuit breaker should open", openQuery);

        // the timeouts before it are remembered, the breaker-open error is not
        Map<List<String>, ?> failureCache = (Map<List<String>, ?>) context.request()
            .getAttribute(MagentoGraphqlClient.class.getName() + ".FailureCache");
        String breakerOpenQuery = openQuery;
        assertFalse(failureCache.isEmpty());
        assertTrue(failureCache.keySet().stream().noneMatch(key -> key.contains(breakerOpenQuery)));
    }

    @Test
    public void testHttpStatusErrorFaultTolerantNotCached() throws IOException {
        MagentoGraphqlClient client = createClient(FAULT_TOLERANT);
        Utils.setupHttpResponse(null, httpClient, HttpStatus.SC_SERVICE_UNAVAILABLE);

        client.execute(QUERY);
        client.execute(QUERY);

        verify(httpClient, times(2)).execute(any(HttpUriRequest.class), any(ResponseHandler.class));
    }

    private void assertFailureCached(IOException failure, Map<String, Object> config) throws IOException {
        MagentoGraphqlClient client = createClient(config);
        when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class))).thenThrow(failure);

        GraphqlResponse<Query, Error> first = client.execute(QUERY);
        GraphqlResponse<Query, Error> second = client.execute(QUERY);

        verify(httpClient, times(1)).execute(any(HttpUriRequest.class), any(ResponseHandler.class));
        assertSame(first, second);
        assertEquals(1, first.getErrors().size());
    }

    private MagentoGraphqlClient createClient(Map<String, Object> config) {
        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Utils.registerGraphqlClient(context, graphqlClient, config);
        context.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);
        context.currentPage("/content/pageD");
        Resource resource = context.create().resource("/content/pageD/jcr:content/product", "sling:resourceType", RESOURCE_TYPE);
        return new MagentoGraphqlClientImpl(resource, context.currentPage(), context.request());
    }
}
