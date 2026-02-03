/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClearCacheButtonRenderConditionServletTest {

    @Rule
    public final AemContext context = new AemContext();

    private ClearCacheButtonRenderConditionServlet servlet;
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private InvalidateCacheSupport invalidateCacheSupport;

    @Before
    public void setUp() {
        servlet = new ClearCacheButtonRenderConditionServlet();
        request = new MockSlingHttpServletRequest(context.resourceResolver());
        response = new MockSlingHttpServletResponse();
        invalidateCacheSupport = new InvalidateCacheSupport();
    }

    @Test
    public void testRenderConditionWithInvalidateCacheSupport() {
        context.registerService(InvalidateCacheSupport.class, invalidateCacheSupport);
        context.registerInjectActivateService(servlet);

        servlet.doGet(request, response);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        assertNotNull("Render condition should not be null", condition);
        assertTrue("Button should be rendered when InvalidateCacheSupport is available", condition.check());
    }

    @Test
    public void testRenderConditionWithoutInvalidateCacheSupport() {
        context.registerInjectActivateService(servlet);

        servlet.doGet(request, response);

        SimpleRenderCondition condition = (SimpleRenderCondition) request.getAttribute(RenderCondition.class.getName());
        assertNotNull("Render condition should not be null", condition);
        assertFalse("Button should not be rendered when InvalidateCacheSupport is not available", condition.check());
    }
}
