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
package com.adobe.cq.commerce.core.search.internal.services;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.testing.GraphqlQueryMatcher;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.__InputValue;
import com.adobe.cq.commerce.magento.graphql.__Type;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchFilterServiceImplTest {

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                (Function<Resource, ComponentsConfiguration>) input -> MOCK_CONFIGURATION_OBJECT);
        })
        .build();

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static final String PAGE = "/content/pageA";

    @Mock
    CloseableHttpClient httpClient;

    SearchFilterServiceImpl searchFilterServiceUnderTest;
    Page page;
    Resource pageResource;
    GraphqlClient graphqlClient;
    private Logger logger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() throws IOException {
        searchFilterServiceUnderTest = context.registerInjectActivateService(new SearchFilterServiceImpl());
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        graphqlClient = new GraphqlClientImpl();

        // Create additionalConfig map
        Map<String, Object> additionalConfig = new HashMap<>();
        additionalConfig.put("httpMethod", "GET");
        Utils.registerGraphqlClient(context, graphqlClient, additionalConfig);

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");

        page = Mockito.spy(context.currentPage(PAGE));
        pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);

        logger = (Logger) LoggerFactory.getLogger(SearchFilterServiceImpl.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(originalLevel);
    }

    @Test
    public void testRetrieveMetadata() {
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);

        final List<FilterAttributeMetadata> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFilters(page);

        assertThat(filterAttributeMetadata).hasSize(29);

        // Range type
        FilterAttributeMetadata price = filterAttributeMetadata.stream().filter(f -> f.getAttributeCode().equals("price")).findFirst()
            .get();
        assertThat(price.getFilterInputType()).isEqualTo("FilterRangeTypeInput");
        assertThat(price.getAttributeType()).isEqualTo("Float");
        assertThat(price.getAttributeInputType()).isEqualTo("price");

        // Equal type for string
        FilterAttributeMetadata material = filterAttributeMetadata.stream().filter(f -> f.getAttributeCode().equals("material")).findFirst()
            .get();
        assertThat(material.getFilterInputType()).isEqualTo("FilterEqualTypeInput");
        assertThat(material.getAttributeType()).isEqualTo("String");
        assertThat(material.getAttributeInputType()).isEqualTo("multiselect");

        // Equal type for int/boolean
        FilterAttributeMetadata newAttr = filterAttributeMetadata.stream().filter(f -> f.getAttributeCode().equals("new")).findFirst()
            .get();
        assertThat(newAttr.getFilterInputType()).isEqualTo("FilterEqualTypeInput");
        assertThat(newAttr.getAttributeType()).isEqualTo("Int");
        assertThat(newAttr.getAttributeInputType()).isEqualTo("boolean");
    }

    @Test
    public void testRetrieveMetadataEnforcedPost() throws IOException {
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);

        MockOsgi.deactivate(searchFilterServiceUnderTest, context.bundleContext());
        MockOsgi.activate(searchFilterServiceUnderTest, context.bundleContext(), "enforcePost", Boolean.TRUE);

        final List<FilterAttributeMetadata> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFilters(page);

        assertThat(filterAttributeMetadata).hasSize(29);

        verify(httpClient).execute(argThat(new GraphqlQueryMatcher("{__type", "GET")), any(ResponseHandler.class));
        verify(httpClient).execute(argThat(new GraphqlQueryMatcher("{customAttributeMetadata", "post")), any(ResponseHandler.class));
    }

    @Test
    public void testFilterQueriesReturnNull() {
        // We want to make sure that components will not fail if the __type and/or customAttributeMetadata fields are null
        // For example, 3rd-party integrations might not support this immediately

        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);

        Query query = new Query();
        GraphqlResponse<Object, Object> response = new GraphqlResponse<Object, Object>();
        response.setData(query);
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        final List<FilterAttributeMetadata> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFilters(page);
        assertThat(filterAttributeMetadata).hasSize(0);
        verify(graphqlClient).execute(any(), any(), any(), any());
    }

    @Test
    public void testNullMagentoClient() {
        context.registerAdapter(Resource.class, GraphqlClient.class, (GraphqlClient) null);

        final List<FilterAttributeMetadata> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFilters(page);

        assertThat(filterAttributeMetadata).isEmpty();
    }

    @Test
    public void testGraphqlResponsesWithErrors() {
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);

        Query query = new Query();
        GraphqlResponse<Object, Object> response = new GraphqlResponse<Object, Object>();
        response.setData(query);
        Error error = new Error();
        response.setErrors(Collections.singletonList(error));
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        final List<FilterAttributeMetadata> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFilters(page);
        assertThat(filterAttributeMetadata).hasSize(0);
    }

    @Test
    public void testRetrieveCurrentlyAvailableCommerceFiltersInfoWithErrors() {
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);

        Query query = new Query();
        GraphqlResponse<Object, Object> response = new GraphqlResponse<Object, Object>();
        response.setData(query);
        Error error = new Error();
        response.setErrors(Collections.singletonList(error));
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        SlingHttpServletRequest request = context.request();

        final Pair<List<FilterAttributeMetadata>, List<Error>> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFiltersInfo(request, page);
        assertThat(filterAttributeMetadata.getLeft()).hasSize(0);
        assertThat(filterAttributeMetadata.getRight()).containsExactly(error);
        verify(graphqlClient).execute(any(), any(), any(), any());
        assertResponseErrorsLoggedAtDebug(1);
    }

    @Test
    public void testEmptyFiltersSkipMetadataEvenWithEnforcedPost() {
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        context.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);
        MockOsgi.deactivate(searchFilterServiceUnderTest, context.bundleContext());
        MockOsgi.activate(searchFilterServiceUnderTest, context.bundleContext(), "enforcePost", Boolean.TRUE);
        Query data = Mockito.mock(Query.class);
        __Type type = Mockito.mock(__Type.class);
        when(data.__getType()).thenReturn(type);
        when(type.getInputFields()).thenReturn(Collections.emptyList());
        GraphqlResponse<Object, Object> response = new GraphqlResponse<>();
        response.setData(data);
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        Pair<List<FilterAttributeMetadata>, List<Error>> result = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFiltersInfo(context.request(), page);

        assertThat(result.getLeft()).isEmpty();
        assertThat(result.getRight()).isEmpty();
        verify(graphqlClient).execute(any(), any(), any(), any());
        assertResponseErrorsLoggedAtDebug(0);
    }

    @Test
    public void testMetadataErrorsLoggedAtDebug() {
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        context.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);
        Query data = Mockito.mock(Query.class);
        __Type type = Mockito.mock(__Type.class);
        __InputValue filter = Mockito.mock(__InputValue.class);
        when(data.__getType()).thenReturn(type);
        when(type.getInputFields()).thenReturn(Collections.singletonList(filter));
        when(filter.getName()).thenReturn("color");
        when(filter.getType()).thenReturn(type);
        when(type.getName()).thenReturn("FilterEqualTypeInput");
        GraphqlResponse<Query, Error> introspection = new GraphqlResponse<>();
        introspection.setData(data);
        GraphqlResponse<Query, Error> metadata = new GraphqlResponse<>();
        Error error = new Error();
        error.setMessage("Metadata unavailable");
        error.setCategory("graphql-input");
        metadata.setErrors(Collections.singletonList(error));
        when(graphqlClient.<Query, Error>execute(any(), Mockito.eq(Query.class), Mockito.eq(Error.class), any()))
            .thenReturn(introspection, metadata);

        Pair<List<FilterAttributeMetadata>, List<Error>> result = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFiltersInfo(context.request(), page);

        assertThat(result.getRight()).containsExactly(error);
        verify(graphqlClient, Mockito.times(2)).execute(any(), any(), any(), any());
        assertResponseErrorsLoggedAtDebug(1);
    }

    private void assertResponseErrorsLoggedAtDebug(long count) {
        assertThat(appender.list.stream().filter(event -> event.getLevel() == Level.ERROR).count()).isZero();
        assertThat(appender.list.stream()
            .filter(event -> event.getLevel() == Level.DEBUG && event.getFormattedMessage().startsWith("An error has occurred: "))
            .count()).isEqualTo(count);
    }
}
