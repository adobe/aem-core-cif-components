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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreviewServletTest {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private PreviewServlet servlet;
    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;
    private ResourceResolver resourceResolver;
    private RequestPathInfo requestPathInfo;
    private UrlProvider urlProvider;

    @Before
    public void setUp() {
        servlet = new PreviewServlet();
        request = spy(context.request());
        response = spy(context.response());

        resourceResolver = mock(ResourceResolver.class);
        when(request.getResourceResolver()).thenReturn(resourceResolver);

        requestPathInfo = mock(RequestPathInfo.class);
        when(request.getRequestPathInfo()).thenReturn(requestPathInfo);

        urlProvider = mock(UrlProvider.class);
        Whitebox.setInternalState(servlet, "urlProvider", urlProvider);
    }

    @Test
    public void testReferer_invalidContext() throws ServletException, IOException {
        thrown.expect(ServletException.class);
        thrown.expectMessage("The path of the edited page cannot be determined");
        when(request.getHeader("Referer")).thenReturn("/invalid/path");

        servlet.doGet(request, null);
    }

    @Test
    public void testReferer_invalidPage() throws ServletException, IOException {
        thrown.expect(ServletException.class);
        thrown.expectMessage("The path of the edited page cannot be determined");

        when(request.getHeader("Referer")).thenReturn("/editor.html/path/to/invalid/page.html");
        when(resourceResolver.getResource("/path/to/invalid/page")).thenReturn(null);

        servlet.doGet(request, null);
    }

    @Test
    public void testPreviewProduct() throws ServletException, IOException {
        // mock referer
        when(request.getHeader("Referer")).thenReturn("/editor.html/path/to/valid/product/page.html");

        // mock selectors
        when(requestPathInfo.getSelectors()).thenReturn(new String[] { "previewproduct" });

        // mock page
        Resource resource = mock(Resource.class);
        when(resourceResolver.getResource("/path/to/valid/product/page")).thenReturn(resource);
        when(resource.adaptTo(Page.class)).thenReturn(mock(Page.class));

        // mock UrlProvider
        String PREVIEW_PRODUCT_URL = "/dummy/preview/url.key.html#variant_key";
        when(urlProvider.toProductUrl(any(SlingHttpServletRequest.class), any(Page.class), any(Map.class))).thenReturn(PREVIEW_PRODUCT_URL);

        // handle request
        servlet.doGet(request, response);

        // verify we redirect to the correct url
        verify(response).sendRedirect(PREVIEW_PRODUCT_URL);
    }

    @Test
    public void testPreviewCategory() throws ServletException, IOException {
        // mock referer
        when(request.getHeader("Referer")).thenReturn("/editor.html/path/to/valid/category/page.html");

        // mock selectors
        when(requestPathInfo.getSelectors()).thenReturn(new String[] { "previewcategory" });

        // mock page
        Resource resource = mock(Resource.class);
        when(resourceResolver.getResource("/path/to/valid/category/page")).thenReturn(resource);
        when(resource.adaptTo(Page.class)).thenReturn(mock(Page.class));

        // mock UrlProvider
        String PREVIEW_CATEGORY_URL = "/dummy/preview/url.id.html";
        when(urlProvider.toCategoryUrl(any(SlingHttpServletRequest.class), any(Page.class), any(Map.class))).thenReturn(
            PREVIEW_CATEGORY_URL);

        // handle request
        servlet.doGet(request, response);

        // verify we redirect to the correct url
        verify(response).sendRedirect(PREVIEW_CATEGORY_URL);
    }
}
