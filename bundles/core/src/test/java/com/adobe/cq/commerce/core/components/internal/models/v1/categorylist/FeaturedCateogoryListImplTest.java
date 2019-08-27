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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeaturedCateogoryListImplTest {

    private GraphqlClient graphqlClient;
    private FeaturedCategoryListImpl slingModel;
    private Query rootQuery;
    private List<CategoryInterface> categories = new ArrayList<CategoryInterface>();
    private static final String TEST_CATEGORY_PAGE_URL = "/content/test-category-page";
    private static final String TEST_IMAGE_URL = "https://test-url.magentosite.cloud/media/catalog/category/500_F_4437974_DbE4NRiaoRtUeivMyfPoXZFNdCnYmjPq_1.jpg";
    private static final int TEST_CATEGORY = 3;
    private static final String TEST_CATEGORY_NAME = "Equipment";

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    @Before
    public void setup() throws Exception {
        String json = IOUtils.toString(
            this.getClass().getResourceAsStream("/graphql/magento-graphql-singlecategory-result.json"),
            StandardCharsets.UTF_8);
        rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        Page categoryPage = mock(Page.class);
        when(categoryPage.getLanguage(false)).thenReturn(Locale.US);
        when(categoryPage.getPath()).thenReturn(TEST_CATEGORY_PAGE_URL);
        Resource resource = Mockito.spy(context.resourceResolver().getResource("/content/pageA"));
        graphqlClient = Mockito.mock(GraphqlClient.class);
        Page page = mock(Page.class);
        MockSlingHttpServletRequest request = context.request();
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.WCM_MODE, new SightlyWCMMode(request));
        slingBindings.put("currentPage", page);
        slingBindings.put("resource", resource);
        Integer categoryId[] = { TEST_CATEGORY };
        Map<String, Object> pageProperties = new HashMap<>();
        pageProperties.put("categoryIds", categoryId);
        ValueMapDecorator vMap = new ValueMapDecorator(pageProperties);
        slingBindings.put("currentPage", categoryPage);
        slingBindings.put("properties", vMap);
        GraphqlResponse<Query, Error> response = mock(GraphqlResponse.class);
        Mockito.when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(graphqlClient.execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn((GraphqlResponse) response);
        when(response.getData()).thenReturn(rootQuery);
        slingModel = request.adaptTo(FeaturedCategoryListImpl.class);
    }

    @Test
    public void verifyModel() {
        Assert.assertNotNull(slingModel);
        List<CategoryInterface> list = slingModel.getCategories();
        Assert.assertNotNull(list);
        Assert.assertEquals(list.size(), 1);
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

    private AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            context.load().json(contentPath, "/content");
        }, ResourceResolverType.JCR_MOCK);
    }
}