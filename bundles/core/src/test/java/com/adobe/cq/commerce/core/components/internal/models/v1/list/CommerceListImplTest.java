/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.list;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
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
import org.hamcrest.CustomTypeSafeMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentQuery;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.PageParams;
import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.wcm.core.components.models.List;
import com.adobe.cq.wcm.core.components.models.ListItem;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommerceListImplTest {
    private static final String COMMERCE_LIST_PATH_0 = "/content/pageA/jcr:content/root/responsivegrid/commercelist0";
    private static final String COMMERCE_LIST_PATH_1 = "/content/product-page/jcr:content/root/responsivegrid/commercelist1";
    private static final String COMMERCE_LIST_PATH_2 = "/content/category-page/jcr:content/root/responsivegrid/commercelist2";
    private static final String COMMERCE_LIST_PATH_3 = "/content/category-page/jcr:content/root/responsivegrid/commercelist3";
    private static final String COMMERCE_LIST_PATH_4 = "/content/category-page/jcr:content/root/responsivegrid/commercelist4";
    private static final String COMMERCE_LIST_PATH_5 = "/content/product-page/jcr:content/root/responsivegrid/commercelist5";
    private static final String COMMERCE_LIST_PATH_6 = "/content/pageA/jcr:content/root/responsivegrid/commercelist6";
    private static final String COMMERCE_LIST_PATH_7 = "/content/pageA/jcr:content/root/responsivegrid/commercelist7";
    private static final String COMMERCE_LIST_PATH_8 = "/content/pageA/jcr:content/root/responsivegrid/commercelist8";
    private static final String COMMERCE_LIST_PATH_9 = "/content/category-page/jcr:content/root/responsivegrid/commercelist9";

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
            Utils.addDataLayerConfig(mockConfigBuilder, true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
        })
        .build();

    private MockSlingHttpServletRequest request;
    private CloseableHttpClient httpClient;
    private AssociatedContentService associatedContentService;

    @Before
    public void setup() throws Exception {
        ResourceResolver originalResourceResolver = context.resourceResolver();
        ResourceResolver resourceResolver = Mockito.spy(originalResourceResolver);

        // setup graphql client
        httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        GraphqlClient graphqlClient = Mockito.spy(new GraphqlClientImpl());

        // Mock and set the protected 'client' field
        Utils.setClientField(graphqlClient, mock(HttpClient.class));

        // Activate the GraphqlClientImpl with configuration
        context.registerInjectActivateService(graphqlClient, ImmutableMap.<String, Object>builder()
            .put("httpMethod", "POST")
            .put("url", "https://localhost")
            .build());

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        Utils.setupHttpResponse("graphql/magento-graphql-product-sku.json", httpClient, HttpStatus.SC_OK, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-category-uid.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path");

        associatedContentService = mock(AssociatedContentService.class);
        context.registerService(AssociatedContentService.class, associatedContentService);

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

        request = new MockSlingHttpServletRequest(resourceResolver, context.bundleContext());
        request.setAttribute(SlingBindings.class.getName(), slingBindings);
        request.setContextPath("");

        AssociatedContentQuery<Page> query = mock(AssociatedContentQuery.class);
        when(query.withLimit(anyLong())).thenReturn(query);
        when(associatedContentService.listProductContentPages(any(), any())).thenReturn(query);
        when(associatedContentService.listCategoryContentPages(any(), any())).thenReturn(query);

        ArrayList<Page> contentPages = new ArrayList<>();
        contentPages.add(context.pageManager().getPage("/content/pageK"));
        contentPages.add(context.pageManager().getPage("/content/pageI"));
        contentPages.add(context.pageManager().getPage("/content/pageJ"));
        when(query.execute()).thenReturn(contentPages.iterator());

    }

    private void prepareRequest(String path) {
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
    }

    @NotNull
    private CustomTypeSafeMatcher<PageParams> hasId(String id) {
        return new CustomTypeSafeMatcher<PageParams>("PageParams with only identifier: " + id) {
            @Override
            protected boolean matchesSafely(PageParams params) {

                return params.identifiers().size() == 1 && params.identifiers().contains(id);
            }
        };
    }

    @Test
    public void testCommerceListForProductPage() {
        prepareRequest(COMMERCE_LIST_PATH_1);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/sku.html");
        Product product = mock(Product.class);
        when(product.getFound()).thenReturn(true);
        when(product.getSku()).thenReturn("sku");
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        List list = request.adaptTo(List.class);

        assertNotNull(list);
        assertTrue(list.displayItemAsTeaser());
        assertTrue(list.linkItems());
        assertTrue(list.showDescription());
        assertTrue(list.showModificationDate());
        assertEquals("yyyy-MM-dd", list.getDateFormatString());
        assertEquals("myId", list.getId());
        assertEquals(CommerceListImpl.RESOURCE_TYPE, list.getExportedType());
        assertNull(list.getAppliedCssClasses());

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(3, listItems.size());
        ListItem item = listItems.iterator().next();
        assertNotNull(item);
        assertEquals("/content/pageK", item.getPath());
        assertEquals("pageK", item.getName());
        assertEquals("Page K Title", item.getTitle());
        assertEquals("Page K description", item.getDescription());
        assertEquals("/content/pageK.html", item.getURL());

        verify(associatedContentService).listProductContentPages(isA(ResourceResolver.class), argThat(hasId("MJ01")));
        verify(associatedContentService, times(0)).listCategoryContentPages(any(), any());
    }

    @Test
    public void testCommerceListForProductPageWithProduct() {
        prepareRequest(COMMERCE_LIST_PATH_5);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSelectorString("slug");

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(3, listItems.size());

        verify(associatedContentService).listProductContentPages(isA(ResourceResolver.class), argThat(hasId("mySKU")));
        verify(associatedContentService, times(0)).listCategoryContentPages(any(), any());
    }

    @Test
    public void testCommerceListForContentPageWithProduct() {
        prepareRequest(COMMERCE_LIST_PATH_6);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSelectorString("slug");

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(3, listItems.size());

        verify(associatedContentService).listProductContentPages(isA(ResourceResolver.class), argThat(hasId("mySKU")));
        verify(associatedContentService, times(0)).listCategoryContentPages(any(), any());
    }

    @Test
    public void testCommerceListForContentPageNoProduct() {
        prepareRequest(COMMERCE_LIST_PATH_0);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSelectorString("slug");

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(0, listItems.size());

        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
        verify(associatedContentService, times(0)).listCategoryContentPages(any(), any());
    }

    @Test
    public void testCommerceListForProductPageNoProduct() {
        prepareRequest(COMMERCE_LIST_PATH_1);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSelectorString("slug");

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(0, listItems.size());

        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
        verify(associatedContentService, times(0)).listCategoryContentPages(any(), any());
    }

    @Test
    public void testCommerceListForCategoryPage() {
        prepareRequest(COMMERCE_LIST_PATH_2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/slug.html");

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        assertFalse(list.displayItemAsTeaser());
        assertFalse(list.linkItems());
        assertFalse(list.showDescription());
        assertFalse(list.showModificationDate());

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(3, listItems.size());

        verify(associatedContentService).listCategoryContentPages(isA(ResourceResolver.class), argThat(hasId("MTI==")));
        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
    }

    @Test
    public void testCommerceListForCategoryPageWithMaxItems() {
        prepareRequest(COMMERCE_LIST_PATH_3);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/slug.html");

        List list = request.adaptTo(List.class);

        assertNotNull(list);
        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(2, listItems.size());

        verify(associatedContentService).listCategoryContentPages(isA(ResourceResolver.class), argThat(hasId("MTI==")));
        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
    }

    @Test
    public void testCommerceListForCategoryPageNoSelector() {
        prepareRequest(COMMERCE_LIST_PATH_2);

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(0, listItems.size());

        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
        verify(associatedContentService, times(0)).listCategoryContentPages(any(), any());
    }

    @Test
    public void testCommerceListForCategoryPageWithCategory() {
        prepareRequest(COMMERCE_LIST_PATH_9);

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(3, listItems.size());

        verify(associatedContentService).listCategoryContentPages(isA(ResourceResolver.class), argThat(hasId("myUID")));
        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
    }

    @Test
    public void testCommerceListForContentPageWithCategory() {
        prepareRequest(COMMERCE_LIST_PATH_8);

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(3, listItems.size());

        verify(associatedContentService).listCategoryContentPages(isA(ResourceResolver.class), argThat(hasId("myUID")));
        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
    }

    @Test
    public void testCommerceListForContentPageNoCategory() {
        prepareRequest(COMMERCE_LIST_PATH_7);

        List list = request.adaptTo(List.class);

        assertNotNull(list);

        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(0, listItems.size());

        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
        verify(associatedContentService, times(0)).listCategoryContentPages(any(), any());
    }

    @Test
    public void testCommerceListNoConfiguration() {
        prepareRequest(COMMERCE_LIST_PATH_4);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/slug.html");

        List list = request.adaptTo(List.class);

        assertNotNull(list);
        Collection<ListItem> listItems = list.getListItems();
        assertNotNull(listItems);
        assertEquals(0, listItems.size());

        verify(associatedContentService, times(0)).listProductContentPages(any(), any());
        verify(associatedContentService, times(0)).listCategoryContentPages(any(), any());
    }
}
