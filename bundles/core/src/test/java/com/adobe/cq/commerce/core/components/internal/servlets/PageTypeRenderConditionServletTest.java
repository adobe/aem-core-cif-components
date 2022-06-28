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

import java.util.HashMap;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;

public class PageTypeRenderConditionServletTest {

    @Rule
    public final AemContext context = newAemContext("/context/jcr-page-filter.json");

    private MockSlingHttpServletRequest request;
    private PageTypeRenderConditionServlet servlet;

    @Before
    public void setUp() {
        request = new MockSlingHttpServletRequest(context.resourceResolver());
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getPath()).thenReturn("/here");
        Mockito.when(resource.getResourceResolver()).thenReturn(context.resourceResolver());
        Mockito.when(resource.getValueMap()).thenReturn(new ModifiableValueMapDecorator(new HashMap<>()));
        request.setResource(resource);
        servlet = context.registerInjectActivateService(new PageTypeRenderConditionServlet());
    }

    @Test
    public void testProductPage() {
        request.getResource().getValueMap().put("pageType", "product");
        request.setQueryString("item=/content/product-page/sub-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertTrue(condition.check());
    }

    @Test
    public void testCategoryPage() {
        request.getResource().getValueMap().put("pageType", "category");
        request.setQueryString("item=/content/category-page/sub-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertTrue(condition.check());
    }

    @Test
    public void testNestedProductPage() {
        request.getResource().getValueMap().put("pageType", "product");
        request.setQueryString("item=/content/product-page/sub-page/nested-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertTrue(condition.check());
    }

    @Test
    public void testCatalogPage() {
        request.getResource().getValueMap().put("pageType", "catalog");
        request.setQueryString("item=/content/catalog-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertTrue(condition.check());
    }

    @Test
    public void testNestedCategoryPage() {
        request.getResource().getValueMap().put("pageType", "category");
        request.setQueryString("item=/content/category-page/sub-page/nested-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertTrue(condition.check());
    }

    @Test
    public void testNotCatalogPage() {
        request.getResource().getValueMap().put("pageType", "catalog");
        request.setQueryString("item=/content/category-page/sub-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }

    @Test
    public void testNotProductPage() {
        request.getResource().getValueMap().put("pageType", "product");
        request.setQueryString("item=/content/category-page/sub-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }

    @Test
    public void testNotCategoryPage() {
        request.getResource().getValueMap().put("pageType", "category");
        request.setQueryString("item=/content/product-page/sub-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }

    @Test
    public void testNotPage() {
        request.getResource().getValueMap().put("pageType", "product");
        request.setQueryString("item=/content/product-page/ignored");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }

    @Test
    public void testNoPageType() {
        request.setQueryString("item=/content/product-page/ignored");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }

    @Test
    public void testInvalidPageType() {
        request.getResource().getValueMap().put("pageType", "something");
        request.setQueryString("item=/content/product-page/sub-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }
}
