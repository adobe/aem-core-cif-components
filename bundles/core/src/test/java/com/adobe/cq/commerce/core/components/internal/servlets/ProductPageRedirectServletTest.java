/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlKey;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductPageRedirectServletTest {
    private ProductPageRedirectServlet servlet;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private Resource resource;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private RequestPathInfo requestPathInfo;
    private UrlProviderImpl urlProvider;
    private MockUrlProviderConfiguration config;

    @Mock
    private PageManagerFactory pageManagerFactory;

    @Mock
    private PageManager pageManager;

    @Rule
    public final AemContext context = createContext("/context/jcr-content-redirect-servlet.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Before
    public void setUp() {
        servlet = new ProductPageRedirectServlet();

        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
        when(request.getResource()).thenReturn(resource);

        Page currentPage = context.pageManager().getPage("/content/venia/us/en");
        when(pageManager.getContainingPage(resource)).thenReturn(currentPage);
        when(pageManagerFactory.getPageManager(resourceResolver)).thenReturn(pageManager);

        config = new MockUrlProviderConfiguration();
        urlProvider = new UrlProviderImpl();

        Whitebox.setInternalState(servlet, "urlProvider", urlProvider);
        Whitebox.setInternalState(servlet, "pageManagerFactory", pageManagerFactory);
    }

    @Test
    public void testMissingProductSuffix() throws IOException {
        when(requestPathInfo.getSuffix()).thenReturn(null);

        servlet.doGet(request, response);
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing redirect suffix.");
    }

    @Test
    public void testWrongSuffixLength() throws IOException {
        when(requestPathInfo.getSuffix()).thenReturn("/some/wrong/suffix");

        servlet.doGet(request, response);
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Redirect suffix has wrong format.");
    }

    @Test
    public void testSkuMatchingUrlProviderConfig() throws IOException {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] { ProductPageRedirectServlet.SELECTOR });
        when(requestPathInfo.getSuffix()).thenReturn("/test_sku");

        config.setProductPageUrlFormat(ProductPageWithSku.PATTERN);

        urlProvider.activate(config);

        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "/content/venia/us/en/products/product-page.html/test_sku.html");
    }

    @Test
    public void testSkuNotMatchingUrlProviderConfig() throws IOException {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] { ProductPageRedirectServlet.SELECTOR });
        when(requestPathInfo.getSuffix()).thenReturn("/test_sku");

        config.setProductPageUrlFormat(ProductPageWithUrlKey.PATTERN);

        urlProvider.activate(config);

        MagentoGraphqlClient mockClient = mock(MagentoGraphqlClient.class);
        Query mockQuery = mock(Query.class);
        Products products = mock(Products.class);
        ProductInterface productInterface = mock(ProductInterface.class);

        when(productInterface.getUrlKey()).thenReturn("test_url_key");
        when(products.getTotalCount()).thenReturn(1);
        when(products.getItems()).thenReturn(Collections.singletonList(productInterface));
        when(mockQuery.getProducts()).thenReturn(products);

        GraphqlResponse<Query, Error> graphQlResponse = new GraphqlResponse<Query, Error>();
        graphQlResponse.setData(mockQuery);

        when(request.adaptTo(MagentoGraphqlClient.class)).thenReturn(mockClient);
        when(mockClient.execute(any())).thenReturn(graphQlResponse);

        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        verify(response).setHeader("Location", "/content/venia/us/en/products/product-page.html/test_url_key.html");
    }
}
