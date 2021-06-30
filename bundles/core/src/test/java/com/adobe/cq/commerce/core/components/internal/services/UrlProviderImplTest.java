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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.UrlProvider.IdentifierLocation;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

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

    @Before
    public void setup() {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        Assert.assertTrue(config.productUrlTemplate().contains("{{"));
        Assert.assertTrue(config.categoryUrlTemplate().contains("{{"));

        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        request = new MockSlingHttpServletRequest(context.resourceResolver());
    }

    @Test
    public void testProductUrl() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        Assert.assertEquals("/content/product-page.beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithCustomPage() {
        Map<String, String> params = new ParamsBuilder()
            .urlKey("beaumont-summit-kit")
            .page("/content/custom-page")
            .map();

        String url = urlProvider.toProductUrl(request, null, params);
        Assert.assertEquals("/content/custom-page.beaumont-summit-kit.html", url);
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
        Assert.assertEquals("/content/product-page/sub-page-2.productId2.html#variantSku", url);
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
        Assert.assertEquals("/content/product-page/sub-page/nested-page.productId1.1.html#variantSku", url);
    }

    @Test
    public void testProductUrlMissingParams() {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductUrlTemplate("{{page}}.{{sku}}.html/{{something}}");
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
        Map<String, String> params = new ParamsBuilder()
            .sku("MJ01")
            .map();

        String url = urlProvider.toProductUrl(request, page, params);
        Assert.assertEquals("/content/product-page.MJ01.html/{{something}}", url);
    }

    @Test
    public void testProductUrlWithGraphQLClient() throws IOException {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom(
            "graphql/magento-graphql-product-result.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String url = urlProvider.toProductUrl(request, page, "MJ01", magentoGraphqlClient);
        Assert.assertEquals("/content/product-page.beaumont-summit-kit.html", url);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(magentoGraphqlClient, Mockito.times(1)).execute(captor.capture());
    }

    @Test
    public void testProductUrlNotFoundWithGraphQLClient() throws IOException {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom(
            "graphql/magento-graphql-product-not-found-result.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String url = urlProvider.toProductUrl(request, page, "MJ02", magentoGraphqlClient);
        Assert.assertEquals("/content/product-page.{{url_key}}.html", url);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(magentoGraphqlClient, Mockito.times(1)).execute(captor.capture());
    }

    @Test
    public void testProductUrlWithGraphQLClientMissingParameters() throws IOException {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        String url = urlProvider.toProductUrl(request, page, "MJ01", null);
        Assert.assertEquals("/content/product-page.{{url_key}}.html", url);

        url = urlProvider.toProductUrl(request, page, StringUtils.EMPTY, null);
        Assert.assertEquals("/content/product-page.{{url_key}}.html", url);

        url = urlProvider.toProductUrl(request, page, StringUtils.EMPTY, Mockito.mock(MagentoGraphqlClient.class));
        Assert.assertEquals("/content/product-page.{{url_key}}.html", url);
    }

    @Test
    public void testProductUrlOnlySKU() throws IOException {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductUrlTemplate("{{page}}.html/{{sku}}"); // configure sku pattern only, this should not do any graphql call
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom(
            "graphql/magento-graphql-product-result.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String url = urlProvider.toProductUrl(request, page, "MJ01", magentoGraphqlClient);
        Assert.assertEquals("/content/product-page.html/MJ01", url);

        Mockito.verify(magentoGraphqlClient, Mockito.never()).execute(Mockito.anyString());
    }

    @Test
    public void testCategoryUrl() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("men/tops")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page.men_tops.html", url);
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
        Assert.assertEquals("/content/category-page/sub-page-with-urlpath.men_tops_shirts.html", url);
    }

    @Test
    public void testNestedCategoryUrl() {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new ParamsBuilder()
            .urlPath("category-uid-1.1")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page/sub-page/nested-page.category-uid-1.1.html", url);
    }

    @Test
    public void testCategoryUrlMissingParams() {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setCategoryUrlTemplate("{{page}}.{{uid}}.html/{{url_path}}");
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
        Map<String, String> params = new ParamsBuilder()
            .uid("UID-42")
            .map();

        String url = urlProvider.toCategoryUrl(request, page, params);
        Assert.assertEquals("/content/category-page.UID-42.html/{{url_path}}", url);
    }

    @Test
    public void testCategoryUrlWithGraphQLClient() throws IOException {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom(
            "graphql/magento-graphql-category-list-result.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String url = urlProvider.toCategoryUrl(request, page, "uid-5", magentoGraphqlClient);
        Assert.assertEquals("/content/category-page.equipment.html", url);
    }

    @Test
    public void testCategoryUrlNotFoundWithGraphQLClient() throws IOException {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom(
            "graphql/magento-graphql-empty-data.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String url = urlProvider.toCategoryUrl(request, page, "MJ02", magentoGraphqlClient);
        Assert.assertEquals("/content/category-page.{{url_path}}.html", url);
    }

    @Test
    public void testCategoryUrlWithGraphQLClientMissingParameters() throws IOException {
        Page page = context.currentPage("/content/category-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        String url = urlProvider.toCategoryUrl(request, page, "MJ01", null);
        Assert.assertEquals("/content/category-page.{{url_path}}.html", url);

        url = urlProvider.toCategoryUrl(request, page, StringUtils.EMPTY, null);
        Assert.assertEquals("/content/category-page.{{url_path}}.html", url);

        url = urlProvider.toCategoryUrl(request, page, StringUtils.EMPTY, Mockito.mock(MagentoGraphqlClient.class));
        Assert.assertEquals("/content/category-page.{{url_path}}.html", url);
    }

    @Test
    public void testProductIdentifierParsingInSelectorUrlKey() throws IOException {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("lazy.beaumont-summit-kit");

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-product-sku.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String identifier = urlProvider.getProductIdentifier(context.request(), magentoGraphqlClient);
        Assert.assertEquals("MJ01", identifier);
    }

    @Test
    public void testProductIdentifierParsingInSelectorSKU() {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierType(ProductIdentifierType.SKU);
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("lazy.beaumont-summit-kit");

        String identifier = urlProvider.getProductIdentifier(context.request(), null);
        Assert.assertEquals("beaumont-summit-kit", identifier);
    }

    @Test
    public void testProductIdentifierParsingInSuffixUrlKey() throws IOException {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierLocation(IdentifierLocation.SUFFIX);
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/MJ01");

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-product-sku.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String identifier = urlProvider.getProductIdentifier(context.request(), magentoGraphqlClient);
        Assert.assertEquals("MJ01", identifier);
    }

    @Test
    public void testProductIdentifierParsingInSuffixSKU() {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierLocation(IdentifierLocation.SUFFIX);
        config.setProductIdentifierType(ProductIdentifierType.SKU);
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/MJ01");
        String identifier = urlProvider.getProductIdentifier(context.request(), null);
        Assert.assertEquals("MJ01", identifier);
    }

    @Test
    public void testProductIdentifierParsingInNoSuffix() {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierLocation(IdentifierLocation.SUFFIX);
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        String identifier = urlProvider.getProductIdentifier(context.request(), null);
        Assert.assertNull(identifier);
    }

    @Test
    public void testProductIdentifierParsingInQueryParameterUrlKey() throws IOException {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierLocation(IdentifierLocation.QUERY_PARAM);
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-product-sku.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(config.identifierQueryParamater(), "MJ01");
        params.put("other", "abc");
        request.setParameterMap(params);

        String identifier = urlProvider.getProductIdentifier(request, magentoGraphqlClient);
        Assert.assertEquals("MJ01", identifier);
    }

    @Test
    public void testProductIdentifierParsingInQueryParameterSKU() {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierLocation(IdentifierLocation.QUERY_PARAM);
        config.setProductIdentifierType(ProductIdentifierType.SKU);
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(config.identifierQueryParamater(), "MJ01");
        params.put("other", "abc");
        request.setParameterMap(params);

        String identifier = urlProvider.getProductIdentifier(request, null);
        Assert.assertEquals("MJ01", identifier);
    }

    @Test
    public void testProductIdentifierParsingEmptyQueryParameter() {
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierLocation(IdentifierLocation.QUERY_PARAM);

        urlProvider = new UrlProviderImpl();
        urlProvider.activate(config);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(config.identifierQueryParamater(), StringUtils.EMPTY);
        params.put("other", "abc");
        request.setParameterMap(params);

        String identifier = urlProvider.getProductIdentifier(request, null);
        Assert.assertNull(identifier);
    }

    @Test
    public void testProductIdentifierMissingGraphQlClient() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("lazy.beaumont-summit-kit");

        String identifier = urlProvider.getProductIdentifier(context.request(), null);
        Assert.assertNull(identifier);
    }

    @Test
    public void testCategoryIdentifierParsingUrlPath() throws IOException {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("men_tops-men_jackets-men");

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom(
            "graphql/magento-graphql-category-uid.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String identifier = urlProvider.getCategoryIdentifier(context.request(), magentoGraphqlClient);
        Assert.assertEquals("MTI==", identifier);
    }

    @Test
    public void testCategoryIdentifierParsingUrlPathNotFound() throws IOException {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("men_tops-men_jackets-men");

        GraphqlClient graphqlClient = Mockito.spy(Utils.setupGraphqlClientWithHttpResponseFrom(
            "graphql/magento-graphql-empty-data.json"));
        MagentoGraphqlClient magentoGraphqlClient = Mockito.mock(MagentoGraphqlClient.class);
        Whitebox.setInternalState(magentoGraphqlClient, "graphqlClient", graphqlClient);
        Mockito.when(magentoGraphqlClient.execute(Mockito.anyString())).thenCallRealMethod();

        String identifier = urlProvider.getCategoryIdentifier(context.request(), magentoGraphqlClient);
        Assert.assertNull(identifier);
    }

    @Test
    public void testCategoryIdentifierMissingGraphQlClient() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("men_tops-men_jackets-men");

        String identifier = urlProvider.getCategoryIdentifier(context.request(), null);
        Assert.assertNull(identifier);
    }

    @Test
    public void testStringSubstitutor() {
        Map<String, String> params = new HashMap<>();

        // empty params, valid prefix & suffix
        UrlProviderImpl.StringSubstitutor sub = new UrlProviderImpl.StringSubstitutor(params, "${", "}");
        Assert.assertEquals("Wrong substitution", "${test}", sub.replace("${test}"));

        // valid params, no prefix & suffix
        params.put("test", "value");
        sub = new UrlProviderImpl.StringSubstitutor(params, null, null);
        Assert.assertEquals("Wrong substitution", "${value}-value", sub.replace("${test}-test"));

        // valid params, prefix & suffix
        sub = new UrlProviderImpl.StringSubstitutor(params, "${", "}");
        Assert.assertEquals("Wrong substitution", "value-value", sub.replace("${test}-${test}"));
    }
}
