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

package com.adobe.cq.commerce.core.components.internal.models.v1.categorylist;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeaturedCategoryListImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private FeaturedCategoryListImpl slingModelConfigured;
    private FeaturedCategoryListImpl slingModelNotConfigured;
    private FeaturedCategoryListImpl slingModelBadId;
    private FeaturedCategoryListImpl slingModelConfiguredNoGraphqlClient;
    private List<CategoryTree> categories = new ArrayList<>();

    private static final String CATEGORY_PAGE = "/content/category-page";
    private static final String TEST_CATEGORY_PAGE_URL = "/content/pageA";
    private static final String TEST_IMAGE_URL = "https://test-url.magentosite.cloud/media/catalog/category/500_F_4437974_DbE4NRiaoRtUeivMyfPoXZFNdCnYmjPq_1.jpg";
    private static final int TEST_CATEGORY = 5;
    private static final String TEST_CATEGORY_NAME = "Equipment";
    private static final String TEST_ASSET_PATH = "/content/dam/venia/landing_page_image4.jpg";
    private static final String TEST_RENDITION_PATH = "/content/dam/venia/landing_page_image4.web.jpg";

    private static final String COMPONENT_PATH = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist";
    private static final String COMPONENT_PATH_NOCONFIG = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist2";
    private static final String COMPONENT_PATH_NOCLIENT = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist3";
    private static final String COMPONENT_PATH_BADID = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist4";

    @Rule
    public final AemContext contextConfigured = createContext("/context/jcr-content.json");

    @Rule
    public final AemContext contextNotConfigured = createContext("/context/jcr-content.json");

    @Rule
    public final AemContext contextBadId = createContext("/context/jcr-content.json");

    @Rule
    public final AemContext contextNotConfiguredClient = createContext("/context/jcr-content.json");

    @Before
    public void setup() throws Exception {

        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-category-alias-result.json");

        Stream.of(contextBadId, contextConfigured, contextNotConfigured).forEach(ctx -> {
            ctx.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> graphqlClient);
        });

        // Mock resource and resolver
        Resource resource = Mockito.spy(contextConfigured.resourceResolver().getResource(COMPONENT_PATH));
        ResourceResolver resolver = Mockito.spy(resource.getResourceResolver());
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        // Mock asset
        Resource assetResource = mock(Resource.class);
        Asset mockAsset = mock(Asset.class);
        Rendition mockRendition = mock(Rendition.class);
        when(assetResource.adaptTo(Asset.class)).thenReturn(mockAsset);
        when(mockAsset.getRendition(anyString())).thenReturn(mockRendition);
        when(mockRendition.getPath()).thenReturn(TEST_RENDITION_PATH);
        when(resolver.getResource(TEST_ASSET_PATH)).thenReturn(assetResource);

        // init sling model
        Page page = contextConfigured.currentPage(TEST_CATEGORY_PAGE_URL);
        contextConfigured.currentResource(resource);

        SlingBindings slingBindings = (SlingBindings) contextConfigured.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(resource);
        slingBindings.put(WCMBindings.WCM_MODE, new SightlyWCMMode(contextConfigured.request()));
        slingBindings.put("currentPage", page);
        slingModelConfigured = contextConfigured.request().adaptTo(FeaturedCategoryListImpl.class);

        // init not configured sling model
        resource = Mockito.spy(contextConfigured.resourceResolver().getResource(COMPONENT_PATH_NOCONFIG));
        contextNotConfigured.currentResource(resource);
        slingBindings = (SlingBindings) contextNotConfigured.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(resource);
        slingBindings.put("currentPage", page);
        slingModelNotConfigured = contextNotConfigured.request().adaptTo(FeaturedCategoryListImpl.class);

        // init bad category id sling model
        resource = Mockito.spy(contextConfigured.resourceResolver().getResource(COMPONENT_PATH_BADID));
        contextBadId.currentResource(resource);
        slingBindings = (SlingBindings) contextBadId.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(resource);
        slingBindings.put("currentPage", page);
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        slingModelBadId = contextBadId.request().adaptTo(FeaturedCategoryListImpl.class);

        // init slingmodel with no graphql client
        when(resource.adaptTo(GraphqlClient.class)).thenReturn(null);
        resource = Mockito.spy(contextConfigured.resourceResolver().getResource(COMPONENT_PATH_NOCLIENT));
        contextNotConfiguredClient.currentResource(resource);
        slingBindings = (SlingBindings) contextNotConfiguredClient.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(resource);
        slingBindings.put(WCMBindings.WCM_MODE, new SightlyWCMMode(contextNotConfiguredClient.request()));
        slingBindings.put("currentPage", page);
        slingModelConfiguredNoGraphqlClient = contextNotConfiguredClient.request().adaptTo(FeaturedCategoryListImpl.class);
    }

    @Test
    public void verifyModel() {
        Assert.assertNotNull(slingModelConfigured);
        Assert.assertTrue(slingModelConfigured.isConfigured());
        List<CategoryTree> list = slingModelConfigured.getCategories();
        Assert.assertNotNull(list);
        Assert.assertEquals(list.size(), 3);
    }

    @Test
    public void verifyCategory() {
        Assert.assertNotNull(slingModelConfigured);
        Assert.assertTrue(slingModelConfigured.isConfigured());
        categories = slingModelConfigured.getCategories();
        Assert.assertNotNull(categories);
        categories.stream().forEach(c -> Assert.assertNotNull(c));
        Assert.assertEquals(categories.get(0).getName(), TEST_CATEGORY_NAME);
        Assert.assertEquals(categories.get(0).getImage(), TEST_IMAGE_URL);
        Assert.assertEquals(categories.get(0).getPath(), String.format("%s.%s.html", CATEGORY_PAGE, TEST_CATEGORY));
    }

    @Test
    public void verifyAssetOverride() {
        categories = slingModelConfigured.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(categories.get(1).getImage(), TEST_RENDITION_PATH);
    }

    @Test
    public void verifyNotConfigured() {
        Assert.assertNotNull(slingModelNotConfigured);
        Assert.assertNull(slingModelNotConfigured.getCategoriesRetriever());
        Assert.assertFalse(slingModelNotConfigured.isConfigured());
        categories = slingModelNotConfigured.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(0, categories.size());
    }

    @Test
    public void verifyBadId() {
        Assert.assertNotNull("Sling model is not null", slingModelBadId);
        Assert.assertNotNull("Categories retriever is not null", slingModelBadId.getCategoriesRetriever());
        Assert.assertTrue("The components is configured", slingModelBadId.isConfigured());
        categories = slingModelBadId.getCategories();
        Assert.assertNotNull("The categories list is not null", categories);
        Assert.assertEquals("There are two categories in the list", 2, categories.size());
    }

    @Test
    public void verifyGraphQLClientNotConfigured() {
        Assert.assertNotNull(slingModelConfiguredNoGraphqlClient);
        Assert.assertNull(slingModelConfiguredNoGraphqlClient.getCategoriesRetriever());
        Assert.assertTrue(slingModelConfiguredNoGraphqlClient.isConfigured());
        categories = slingModelConfiguredNoGraphqlClient.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(0, categories.size());
    }

    @Test
    public void verifyIgnoreInvalidAsset() {
        categories = slingModelConfigured.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(categories.get(2).getImage(), TEST_IMAGE_URL);
    }

    private AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            context.load().json(contentPath, "/content");
            UrlProviderImpl urlProvider = new UrlProviderImpl();
            urlProvider.activate(new MockUrlProviderConfiguration());
            context.registerService(UrlProvider.class, urlProvider);
        }, ResourceResolverType.JCR_MOCK);
    }
}
