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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.client.MagentoGraphqlClientImpl;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import com.google.common.base.Function;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.*;

public class UrlProviderImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-page-filter.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    private UrlProviderImpl urlProvider;
    private MockSlingHttpServletRequest request;
    private HttpClient httpClient;
    private GraphqlClient graphqlClient;

    @Before
    public void setup() throws Exception {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

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
        Utils.setupHttpResponse("graphql/magento-graphql-category-uid.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path:{eq:\"men/tops-men/jackets-men\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK,
            "categoryList(filters:{url_path:{eq:\"does/not/exist\"}");
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
        MockUrlProviderConfiguration newConfig = new MockUrlProviderConfiguration();
        newConfig.setProductPageUrlFormat(ProductPageWithSku.PATTERN);
        urlProvider.activate(newConfig);
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

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testProductIdentifierParsingInSuffixSKU() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/MJ01.html");

        MockUrlProviderConfiguration newConfig = new MockUrlProviderConfiguration();
        newConfig.setProductPageUrlFormat(ProductPageWithSku.PATTERN);
        urlProvider.activate(newConfig);

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

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testCategoryIdentifierParsingUrlPathNotFound() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/does_not_exist.html");

        String identifier = urlProvider.getCategoryIdentifier(context.request());
        Assert.assertNull(identifier);

        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }
}
