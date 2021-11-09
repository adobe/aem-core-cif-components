/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlethelpers.MockRequestDispatcherFactory;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.day.cq.wcm.api.WCMMode;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;

public class SpecificPageServletTest {

    @Rule
    public final AemContext context = TestContext.newAemContext("/context/jcr-page-filter.json");

    private SpecificPageServlet servlet;
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private MockRequestDispatcherFactoryImpl requestDispatcherFactory;

    @Before
    public void setUp() throws Exception {
        servlet = new SpecificPageServlet();
        request = Mockito.spy(new MockSlingHttpServletRequest(context.resourceResolver()));
        response = new MockSlingHttpServletResponse();

        requestDispatcherFactory = Mockito.spy(new MockRequestDispatcherFactoryImpl());
        request.setRequestDispatcherFactory(requestDispatcherFactory);

        // The filter only does something on publish, so only when WCMMode is DISABLED
        request.setAttribute(WCMMode.class.getName(), WCMMode.DISABLED);

        context.registerInjectActivateService(servlet);
    }

    @Test
    public void testForwardingWithStringProperty() throws IOException, ServletException {
        // The Servlet gets the jcr:content under the cq:Page node
        request.setResource(context.resourceResolver().resolve("/content/product-page/jcr:content"));

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(SpecificPageServlet.SELECTOR + ".productId1");
        servlet.doGet(request, response);

        // Check that the request dispatcher is called for the matching sub-page
        ResourcePathMatcher matcher = new ResourcePathMatcher("/content/product-page/sub-page");

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("productId1");

        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(argThat(matcher), eq(options));
    }

    @Test
    public void testNoopWcmmode() throws IOException, ServletException {
        // The Servlet gets the jcr:content under the cq:Page node
        request.setResource(context.resourceResolver().resolve("/content/product-page/jcr:content"));

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(SpecificPageServlet.SELECTOR + ".productId1");

        // Verify that the filter does nothing if WCMMode is something else than DISABLED
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        servlet.doGet(request, response);

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("productId1");

        // Verify that the request is forwarded to same page
        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(eq(request.getResource()), eq(options));
    }

    @Test
    public void testForwardingWithStringArrayProperty() throws IOException, ServletException {
        // The Servlet gets the jcr:content under the cq:Page node
        request.setResource(context.resourceResolver().resolve("/content/category-page/jcr:content"));

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(SpecificPageServlet.SELECTOR + ".category-uid-2");

        servlet.doGet(request, response);

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("category-uid-2");

        // Check that the request dispatcher is called for the matching sub-page
        ResourcePathMatcher matcher = new ResourcePathMatcher("/content/category-page/sub-page");
        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(argThat(matcher), eq(options));
    }

    @Test
    public void testForwardingWithMatchingUrlPath() throws IOException, ServletException {
        ProductListImpl productList = Mockito.mock(ProductListImpl.class);
        Mockito.when(productList.getUrlPath()).thenReturn("men/tops/shirts");
        context.registerAdapter(SlingHttpServletRequest.class, ProductList.class, productList);

        // The Servlet gets the jcr:content under the cq:Page node
        request.setResource(context.resourceResolver().resolve("/content/category-page/jcr:content"));

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(SpecificPageServlet.SELECTOR + ".category-uid-3");
        servlet.doGet(request, response);

        // Check that the request dispatcher is called for the matching sub-page
        ResourcePathMatcher matcher = new ResourcePathMatcher("/content/category-page/sub-page-with-urlpath");

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("category-uid-3");

        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(argThat(matcher), eq(options));
    }

    @Test
    public void testForwardingWithNonMatchingUrlPath() throws IOException, ServletException {
        ProductListImpl productList = Mockito.mock(ProductListImpl.class);
        Mockito.when(productList.getUrlPath()).thenReturn("women/accessories");
        context.registerAdapter(SlingHttpServletRequest.class, ProductList.class, productList);

        // The Servlet gets the jcr:content under the cq:Page node
        request.setResource(context.resourceResolver().resolve("/content/category-page/jcr:content"));

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(SpecificPageServlet.SELECTOR + ".women/accessories");
        servlet.doGet(request, response);

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("women/accessories");

        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(eq(request.getResource()), eq(options));
        Mockito.verify(request).adaptTo(ProductList.class); // verify that the model is only adapted once
    }

    @Test
    public void testForwardingNoMatch() throws IOException, ServletException {
        request.setResource(context.resourceResolver().resolve("/content/product-page"));
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(SpecificPageServlet.SELECTOR + ".productId3");

        servlet.doGet(request, response);

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("productId3");

        // Verify that the request is forwarded to same page
        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(eq(request.getResource()), eq(options));
    }

    private static class MockRequestDispatcherFactoryImpl implements MockRequestDispatcherFactory {

        private RequestDispatcher requestDispatcher;

        private MockRequestDispatcherFactoryImpl() {
            this.requestDispatcher = Mockito.mock(RequestDispatcher.class);
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
            return requestDispatcher;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
            return requestDispatcher;
        }
    }

    private static class ResourcePathMatcher extends ArgumentMatcher<Resource> {

        private String path;

        public ResourcePathMatcher(String path) {
            this.path = path;
        }

        @Override
        public boolean matches(Object obj) {
            if (!(obj instanceof Resource)) {
                return false;
            }
            Resource res = (Resource) obj;
            return path.equals(res.getPath());
        }

    }
}
