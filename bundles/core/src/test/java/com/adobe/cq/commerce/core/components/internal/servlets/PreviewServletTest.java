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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.mockito.Mockito.*;

public class PreviewServletTest {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

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
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
        response = spy(context.response());

        resourceResolver = mock(ResourceResolver.class);
        when(request.getResourceResolver()).thenReturn(resourceResolver);

        requestPathInfo = mock(RequestPathInfo.class);
        when(request.getRequestPathInfo()).thenReturn(requestPathInfo);

        urlProvider = mock(UrlProvider.class);
        Whitebox.setInternalState(servlet, "urlProvider", urlProvider);
    }

    @Test
    public void testInvalidMode() throws IOException {
        // mock WCMMode
        request.setAttribute(WCMMode.class.getName(), WCMMode.DISABLED);

        // handle request
        servlet.doGet(request, response);

        // verify error
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "The request not permitted");
    }

    @Test
    public void testReferer_invalidContext() throws IOException {
        // mock referer
        when(request.getHeader(HttpHeaders.REFERER)).thenReturn("/invalid/path");

        // handle request
        servlet.doGet(request, response);

        // verify error
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "The path of the edited page cannot be determined");
    }

    @Test
    public void testReferer_invalidPage() throws IOException {
        // mock referer and page
        when(request.getHeader(HttpHeaders.REFERER)).thenReturn("/editor.html/path/to/invalid/page.html");
        String path = "/path/to/invalid/page.html";
        when(resourceResolver.resolve(path)).thenReturn(new NonExistingResource(resourceResolver, path));

        // handle request
        servlet.doGet(request, response);

        // verify error
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "The path of the edited page cannot be determined");
    }

    @Test
    public void testPreviewProduct() throws IOException {
        // mock referer
        when(request.getHeader(HttpHeaders.REFERER)).thenReturn("/editor.html/path/to/valid/product/page.html");

        // mock selectors
        when(requestPathInfo.getSelectors()).thenReturn(new String[] { "previewproduct" });

        // mock page
        Resource resource = mock(Resource.class);
        when(resourceResolver.resolve("/path/to/valid/product/page.html")).thenReturn(resource);
        when(resource.adaptTo(Page.class)).thenReturn(mock(Page.class));

        // mock UrlProvider
        String PREVIEW_PRODUCT_URL = "/dummy/preview/url.key.html#variant_key";
        when(urlProvider.toProductUrl(any(), any(), Matchers.<Map<String, String>>any())).thenReturn(PREVIEW_PRODUCT_URL);

        // mock request parameters
        when(request.getParameter(UrlProvider.UID_PARAM)).thenReturn("testUid");
        when(request.getParameter(UrlProvider.SKU_PARAM)).thenReturn("testSku");
        when(request.getParameter(UrlProvider.URL_KEY_PARAM)).thenReturn("testUrlKey");
        when(request.getParameter(UrlProvider.VARIANT_SKU_PARAM)).thenReturn("testVariantSku");
        when(request.getParameter(UrlProvider.VARIANT_URL_KEY_PARAM)).thenReturn("testVariantUrlKey");
        when(request.getParameter(UrlProvider.URL_PATH_PARAM)).thenReturn("testUrlPath");
        when(request.getParameter("invalidParam")).thenReturn("invalidValue");

        // handle request
        servlet.doGet(request, response);

        // verify UrlProvider called and capture arguments
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(urlProvider).toProductUrl(any(), any(), captor.capture());

        // verify UrlProvider parameters
        Map<String, String> params = captor.getValue();
        Assert.assertEquals("Incorrect number of parameters", 7, params.size());
        Assert.assertEquals("Incorrect UID parameter", "testUid", params.get(UrlProvider.UID_PARAM));
        Assert.assertEquals("Incorrect SKU parameter", "testSku", params.get(UrlProvider.SKU_PARAM));
        Assert.assertEquals("Incorrect URL_KEY parameter", "testUrlKey", params.get(UrlProvider.URL_KEY_PARAM));
        Assert.assertEquals("Incorrect VARIANT_SKU parameter", "testVariantSku", params.get(UrlProvider.VARIANT_SKU_PARAM));
        Assert.assertEquals("Incorrect VARIANT_URL_KEY parameter", "testVariantUrlKey", params.get(UrlProvider.VARIANT_URL_KEY_PARAM));
        Assert.assertEquals("Incorrect URL_PATH parameter", "testUrlPath", params.get(UrlProvider.URL_PATH_PARAM));

        // verify we redirect to the correct url
        verify(response).sendRedirect(PREVIEW_PRODUCT_URL);
    }

    @Test
    public void testPreviewCategory() throws IOException {
        // mock referer
        when(request.getHeader(HttpHeaders.REFERER)).thenReturn("/editor.html/path/to/valid/category/page.html");

        // mock selectors
        when(requestPathInfo.getSelectors()).thenReturn(new String[] { "previewcategory" });

        // mock page
        Resource resource = mock(Resource.class);
        when(resourceResolver.resolve("/path/to/valid/category/page.html")).thenReturn(resource);
        when(resource.adaptTo(Page.class)).thenReturn(mock(Page.class));

        // mock UrlProvider
        String PREVIEW_CATEGORY_URL = "/dummy/preview/url.id.html";
        when(urlProvider.toCategoryUrl(any(SlingHttpServletRequest.class), any(Page.class), any(Map.class))).thenReturn(
            PREVIEW_CATEGORY_URL);

        // mock request parameters
        when(request.getParameter(UrlProvider.UID_PARAM)).thenReturn("testUid");
        when(request.getParameter(UrlProvider.SKU_PARAM)).thenReturn("testSku");
        when(request.getParameter(UrlProvider.URL_KEY_PARAM)).thenReturn("testUrlKey");
        when(request.getParameter(UrlProvider.VARIANT_SKU_PARAM)).thenReturn("testVariantSku");
        when(request.getParameter(UrlProvider.VARIANT_URL_KEY_PARAM)).thenReturn("testVariantUrlKey");
        when(request.getParameter(UrlProvider.URL_PATH_PARAM)).thenReturn("testUrlPath");
        when(request.getParameter("invalidParam")).thenReturn("invalidValue");

        // handle request
        servlet.doGet(request, response);

        // verify UrlProvider called and capture arguments
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(urlProvider).toCategoryUrl(any(), any(), captor.capture());

        // verify UrlProvider parameters
        Map<String, String> params = captor.getValue();
        Assert.assertEquals("Incorrect number of parameters", 7, params.size());
        Assert.assertEquals("Incorrect UID parameter", "testUid", params.get(UrlProvider.UID_PARAM));
        Assert.assertEquals("Incorrect SKU parameter", "testSku", params.get(UrlProvider.SKU_PARAM));
        Assert.assertEquals("Incorrect URL_KEY parameter", "testUrlKey", params.get(UrlProvider.URL_KEY_PARAM));
        Assert.assertEquals("Incorrect VARIANT_SKU parameter", "testVariantSku", params.get(UrlProvider.VARIANT_SKU_PARAM));
        Assert.assertEquals("Incorrect VARIANT_URL_KEY parameter", "testVariantUrlKey", params.get(UrlProvider.VARIANT_URL_KEY_PARAM));
        Assert.assertEquals("Incorrect URL_PATH parameter", "testUrlPath", params.get(UrlProvider.URL_PATH_PARAM));

        // verify we redirect to the correct url
        verify(response).sendRedirect(PREVIEW_CATEGORY_URL);
    }
}
