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
package com.adobe.cq.commerce.core.components.internal.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.internal.verification.AtMost;
import org.mockito.stubbing.Stubber;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.ComponentsConfigurationAdapterFactory;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.MockLaunch;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.adobe.cq.commerce.graphql.client.CachingStrategy;
import com.adobe.cq.commerce.graphql.client.CachingStrategy.DataFetchingPolicy;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequestException;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.launches.api.Launch;
import com.day.cq.wcm.api.Page;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MagentoGraphqlClientImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);
    private static final String BACKEND_CALL_DURATION_ATTRIBUTE = "com.adobe.cif.backendCallDurationInMs";

    private static final String PAGE_A = "/content/pageA";
    private static final String LAUNCH_BASE_PATH = "/content/launches/2020/09/14/mylaunch";
    private static final String LAUNCH_PAGE_A = LAUNCH_BASE_PATH + PAGE_A;
    private static final String PRODUCT_COMPONENT_PATH = "/content/pageA/jcr:content/root/responsivegrid/product";

    private GraphqlClient graphqlClient;

    @Rule
    public final AemContext context = TestContext.newAemContext("/context/jcr-content.json");

    @Before
    public void setup() {
        graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(graphqlClient.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(null);

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> StringUtils.isNotEmpty(
            input.getValueMap().get("cq:graphqlClient", String.class)) ? graphqlClient : null);
    }

    private void testMagentoStoreProperty(Resource resource, boolean withStoreHeader) {
        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(resource, null, null);
        assertNotNull("GraphQL client created successfully", client);
        executeAndCheck(withStoreHeader, client);
    }

    private void executeAndCheck(boolean withStoreHeader, MagentoGraphqlClient client) {
        // Verify parameters with default execute() method and store property
        client.execute("{dummy}");
        List<Header> headers = withStoreHeader ? Collections.singletonList(new BasicHeader("Store", "my-store")) : Collections.emptyList();
        RequestOptionsMatcher matcher = new RequestOptionsMatcher(headers, null);
        verify(graphqlClient).execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.argThat(matcher));

        // Verify setting a custom HTTP method
        client.execute("{dummy}", HttpMethod.GET);
        matcher = new RequestOptionsMatcher(headers, HttpMethod.GET);
        verify(graphqlClient).execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.argThat(matcher));
    }

    @Test
    public void testCustomHeaders() {
        List<Header> expectedHeaders = new ArrayList<>();
        expectedHeaders.add(new BasicHeader("Store", "my-store"));
        expectedHeaders.add(new BasicHeader("customHeader-1", "value1"));
        expectedHeaders.add(new BasicHeader("customHeader-2", "value2"));
        expectedHeaders.add(new BasicHeader("customHeader-2", "value3"));
        expectedHeaders.add(new BasicHeader("customHeader-3", "=value3=3=3=3=3"));

        ValueMap MOCK_CONFIGURATION_CUSTOM_HEADERS = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
            "my-store", "httpHeaders", new String[] { "customHeader-1=value1", "customHeader-2=value2", "customHeader-2=value3",
                "customHeader-3==value3=3=3=3=3", "Authorization=099sx8x7v1" }));
        ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION_CUSTOM_HEADERS);

        Page pageWithConfig = spy(context.pageManager().getPage(PAGE_A));
        Resource pageResource = spy(pageWithConfig.adaptTo(Resource.class));
        when(pageWithConfig.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        RequestOptionsMatcher matcher = new RequestOptionsMatcher(expectedHeaders, HttpMethod.GET);
        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(pageResource, pageWithConfig, null);
        client.execute("{dummy}", HttpMethod.GET);
        graphqlClient.getConfiguration();

        verify(graphqlClient).execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.argThat(matcher));

        Map<String, String> legacyHeaderMap = client.getHttpHeaders();
        Map<String, String[]> headerMap = client.getHttpHeaderMap();

        assertThat(legacyHeaderMap.keySet(), hasItems("customHeader-1", "customHeader-2", "customHeader-3"));
        assertThat(legacyHeaderMap.values(), hasItems("value1", "value2", "=value3=3=3=3=3"));
        assertThat(headerMap.keySet(), hasItems("customHeader-1", "customHeader-2", "customHeader-3"));
        assertThat(
            headerMap.values().stream().flatMap(Arrays::stream).collect(Collectors.toSet()),
            hasItems("value1", "value2", "value3", "=value3=3=3=3=3"));
    }

    @Test
    public void testLocalCacheExactMatch() {
        context.currentPage("/content/pageD");
        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(context.currentResource(), null, context.request());
        GraphqlResponse expected = mock(GraphqlResponse.class);
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(expected);

        // execute the same query twice
        GraphqlResponse resp1 = client.execute("{dummy}");
        GraphqlResponse resp2 = client.execute("{dummy}");

        verify(graphqlClient, new AtMost(1)).execute(any(), any(), any(), any());
        assertSame(resp1, resp2);
    }

    @Test
    public void testLocalCacheFuzzyMatch() {
        context.currentPage("/content/pageD");
        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(context.currentResource(), null, context.request());
        GraphqlResponse expected = mock(GraphqlResponse.class);
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(expected);

        // execute the same query twice
        GraphqlResponse<?, ?> resp1 = client.execute(
            "{products(filter:{sku:{eq:\"ABC\"}}){items{__typename,sku,name,url_key,price_range{minimum_price{regular_price{value,currency},final_price{value,currency},discount{amount_off,percent_off}}}}}");
        GraphqlResponse<?, ?> resp2 = client.execute("{products(filter:{sku:{eq:\"ABC\"}}){items{__typename,sku}}");

        verify(graphqlClient, new AtMost(1)).execute(any(), any(), any(), any());
        assertSame(resp1, resp2);
    }

    @Test
    public void testMagentoStorePropertyWithConfigBuilder() {
        Page pageWithConfig = spy(context.pageManager().getPage(PAGE_A));
        Resource pageResource = spy(pageWithConfig.adaptTo(Resource.class));
        when(pageWithConfig.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(pageWithConfig.adaptTo(Resource.class), pageWithConfig, null);
        assertNotNull("GraphQL client created successfully", client);
        executeAndCheck(true, client);
    }

    @Test
    public void testCachingStrategyParametersForComponents() {
        Resource resource = context.resourceResolver().getResource(PRODUCT_COMPONENT_PATH);
        testCachingStrategyParameters(resource);
    }

    @Test
    public void testCachingStrategyParametersForOsgiService() {
        Resource resource = new SyntheticResource(null, (String) null, "com.adobe.myosgiservice");
        testCachingStrategyParameters(resource);
    }

    private void testCachingStrategyParameters(Resource resource) {
        Page page = spy(context.pageManager().getPage(PAGE_A));
        Resource pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(resource, page, null);
        assertNotNull("GraphQL client created successfully", client);
        client.execute("{dummy}");

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        verify(graphqlClient).execute(Mockito.any(), Mockito.any(), Mockito.any(), captor.capture());

        CachingStrategy cachingStrategy = captor.getValue().getCachingStrategy();
        assertEquals(resource.getResourceType(), cachingStrategy.getCacheName());
        assertEquals(DataFetchingPolicy.CACHE_FIRST, cachingStrategy.getDataFetchingPolicy());
    }

    @Test
    public void testMagentoStoreProperty() {
        // Get page which has the magentoStore property in its jcr:content node
        Resource resource = spy(context.resourceResolver().getResource("/content/pageA"));
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testInheritedMagentoStoreProperty() {
        // Get page whose parent has the magentoStore property in its jcr:content node
        Resource resource = spy(context.resourceResolver().getResource("/content/pageB/pageC"));
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testMissingMagentoStoreProperty() {
        // Get page which has the magentoStore property in its jcr:content node
        Resource resource = spy(context.resourceResolver().getResource("/content/pageD/jcr:content"));
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(ComponentsConfiguration.EMPTY);
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testOldMagentoStoreProperty() {
        // Get page which has the old cq:magentoStore property in its jcr:content node
        Resource resource = spy(context.resourceResolver().getResource("/content/pageE/jcr:content"));
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(ComponentsConfiguration.EMPTY);
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testNewMagentoStoreProperty() {
        // Get page which has both the new magentoStore property and old cq:magentoStore property
        // in its jcr:content node and make sure the new one is prefered
        Resource resource = spy(context.resourceResolver().getResource("/content/pageF"));
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        testMagentoStoreProperty(resource, true);
    }

    @Test(expected = IllegalStateException.class)
    public void testError() {
        // Get page which has the magentoStore property in its jcr:content node
        Resource resource = spy(context.resourceResolver().getResource("/content/pageG"));
        new MagentoGraphqlClientImpl(resource, null, null);
    }

    @Test
    public void testPreviewVersionHeaderOnLaunchPage() {
        context.registerAdapter(Resource.class, Launch.class, (Function<Resource, Launch>) resource -> new MockLaunch(resource));

        context.registerService(ServiceUserMapped.class, mock(ServiceUserMapped.class), ServiceUserMapped.SUBSERVICENAME,
            "cif-components-configuration");
        context.registerInjectActivateService(new ComponentsConfigurationAdapterFactory());
        // We configure the adapter to get a config for PAGE_A, so we test that the code gets the config from the Launch production page
        context.registerAdapter(Resource.class, ConfigurationBuilder.class, (Function<Resource, ConfigurationBuilder>) resource -> {
            ConfigurationBuilder configBuilder = mock(ConfigurationBuilder.class);
            ValueMap data = resource.getPath().equals(PAGE_A) ? MOCK_CONFIGURATION : ValueMap.EMPTY;
            when(configBuilder.name(anyString())).thenReturn(configBuilder);
            when(configBuilder.asValueMap()).thenReturn(data);
            return configBuilder;
        });

        // We test that a component rendered on an AEM Launch page will add the Preview-Version header
        Page launchPage = context.pageManager().getPage(LAUNCH_PAGE_A);
        Resource launchProductResource = context.resourceResolver().getResource(LAUNCH_BASE_PATH + PRODUCT_COMPONENT_PATH);

        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(launchProductResource, launchPage, null);

        // Verify parameters with default execute() method and store property
        client.execute("{dummy}");

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Store", "my-store"));
        headers.add(new BasicHeader("Preview-Version", "1606809600")); // Tuesday, 1 December 2020 09:00:00 GMT+01:00

        RequestOptionsMatcher matcher = new RequestOptionsMatcher(headers, HttpMethod.POST);
        verify(graphqlClient).execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.argThat(matcher));
    }

    @Test
    public void testPreviewVersionHeaderWithTimewarpRequestParameter() {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(time.getTimeInMillis() + 3600000); // 1h in future from now

        // Set a date in future so the unit test never fails (the code checks that the timewarp epoch is in the future)
        context.request().setParameterMap(Collections.singletonMap("timewarp", String.valueOf(time.getTimeInMillis()))); // Time in ms
        testPreviewVersionHeaderWithTimewarp(time.getTimeInMillis());
    }

    @Test
    public void testPreviewVersionHeaderWithTimewarpCookie() {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(time.getTimeInMillis() + 3600000); // 1h in future from now

        // Set a date in future so the unit test never fails (the code checks that the timewarp epoch is in the future)
        context.request().addCookie(new Cookie("timewarp", String.valueOf(time.getTimeInMillis()))); // Time in ms
        testPreviewVersionHeaderWithTimewarp(time.getTimeInMillis());
    }

    @Test
    public void testPreviewVersionHeaderWithTimewarpCookieInThePast() {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(time.getTimeInMillis() - 3600000); // 1h in past from now

        // Set a date in past so the unit test always fails (the code checks that the timewarp epoch is in the future)
        context.request().addCookie(new Cookie("timewarp", String.valueOf(time.getTimeInMillis()))); // Time in ms
        testPreviewVersionHeaderWithTimewarp(null);
    }

    @Test
    public void testPreviewVersionHeaderWithInvalidTimewarpValue() {
        context.request().addCookie(new Cookie("timewarp", "invalid"));
        testPreviewVersionHeaderWithTimewarp(null);
    }

    private void testPreviewVersionHeaderWithTimewarp(Long expectedTimeInMillis) {
        Page page = spy(context.pageManager().getPage(PAGE_A));
        Resource pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(pageResource, page, context.request());

        // Verify parameters with default execute() method and store property
        client.execute("{dummy}");

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Store", "my-store"));
        if (expectedTimeInMillis != null) {
            String expectedPreviewVersion = String.valueOf(expectedTimeInMillis.longValue() / 1000);
            headers.add(new BasicHeader("Preview-Version", expectedPreviewVersion));
        }

        // The POST is only explicitly added when there is a Preview-Version header
        RequestOptionsMatcher matcher = new RequestOptionsMatcher(headers, expectedTimeInMillis != null ? HttpMethod.POST : null);
        verify(graphqlClient).execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.argThat(matcher));
    }

    @Test
    public void testErrorResponses() {
        Page page = spy(context.pageManager().getPage(PAGE_A));
        Resource pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        MagentoGraphqlClient client = new MagentoGraphqlClientImpl(pageResource, page, context.request());
        RuntimeException foobar = new RuntimeException("foobar");
        doThrow(foobar).when(graphqlClient).execute(any(), any(), any(), any());

        GraphqlResponse<Query, Error> response = client.execute("query");
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        assertEquals("[java.lang.RuntimeException: \"foobar\"]", response.getErrors().get(0).getMessage());
        assertEquals(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY, response.getErrors().get(0).getCategory());

        RuntimeException bar = new RuntimeException("bar");
        RuntimeException foo = new RuntimeException("foo", bar);
        doThrow(foo).when(graphqlClient).execute(any(), any(), any(), any());

        response = client.execute("query", HttpMethod.POST);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        assertEquals("[java.lang.RuntimeException: \"foo\"] Caused by: [java.lang.RuntimeException: \"bar\"]",
            response.getErrors().get(0).getMessage());
        assertEquals(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY, response.getErrors().get(0).getCategory());
    }

    @Test
    public void checkGraphqlDurationBeenSet() {
        MagentoGraphqlClient client = createTestClient();
        mockSuccessfulResponse(100L);

        client.execute("{dummy}");
        client.execute("{dummy123}");

        assertDurationAccumulation(200L, "Duration should be accumulated from both calls");
    }

    @Test
    public void checkGraphqlDurationBeenSetDuringErrorResponse() {
        MagentoGraphqlClient client = createTestClient();
        mockExceptionResponse(
            new GraphqlRequestException("Test error 1", 150L),
            new GraphqlRequestException("Test error 2", 200L));

        client.execute("{dummy}");
        client.execute("{dummy123}");

        assertDurationAccumulation(350L, "Duration should be accumulated from both error calls");
    }

    @Test
    public void checkGraphqlDurationBeenSetWithHttpMethod() {
        MagentoGraphqlClient client = createTestClient();
        mockSuccessfulResponse(100L);

        client.execute("{dummy1}", HttpMethod.GET);   // caching path
        client.execute("{dummy2}", HttpMethod.POST);  // non-caching path

        assertDurationAccumulation(200L, "Duration should be accumulated from both calls");
    }

    @Test
    public void checkGraphqlDurationBeenSetWithHttpMethodDuringError() {
        MagentoGraphqlClient client = createTestClient();
        mockExceptionResponse(
            new GraphqlRequestException("Test error with GET", 180L),
            new GraphqlRequestException("Test error with POST", 220L));

        client.execute("{dummy1}", HttpMethod.GET);   // caching path error
        client.execute("{dummy2}", HttpMethod.POST);  // non-caching path error

        assertDurationAccumulation(400L, "Duration should be accumulated from both error calls");
    }

    // Helper methods for optimized duration testing
    private MagentoGraphqlClient createTestClient() {
        context.currentPage("/content/pageD");
        return new MagentoGraphqlClientImpl(context.currentResource(), null, context.request());
    }

    private GraphqlResponse mockSuccessfulResponse(long duration) {
        GraphqlResponse expected = mock(GraphqlResponse.class);
        when(expected.getDuration()).thenReturn(duration);
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(expected);
        return expected;
    }

    private void mockExceptionResponse(GraphqlRequestException... exceptions) {
        if (exceptions.length == 0)
            return;

        // Build the doThrow chain dynamically
        Stubber stubbing = doThrow(exceptions[0]);
        for (int i = 1; i < exceptions.length; i++) {
            stubbing = stubbing.doThrow(exceptions[i]);
        }

        stubbing.when(graphqlClient).execute(any(), any(), any(), any());
    }

    private void assertDurationAccumulation(long expectedDuration, String message) {
        AtomicLong existingDuration = (AtomicLong) context.request().getAttribute(BACKEND_CALL_DURATION_ATTRIBUTE);
        assertNotNull("Duration should be set in request attributes", existingDuration);
        assertEquals(message, expectedDuration, existingDuration.get());
    }

    /**
     * Matcher class used to check that the RequestOptions added by the wrapper are correct.
     */
    private static class RequestOptionsMatcher extends ArgumentMatcher<RequestOptions> {

        private List<Header> headers;

        private HttpMethod httpMethod;

        public RequestOptionsMatcher(List<Header> headers, HttpMethod httpMethod) {
            this.headers = headers;
            this.httpMethod = httpMethod;
        }

        @Override
        public boolean matches(Object obj) {
            if (!(obj instanceof RequestOptions)) {
                return false;
            }
            RequestOptions requestOptions = (RequestOptions) obj;
            try {
                // We expect a RequestOptions object with the custom Magento deserializer
                // and the same headers as the list given in the constructor

                if (requestOptions.getGson() != QueryDeserializer.getGson()) {
                    return false;
                }

                List<Header> actualHeaders = requestOptions.getHeaders();

                if (headers.size() != actualHeaders.size()) {
                    return false;
                }

                for (Header header : headers) {
                    if (actualHeaders
                        .stream()
                        .noneMatch(h -> h.getName()
                            .equals(header.getName()) && h.getValue()
                                .equals(header.getValue()))) {
                        return false;
                    }
                }

                if (httpMethod != null && !httpMethod.equals(requestOptions.getHttpMethod())) {
                    return false;
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
