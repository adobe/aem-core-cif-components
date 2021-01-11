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

package com.adobe.cq.commerce.core.components.internal.models.v1.storeconfigexporter;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MockExternalizer;
import com.adobe.cq.commerce.core.components.client.MockLaunch;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.launches.api.Launch;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreConfigExporterTest {

    private static final ValueMap MOCK_CONFIGURATION_1 = new ValueMapDecorator(
        ImmutableMap.of("magentoGraphqlEndpoint", "/my/magento/graphql", "magentoStore", "my-magento-store", "cq:graphqlClient",
            "my-graphql-client"));
    private static final ValueMap MOCK_CONFIGURATION_2 = new ValueMapDecorator(
        ImmutableMap.of("magentoGraphqlEndpoint", "/my/magento/graphql", "magentoStore", "my-magento-store", "cq:graphqlClient",
            "my-graphql-client", "usePublishGraphqlEndpoint", true));
    private static final ValueMap MOCK_CONFIGURATION_3 = new ValueMapDecorator(
        ImmutableMap.of("magentoGraphqlEndpoint", "https://www.magento.com/my/magento/graphql", "magentoStore", "my-magento-store",
            "cq:graphqlClient", "my-graphql-client", "usePublishGraphqlEndpoint", true));
    private static final ValueMap MOCK_CONFIGURATION_4 = new ValueMapDecorator(
        ImmutableMap.of("magentoGraphqlEndpoint", "//localhost:3002/graphql", "magentoStore", "my-magento-store",
            "cq:graphqlClient", "my-graphql-client", "usePublishGraphqlEndpoint", true));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT_1 = new ComponentsConfiguration(MOCK_CONFIGURATION_1);
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT_2 = new ComponentsConfiguration(MOCK_CONFIGURATION_2);
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT_3 = new ComponentsConfiguration(MOCK_CONFIGURATION_3);
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT_4 = new ComponentsConfiguration(MOCK_CONFIGURATION_4);

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                context.load().json(contentPath, "/content");
                context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                    (Function<Resource, ComponentsConfiguration>) input -> {
                        if (input.getPath().contains("pageH")) {
                            return MOCK_CONFIGURATION_OBJECT_1;
                        } else if (input.getPath().contains("pageI")) {
                            return MOCK_CONFIGURATION_OBJECT_2;
                        } else if (input.getPath().contains("pageJ")) {
                            return MOCK_CONFIGURATION_OBJECT_3;
                        } else if (input.getPath().contains("pageK")) {
                            return MOCK_CONFIGURATION_OBJECT_4;
                        } else {
                            return ComponentsConfiguration.EMPTY;
                        }
                    });

                context.registerService(Externalizer.class, new MockExternalizer());
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
    public void testGraphqlEndpointUsePublishOnAuthor() {
        context.runMode("author");
        setupWithPage("/content/pageI", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("https://publish/my/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlEndpointUseNoPublishOnAuthor() {
        context.runMode("author");
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("/my/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlEndpointUsePublishOnPublish() {
        context.runMode("publish");
        setupWithPage("/content/pageI", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("/my/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlEndpointUseNoPublishOnPublish() {
        context.runMode("publish");
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("/my/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testAbsolutGraphqlEndpointUsePublishOnAuthor1() {
        context.runMode("publish");
        setupWithPage("/content/pageJ", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("https://www.magento.com/my/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testAbsolutGraphqlEndpointUsePublishOnAuthor2() {
        context.runMode("publish");
        setupWithPage("/content/pageK", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("//localhost:3002/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlEndpointDefault() {
        setupWithPage("/content/pageD", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
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

    private void setupWithPage(String pagePath, HttpMethod method) {
        Page page = context.pageManager().getPage(pagePath);
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
