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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.reference.Reference;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static com.day.cq.commons.jcr.JcrConstants.JCR_LASTMODIFIED;

public class AssetsReferenceProviderTest {

    private static final String ASSET_PATH = "/content/dam/summit-kit-image.jpg";
    private static final String PRODUCTTEASER_SIMPLE = "/content/pageA/jcr:content/root/responsivegrid/productteaser-simple";

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("cq:graphqlClient", "default", "magentoStore", "my-store"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            // Load page structure
            context.load().json(contentPath, "/content");

            UrlProviderImpl urlProvider = new UrlProviderImpl();
            urlProvider.activate(new MockUrlProviderConfiguration());
            context.registerService(UrlProvider.class, urlProvider);
        }, ResourceResolverType.JCR_MOCK);
    }

    private final AssetsReferenceProvider assetsReferenceProvider = new AssetsReferenceProvider();

    private Resource teaserResource;

    @Before
    public void setUp() throws Exception {
        // Mock asset
        Asset asset = Mockito.mock(Asset.class);
        Mockito.when(asset.getPath()).thenReturn(ASSET_PATH);

        // Mock rendition
        Rendition rendition = Mockito.mock(Rendition.class);
        Mockito.when(rendition.getAsset()).thenReturn(asset);

        // Mock image resource
        Calendar calendar = Mockito.mock(Calendar.class);
        Mockito.when(calendar.getTimeInMillis()).thenReturn(new Date().getTime());
        ValueMap valueMap = Mockito.mock(ValueMap.class);
        Mockito.when(valueMap.get(JCR_LASTMODIFIED, Calendar.class)).thenReturn(calendar);
        Resource imageResource = Mockito.mock(Resource.class);
        Mockito.when(imageResource.adaptTo(Asset.class)).thenReturn(asset);
        Mockito.when(imageResource.adaptTo(Rendition.class)).thenReturn(rendition);
        Mockito.when(imageResource.getValueMap()).thenReturn(valueMap);
        Mockito.when(imageResource.getPath()).thenReturn(ASSET_PATH);

        // Mock ResourceResolver
        ResourceResolver resourceResolver = Mockito.spy(context.resourceResolver());
        Mockito.when(resourceResolver.resolve(ASSET_PATH)).thenReturn(imageResource);

        teaserResource = Mockito.spy(resourceResolver.getResource(PRODUCTTEASER_SIMPLE));
        context.currentResource(teaserResource);
        Mockito.when(teaserResource.getResourceResolver()).thenReturn(resourceResolver);
        Mockito.when(teaserResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom(
            "graphql/magento-graphql-productteaser-result.json");
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);
    }

    @Test
    public void testProductTeaserWithAemAsset() {
        List<Reference> referenceList = assetsReferenceProvider.findReferences(teaserResource);
        Assert.assertNotNull(referenceList);
        Assert.assertEquals(1, referenceList.size());
        Assert.assertEquals(ASSET_PATH, referenceList.get(0).getResource().getPath());
    }
}
