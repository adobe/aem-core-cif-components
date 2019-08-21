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

package com.adobe.cq.commerce.core.components.utils;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class TemplateRenderConditionTest {

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

    private MockSlingHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(context.resourceResolver());
    }

    @Test
    public void testConditionTrue() {
        request.setQueryString("item=%2Fcontent%2Fproduct-page%2Fsub-page");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");
        Assert.assertTrue(TemplateRenderCondition.isTemplate(request, "/conf/venia/settings/wcm/templates/product-page"));
    }

    @Test
    public void testConditionFalse() {
        request.setQueryString("item=%2Fcontent%2Fproduct-page%2Fignored");
        request.setPathInfo("/mnt/overlay/wcm/core/content/sites/properties.html");
        Assert.assertFalse(TemplateRenderCondition.isTemplate(request, "/conf/venia/settings/wcm/templates/product-page"));
    }
}
