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
package com.adobe.cq.commerce.core.components.internal.models.v1.categorylist;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractRetriever;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeaturedCategoryListImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private FeaturedCategoryListImpl featuredCategoryList;
    private List<CategoryTree> categories = new ArrayList<>();

    private static final String CATEGORY_PAGE = "/content/category-page";
    private static final String PAGE = "/content/pageA";
    private static final String TEST_IMAGE_URL = "https://test-url.magentosite.cloud/media/catalog/category/500_F_4437974_DbE4NRiaoRtUeivMyfPoXZFNdCnYmjPq_1.jpg";
    private static final String TEST_URL_PATH = "equipment";
    private static final String TEST_CATEGORY_NAME = "Equipment";
    private static final String TEST_ASSET_PATH = "/content/dam/venia/landing_page_image4.jpg";
    private static final String TEST_RENDITION_PATH = "/content/dam/venia/landing_page_image4.web.jpg";

    private static final String COMPONENT_PATH = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist";
    private static final String COMPONENT_PATH_NOCONFIG = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist2";
    private static final String COMPONENT_PATH_NOCLIENT = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist3";
    private static final String COMPONENT_PATH_FALLBACK_ID_TYPE = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorylist4";
    private static final String COMPONENT_PATH_LINK_TARGET_UNCHECKED = "/content/pageA/jcr:content/root/responsivegrid/productcarousel_with_link_target_unchecked";
    private static final String COMPONENT_PATH_LINK_TARGET_CHECKED = "/content/pageA/jcr:content/root/responsivegrid/productcarousel_with_link_target_checked";
    private static final String COMPONENT_PATH_WITH_URL_PATH = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorywithurlpathlist3";
    private static final String COMPONENT_PATH_WITH_MULTIPLE_URL_PATH = "/content/pageA/jcr:content/root/responsivegrid/featuredcategorywithurlpathlist4";

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
            Utils.addDataLayerConfig(mockConfigBuilder, true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
        })
        .build();

    private void setupTest(String componentPath) throws Exception {
        setupTest(componentPath, false);
    }

    private void setupTest(String componentPath, boolean withNullGraphqlClient) throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));
        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Utils.registerGraphqlClient(context, graphqlClient, null);

        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid:{in:[\"uid-5");
        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path:{in:[\"equipment");

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> withNullGraphqlClient
            ? null
            : graphqlClient);

        // Mock resource and resolver
        Resource resource = Mockito.spy(context.resourceResolver().getResource(componentPath));
        ResourceResolver resolver = Mockito.spy(resource.getResourceResolver());
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        // Mock asset
        Resource assetResource = mock(Resource.class);
        Asset mockAsset = mock(Asset.class);
        Rendition mockRendition = mock(Rendition.class);
        when(assetResource.adaptTo(Asset.class)).thenReturn(mockAsset);
        when(mockAsset.getRendition(anyString())).thenReturn(mockRendition);
        when(mockRendition.getPath()).thenReturn(TEST_RENDITION_PATH);
        when(resolver.getResource(TEST_ASSET_PATH)).thenReturn(assetResource);

        Page page = Mockito.spy(context.currentPage(PAGE));
        Resource pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        context.currentResource(resource);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(resource);
        slingBindings.put(WCMBindings.WCM_MODE, new SightlyWCMMode(context.request()));
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_STYLE, style);

        featuredCategoryList = context.request().adaptTo(FeaturedCategoryListImpl.class);
    }

    @Test
    public void verifyModel() throws Exception {
        setupTest(COMPONENT_PATH);

        Assert.assertNotNull(featuredCategoryList);
        Assert.assertTrue(featuredCategoryList.isConfigured());
        Assert.assertEquals("h2", featuredCategoryList.getTitleType());
        List<CategoryTree> list = featuredCategoryList.getCategories();
        Assert.assertNotNull(list);
        Assert.assertEquals(list.size(), 3);
        Assert.assertNull(featuredCategoryList.getLinkTarget());
    }

    @Test
    public void verifyCategory() throws Exception {
        setupTest(COMPONENT_PATH);

        Assert.assertNotNull(featuredCategoryList);
        Assert.assertTrue(featuredCategoryList.isConfigured());
        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);
        categories.stream().forEach(c -> Assert.assertNotNull(c));
        Assert.assertEquals(categories.get(0).getName(), TEST_CATEGORY_NAME);
        Assert.assertEquals(categories.get(0).getImage(), TEST_IMAGE_URL);
        Assert.assertEquals(categories.get(0).getPath(), String.format("%s.html/%s.html", CATEGORY_PAGE, TEST_URL_PATH));
    }

    @Test
    public void verifyAssetOverride() throws Exception {
        setupTest(COMPONENT_PATH);

        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(categories.get(1).getImage(), TEST_RENDITION_PATH);
    }

    @Test
    public void verifyNotConfigured() throws Exception {
        setupTest(COMPONENT_PATH_NOCONFIG);

        Assert.assertNotNull(featuredCategoryList);
        Assert.assertNull(featuredCategoryList.getCategoriesRetriever());
        Assert.assertFalse(featuredCategoryList.isConfigured());
        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(0, categories.size());
    }

    @Test
    public void verifyGraphQLClientNotConfigured() throws Exception {
        setupTest(COMPONENT_PATH_NOCLIENT, true);

        Assert.assertNotNull(featuredCategoryList);
        Assert.assertNull(featuredCategoryList.getCategoriesRetriever());
        Assert.assertTrue(featuredCategoryList.isConfigured());
        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(0, categories.size());
    }

    @Test
    public void verifyIgnoreInvalidAsset() throws Exception {
        setupTest(COMPONENT_PATH);

        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(categories.get(2).getImage(), TEST_IMAGE_URL);
    }

    @Test
    public void testFallbackCategoryType() throws Exception {
        setupTest(COMPONENT_PATH_FALLBACK_ID_TYPE);
        AbstractRetriever retriever = featuredCategoryList.getCategoriesRetriever();

        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);

        Field retrieverQueryField = AbstractRetriever.class.getDeclaredField("query");
        retrieverQueryField.setAccessible(true);
        String query = (String) retrieverQueryField.get(retriever);

        Assert.assertTrue(query.contains("categoryList(filters:{category_uid:{in:[\"uid-5\",\"uid-6\",\"uid-7\"]}})"));
    }

    @Test
    public void testLinkTargetUnchecked() throws Exception {
        setupTest(COMPONENT_PATH_LINK_TARGET_UNCHECKED);

        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertNull(featuredCategoryList.getLinkTarget());
    }

    @Test
    public void testLinkTargetChecked() throws Exception {
        setupTest(COMPONENT_PATH_LINK_TARGET_CHECKED);

        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals("_blank", featuredCategoryList.getLinkTarget());
    }

    @Test
    public void testJsonRender() throws Exception {
        setupTest(COMPONENT_PATH);
        ObjectMapper mapper = new ObjectMapper();
        String expected = Utils.getResource("results/result-datalayer-featuredcategorylist-component.json");
        String jsonResult = featuredCategoryList.getData().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }

    @Test
    public void testJsonExport() throws Exception {
        setupTest(COMPONENT_PATH);
        Utils.testJSONExport(featuredCategoryList, "/exporter/featuredcategories.json");
    }

    @Test
    public void verifyUrlPathCategoryType() throws Exception {
        setupTest(COMPONENT_PATH_WITH_URL_PATH);
        AbstractRetriever retriever = featuredCategoryList.getCategoriesRetriever();

        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);

        Field retrieverQueryField = AbstractRetriever.class.getDeclaredField("query");
        retrieverQueryField.setAccessible(true);
        String query = (String) retrieverQueryField.get(retriever);

        Assert.assertTrue(query.contains("categoryList(filters:{url_path:{in:[\"equipment\"]}})"));
    }

    @Test
    public void testMultipleUrlPathCategoryType() throws Exception {
        setupTest(COMPONENT_PATH_WITH_MULTIPLE_URL_PATH);
        AbstractRetriever retriever = featuredCategoryList.getCategoriesRetriever();

        categories = featuredCategoryList.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertEquals(3, categories.size());

        Field retrieverQueryField = AbstractRetriever.class.getDeclaredField("query");
        retrieverQueryField.setAccessible(true);
        String query = (String) retrieverQueryField.get(retriever);

        Assert.assertTrue(query.contains("categoryList(filters:{url_path:{in:[\"equipment\",\"equipment/running\",\"equipment/bike\"]}})"));
    }
}
