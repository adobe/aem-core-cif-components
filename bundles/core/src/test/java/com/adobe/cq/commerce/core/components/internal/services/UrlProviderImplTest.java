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
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithCategoryAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlPath;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class UrlProviderImplTest {

    @Rule
    public final AemContext context = newAemContext("/context/jcr-page-filter.json");

    private UrlProvider urlProvider;
    private MockSlingHttpServletRequest request;
    private CloseableHttpClient httpClient;
    private GraphqlClient graphqlClient;
    private Map<String, Object> caConfig = new HashMap<>();

    @Before
    public void setup() throws Exception {
        urlProvider = context.getService(UrlProvider.class);
        request = context.request();

        httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        graphqlClient = spy(new GraphqlClientImpl());
        context.registerInjectActivateService(graphqlClient, "httpMethod", "POST");
        context.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);
        context.registerAdapter(Resource.class, ComponentsConfiguration.class,
            (Function<Resource, ComponentsConfiguration>) resource -> new ComponentsConfiguration(new ValueMapDecorator(caConfig)));

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

    @Test
    public void testProductUrl() {
        Page page = context.currentPage("/content/product-page");

        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page.html/beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithCustomPage() {
        Page page = context.create().page("/content/custom-page");
        context.currentPage(page);
        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .page("/content/custom-page")
            .map();

        String url = urlProvider.toProductUrl(request, null, params);
        assertEquals("/content/custom-page.html/beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithSubpageAndAnchor() {
        Page page = context.currentPage("/content/product-page");
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
        Page page = context.currentPage("/content/product-page");
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
        Page page = context.currentPage("/content/product-page");
        Map<String, String> params = new ParamsBuilder()
            .sku("MJ01")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        assertEquals("/content/product-page.html/{{url_key}}.html", url);
    }

    @Test
    public void testProductUrlWithGraphQLClient() {
        Page page = context.currentPage("/content/product-page");

        String url = urlProvider.toProductUrl(request, page, "MJ01");
        assertEquals("/content/product-page.html/beaumont-summit-kit.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlNotFoundWithGraphQLClient() {
        Page page = context.currentPage("/content/product-page");

        String url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/content/product-page.html/{{url_key}}.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlWithGraphQLClientMissingParameters() {
        Page page = context.currentPage("/content/product-page");

        String url = urlProvider.toProductUrl(request, page, StringUtils.EMPTY);
        assertEquals("/content/product-page.html/{{url_key}}.html", url);
    }

    @Test
    public void testProductUrlOnlySKU() {
        Page page = context.currentPage("/content/product-page");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithSku.PATTERN);

        String url = urlProvider.toProductUrl(request, page, "MJ01");
        assertEquals("/content/product-page.html/MJ01.html", url);

        // not required when only sku is used
        verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlWithSKUProductNotFound() {
        Page page = context.currentPage("/content/product-page");
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
        Page page = context.currentPage("/content/product-page");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
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
        Page currentPage = context.currentPage("/content/category-page");
        Page productPage = context.currentPage("/content/product-page");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, currentPage);
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
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
        Page currentPage = context.currentPage("/content/category-page");
        Page productPage = context.currentPage("/content/product-page");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, currentPage);
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
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
    public void testCategoryUrl() {
        Page page = context.currentPage("/content/category-page");

        Map<String, String> params = new ParamsBuilder()
            .urlPath("men")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html/men.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpage() {
        Page page = context.currentPage("/content/category-page");
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
        Page page = context.currentPage("/content/category-page");
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
        Page page = context.currentPage("/content/category-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("women/tops/shirts")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page/sub-page-with-urlpath-v2.html/women/tops/shirts.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpageArrayV2() {
        Page page = context.currentPage("/content/category-page");
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
        Page page = context.currentPage("/content/category-page");
        configureSpecificPageStrategy(true);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("category-uid-1.1")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page/sub-page/nested-page.html/category-uid-1.1.html", url);
    }

    @Test
    public void testCategoryUrlMissingParams() {
        Page page = context.currentPage("/content/category-page");
        Map<String, String> params = new ParamsBuilder()
            .uid("UID-42")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/content/category-page.html/{{url_path}}.html", url);
    }

    @Test
    public void testCategoryUrlWithGraphQLClient() {
        Page page = context.currentPage("/content/category-page");

        String url = urlProvider.toCategoryUrl(request, page, "uid-5");
        assertEquals("/content/category-page.html/equipment.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryUrlNotFoundWithGraphQLClient() {
        Page page = context.currentPage("/content/category-page");

        String url = urlProvider.toCategoryUrl(request, page, "uid-99");
        assertEquals("/content/category-page.html/{{url_path}}.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryUrlWithGraphQLClientMissingParameters() {
        Page page = context.currentPage("/content/category-page");

        String url = urlProvider.toCategoryUrl(request, page, StringUtils.EMPTY);
        assertEquals("/content/category-page.html/{{url_path}}.html", url);
    }

    @Test
    public void testProductIdentifierParsingInSuffixUrlKey() {
        context.currentPage("/content/catalog-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");

        String identifier = urlProvider.getProductIdentifier(context.request());
        assertEquals("MJ01", identifier);
        // second access should be cached in request attributes
        identifier = urlProvider.getProductIdentifier(context.request());
        assertEquals("MJ01", identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductIdentifierParsingInSuffixUrlKeyWithGraphqlClientError() {
        context.currentPage("/content/product-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");

        doThrow(new RuntimeException()).when(graphqlClient).execute(any(), any(), any(), any());

        String identifier = urlProvider.getProductIdentifier(context.request());
        Assert.assertNull(identifier);
    }

    @Test
    public void testProductIdentifierParsingInSuffixSKU() {
        context.currentPage("/content/product-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/MJ01.html");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithSku.PATTERN);

        String identifier = urlProvider.getProductIdentifier(context.request());
        assertEquals("MJ01", identifier);

        verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryIdentifierParsingUrlPath() {
        context.currentPage("/content/catalog-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/men/tops-men/jackets-men");

        String identifier = urlProvider.getCategoryIdentifier(context.request());
        assertEquals("MTI==", identifier);
        // second access should be cached in request attributes
        identifier = urlProvider.getCategoryIdentifier(context.request());
        assertEquals("MTI==", identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryIdentifierParsingUrlPathNotFound() {
        context.currentPage("/content/catalog-page");
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/does/not/exist.html");

        String identifier = urlProvider.getCategoryIdentifier(context.request());
        Assert.assertNull(identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCustomProductPageFormat() {
        Page page = context.create().page("/page");
        context.currentPage(page);
        context.request().setQueryString("sku=MJ02");
        context.registerService(UrlFormat.class, new CustomLegacyUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.PRODUCT_PAGE_URL_FORMAT);
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);

        // verify parse
        String identifier = urlProvider.getProductIdentifier(context.request());
        assertEquals("MJ02", identifier);

        // verify format
        String url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/page.html?sku=MJ02", url);

        // verify the product page url format is not used for categories
        url = urlProvider.toCategoryUrl(request, page, "uid-5");
        assertEquals("/page.html/equipment.html", url);
    }

    @Test
    public void testCustomCategoryPageFormat() {
        Page page = context.create().page("/page");
        context.currentPage(page);
        context.request().setQueryString("uid=uid-5");
        context.registerService(UrlFormat.class, new CustomLegacyUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.CATEGORY_PAGE_URL_FORMAT);
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);

        // verify parse
        String identifier = urlProvider.getCategoryIdentifier(context.request());
        assertEquals("uid-5", identifier);

        // verify format
        String url = urlProvider.toCategoryUrl(request, page, "uid-5");
        assertEquals("/page.html?uid=uid-5", url);

        // verify the category page url format is not used for products
        url = urlProvider.toProductUrl(request, page, "MJ01");
        assertEquals("/page.html/beaumont-summit-kit.html", url);
    }

    @Test
    public void testCustomCategoryPageFormatWithSpecificPage() {
        Page page = context.currentPage("/content/category-page");
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
        Page page = context.currentPage("/content/product-page");
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
        Page page = context.currentPage("/content/category-page");
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
        // always create new page to not cache the Resource -> ComponentsConfiguration adaption
        // no configuration, defaults to highest-ranked/first new custom format)
        Page page = context.currentPage(context.create().page("/page1"));
        String url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/page1.html?sku=MJ02#A", url);

        // configure another new custom url format
        page = context.currentPage(context.create().page("/page2"));
        caConfig.put(UrlFormat.PRODUCT_PAGE_URL_FORMAT, AnotherCustomProductPageUrlFormat.class.getName());
        url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/page2.html?sku=MJ02#B", url);

        // configure legacy custom url format
        page = context.currentPage(context.create().page("/page3"));
        caConfig.put(UrlFormat.PRODUCT_PAGE_URL_FORMAT, CustomLegacyUrlFormat.class.getName());
        url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/page3.html?sku=MJ02", url);

        // use default format
        page = context.currentPage(context.create().page("/page4"));
        caConfig.put(UrlFormat.PRODUCT_PAGE_URL_FORMAT, ProductPageWithSku.PATTERN);
        url = urlProvider.toProductUrl(request, page, "MJ02");
        assertEquals("/page4.html/MJ02.html", url);
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
        // always create new page to not cache the Resource -> ComponentsConfiguration adaption
        // no configuration, defaults to highest-ranked/first new custom format)
        Page page = context.currentPage(context.create().page("/page1"));
        String url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/page1.html?uid=uid-1#A", url);

        // configure another new custom url format
        page = context.currentPage(context.create().page("/page2"));
        caConfig.put(UrlFormat.CATEGORY_PAGE_URL_FORMAT, AnotherCustomCategoryPageUrlFormat.class.getName());
        url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/page2.html?uid=uid-1#B", url);

        // configure legacy custom url format
        page = context.currentPage(context.create().page("/page3"));
        caConfig.put(UrlFormat.CATEGORY_PAGE_URL_FORMAT, CustomLegacyUrlFormat.class.getName());
        url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/page3.html?uid=uid-1", url);

        // use default format
        page = context.currentPage(context.create().page("/page4"));
        caConfig.put(UrlFormat.CATEGORY_PAGE_URL_FORMAT, CategoryPageWithUrlKey.PATTERN);
        url = urlProvider.toCategoryUrl(request, page, params);
        assertEquals("/page4.html/bar.html", url);
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
}
