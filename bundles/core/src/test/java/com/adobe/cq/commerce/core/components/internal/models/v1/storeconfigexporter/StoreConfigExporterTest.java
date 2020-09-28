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

import com.adobe.cq.commerce.core.components.client.MockLaunch;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.launches.api.Launch;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class StoreConfigExporterTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("magentoGraphqlEndpoint", "/my/magento/graphql", "magentoStore", "my-magento-store"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                context.load().json(contentPath, "/content");
                context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                    (Function<Resource, ComponentsConfiguration>) input -> input.getValueMap().get("cq:conf", String.class) != null
                        ? MOCK_CONFIGURATION_OBJECT
                        : ComponentsConfiguration.EMPTY);
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Test
    public void testStoreView() {
        setupWithPage("/content/pageH");
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("my-magento-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewOnLaunchPage() {
        context.registerAdapter(Resource.class, Launch.class, (Function<Resource, Launch>) resource -> new MockLaunch(resource));

        setupWithPage("/content/launches/2020/09/14/mylaunch/content/pageH");
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("my-magento-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewDefault() {
        setupWithPage("/content/pageD");

        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("default", storeConfigExporter.getStoreView());
    }

    @Test
    public void testGraphqlEndpoint() {
        setupWithPage("/content/pageH");
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("/my/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlEndpointDefault() {
        setupWithPage("/content/pageD");
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);
        Assert.assertEquals("/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    private void setupWithPage(String pagetPath) {
        Page page = context.pageManager().getPage(pagetPath);
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

    }
}
