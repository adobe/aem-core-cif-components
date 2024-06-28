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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureImpl;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithCategoryAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlPath;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UrlProviderImplTest {

    @Rule
    public final AemContext context = newAemContext("/context/jcr-page-filter.json");
    private UrlProvider urlProvider;
    private MockSlingHttpServletRequest request;
    private CloseableHttpClient httpClient;
    private GraphqlClient graphqlClient;
    private Map<String, Object> caConfig = new HashMap<>();

    private Function<Resource, ComponentsConfiguration> caConfigSupplier = r -> new ComponentsConfiguration(
        new ValueMapDecorator(caConfig));

    @Before
    public void setup() throws Exception {
        urlProvider = context.getService(UrlProvider.class);
        request = newRequest();

        httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        graphqlClient = spy(new GraphqlClientImpl());
        context.registerInjectActivateService(graphqlClient, "httpMethod", "POST");
        context.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);
        context.registerAdapter(Resource.class, ComponentsConfiguration.class,
            (Function<Resource, ComponentsConfiguration>) r -> caConfigSupplier.apply(r));

        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ01\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result-url-parameters.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ03\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-product-not-found-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ02\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-product-sku.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{url_key:{eq:\"beaumont-summit-kit\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid:{eq:\"uid-5\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid:{eq:\"uid-99\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-category-uid.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path:{eq:\"men/tops-men/jackets-men\"}}");
        // from url_path men/tops-men/jackets-men
        Utils.setupHttpResponse("graphql/magento-graphql-category-uid.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_key:{eq:\"jackets-men\"}}");
        // from url_path does/not/exist
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK,
            "categoryList(filters:{url_key:{eq:\"exist\"}");

        context.registerInjectActivateService(urlProvider);
    }

    private void configureSpecificPageStrategy(boolean generateSpecificPageUrls) {
        // TODO: CIF-2469
        // With a newer version of OSGI mock we could re-inject the reference into the existing UrlProviderImpl
        // context.registerInjectActivateService(new SpecificPageStrategy(), "generateSpecificPageUrls", true);
        SpecificPageStrategy specificPageStrategy = context.getService(SpecificPageStrategy.class);
        Whitebox.setInternalState(specificPageStrategy, "generateSpecificPageUrls", generateSpecificPageUrls);
    }

    private MockSlingHttpServletRequest newRequest() {
        // we cannot use context.request() as this will create a single instance with an adapter cache that we cannot flush during a test
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        SlingBindings bindings = new SlingBindings();
        bindings.put("request", this.request);
        bindings.put("response", context.response());
        bindings.put("sling", context.slingScriptHelper());
        request.setAttribute(SlingBindings.class.getName(), bindings);
        return request;
    }

    private Page setCurrentPage(String path) {
        return setCurrentPage(context.currentPage(path));
    }

    private Page setCurrentPage(Page page) {
        page = context.currentPage(page);
        SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        bindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        request.setAttribute(SiteStructure.class.getName(), null);
        request.setResource(page.adaptTo(Resource.class));

        return page;
    }

    @Test
    public void testProductUrl() {
        Page page = setCurrentPage("/content/product-page");

        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page.html/beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithCustomPage() {
        setCurrentPage(context.create().page("/content/custom-page"));
        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .page("/content/custom-page")
            .map();

        String url = urlProvider.toProductUrl(request, null, params);
        assertEquals("/content/custom-page.html/beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithSubpageAndAnchor() {
        Page page = setCurrentPage("/content/product-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .urlKey("productId2")
            .variantSku("variantSku")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page/sub-page-2.html/productId2.html#variantSku", url);
    }

    @Test
    public void testNestedProductUrlWithAnchor() {
        Page page = setCurrentPage("/content/product-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .urlKey("productId1.1")
            .variantSku("variantSku")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page/sub-page/nested-page.html/productId1.1.html#variantSku", url);
    }

    @Test
    public void testProductUrlMissingParams() {
        Page page = setCurrentPage("/content/product-page");
        Map<String, String> params = new ParamsBuilder()
            .sku("MJ01")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page.html/{{url_key}}.html", url);
    }

    @Test
    public void testProductUrlWithGraphQLClient() {
        Page page = setCurrentPage("/content/product-page");

        String url = urlProvider.toProductUrl(request, page, "MJ01");
        assertEquals("/content/product-page.html/beaumont-summit-kit.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlNotFoundWithGraphQLClient() {
        Page page = setCurrentPage("/content/product-page");

        String url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/content/product-page.html/{{url_key}}.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlWithGraphQLClientMissingParameters() {
        Page page = setCurrentPage("/content/product-page");

        String url = urlProvider.toProductUrl(request, page, StringUtils.EMPTY);
        assertEquals("/content/product-page.html/{{url_key}}.html", url);
    }

    @Test
    public void testProductUrlOnlySKU() {
        Page page = setCurrentPage("/content/product-page");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithSku.PATTERN);

        String url = urlProvider.toProductUrl(request, page, "MJ01");
        assertEquals("/content/product-page.html/MJ01.html", url);

        // not required when only sku is used
        verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlWithSKUProductNotFound() {
        Page page = setCurrentPage("/content/product-page");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithSkuAndUrlKey.PATTERN);

        // Not found, sku set
        String url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/content/product-page.html/MJ02.html", url);

        // found, parameters queried without sku set
        url = urlProvider.toProductUrl(request, page, "MJ03");
        assertEquals("/content/product-page.html/MJ03/test-url-key.html", url);
    }

    @Test
    public void testProductPageWithinAnotherProductPagesContext() {
        Page page = setCurrentPage("/content/product-page");
        SlingBindings slingBindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSuffix("/category-b/another-product.html");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithCategoryAndUrlKey.PATTERN);

        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setUrlRewrites(Arrays.asList(
            new UrlRewrite().setUrl("product"),
            new UrlRewrite().setUrl("category-a/product"),
            new UrlRewrite().setUrl("category-b/product")));
        params.setUrlKey("product");

        // when enableContextAwareProductUrls true
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            "productPageUrlFormat", ProductPageWithCategoryAndUrlKey.PATTERN,
            "enableContextAwareProductUrls", true);

        String url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page.html/category-b/product.html", url);

        // when enableContextAwareProductUrls disabled
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            "productPageUrlFormat", ProductPageWithCategoryAndUrlKey.PATTERN,
            "enableContextAwareProductUrls", false);
        params.getCategoryUrlParams().setUrlKey("category-b");
        params.getCategoryUrlParams().setUrlPath("category-b");

        url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page.html/category-a/product.html", url);

        // not required when only sku is used
        verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductPageWithinCategoryContext() {
        setCurrentPage("/content/category-page");

        Page productPage = context.pageManager().getPage("/content/product-page");
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSuffix("/category-b.html");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithCategoryAndUrlKey.PATTERN);

        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setUrlRewrites(Arrays.asList(
            new UrlRewrite().setUrl("product"),
            new UrlRewrite().setUrl("category-a/product"),
            new UrlRewrite().setUrl("category-b/product")));
        params.setUrlKey("product");

        // when enableContextAwareProductUrls true
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            "productPageUrlFormat", ProductPageWithCategoryAndUrlKey.PATTERN,
            "enableContextAwareProductUrls", true);

        String url = urlProvider.toProductUrl(request, productPage, params);
        assertEquals("/content/product-page.html/category-b/product.html", url);

        // when enableContextAwareProductUrls disabled
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            "productPageUrlFormat", ProductPageWithCategoryAndUrlKey.PATTERN,
            "enableContextAwareProductUrls", false);
        params.getCategoryUrlParams().setUrlKey("category-b");
        params.getCategoryUrlParams().setUrlPath("category-b");

        url = urlProvider.toProductUrl(request, productPage, params);
        assertEquals("/content/product-page.html/category-a/product.html", url);

        // not required when only sku is used
        verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlSpecificPageFromCategoryPageContext() {
        // verify that the specific page can be picked by the category url_key provided by the category url format
        setCurrentPage("/content/category-page");
        Page productPage = context.pageManager().getPage("/content/product-page");

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSuffix("/category-b.html");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            "productPageUrlFormat", ProductPageWithCategoryAndUrlKey.PATTERN,
            "enableContextAwareProductUrls", true);
        configureSpecificPageStrategy(true);

        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setUrlRewrites(Arrays.asList(
            new UrlRewrite().setUrl("product"),
            new UrlRewrite().setUrl("category-a/product"),
            new UrlRewrite().setUrl("category-b/product")));
        params.setUrlKey("product");

        String url = urlProvider.toProductUrl(request, productPage, params);
        assertEquals("/content/product-page/sub-page/nested-page-category.html/category-b/product.html", url);
    }

    @Test
    public void testProductPageWithFormatFromCatalogPage() {
        Page currentPage = setCurrentPage("/content");

        // provide a ComponentsConfiguration specific for the specific search root
        Function<Resource, ComponentsConfiguration> originalSupplier = caConfigSupplier;
        ComponentsConfiguration specificCaConfig = new ComponentsConfiguration(new ValueMapDecorator(ImmutableMap.of(
            UrlFormat.PRODUCT_PAGE_URL_FORMAT, ProductPageWithSkuAndUrlKey.PATTERN)));
        caConfigSupplier = r -> !r.getPath().equals("/content/new-catalog/jcr:content")
            ? originalSupplier.apply(r)
            : specificCaConfig;

        // create a catalog page as specific search root
        context.create().page("/content/new-catalog", "catalogpage", ImmutableMap.of(
            "sling:resourceType", SiteStructure.RT_CATALOG_PAGE_V3,
            SiteStructureImpl.PN_CIF_PRODUCT_PAGE, "/content/new-catalog",
            "selectorFilter", "bar"));

        // enabled specific page strategy and set another product url format than used by the specific search root
        configureSpecificPageStrategy(true);
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            "productPageUrlFormat", ProductPageWithSkuAndUrlPath.PATTERN);

        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setUrlKey("bar");
        params.setUrlPath("foo/bar");
        params.setSku("1234");

        String url = urlProvider.toProductUrl(request, currentPage, params);
        assertEquals("/content/new-catalog.html/1234/bar.html", url);
    }

    /**
     * This test checks that a search root is applicable for a given set of parameters independently of the ones the configured url format
     * would be able to parse from the url.
     * <p>
     * The selection of a specific page is done only with those parameters of the url format that can also be parsed from the url.
     * This is important when deep linking is not enabled (default on publish) to resolve the specific page from the requested page using
     * the parameters available in the url. For the selection of the search root from multiple catalog pages this strict behavior is not
     * necessary as even with deep linking disabled the link to the product/category points to the selected search root.
     */
    @Test
    public void testProductPageWithMultipleCatalogPagesSelectionUsesAllUrlParameters() {
        Page currentPage = setCurrentPage("/content");

        // create a catalog page as specific search root
        context.create().page("/content/new-catalog", "catalogpage", ImmutableMap.of(
            "sling:resourceType", SiteStructure.RT_CATALOG_PAGE_V3,
            SiteStructureImpl.PN_CIF_PRODUCT_PAGE, "/content/new-catalog/product",
            SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER, "foo",
            SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER_TYPE, "urlPath"));
        context.create().page("/content/new-catalog/product");

        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            "productPageUrlFormat", ProductPageWithUrlKey.PATTERN,
            "enableContextAwareProductUrls", true);

        // the category url params cannot be parsed from url by the ProductPageWithUrlKey, but the selection based on the catalog page
        // should still work
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setUrlKey("bar");
        params.setUrlRewrites(Arrays.asList(
            new UrlRewrite().setUrl("bar"),
            new UrlRewrite().setUrl("foo/bar")));

        String url = urlProvider.toProductUrl(request, currentPage, params);
        assertEquals("/content/new-catalog/product.html/bar.html", url);
    }

    @Test
    public void testCategoryUrl() {
        Page page = setCurrentPage("/content/category-page");

        Map<String, String> params = new ParamsBuilder()
            .urlPath("men")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html/men.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpage() {
        Page page = setCurrentPage("/content/category-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .uid("MTE=")
            .urlPath("men/tops/shirts")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page/sub-page-with-urlpath.html/men/tops/shirts.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpageArray() {
        Page page = setCurrentPage("/content/category-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .uid("MTF=")
            .urlPath("men/bottoms")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page/sub-page-with-urlpath-array.html/men/bottoms.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpageV2() {
        Page page = setCurrentPage("/content/category-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("women/tops/shirts")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page/sub-page-with-urlpath-v2.html/women/tops/shirts.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpageArrayV2() {
        Page page = setCurrentPage("/content/category-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("women/women-bottoms/women-bottoms-shorts")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page/sub-page-with-urlpath-array-v2.html/women/women-bottoms/women-bottoms-shorts.html",
            url);
    }

    @Test
    public void testNestedCategoryUrl() {
        Page page = setCurrentPage("/content/category-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("category-uid-1.1")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page/sub-page/nested-page.html/category-uid-1.1.html", url);
    }

    @Test
    public void testCategoryUrlMissingParams() {
        Page page = setCurrentPage("/content/category-page");
        Map<String, String> params = new ParamsBuilder()
            .uid("UID-42")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html/{{url_path}}.html", url);
    }

    @Test
    public void testCategoryUrlWithGraphQLClient() {
        Page page = setCurrentPage("/content/category-page");

        String url = urlProvider.toCategoryUrl(request, page, "uid-5");
        assertEquals("/content/category-page.html/equipment.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryUrlWhenCategoryTypeIsUrlPathWithGraphQLClient() {
        Page page = setCurrentPage("/content/category-page");

        urlProvider.setCategoryIdType("urlPath");
        String url = urlProvider.toCategoryUrl(request, page, "men/tops-men/jackets-men");
        assertEquals("/content/category-page.html/men/tops-men/jackets-men.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryUrlNotFoundWithGraphQLClient() {
        Page page = setCurrentPage("/content/category-page");

        String url = urlProvider.toCategoryUrl(request, page, "uid-99");
        assertEquals("/content/category-page.html/{{url_path}}.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryUrlWithGraphQLClientMissingParameters() {
        Page page = setCurrentPage("/content/category-page");

        String url = urlProvider.toCategoryUrl(request, page, StringUtils.EMPTY);
        assertEquals("/content/category-page.html/{{url_path}}.html", url);
    }

    @Test
    public void testProductIdentifierParsingInSuffixUrlKey() {
        setCurrentPage("/content/catalog-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");

        String identifier = urlProvider.getProductIdentifier(request);
        assertEquals("MJ01", identifier);
        // second access should be cached in request attributes
        identifier = urlProvider.getProductIdentifier(request);
        assertEquals("MJ01", identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductIdentifierParsingInSuffixUrlKeyWithGraphqlClientError() {
        setCurrentPage("/content/product-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");

        doThrow(new RuntimeException()).when(graphqlClient).execute(any(), any(), any(), any());

        String identifier = urlProvider.getProductIdentifier(request);
        Assert.assertNull(identifier);
    }

    @Test
    public void testProductIdentifierParsingInSuffixSKU() {
        setCurrentPage("/content/product-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/MJ01.html");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithSku.PATTERN);

        String identifier = urlProvider.getProductIdentifier(request);
        assertEquals("MJ01", identifier);

        verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryIdentifierParsingUrlPath() {
        ArgumentCaptor<GraphqlRequest> reqCaptor = ArgumentCaptor.forClass(GraphqlRequest.class);
        setCurrentPage("/content/catalog-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/men/tops-men/jackets-men");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "categoryPageUrlFormat", CategoryPageWithUrlPath.PATTERN);

        String identifier = urlProvider.getCategoryIdentifier(request);
        assertEquals("MTI==", identifier);
        // second access should be cached in request attributes
        identifier = urlProvider.getCategoryIdentifier(request);
        assertEquals("MTI==", identifier);

        verify(graphqlClient, times(1)).execute(reqCaptor.capture(), any(), any(), any());

        GraphqlRequest gqlReq = reqCaptor.getValue();
        assertEquals("{categoryList(filters:{url_path:{eq:\"men/tops-men/jackets-men\"}}){uid}}", gqlReq.getQuery());
    }

    @Test
    public void testCategoryIdentifierParsingUrlKey() {
        ArgumentCaptor<GraphqlRequest> reqCaptor = ArgumentCaptor.forClass(GraphqlRequest.class);
        setCurrentPage("/content/catalog-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/jackets-men");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "categoryPageUrlFormat", CategoryPageWithUrlKey.PATTERN);

        String identifier = urlProvider.getCategoryIdentifier(request);
        assertEquals("MTI==", identifier);
        // second access should be cached in request attributes
        identifier = urlProvider.getCategoryIdentifier(request);
        assertEquals("MTI==", identifier);

        verify(graphqlClient, times(1)).execute(reqCaptor.capture(), any(), any(), any());

        GraphqlRequest gqlReq = reqCaptor.getValue();
        assertEquals("{categoryList(filters:{url_key:{eq:\"jackets-men\"}}){uid}}", gqlReq.getQuery());
    }

    @Test
    public void testCategoryIdentifierParsingUrlPathNotFound() {
        setCurrentPage("/content/catalog-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/does/not/exist.html");

        String identifier = urlProvider.getCategoryIdentifier(request);
        Assert.assertNull(identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCustomProductPageFormat() {
        Page page = setCurrentPage("/content/product-page");
        request.setQueryString("sku=MJ02");
        context.registerService(UrlFormat.class, new CustomLegacyUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.PRODUCT_PAGE_URL_FORMAT);
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);

        // verify parse
        String identifier = urlProvider.getProductIdentifier(request);
        assertEquals("MJ02", identifier);

        // verify format
        String url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/content/product-page.html?sku=MJ02", url);

        // verify the product page url format is not used for categories
        url = urlProvider.toCategoryUrl(request, page, "uid-5");
        assertEquals("/content/category-page.html/equipment.html", url);
    }

    @Test
    public void testCustomCategoryPageFormat() {
        Page page = setCurrentPage("/content/category-page");
        request.setQueryString("uid=uid-5");
        context.registerService(UrlFormat.class, new CustomLegacyUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.CATEGORY_PAGE_URL_FORMAT);
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);

        // verify parse
        String identifier = urlProvider.getCategoryIdentifier(request);
        assertEquals("uid-5", identifier);

        // verify format
        String url = urlProvider.toCategoryUrl(request, page, "uid-5");
        assertEquals("/content/category-page.html?uid=uid-5", url);

        // verify the category page url format is not used for products
        url = urlProvider.toProductUrl(request, page, "MJ01");
        assertEquals("/content/product-page.html/beaumont-summit-kit.html", url);
    }

    @Test
    public void testCustomCategoryPageFormatWithSpecificPage() {
        Page page = setCurrentPage("/content/category-page");
        context.registerService(UrlFormat.class, new CustomLegacyUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.CATEGORY_PAGE_URL_FORMAT);
        configureSpecificPageStrategy(true);
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);

        // verify format, it should pick based on the uid even though the other parameters are available as well
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setPage(page.getPath());
        params.setUrlPath("men/tops");
        params.setUrlKey("tops");
        params.setUid("category-uid-3");

        assertEquals("/content/category-page/sub-page-with-urlpath-array.html?uid=category-uid-3", urlProvider.toCategoryUrl(request, page,
            params));
    }

    @Test
    public void testCAConfigWithDefaultProductUrlFormat() {
        Page page = setCurrentPage("/content/product-page");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            UrlFormat.PRODUCT_PAGE_URL_FORMAT, ProductPageWithSku.PATTERN);

        caConfig.put(UrlFormat.PRODUCT_PAGE_URL_FORMAT, ProductPageWithUrlPath.PATTERN);

        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .urlPath("foobar/beaumont-summit-kit")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page.html/foobar/beaumont-summit-kit.html", url);
    }

    @Test
    public void testCAConfigWithDefaultCategoryUrlFormat() {
        Page page = setCurrentPage("/content/category-page");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            UrlFormat.CATEGORY_PAGE_URL_FORMAT,
            CategoryPageWithUrlKey.PATTERN);

        caConfig.put(UrlFormat.CATEGORY_PAGE_URL_FORMAT, CategoryPageWithUrlKey.PATTERN);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("tops/men")
            .urlKey("men")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html/men.html", url);
    }

    @Test
    public void testCAConfigWithLegacyCustomProductUrlFormat() {
        // register multiple formats
        context.registerService(UrlFormat.class, new CustomLegacyUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.PRODUCT_PAGE_URL_FORMAT);
        context.registerService(ProductUrlFormat.class, new OneCustomProductPageUrlFormat());
        context.registerService(ProductUrlFormat.class, new AnotherCustomProductPageUrlFormat());
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);

        // verify format
        // always reset the current page to not cache the Resource -> ComponentsConfiguration adaption
        // and always create a new request to not cache Request -> SiteStructure adaption
        // no configuration, defaults to highest-ranked/first new custom format
        request = newRequest();
        Page page = setCurrentPage("/content/product-page");
        String url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/content/product-page.html?sku=MJ02#A", url);

        // configure another new custom url format
        request = newRequest();
        page = setCurrentPage("/content/product-page");
        caConfig.put(UrlFormat.PRODUCT_PAGE_URL_FORMAT, AnotherCustomProductPageUrlFormat.class.getName());
        url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/content/product-page.html?sku=MJ02#B", url);

        // configure legacy custom url format
        request = newRequest();
        page = setCurrentPage("/content/product-page");
        caConfig.put(UrlFormat.PRODUCT_PAGE_URL_FORMAT, CustomLegacyUrlFormat.class.getName());
        url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/content/product-page.html?sku=MJ02", url);

        // use default format
        request = newRequest();
        page = setCurrentPage("/content/product-page");
        caConfig.put(UrlFormat.PRODUCT_PAGE_URL_FORMAT, ProductPageWithSku.PATTERN);
        url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/content/product-page.html/MJ02.html", url);
    }

    @Test
    public void testCAConfigWithCustomCategoryUrlFormat() {
        // register multiple formats
        context.registerService(UrlFormat.class, new CustomLegacyUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.CATEGORY_PAGE_URL_FORMAT);
        context.registerService(CategoryUrlFormat.class, new OneCustomCategoryPageUrlFormat());
        context.registerService(CategoryUrlFormat.class, new AnotherCustomCategoryPageUrlFormat());
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUrlKey("bar");
        params.setUrlPath("foo");
        params.setUid("uid-1");

        // verify format
        // always reset the current page to not cache the Resource -> ComponentsConfiguration adaption
        // and always create a new request to not cache Request -> SiteStructure adaption
        // no configuration, defaults to highest-ranked/first new custom format
        request = newRequest();
        Page page = setCurrentPage("/content/category-page");
        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html?uid=uid-1#A", url);

        // configure another new custom url format
        request = newRequest();
        page = setCurrentPage("/content/category-page");
        caConfig.put(UrlFormat.CATEGORY_PAGE_URL_FORMAT, AnotherCustomCategoryPageUrlFormat.class.getName());
        url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html?uid=uid-1#B", url);

        // configure legacy custom url format
        request = newRequest();
        page = setCurrentPage("/content/category-page");
        caConfig.put(UrlFormat.CATEGORY_PAGE_URL_FORMAT, CustomLegacyUrlFormat.class.getName());
        url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html?uid=uid-1", url);

        // use default format
        request = newRequest();
        page = setCurrentPage("/content/category-page");
        caConfig.put(UrlFormat.CATEGORY_PAGE_URL_FORMAT, CategoryPageWithUrlKey.PATTERN);
        url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html/bar.html", url);
    }

    private static class CustomLegacyUrlFormat implements UrlFormat {

        String anchor = null;

        @Override
        public String format(Map<String, String> parameters) {
            return parameters.get("page") + ".html" +
                (StringUtils.isNotEmpty(parameters.get("sku")) ? "?sku=" + parameters.get("sku") : "") +
                (StringUtils.isNotEmpty(parameters.get("uid")) ? "?uid=" + parameters.get("uid") : "") +
                (StringUtils.isNotEmpty(anchor) ? "#" + anchor : "");
        }

        @Override
        public Map<String, String> parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap) {
            Map<String, String> parameters = new HashMap<>(1);
            Optional.ofNullable(parameterMap.getValue("sku"))
                .map(RequestParameter::getString)
                .ifPresent(p -> parameters.put("sku", p));
            Optional.ofNullable(parameterMap.getValue("uid"))
                .map(RequestParameter::getString)
                .ifPresent(p -> parameters.put("uid", p));
            return parameters;
        }

        @Override
        public Set<String> getParameterNames() {
            return ImmutableSet.of("uid", "sku");
        }
    }

    private static class OneCustomCategoryPageUrlFormat extends CustomCategoryPageUrlFormat {
        OneCustomCategoryPageUrlFormat() {
            anchor = "A";
        }
    }

    private static class AnotherCustomCategoryPageUrlFormat extends CustomCategoryPageUrlFormat {
        AnotherCustomCategoryPageUrlFormat() {
            anchor = "B";
        }
    }

    private static class CustomCategoryPageUrlFormat implements CategoryUrlFormat {

        String anchor = null;

        @Override
        public String format(Params parameters) {
            return parameters.getPage() + ".html" +
                "?uid=" + (StringUtils.isNotBlank(parameters.getUid()) ? parameters.getUid() : "{{uid}}") +
                (StringUtils.isNotEmpty(anchor) ? "#" + anchor : "");

        }

        @Override
        public Params parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap) {
            Params params = new Params();
            RequestParameter[] uidParameter = parameterMap.get("uid");
            if (uidParameter != null && uidParameter.length > 0) {
                params.setUid(uidParameter[0].getString());
            }
            return params;
        }

        @Override
        public Params retainParsableParameters(Params parameters) {
            Params copy = new Params();
            copy.setUid(parameters.getUid());
            return copy;
        }
    }

    private static class OneCustomProductPageUrlFormat extends CustomProductPageUrlFormat {
        OneCustomProductPageUrlFormat() {
            anchor = "A";
        }
    }

    private static class AnotherCustomProductPageUrlFormat extends CustomProductPageUrlFormat {
        AnotherCustomProductPageUrlFormat() {
            anchor = "B";
        }
    }

    private static class CustomProductPageUrlFormat implements ProductUrlFormat {

        String anchor = null;

        @Override
        public String format(Params parameters) {
            return parameters.getPage() + ".html" +
                "?sku=" + (StringUtils.isNotBlank(parameters.getSku()) ? parameters.getSku() : "{{sku}}") +
                (StringUtils.isNotEmpty(anchor) ? "#" + anchor : "");

        }

        @Override
        public Params parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap) {
            Params params = new Params();
            RequestParameter[] uidParameter = parameterMap.get("uid");
            if (uidParameter != null && uidParameter.length > 0) {
                params.setSku(uidParameter[0].getString());
            }
            return params;
        }

        @Override
        public Params retainParsableParameters(Params parameters) {
            Params copy = new Params();
            copy.setSku(parameters.getSku());
            return copy;
        }
    }

    @Test
    public void testProductFilterHook() {
        setCurrentPage("/content");
        // no information in the url
        UnaryOperator<ProductAttributeFilterInput> inputHook = urlProvider.getProductFilterHook(request);
        assertNull(inputHook);

        // default url format (url_key)
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/foobar.html");
        ProductAttributeFilterInput input = urlProvider.getProductFilterHook(request).apply(null);
        assertNotNull(input);
        assertNull(input.getSku());
        assertNotNull(input.getUrlKey());
        assertEquals("foobar", input.getUrlKey().getEq());

        // with sku and url_key, sku takes precedence
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/FOO007/foobar.html");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(),
            UrlFormat.PRODUCT_PAGE_URL_FORMAT, ProductPageWithSkuAndUrlKey.PATTERN);
        input = urlProvider.getProductFilterHook(request).apply(null);
        assertNotNull(input);
        assertNull(input.getUrlKey());
        assertNotNull(input.getSku());
        assertEquals("FOO007", input.getSku().getEq());
    }
}
