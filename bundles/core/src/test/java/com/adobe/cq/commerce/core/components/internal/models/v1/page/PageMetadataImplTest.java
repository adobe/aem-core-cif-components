/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import java.util.Collection;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.CommerceComponentModelFinder;
import com.adobe.cq.commerce.core.components.internal.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.page.PageMetadata;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.core.testing.MockLaunch;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.launches.api.Launch;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PageMetadataImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private final CommerceComponentModelFinder componentModelFinder = new CommerceComponentModelFinder();

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content-breadcrumb.json")
        .<AemContext>afterSetUp(context -> {
            context.registerInjectActivateService(new SearchFilterServiceImpl());
            context.registerInjectActivateService(new SearchResultsServiceImpl());

            context.registerAdapter(Resource.class, ComponentsConfiguration.class, MOCK_CONFIGURATION_OBJECT);
            context.registerAdapter(Resource.class, Launch.class, (Function<Resource, Launch>) resource -> new MockLaunch(resource));

            ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
            Utils.addDataLayerConfig(mockConfigBuilder, true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);

            // TODO: CIF-2469
            Whitebox.setInternalState(componentModelFinder, "modelFactory", context.getService(ModelFactory.class));
            context.registerService(CommerceComponentModelFinder.class, componentModelFinder);

            context.registerService(CommerceExperienceFragmentsRetriever.class, mock(CommerceExperienceFragmentsRetriever.class));
        })
        .build();

    private void provideSlingBindings(Bindings bindings) {
        bindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, context.currentPage());
        bindings.put(WCMBindingsConstants.NAME_PROPERTIES, context.currentResource().getValueMap());

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        bindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        bindings.put("wcmmode", wcmMode);
    }

    private GraphqlClient graphqlClient;

    public void prepareModel(String pagePath) throws Exception {
        context.currentPage(pagePath);

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        // The method 'modelFactory.getModelFromWrappedRequest()' used in PageMetadataImpl does not reinject "HTL bindings"
        // so we register this service to reinject them for the Product or ProductList component
        // (check ResourceOverridingRequestWrapper to see how bindings are copied by the wrapped request)
        context.registerService(BindingsValuesProvider.class, b -> provideSlingBindings(b));

        // We also need these bindings for the PageMetadata component itself
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(context.currentResource());
        SimpleBindings bindings = new SimpleBindings();
        provideSlingBindings(bindings);
        slingBindings.putAll(bindings);
    }

    @Test
    public void testPageMetadataModelOnProductPage() throws Exception {
        testPageMetadataModelOnProductPage("/content/venia/us/en/products/product-page");

        Product productModel = componentModelFinder.findProductComponentModel(context.request());
        assertTrue(productModel instanceof com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImpl);
        assertEquals("MJ01", productModel.getSku()); // This ensures the data is fetched
        assertFalse("The product doesn't have staged data", productModel.isStaged());

        // Verify that GraphQL client is only called once, so Sling model caching works as expected
        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
        verify(graphqlClient, never()).execute(any(), any(), any());

        // Asserts that the right product resource is used when PageMetadataImpl adapts the request to the Product component
        ComponentData data = productModel.getData();
        assertEquals("venia/components/commerce/product", data.getType());
        assertEquals("product-3944cc709b", data.getId());
    }

    @Test
    public void testPageMetadataModelOnProductSpecificPage() throws Exception {
        testPageMetadataModelOnProductPage("/content/venia/us/en/products/product-page/product-specific-page");

        // see jcr-content-breadcrumb.json : this product component is configured to be version 2
        // so we test that the adaptation in PageMetadataImpl is done with the right resource type
        Product productModel = componentModelFinder.findProductComponentModel(context.request());
        assertTrue(productModel instanceof com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImpl);
        assertEquals("MJ01", productModel.getSku()); // This ensures the data is fetched
        assertTrue("The product has staged data", productModel.isStaged());
    }

    @Test
    public void testPageMetadataModelOnProductSpecificPageNoProduct() throws Exception {
        String pagePath = "/content/venia/us/en/products/product-page/product-specific-page-no-product";

        prepareModel(pagePath);
        PageMetadata pageMetadataModel = context.request().adaptTo(PageMetadata.class);

        Assert.assertNull(pageMetadataModel.getMetaDescription());
        Assert.assertNull(pageMetadataModel.getMetaKeywords());
        Assert.assertNull(pageMetadataModel.getMetaTitle());
        Assert.assertNull(pageMetadataModel.getCanonicalUrl());
    }

    @Test
    public void testPageMetadataModelOnProductPageOnLaunch() throws Exception {
        testPageMetadataModelOnProductPage("/content/launches/2020/09/14/mylaunch/content/venia/us/en/products/product-page");
    }

    private void testPageMetadataModelOnProductPage(String pagePath) throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));
        graphqlClient = Mockito.spy(new GraphqlClientImpl());

        // Mock and set the protected 'client' field
        Utils.setClientField(graphqlClient, mock(HttpClient.class));

        // Activate the GraphqlClientImpl with configuration
        context.registerInjectActivateService(graphqlClient, ImmutableMap.<String, Object>builder()
            .put("httpMethod", "POST")
            .put("url", "https://localhost")
            .build());

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient,
            HttpStatus.SC_OK,
            "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK, "{products(filter:{sku");

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");
        context.request().setServletPath(pagePath + ".html/beaumont-summit-kit.html"); // used by context.request().getRequestURI();

        prepareModel(pagePath);
        PageMetadata pageMetadataModel = context.request().adaptTo(PageMetadata.class);

        Assert.assertEquals("Some product meta description", pageMetadataModel.getMetaDescription());
        Assert.assertEquals("Some product meta keywords", pageMetadataModel.getMetaKeywords());
        Assert.assertEquals("Some product meta title", pageMetadataModel.getMetaTitle());
        Assert.assertEquals("https://author" + pagePath + ".html/beaumont-summit-kit.html", pageMetadataModel.getCanonicalUrl());
    }

    @Test
    public void testPageMetadataModelOnCategoryPage() throws Exception {
        testPageMetadataModelOnCategoryPage("/content/venia/us/en/products/category-page");

        ProductList productListModel = componentModelFinder.findProductListComponentModel(context.request());
        assertTrue(productListModel instanceof com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl);
        assertEquals("Running", productListModel.getTitle()); // This ensures the data is fetched
        assertFalse("The category doesn't have staged data", productListModel.isStaged());

        // Verify that GraphQL client is only called 5 times, so Sling model caching works as expected
        // --> see testPageMetadataModelOnCategoryPage(String) to see why we expect 5 queries
        verify(graphqlClient, times(4)).execute(any(), any(), any(), any());
        verify(graphqlClient, never()).execute(any(), any(), any());

        // Asserts that the right productlist resource is used when PageMetadataImpl adapts the request to the ProductList component
        ComponentData data = productListModel.getData();
        assertEquals("venia/components/commerce/productlist", data.getType());
        assertEquals("productlist-3adb614ac3", data.getId());

        Collection<ProductListItem> products = productListModel.getProducts();
        for (ProductListItem product : products) {
            assertEquals("core/cif/components/commerce/productlistitem", product.getData().getType());
            assertTrue(product.getData().getId().startsWith("productlist-3adb614ac3-item-"));
        }
    }

    @Test
    public void testPageMetadataModelOnCategorySpecificPage() throws Exception {
        testPageMetadataModelOnCategoryPage("/content/venia/us/en/products/category-page/category-specific-page");

        // see jcr-content-breadcrumb.json : this productlist component is configured to be version 2
        // so we test that the adaptation in PageMetadataImpl is done with the right resource type
        ProductList productListModel = componentModelFinder.findProductListComponentModel(context.request());
        assertTrue(productListModel instanceof com.adobe.cq.commerce.core.components.internal.models.v2.productlist.ProductListImpl);
        assertTrue("The category has staged data", productListModel.isStaged());
    }

    @Test
    public void testPageMetadataModelOnCategorySpecificPageNoProductlist() throws Exception {
        String pagePath = "/content/venia/us/en/products/category-page/category-specific-page-no-productlist";

        prepareModel(pagePath);
        PageMetadata pageMetadataModel = context.request().adaptTo(PageMetadata.class);

        Assert.assertNull(pageMetadataModel.getMetaDescription());
        Assert.assertNull(pageMetadataModel.getMetaKeywords());
        Assert.assertNull(pageMetadataModel.getMetaTitle());
        Assert.assertNull(pageMetadataModel.getCanonicalUrl());
    }

    @Test
    public void testPageMetadataModelOnCategoryPageOnLaunch() throws Exception {
        testPageMetadataModelOnCategoryPage("/content/launches/2020/09/14/mylaunch/content/venia/us/en/products/category-page");
    }

    private void testPageMetadataModelOnCategoryPage(String pagePath) throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        graphqlClient = Mockito.spy(new GraphqlClientImpl());

        // Mock and set the protected 'client' field
        Utils.setClientField(graphqlClient, mock(HttpClient.class));

        // Activate the GraphqlClientImpl with configuration
        context.registerInjectActivateService(graphqlClient, ImmutableMap.<String, Object>builder()
            .put("httpMethod", "POST")
            .put("url", "https://localhost")
            .build());

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-category.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-products.json", httpClient, HttpStatus.SC_OK,
            "{products");

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");
        context.request().setServletPath(pagePath + ".html/beaumont-summit-kit.html"); // used by context.request().getRequestURI();

        prepareModel(pagePath);
        PageMetadata pageMetadataModel = context.request().adaptTo(PageMetadata.class);

        Assert.assertEquals("Some category meta description", pageMetadataModel.getMetaDescription());
        Assert.assertEquals("Some category meta keywords", pageMetadataModel.getMetaKeywords());
        Assert.assertEquals("Some category meta title", pageMetadataModel.getMetaTitle());
        Assert.assertEquals("https://author" + pagePath + ".html/beaumont-summit-kit.html", pageMetadataModel.getCanonicalUrl());
    }

    @Test
    public void testPageMetadataModelOnContentPage() throws Exception {
        prepareModel("/content/venia");
        PageMetadata pageMetadataModel = context.request().adaptTo(PageMetadata.class);

        Assert.assertEquals("The Venia page", pageMetadataModel.getMetaDescription());
        Assert.assertEquals("Venia", pageMetadataModel.getMetaTitle());
        Assert.assertNull(pageMetadataModel.getMetaKeywords());
        Assert.assertNull(pageMetadataModel.getCanonicalUrl());
    }
}
