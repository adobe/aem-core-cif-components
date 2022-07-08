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

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.MockLaunch;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.launches.api.Launch;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreConfigExporterImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        new ImmutableMap.Builder<String, Object>()
            .put("magentoGraphqlEndpoint", "/my/api/graphql")
            .put("magentoStore", "my-magento-store")
            .put("enableUIDSupport", "true")
            .put("cq:graphqlClient", "my-graphql-client")
            .put("httpHeaders", new String[] { "customHeader-1=value1", "customHeader-2=value2" })
            .put("jcr:language", "de_de")
            .put("enableClientSidePriceLoading", true).build());

    @Rule
    public final AemContext context = TestContext.newAemContext();

    private ComponentsConfiguration mockConfiguration = ComponentsConfiguration.EMPTY;

    @Before
    public void setup() {
        context.load().json("/context/jcr-content.json", "/content");
        context.registerAdapter(Resource.class, ComponentsConfiguration.class,
            (Function<Resource, ComponentsConfiguration>) input -> mockConfiguration);
    }

    @Test
    public void testStoreView() {
        mockConfiguration = new ComponentsConfiguration(MOCK_CONFIGURATION);
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("my-magento-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewEmptyWithoutConfiguration() {
        mockConfiguration = null;
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertNotNull(storeConfigExporter);
        Assert.assertNull(null, storeConfigExporter.getStoreView());
        assertEquals("/api/graphql", storeConfigExporter.getGraphqlEndpoint());
        assertNotNull(storeConfigExporter.getHttpHeaders());
        assertTrue(storeConfigExporter.getHttpHeaders().isEmpty());
        assertEquals("POST", storeConfigExporter.getMethod());
    }

    @Test
    public void testStoreViewOnLaunchPage() {
        mockConfiguration = new ComponentsConfiguration(MOCK_CONFIGURATION);
        context.registerAdapter(Resource.class, Launch.class, (Function<Resource, Launch>) MockLaunch::new);
        setupWithPage("/content/launches/2020/09/14/mylaunch/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("my-magento-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewDefault() {
        setupWithPage("/content/pageD", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        Assert.assertNull(storeConfigExporter.getStoreView());
    }

    @Test
    public void testGraphqlEndpoint() {
        mockConfiguration = new ComponentsConfiguration(MOCK_CONFIGURATION);
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("/my/api/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlEndpointDefault() {
        setupWithPage("/content/pageD", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("/api/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlMethodGet() {
        mockConfiguration = new ComponentsConfiguration(MOCK_CONFIGURATION);
        setupWithPage("/content/pageH", HttpMethod.GET);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("GET", storeConfigExporter.getMethod());
    }

    @Test
    public void testGraphqlMethodPost() {
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("POST", storeConfigExporter.getMethod());
    }

    @Test
    public void testGetStoreRootUrl() {
        setupWithPage("/content/pageB/pageC", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("/content/pageB.html", storeConfigExporter.getStoreRootUrl());
    }

    @Test
    public void testGetStoreRootUrlWithMapping() {
        context.create().resource("/etc/map/http/localhost.80", "sling:internalRedirect", new String[] { "/", "/content" });
        // update MapEntries as we don't have support for jcr events
        context.getService(EventAdmin.class).sendEvent(new Event(
            "org/apache/sling/api/resource/Resource/ADDED",
            Collections.singletonMap("path", "/etc/map/http/localhost.80")));
        setupWithPage("/content/pageB/pageC", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("/pageB.html", storeConfigExporter.getStoreRootUrl());
    }

    @Test
    public void testGetStoreRootUrlWithMappingInDifferentDomain() {
        context.create().resource("/etc/map/http/foobar.80", "sling:internalRedirect", new String[] { "/", "/content" });
        // update MapEntries as we don't have support for jcr events
        context.getService(EventAdmin.class).sendEvent(new Event(
            "org/apache/sling/api/resource/Resource/ADDED",
            Collections.singletonMap("path", "/etc/map/http/foobar.80")));
        setupWithPage("/content/pageB/pageC", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("/content/pageB.html", storeConfigExporter.getStoreRootUrl());
    }

    @Test
    public void testCustomHttpHeaders() {
        mockConfiguration = new ComponentsConfiguration(MOCK_CONFIGURATION);
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);

        Map<String, String[]> headers = storeConfigExporter.getHttpHeaders();
        assertArrayEquals(new String[] { "my-magento-store" }, headers.get("Store"));
        assertArrayEquals(new String[] { "value1" }, headers.get("customHeader-1"));
        assertArrayEquals(new String[] { "value2" }, headers.get("customHeader-2"));
    }

    @Test
    public void testLanguage() {
        mockConfiguration = new ComponentsConfiguration(MOCK_CONFIGURATION);
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertEquals("de-de", storeConfigExporter.getLanguage());
    }

    @Test
    public void testEnableClientSidePriceLoading() {
        mockConfiguration = new ComponentsConfiguration(MOCK_CONFIGURATION);
        setupWithPage("/content/pageH", HttpMethod.POST);
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        assertNotNull(storeConfigExporter);
        assertTrue(storeConfigExporter.isClientSidePriceLoadingEnabled());
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
