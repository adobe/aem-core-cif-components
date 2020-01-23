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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeaturedCateogoryListImplTest {

    private FeaturedCategoryListImpl slingModel;
    private Query rootQuery;
    private List<CategoryTree> categories = new ArrayList<>();
    private static final String TEST_CATEGORY_PAGE_URL = "/content/pageA";
    private static final String TEST_IMAGE_URL = "https://test-url.magentosite.cloud/media/catalog/category/500_F_4437974_DbE4NRiaoRtUeivMyfPoXZFNdCnYmjPq_1.jpg";
    private static final int TEST_CATEGORY = 5;
    private static final String TEST_CATEGORY_NAME = "Equipment";
    private static final String TEST_ASSET_PATH = "/content/dam/venia/landing_page_image4.jpg";
    private static final String TEST_RENDITION_PATH = "/content/dam/venia/landing_page_image4.web.jpg";

    private static final String COMPONENT_PATH = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist";

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    @Before
    public void setup() throws Exception {
        // Mock GraphQL response
        String json = IOUtils.toString(
            this.getClass().getResourceAsStream("/graphql/magento-graphql-category-alias-result.json"),
            StandardCharsets.UTF_8);
        rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        GraphqlResponse<Query, Error> response = mock(GraphqlResponse.class);
        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        when(graphqlClient.execute(any(), any(), any(), any()))
            .thenReturn((GraphqlResponse) response);
        when(response.getData()).thenReturn(rootQuery);

        // Mock resource and resolver
        Resource resource = Mockito.spy(context.resourceResolver().getResource(COMPONENT_PATH));
        ResourceResolver resolver = Mockito.spy(resource.getResourceResolver());
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        // Mock asset
        Resource assetResource = mock(Resource.class);
        Asset mockAsset = mock(Asset.class);
        Rendition mockRendition = mock(Rendition.class);
        when(assetResource.adaptTo(Asset.class)).thenReturn(mockAsset);
        when(mockAsset.getRendition(anyString())).thenReturn(mockRendition);
        when(mockRendition.getPath()).thenReturn(TEST_RENDITION_PATH);
        when(resolver.getResource(TEST_ASSET_PATH)).thenReturn(assetResource);

        Page page = context.currentPage(TEST_CATEGORY_PAGE_URL);
        context.currentResource(COMPONENT_PATH);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(resource);
        slingBindings.put(WCMBindings.WCM_MODE, new SightlyWCMMode(context.request()));
        slingBindings.put("currentPage", page);

        slingModel = context.request().adaptTo(FeaturedCategoryListImpl.class);
    }

    @Test
    public void verifyModel() {
        Assert.assertNotNull(slingModel);
        List<CategoryTree> list = slingModel.getCategories();
        Assert.assertNotNull(list);
        Assert.assertEquals(list.size(), 3);
    }

    @Test
    public void verifyCategory() {
        categories = slingModel.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(categories.get(0).getName(), TEST_CATEGORY_NAME);
        Assert.assertEquals(categories.get(0).getImage(), TEST_IMAGE_URL);
        Assert.assertEquals(categories.get(0).getPath(),
            String.format("%s.%s.html", TEST_CATEGORY_PAGE_URL, TEST_CATEGORY));
    }

    @Test
    public void verifyAssetOverride() {
        categories = slingModel.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(categories.get(1).getImage(), TEST_RENDITION_PATH);
    }

    @Test
    public void verifyIgnoreInvalidAsset() {
        categories = slingModel.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(categories.get(2).getImage(), TEST_IMAGE_URL);
    }

    private AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            context.load().json(contentPath, "/content");
        }, ResourceResolverType.JCR_MOCK);
    }
}
