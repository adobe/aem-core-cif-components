/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlethelpers.MockRequestDispatcherFactory;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.day.cq.wcm.api.WCMMode;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNull;

public class SpecificPageFilterFactoryTest {

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

    private SpecificPageFilterFactory filter;
    private MockSlingHttpServletRequest request;
    private MockRequestDispatcherFactoryImpl requestDispatcherFactory;
    private FilterChain chain;

    @Before
    public void setUp() throws Exception {
        filter = new SpecificPageFilterFactory();
        request = new MockSlingHttpServletRequest(context.resourceResolver());

        requestDispatcherFactory = Mockito.spy(new MockRequestDispatcherFactoryImpl());
        request.setRequestDispatcherFactory(requestDispatcherFactory);

        // The filter only does something on publish, so only when WCMMode is DISABLED
        request.setAttribute(WCMMode.class.getName(), WCMMode.DISABLED);

        chain = Mockito.mock(FilterChain.class);
    }

    @Test
    public void testFilterForwardingWithStringProperty() throws IOException, ServletException {
        request.setResource(context.resourceResolver().resolve("/content/product-page"));
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString("productId1");
        filter.doFilter(request, null, chain);

        // Check that the request dispatcher is called for the matching sub-page
        ResourcePathMatcher matcher = new ResourcePathMatcher("/content/product-page/sub-page");
        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(argThat(matcher), isNull(RequestDispatcherOptions.class));
        Mockito.verify(chain, Mockito.times(0)).doFilter(request, null);
    }

    @Test
    public void testFilterNoopWcmmode() throws IOException, ServletException {
        request.setResource(context.resourceResolver().resolve("/content/product-page"));
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString("productId1");

        // Verify that the filter does nothing if WCMMode is something else than DISABLED
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        filter.doFilter(request, null, chain);

        // Verify that the request is passed unchanged down the filter chain
        Mockito.verify(requestDispatcherFactory, Mockito.times(0)).getRequestDispatcher(any(Resource.class), any());
        Mockito.verify(chain).doFilter(request, null);
    }

    @Test
    public void testFilterForwardingWithStringArrayProperty() throws IOException, ServletException {
        request.setResource(context.resourceResolver().resolve("/content/category-page"));
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString("categoryId2");

        filter.doFilter(request, null, chain);

        // Check that the request dispatcher is called for the matching sub-page
        ResourcePathMatcher matcher = new ResourcePathMatcher("/content/category-page/sub-page");
        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(argThat(matcher), isNull(RequestDispatcherOptions.class));
        Mockito.verify(chain, Mockito.times(0)).doFilter(request, null);
    }

    @Test
    public void testFilterForwardingNoMatch() throws IOException, ServletException {
        request.setResource(context.resourceResolver().resolve("/content/product-page"));
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString("productId3");

        filter.doFilter(request, null, chain);

        // Verify that the request is passed unchanged down the filter chain
        Mockito.verify(requestDispatcherFactory, Mockito.times(0)).getRequestDispatcher(any(Resource.class), any());
        Mockito.verify(chain).doFilter(request, null);
    }

    @Test
    public void testFilterForwardingNoSelector() throws IOException, ServletException {
        request.setResource(context.resourceResolver().resolve("/content/product-page"));

        filter.doFilter(request, null, chain);

        // Verify that the request is passed unchanged down the filter chain
        Mockito.verify(requestDispatcherFactory, Mockito.times(0)).getRequestDispatcher(any(Resource.class), any());
        Mockito.verify(chain).doFilter(request, null);
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
