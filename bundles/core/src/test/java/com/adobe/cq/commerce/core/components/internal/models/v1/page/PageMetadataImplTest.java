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

package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.client.MockExternalizer;
import com.adobe.cq.commerce.core.components.client.MockLaunch;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.page.PageMetadata;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.launches.api.Launch;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageMetadataImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content-breadcrumb.json");

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("cq:graphqlClient", "default", "magentoStore", "my-store"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);

                context.registerInjectActivateService(new SearchFilterServiceImpl());
                context.registerInjectActivateService(new SearchResultsServiceImpl());

                context.registerAdapter(Resource.class, ComponentsConfiguration.class, MOCK_CONFIGURATION_OBJECT);
                context.registerAdapter(Resource.class, Launch.class, (Function<Resource, Launch>) resource -> new MockLaunch(resource));

                context.registerService(Externalizer.class, new MockExternalizer());
            },
            ResourceResolverType.JCR_MOCK);
    }

    private GraphqlClient graphqlClient;

    public void prepareModel(String pagePath) throws Exception {
        Page page = context.currentPage(pagePath);

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(context.currentResource());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, context.currentResource().getValueMap());

        XSSAPI xssApi = mock(XSSAPI.class);
        when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
        slingBindings.put("xssApi", xssApi);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyBoolean())).then(i -> i.getArgumentAt(1, Boolean.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);
    }

    @Test
    public void testPageMetadataModelOnProductPage() throws Exception {
        testPageMetadataModelOnProductPage("/content/venia/us/en/products/product-page");
    }

    @Test
    public void testPageMetadataModelOnProductPageOnLaunch() throws Exception {
        testPageMetadataModelOnProductPage("/content/launches/2020/09/14/mylaunch/content/venia/us/en/products/product-page");
    }

    private void testPageMetadataModelOnProductPage(String pagePath) throws Exception {
        graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-product-result.json");

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("beaumont-summit-kit");
        context.request().setServletPath(pagePath + ".beaumont-summit-kit.html"); // used by context.request().getRequestURI();

        prepareModel(pagePath);
        PageMetadata pageMetadataModel = context.request().adaptTo(PageMetadata.class);

        Assert.assertEquals("Some product meta description", pageMetadataModel.getMetaDescription());
        Assert.assertEquals("Some product meta keywords", pageMetadataModel.getMetaKeywords());
        Assert.assertEquals("Some product meta title", pageMetadataModel.getMetaTitle());
        Assert.assertEquals("https://author" + pagePath + ".beaumont-summit-kit.html", pageMetadataModel.getCanonicalUrl());
    }

    @Test
    public void testPageMetadataModelOnCategoryPage() throws Exception {
        testPageMetadataModelOnCategoryPage("/content/venia/us/en/products/category-page");
    }

    @Test
    public void testPageMetadataModelOnCategoryPageOnLaunch() throws Exception {
        testPageMetadataModelOnCategoryPage("/content/launches/2020/09/14/mylaunch/content/venia/us/en/products/category-page");
    }

    private void testPageMetadataModelOnCategoryPage(String pagePath) throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        graphqlClient = Mockito.spy(new GraphqlClientImpl());
        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "httpMethod", HttpMethod.POST);

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-result-with-category.json", httpClient, HttpStatus.SC_OK, "{products");

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("6");
        context.request().setServletPath(pagePath + ".6.html"); // used by context.request().getRequestURI();

        prepareModel(pagePath);
        PageMetadata pageMetadataModel = context.request().adaptTo(PageMetadata.class);

        Assert.assertEquals("Some category meta description", pageMetadataModel.getMetaDescription());
        Assert.assertEquals("Some category meta keywords", pageMetadataModel.getMetaKeywords());
        Assert.assertEquals("Some category meta title", pageMetadataModel.getMetaTitle());
        Assert.assertEquals("https://author" + pagePath + ".6.html", pageMetadataModel.getCanonicalUrl());
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
