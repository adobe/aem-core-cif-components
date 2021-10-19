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

import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.testing.MockRequestDispatcherFactory;
import com.day.cq.wcm.api.WCMMode;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

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
        ;
        context.request().setAttribute(WCMMode.class.getName(), WCMMode.DISABLED);
    }

    @Test
    public void testFilterForwarding() throws IOException, ServletException {
        context.currentResource("/content/product-page");
        context.requestPathInfo().setSuffix("/productId1.html");
        filter.doFilter(context.request(), null, chain);

        // Check that the request dispatcher adds the extra selector and forwards to the same page
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors(SpecificPageServlet.SELECTOR + ".productId1");

        Mockito.verify(requestDispatcherFactory).getRequestDispatcher(argThat(ResourceMatchers.path("/content/product-page")), eq(options));
        Mockito.verify(chain, times(0)).doFilter(context.request(), null);
    }

    @Test
    public void testFilterNoopWcmmode() throws IOException, ServletException {
        context.currentResource("/content/product-page");
        context.requestPathInfo().setSelectorString("productId1");
        context.request().setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        filter.doFilter(context.request(), null, chain);

        // Verify that the request is passed unchanged down the filter chain
        Mockito.verify(requestDispatcherFactory, times(0)).getRequestDispatcher(any(Resource.class), any());
        Mockito.verify(chain).doFilter(context.request(), null);
    }

    @Test
    public void testFilterForwardingNoSelector() throws IOException, ServletException {
        context.currentResource("/content/product-page");
        filter.doFilter(context.request(), null, chain);

        // Verify that the request is passed unchanged down the filter chain
        Mockito.verify(requestDispatcherFactory, times(0)).getRequestDispatcher(any(Resource.class), any());
        Mockito.verify(chain).doFilter(context.request(), null);
    }
}
