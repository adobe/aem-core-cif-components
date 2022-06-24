/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.header;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;

public class HeaderImplTest {

    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");
    private HeaderImpl header;

    private void setupPage(String pagePath, String headerPath) {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, context.currentPage(pagePath));
        slingBindings.setResource(context.currentResource(headerPath));

        header = context.request().adaptTo(HeaderImpl.class);
    }

    @Test
    public void testHeader() {
        setupPage("/content/pageI", "/content/pageI/jcr:content/header");

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

    @Test
    public void testHeaderNoRootPageTitle() {
        setupPage("/content/pageJ", "/content/pageJ/jcr:content/header");

        Assert.assertEquals("/content/pageJ.html", header.getNavigationRootPageUrl());
        Assert.assertEquals("Page J", header.getNavigationRootPageTitle());
        Assert.assertNull(header.getMiniaccountResource());
        Assert.assertNull(header.getMinicartResource());
        Assert.assertNull(header.getSearchbarResource());
    }

    @Test
    public void testHeaderNoRootPage() {
        context.create().page("/var/pageK");
        context.create().resource("/var/pageK/jcr:content/header");
        setupPage("/var/pageK", "/var/pageK/jcr:content/header");

        Assert.assertNull(header.getNavigationRootPageUrl());
        Assert.assertNull(header.getNavigationRootPageTitle());
        Assert.assertNull(header.getMiniaccountResource());
        Assert.assertNull(header.getMinicartResource());
        Assert.assertNull(header.getSearchbarResource());
    }
}
