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

package com.adobe.cq.commerce.core.components.internal.models.v1.header;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class HeaderImplTest {
    private static final String PAGE_PATH = "/content/pageI";
    private static final String HEADER_PATH = "/content/pageI/jcr:content/header";

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");
    private HeaderImpl header;

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Before
    public void setup() {
        context.currentResource(HEADER_PATH);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(context.resourceResolver().getResource(HEADER_PATH));
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, context.currentPage(PAGE_PATH));

        header = context.request().adaptTo(HeaderImpl.class);
    }

    @Test
    public void testHeader() {
        Assert.assertEquals("/content/pageI.html", header.getNavigationRootPageUrl());
        Assert.assertEquals("Page I", header.getNavigationRootPageTitle());

        Resource resource = header.getMiniaccountResource();
        Assert.assertNotNull(resource);
        Assert.assertEquals(HeaderImpl.MINIACCOUNT_NODE_NAME, resource.getName());

        resource = header.getMinicartResource();
        Assert.assertNotNull(resource);
        Assert.assertEquals(HeaderImpl.MINICART_NODE_NAME, resource.getName());

        resource = header.getSearchbarResource();
        Assert.assertNotNull(resource);
        Assert.assertEquals(HeaderImpl.SEARCHBAR_NODE_NAME, resource.getName());
    }
}
