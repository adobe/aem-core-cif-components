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
package com.adobe.cq.commerce.core.components.internal.models.v1.storeconfigexporter;

import java.io.IOException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.MockLaunch;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.launches.api.Launch;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreConfigExporterTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("magentoGraphqlEndpoint", "/my/api/graphql", "magentoStore", "my-magento-store", "enableUIDSupport", "true",
            "cq:graphqlClient",
            "my-graphql-client", "httpHeaders", new String[] { "customHeader-1=value1", "customHeader-2=value2" }));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                context.load().json(contentPath, "/content");
                context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                    (Function<Resource, ComponentsConfiguration>) input -> input.getPath().contains("pageH")
                        ? MOCK_CONFIGURATION_OBJECT
                        : ComponentsConfiguration.EMPTY);
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Test
    public void testStoreView() {
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("my-magento-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewOnLaunchPage() {
        context.registerAdapter(Resource.class, Launch.class, (Function<Resource, Launch>) resource -> new MockLaunch(resource));

        setupWithPage("/content/launches/2020/09/14/mylaunch/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("my-magento-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewDefault() {
        setupWithPage("/content/pageD", HttpMethod.POST);

        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("default", storeConfigExporter.getStoreView());
    }

    @Test
    public void testGraphqlEndpoint() {
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("/my/api/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlEndpointDefault() {
        setupWithPage("/content/pageD", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("/api/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlMethodGet() {
        setupWithPage("/content/pageH", HttpMethod.GET);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("GET", storeConfigExporter.getMethod());
    }

    @Test
    public void testGraphqlMethodPost() {
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("POST", storeConfigExporter.getMethod());
    }

    @Test
    public void testGetStoreRootUrl() {
        setupWithPage("/content/pageB/pageC", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("/content/pageB.html", storeConfigExporter.getStoreRootUrl());
    }

    @Test
    public void testCustomHttpHeaders() throws IOException {
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        String expectedHeaders = "{\"Store\":\"my-magento-store\",\"customHeader-1\":\"value1\",\"customHeader-2\":\"value2\"}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualNode = mapper.readTree(storeConfigExporter.getHttpHeaders());
        JsonNode expectedNode = mapper.readTree(expectedHeaders);

        Assert.assertEquals("The custom HTTP headers are correctly parsed", expectedNode, actualNode);
    }

    private void setupWithPage(String pagePath, HttpMethod method) {
        Page page = context.pageManager().getPage(pagePath);
        context.request().setResource(page.getContentResource());
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.setResource(page.getContentResource());

        GraphqlClientConfiguration graphqlClientConfiguration = mock(GraphqlClientConfiguration.class);
        when(graphqlClientConfiguration.httpMethod()).thenReturn(method);
        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        when(graphqlClient.getConfiguration()).thenReturn(graphqlClientConfiguration);

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);
    }
}
