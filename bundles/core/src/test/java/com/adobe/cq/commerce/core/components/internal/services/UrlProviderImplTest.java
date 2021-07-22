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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.client.MagentoGraphqlClientImpl;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.*;

public class UrlProviderImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-page-filter.json");
    private final UrlProviderImpl urlProvider = new UrlProviderImpl();

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    private MockSlingHttpServletRequest request;
    private HttpClient httpClient;
    private GraphqlClient graphqlClient;

    @Before
    public void setup() throws Exception {
        request = new MockSlingHttpServletRequest(context.resourceResolver());

        GraphqlClientConfiguration graphqlClientConfiguration = mock(GraphqlClientConfiguration.class);
        when(graphqlClientConfiguration.httpMethod()).thenReturn(HttpMethod.POST);
        httpClient = mock(HttpClient.class);
        graphqlClient = spy(new GraphqlClientImpl());
        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "configuration", graphqlClientConfiguration);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        MagentoGraphqlClient mockClient = spy(new MagentoGraphqlClientImpl(request));
        Whitebox.setInternalState(mockClient, "graphqlClient", graphqlClient);
        context.registerAdapter(SlingHttpServletRequest.class, MagentoGraphqlClient.class, mockClient);

        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ01\"}}");
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

    @Test
    public void testProductUrl() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        Assert.assertEquals("/content/product-page.html/beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithCustomPage() {
        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .page("/content/custom-page")
            .map();

        String url = urlProvider.toProductUrl(request, null, params);
        Assert.assertEquals("/content/custom-page.html/beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithSubpageAndAnchor() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlKey("productId2")
            .variantSku("variantSku")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        Assert.assertEquals("/content/product-page/sub-page-2.html/productId2.html#variantSku", url);
    }

    @Test
    public void testNestedProductUrlWithAnchor() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlKey("productId1.1")
            .variantSku("variantSku")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        Assert.assertEquals("/content/product-page/sub-page/nested-page.html/productId1.1.html#variantSku", url);
    }

    @Test
    public void testProductUrlMissingParams() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
        Map<String, String> params = new ParamsBuilder()
            .sku("MJ01")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        Assert.assertEquals("/content/product-page.html/{{url_key}}.html", url);
    }

    @Test
    public void testProductUrlWithGraphQLClient() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        String url = urlProvider.toProductUrl(request, page, "MJ01");
        Assert.assertEquals("/content/product-page.html/beaumont-summit-kit.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlNotFoundWithGraphQLClient() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        String url = urlProvider.toProductUrl(request, page, "MJ02");
        Assert.assertEquals("/content/product-page.html/{{url_key}}.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductUrlWithGraphQLClientMissingParameters() throws IOException {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        String url = urlProvider.toProductUrl(request, page, StringUtils.EMPTY);
        Assert.assertEquals("/content/product-page.html/{{url_key}}.html", url);
    }

    @Test
    public void testProductUrlOnlySKU() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithSku.PATTERN);

        String url = urlProvider.toProductUrl(request, page, "MJ01");
        Assert.assertEquals("/content/product-page.html/MJ01.html", url);

        // not required when only sku is used
        verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryUrl() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("men")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page.html/men.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpage() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .uid("MTE=")
            .urlPath("men/tops/shirts")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page/sub-page-with-urlpath.html/men/tops/shirts.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpageArray() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .uid("MTF=")
            .urlPath("men/bottoms")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page/sub-page-with-urlpath-array.html/men/bottoms.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpageV2() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("women/tops/shirts")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page/sub-page-with-urlpath-v2.html/women/tops/shirts.html", url);
    }

    @Test
    public void testCategoryUrlWithSubpageArrayV2() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("women/bottoms/shorts")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page/sub-page-with-urlpath-array-v2.html/women/bottoms/shorts.html", url);
    }

    @Test
    public void testNestedCategoryUrl() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("category-uid-1.1")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page/sub-page/nested-page.html/category-uid-1.1.html", url);
    }

    @Test
    public void testCategoryUrlMissingParams() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
        Map<String, String> params = new ParamsBuilder()
            .uid("UID-42")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page.html/{{url_path}}.html", url);
    }

    @Test
    public void testCategoryUrlWithGraphQLClient() throws IOException {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        String url = urlProvider.toCategoryUrl(request, page, "uid-5");
        Assert.assertEquals("/content/category-page.html/equipment.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryUrlNotFoundWithGraphQLClient() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        String url = urlProvider.toCategoryUrl(request, page, "uid-99");
        Assert.assertEquals("/content/category-page.html/{{url_path}}.html", url);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryUrlWithGraphQLClientMissingParameters() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        String url = urlProvider.toCategoryUrl(request, page, StringUtils.EMPTY);
        Assert.assertEquals("/content/category-page.html/{{url_path}}.html", url);
    }

    @Test
    public void testProductIdentifierParsingInSuffixUrlKey() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");

        String identifier = urlProvider.getProductIdentifier(context.request());
        Assert.assertEquals("MJ01", identifier);
        // second access should be cached in request attributes
        identifier = urlProvider.getProductIdentifier(context.request());
        Assert.assertEquals("MJ01", identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductIdentifierParsingInSuffixSKU() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/MJ01.html");
        MockOsgi.deactivate(urlProvider, context.bundleContext());
        MockOsgi.activate(urlProvider, context.bundleContext(), "productPageUrlFormat", ProductPageWithSku.PATTERN);

        String identifier = urlProvider.getProductIdentifier(context.request());
        Assert.assertEquals("MJ01", identifier);

        verify(graphqlClient, never()).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryIdentifierParsingUrlPath() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/men/tops-men/jackets-men");

        String identifier = urlProvider.getCategoryIdentifier(context.request());
        Assert.assertEquals("MTI==", identifier);
        // second access should be cached in request attributes
        identifier = urlProvider.getCategoryIdentifier(context.request());
        Assert.assertEquals("MTI==", identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryIdentifierParsingUrlPathNotFound() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/does/not/exist.html");

        String identifier = urlProvider.getCategoryIdentifier(context.request());
        Assert.assertNull(identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCustomProductPageFormat() {
        context.request().setQueryString("sku=MJ02");
        context.registerService(UrlFormat.class, new CustomUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.PRODUCT_PAGE_URL_FORMAT);
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);

        // verify parse
        String identifier = urlProvider.getProductIdentifier(context.request());
        Assert.assertEquals("MJ02", identifier);

        // verify format
        Page page = context.create().page("/page");
        String url = urlProvider.toProductUrl(request, page, "MJ02");
        Assert.assertEquals("/page.html?sku=MJ02", url);

        // verify the product page url format is not used for categories
        url = urlProvider.toCategoryUrl(request, page, "uid-5");
        Assert.assertEquals("/page.html/equipment.html", url);
    }

    @Test
    public void testCustomCategoryPageFormat() {
        context.request().setQueryString("uid=uid-5");
        context.registerService(UrlFormat.class, new CustomUrlFormat(), UrlFormat.PROP_USE_AS, UrlFormat.CATEGORY_PAGE_URL_FORMAT);
        // registering the custom format causes a new service to be created
        UrlProvider urlProvider = context.getService(UrlProvider.class);

        // verify parse
        String identifier = urlProvider.getCategoryIdentifier(context.request());
        Assert.assertEquals("uid-5", identifier);

        // verify format
        Page page = context.create().page("/page");
        String url = urlProvider.toCategoryUrl(request, page, "uid-5");
        Assert.assertEquals("/page.html?uid=uid-5", url);

        // verify the category page url format is not used for products
        url = urlProvider.toProductUrl(request, page, "MJ01");
        Assert.assertEquals("/page.html/beaumont-summit-kit.html", url);
    }

    private static class CustomUrlFormat implements UrlFormat {
        @Override
        public String format(Map<String, String> parameters) {
            return parameters.get("page") + ".html" +
                (StringUtils.isNotEmpty(parameters.get("sku")) ? "?sku=" + parameters.get("sku") : "") +
                (StringUtils.isNotEmpty(parameters.get("uid")) ? "?uid=" + parameters.get("uid") : "");
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
}
