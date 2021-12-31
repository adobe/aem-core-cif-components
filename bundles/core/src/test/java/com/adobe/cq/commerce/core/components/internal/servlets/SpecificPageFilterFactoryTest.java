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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy;
import com.adobe.cq.commerce.core.testing.MockRequestDispatcherFactory;
import com.day.cq.wcm.api.WCMMode;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.apache.sling.hamcrest.ResourceMatchers.path;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

public class SpecificPageFilterFactoryTest {

    @Rule
    public final AemContext context = newAemContext("/context/jcr-page-filter.json");

    private final SpecificPageFilterFactory filter = new SpecificPageFilterFactory();
    private final MockRequestDispatcherFactory requestDispatcherFactory = spy(new MockRequestDispatcherFactory());
    @Mock
    private FilterChain chain;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        context.registerInjectActivateService(filter);
        context.request().setRequestDispatcherFactory(requestDispatcherFactory);
        context.request().setAttribute(WCMMode.class.getName(), WCMMode.DISABLED);
    }

    @Test
    public void testSpecificProductPage() throws IOException, ServletException {
        context.currentResource("/content/product-page");
        context.requestPathInfo().setSuffix("/productId1.html");
        filter.doFilter(context.request(), null, chain);

        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(argThat(path("/content/product-page/sub-page/jcr:content")), any());
        Mockito.verify(chain, never()).doFilter(context.request(), null);
    }

    @Test
    public void testSpecificCategoryPage() throws IOException, ServletException {
        context.currentResource("/content/category-page");
        context.requestPathInfo().setSuffix("/men/tops.html");
        filter.doFilter(context.request(), null, chain);

        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(argThat(path(
            "/content/category-page/sub-page-with-urlpath/jcr:content")), any());
        Mockito.verify(chain, never()).doFilter(context.request(), null);
    }

    @Test
    public void testFilterNoop() throws IOException, ServletException {
        context.currentResource("/content/product-page");
        context.requestPathInfo().setSuffix("/productId1.html");
        // TODO: CIF-2469
        // With a newer version of OSGI mock we could re-inject the reference into the existing UrlProviderImpl
        // context.registerInjectActivateService(new SpecificPageStrategy(), "generateSpecificPageUrls", true);
        SpecificPageStrategy specificPageStrategy = context.getService(SpecificPageStrategy.class);
        Whitebox.setInternalState(specificPageStrategy, "generateSpecificPageUrls", true);

        filter.doFilter(context.request(), null, chain);

        // Verify that the request is passed unchanged down the filter chain
        Mockito.verify(requestDispatcherFactory, never()).getRequestDispatcher(any(Resource.class), any());
        Mockito.verify(chain).doFilter(context.request(), null);
    }

    @Test
    public void testFilterForwardingNoParameters() throws IOException, ServletException {
        context.currentResource("/content/product-page");
        filter.doFilter(context.request(), null, chain);

        // Verify that the request is passed unchanged down the filter chain
        Mockito.verify(requestDispatcherFactory, never()).getRequestDispatcher(any(Resource.class), any());
        Mockito.verify(chain).doFilter(context.request(), null);
    }
}
