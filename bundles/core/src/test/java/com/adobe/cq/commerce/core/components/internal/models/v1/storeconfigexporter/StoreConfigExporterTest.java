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

import java.util.Collections;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreConfigExporterTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private ConfigurationBuilder configurationBuilder;

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Before
    public void setup() {
        Page page = mock(Page.class);
        configurationBuilder = mock(ConfigurationBuilder.class, RETURNS_DEEP_STUBS);
        when(configurationBuilder.name(any()).asValueMap()).thenReturn(new ValueMapDecorator(ImmutableMap.of("magentoStore",
            "my-magento-store", "magentoGraphqlEndpoint", "/my/magento/graphql")));
        when(page.adaptTo(ConfigurationBuilder.class)).thenReturn(configurationBuilder);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
    }

    @Test
    public void testStoreView() {
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("my-magento-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewDefault() {
        when(configurationBuilder.name(any()).asValueMap()).thenReturn(new ValueMapDecorator(
            Collections.emptyMap()));

        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("default", storeConfigExporter.getStoreView());
    }

    @Test
    public void testGraphqlEndpoint() {
        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("/my/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

    @Test
    public void testGraphqlEndpointDefault() {
        when(configurationBuilder.name(any()).asValueMap()).thenReturn(new ValueMapDecorator(
            Collections.emptyMap()));

        StoreConfigExporterImpl storeConfigExporter = context.request().adaptTo(StoreConfigExporterImpl.class);

        Assert.assertEquals("/magento/graphql", storeConfigExporter.getGraphqlEndpoint());
    }

}
