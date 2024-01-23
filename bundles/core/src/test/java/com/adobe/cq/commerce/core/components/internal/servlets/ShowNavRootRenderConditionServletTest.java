/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import javax.servlet.ServletException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.mockito.Mockito.mock;

public class ShowNavRootRenderConditionServletTest {

    @Rule
    public final AemContext context = newAemContext("/context/SiteStructureImplTest/jcr-content.json");

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private ShowNavRootRenderConditionServlet servlet;

    @Before
    public void setUp() {
        response = new MockSlingHttpServletResponse();
        request = new MockSlingHttpServletRequest(context.resourceResolver());
        Resource resource = mock(Resource.class);
        request.setResource(resource);
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet = context.registerInjectActivateService(new ShowNavRootRenderConditionServlet());
    }

    @Test
    public void testReturnsTrueForLandingPageEqualsPage() throws ServletException, IOException {
        request.setQueryString("item=/content/nav-root");
        servlet.doGet(request, response);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertTrue(condition.check());
    }

    @Test
    public void testReturnsTrueForNoLandingPage() throws ServletException, IOException {
        request.setQueryString("item=/content/no-nav-root");
        servlet.doGet(request, response);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertTrue(condition.check());
    }

    @Test
    public void testReturnsFalseForDefinedLandingPage() throws ServletException, IOException {
        request.setQueryString("item=/content/nav-root/content-page");
        servlet.doGet(request, response);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }

    @Test
    public void testReturnsFalseForInvalidPage() throws ServletException, IOException {
        request.setQueryString("item=/content/does-not-exist");
        servlet.doGet(request, response);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }
}
