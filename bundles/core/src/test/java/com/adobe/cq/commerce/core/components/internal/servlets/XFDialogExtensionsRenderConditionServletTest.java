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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;

public class XFDialogExtensionsRenderConditionServletTest {

    private MockSlingHttpServletRequest request;
    private XFDialogExtensionsRenderConditionServlet servlet;
    private ComponentContext ctx;
    private BundleContext bundleContext;
    private Bundle bundle;

    @Before
    public void setUp() {
        ctx = Mockito.mock(ComponentContext.class);
        bundleContext = Mockito.mock(BundleContext.class);
        bundle = Mockito.mock(Bundle.class);

        Mockito.when(ctx.getBundleContext()).thenReturn(bundleContext);
        Mockito.when(bundleContext.getBundles()).thenReturn(ArrayUtils.toArray(bundle));

        request = new MockSlingHttpServletRequest(null);
        servlet = new XFDialogExtensionsRenderConditionServlet();
        servlet.activate(ctx);
    }

    @Test
    public void testAddOnBundleFound() {
        Mockito.when(bundle.getSymbolicName()).thenReturn("com.adobe.cq.cif.commerce-addon-bundle");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertTrue(condition.check());
    }

    @Test
    public void testAddOnBundleNotFound() {
        Mockito.when(bundle.getSymbolicName()).thenReturn("com.adobe.cq.cif.not-found");

        servlet.doGet(request, null);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        Assert.assertFalse(condition.check());
    }
}
