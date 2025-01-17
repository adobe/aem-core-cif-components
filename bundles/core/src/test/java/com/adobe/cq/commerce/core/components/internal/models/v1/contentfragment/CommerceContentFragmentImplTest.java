/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.contentfragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.hamcrest.CustomMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentQuery;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.contentfragment.CommerceContentFragment;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.DataType;
import com.adobe.cq.dam.cfm.FragmentData;
import com.adobe.cq.dam.cfm.content.FragmentRenderService;
import com.adobe.cq.dam.cfm.converter.ContentTypeConverter;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.wcm.core.components.testing.MockLanguageManager;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommerceContentFragmentImplTest {
    private static final String CONTENT_FRAGMENT_PATH_0 = "/content/pageA/jcr:content/root/contentfragment0";
    private static final String CONTENT_FRAGMENT_PATH_1 = "/content/pageA/jcr:content/root/contentfragment1";
    private static final String CONTENT_FRAGMENT_PATH_2 = "/content/pageA/jcr:content/root/contentfragment2";
    private static final String CONTENT_FRAGMENT_PATH_3 = "/content/pageB/jcr:content/root/contentfragment4";
    private static final String CONTENT_FRAGMENT_PATH_4 = "/content/pageA/jcr:content/root/contentfragment3";
    private static final String CONTENT_FRAGMENT_PATH_5 = "/content/pageB/jcr:content/root/contentfragment5";

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content-content-fragment.json")
        .<AemContext>afterSetUp(context -> {
            ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
            Utils.addDataLayerConfig(mockConfigBuilder, true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
        })
        .build();

    private MockSlingHttpServletRequest request;
    private List<ContentElement> contentFragmentElements = new ArrayList<>();
    private CloseableHttpClient httpClient;
    private FragmentRenderService renderService;
    private AssociatedContentService associatedContentService;

    @Before
    public void setup() throws Exception {
        ResourceResolver originalResourceResolver = context.resourceResolver();
        ResourceResolver resourceResolver = Mockito.spy(originalResourceResolver);

        // setup graphql client
        httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        GraphqlClient graphqlClient = Mockito.spy(new GraphqlClientImpl());
        Utils.activateGraphqlClient(context, graphqlClient, null);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        Utils.setupHttpResponse("graphql/magento-graphql-product-sku.json", httpClient, HttpStatus.SC_OK, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-cf-category.json", httpClient, HttpStatus.SC_OK, "url_key\"}}){uid}}");
        Utils.setupHttpResponse("graphql/magento-graphql-category-uid.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path");

        associatedContentService = mock(AssociatedContentService.class);
        context.registerService(AssociatedContentService.class, associatedContentService);

        prepareContentFragment(resourceResolver);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(SlingBindings.RESOLVER, resourceResolver);
        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);
        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);

        context.registerService(LiveRelationshipManager.class, mock(LiveRelationshipManager.class));
        context.registerService(LanguageManager.class, new MockLanguageManager());
        renderService = mock((FragmentRenderService.class));
        context.registerService(FragmentRenderService.class, renderService);
        when(renderService.render(any(), any())).thenAnswer(inv -> request.adaptTo(CommerceContentFragment.class)
            .getElements().get(0).getValue());
        ContentTypeConverter converter = mock(ContentTypeConverter.class);
        context.registerService(ContentTypeConverter.class, converter);

        request = new MockSlingHttpServletRequest(resourceResolver, context.bundleContext());
        request.setAttribute(SlingBindings.class.getName(), slingBindings);
        request.setContextPath("");

    }

    private void prepareContentFragment(ResourceResolver resourceResolver) {
        AssociatedContentQuery<ContentFragment> query = mock(AssociatedContentQuery.class);
        when(query.withLimit(anyLong())).thenReturn(query);
        when(associatedContentService.listProductContentFragments(any(), any())).thenReturn(query);
        when(associatedContentService.listCategoryContentFragments(any(), any())).thenReturn(query);

        List<ContentFragment> contentFragments = new ArrayList<>();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.getName()).thenReturn("name");
        when(cf.getDescription()).thenReturn("description");
        when(cf.getTitle()).thenReturn("title");
        when(cf.getElements()).thenAnswer(inv -> contentFragmentElements.iterator());
        when(cf.getAssociatedContent()).thenReturn(Collections.emptyIterator());

        Resource res = mock(Resource.class);
        when(res.getPath()).thenReturn("/content/cf");
        when(res.adaptTo(com.adobe.cq.dam.cfm.ContentFragment.class)).thenReturn(cf);
        when(resourceResolver.getResource(res.getPath())).thenReturn(res);

        when(cf.adaptTo(Resource.class)).thenReturn(res);
        contentFragments.add(cf);
        when(query.execute()).thenReturn(contentFragments.iterator());

        // prepare model
        Resource model = mock(Resource.class);
        when(resourceResolver.getResource("/model")).thenReturn(model);
        ValueMap properties = mock(ValueMap.class);
        when(model.getValueMap()).thenReturn(properties);
        when(properties.get("jcr:content/jcr:title", "")).thenReturn("Test Model");
    }

    private Page prepareRequest(String path) {
        context.currentResource(path);
        SlingBindings slingBindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        ResourceResolver resolver = (ResourceResolver) slingBindings.get(SlingBindings.RESOLVER);
        Resource resource = resolver.getResource(path);
        slingBindings.setResource(resource);
        request.setResource(resource);

        Page page = Mockito.spy(context.pageManager().getContainingPage(path));
        context.currentPage(page.getPath());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(SlingBindings.RESOURCE, resource);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, resource.adaptTo(ValueMap.class));

        Resource pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        return page;
    }

    @Test
    public void testContentFragmentNoConfig() {
        prepareRequest(CONTENT_FRAGMENT_PATH_0);

        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);

        // empty content fragment model
        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isBlank(contentFragment.getName()));
        Assert.assertNull(contentFragment.getDescription());
        Assert.assertNull(contentFragment.getTitle());
        Assert.assertNull(contentFragment.getType());
        Assert.assertEquals(CommerceContentFragmentImpl.RESOURCE_TYPE, contentFragment.getExportedType());
        Assert.assertEquals("", contentFragment.getGridResourceType());
        Assert.assertEquals("{}", contentFragment.getEditorJSON());
        Assert.assertNull(contentFragment.getElements());
        Assert.assertTrue(contentFragment.getExportedItems().isEmpty());
        Assert.assertEquals(0, contentFragment.getExportedItemsOrder().length);
        Assert.assertNull(contentFragment.getAssociatedContent());
        Assert.assertTrue(contentFragment.getExportedElements().isEmpty());
        Assert.assertEquals(0, contentFragment.getExportedElementsOrder().length);
        Assert.assertNull(contentFragment.getParagraphs());

    }

    @Test
    public void testContentFragmentPartialConfig() {
        prepareRequest(CONTENT_FRAGMENT_PATH_1);

        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);

        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isBlank(contentFragment.getName()));
    }

    @Test
    public void testContentFragmentForProductPageNoProduct() {
        prepareRequest(CONTENT_FRAGMENT_PATH_1);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSelectorString("slug");

        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);

        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isBlank(contentFragment.getName()));
    }

    @Test
    public void testContentFragmentForProductPage() {
        prepareRequest(CONTENT_FRAGMENT_PATH_2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/sku.html");
        Product product = mock(Product.class);
        when(product.getFound()).thenReturn(true);
        when(product.getSku()).thenReturn("sku");
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        // valid content fragment
        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);
        Assert.assertNotNull(contentFragment);
        Assert.assertEquals("Test Model", contentFragment.getModelTitle());
        Assert.assertEquals("name", contentFragment.getName());
        Assert.assertEquals("description", contentFragment.getDescription());
        Assert.assertEquals("title", contentFragment.getTitle());
        Assert.assertTrue(StringUtils.isBlank(contentFragment.getType()));
        Assert.assertEquals(CommerceContentFragmentImpl.RESOURCE_TYPE, contentFragment.getExportedType());
        Assert.assertEquals("dam/cfm/components/grid", contentFragment.getGridResourceType());
        Assert.assertEquals("{\"title\":\"title\",\"path\":\"/content/cf\"}", contentFragment.getEditorJSON());
        Assert.assertTrue(contentFragment.getElements().isEmpty());
        Assert.assertTrue(contentFragment.getExportedItems().isEmpty());
        Assert.assertEquals(0, contentFragment.getExportedItemsOrder().length);
        Assert.assertTrue(contentFragment.getAssociatedContent().isEmpty());
        Assert.assertTrue(contentFragment.getExportedElements().isEmpty());
        Assert.assertEquals(0, contentFragment.getExportedElementsOrder().length);
        Assert.assertNull(contentFragment.getParagraphs());
    }

    @Test
    public void testContentFragmentForProductPageNoSku() {
        prepareRequest(CONTENT_FRAGMENT_PATH_2);

        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);
        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isBlank(contentFragment.getName()));
    }

    @Test
    public void testContentFragmentForCategoryPageNoSelector() {
        prepareRequest(CONTENT_FRAGMENT_PATH_3);

        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);
        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isBlank(contentFragment.getName()));
    }

    @Test
    public void testContentFragmentForCategoryPage() {
        prepareRequest(CONTENT_FRAGMENT_PATH_3);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/uid.html");

        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);
        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isNotBlank(contentFragment.getName()));
    }

    @Test
    public void testContentFragmentForCategoryPageUrlPath() {
        Page page = prepareRequest(CONTENT_FRAGMENT_PATH_3);

        // setup url provider
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/url_path.html");

        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);
        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isNotBlank(contentFragment.getName()));
    }

    @Test
    public void testContentFragmentParagraphsProductPage() throws Exception {
        prepareRequest(CONTENT_FRAGMENT_PATH_4);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/slug.html");
        Product product = mock(Product.class);
        when(product.getFound()).thenReturn(true);
        when(product.getSku()).thenReturn("sku");
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        ContentElement element = mock(ContentElement.class);
        when(element.getName()).thenReturn("text");
        FragmentData fragmentData = mock(FragmentData.class);
        when(fragmentData.getContentType()).thenReturn("text/multi");
        when(fragmentData.getValue()).thenReturn("text fragment");
        DataType dataType = mock(DataType.class);
        when(dataType.isMultiValue()).thenReturn(false);
        when(fragmentData.getDataType()).thenReturn(dataType);
        when(fragmentData.getContentType()).thenReturn("text/multi");
        when(element.getValue()).thenReturn(fragmentData);
        contentFragmentElements.add(element);

        // single text field content fragment
        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);
        Utils.setupHttpResponse("graphql/magento-graphql-product-sku.json", httpClient, HttpStatus.SC_OK, "{products(filter:{url_key");
        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isNotBlank(contentFragment.getName()));
        Assert.assertArrayEquals(new String[] { "text fragment" }, contentFragment.getParagraphs());

        verify(renderService).render(any(), argThat(new CustomMatcher<ValueMap>("ValueMap containing cif.identifier=MJ01") {
            @Override
            public boolean matches(Object o) {
                return o instanceof ValueMap && "MJ01".equals(((ValueMap) o).get(UrlProviderImpl.CIF_IDENTIFIER_ATTR, String.class));
            }
        }));

        contentFragmentElements.clear();
    }

    @Test
    public void testContentFragmentParagraphsCategoryPage() {
        prepareRequest(CONTENT_FRAGMENT_PATH_5);

        ContentElement element = mock(ContentElement.class);
        when(element.getName()).thenReturn("text");
        FragmentData fragmentData = mock(FragmentData.class);
        when(fragmentData.getContentType()).thenReturn("text/multi");
        when(fragmentData.getValue()).thenReturn("text fragment");
        DataType dataType = mock(DataType.class);
        when(dataType.isMultiValue()).thenReturn(false);
        when(fragmentData.getDataType()).thenReturn(dataType);
        when(fragmentData.getContentType()).thenReturn("text/multi");
        when(element.getValue()).thenReturn(fragmentData);
        contentFragmentElements.add(element);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/slug.html");

        // single text field content fragment
        CommerceContentFragment contentFragment = request.adaptTo(CommerceContentFragment.class);
        Assert.assertNotNull(contentFragment);
        Assert.assertTrue(StringUtils.isNotBlank(contentFragment.getName()));
        Assert.assertArrayEquals(new String[] { "text fragment" }, contentFragment.getParagraphs());

        contentFragmentElements.clear();
    }
}
