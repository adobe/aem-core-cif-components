/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.models.v1.categorylist;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.internal.models.v1.AssetsProvider;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class CategoryListAssetsProviderTest {

    private static final String COMPONENT_PATH = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist";
    private static final String PRODUCT = "/content/pageA/jcr:content/root/responsivegrid/product";

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("cq:graphqlClient", "default", "magentoStore", "my-store"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private final AssetsProvider categoryListAssetsProvider = new CategoryListAssetsProvider();

    private Resource categoryListResource;

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            context.load().json(contentPath, "/content");
            UrlProviderImpl urlProvider = new UrlProviderImpl();
            urlProvider.activate(new MockUrlProviderConfiguration());
            context.registerService(UrlProvider.class, urlProvider);
        }, ResourceResolverType.JCR_MOCK);
    }

    @Before
    public void setup() throws Exception {
        categoryListResource = Mockito.spy(context.resourceResolver().getResource(COMPONENT_PATH));
        context.currentResource(categoryListResource);
        Mockito.when(categoryListResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-category-alias-result.json");
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);
    }

    @Test
    public void testCanHandle() {
        Assert.assertTrue("Expected resource not handled", categoryListAssetsProvider.canHandle(categoryListResource));

        Resource productResource = context.resourceResolver().getResource(PRODUCT);
        Assert.assertFalse("Unexpected resource handled", categoryListAssetsProvider.canHandle(productResource));
    }

    @Test
    public void testAddAssetPaths() {
        Set<String> assets = new HashSet<>();
        categoryListAssetsProvider.addAssetPaths(categoryListResource, assets);
        Assert.assertEquals("Wrong number of assets returned", 1, assets.size());
        Assert.assertEquals("Wrong asset path returned", "/content/dam/equipment-image.jpg", assets.toArray()[0]);
    }
}
