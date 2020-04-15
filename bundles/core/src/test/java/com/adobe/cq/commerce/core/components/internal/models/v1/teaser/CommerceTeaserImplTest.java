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
 ******************************************************************************/

package com.adobe.cq.commerce.core.components.internal.models.v1.teaser;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaser;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class CommerceTeaserImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PRODUCT_SPECIFIC_PAGE = PRODUCT_PAGE + "/product-specific-page";
    private static final String CATEGORY_PAGE = "/content/category-page";
    private static final String PAGE = "/content/pageA";
    private static final String TEASER = "/content/pageA/jcr:content/root/responsivegrid/commerceteaser";

    private Resource commerceTeaserResource;
    private CommerceTeaser commerceTeaser;

    @Before
    public void setup() {
        Page page = context.currentPage(PAGE);
        context.currentResource(TEASER);
        commerceTeaserResource = context.resourceResolver().getResource(TEASER);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(commerceTeaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

        // Configure the component to create deep links to specific pages
        context.request().setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        commerceTeaser = context.request().adaptTo(CommerceTeaserImpl.class);
    }

    @Test
    public void verifyActions() {
        List<ListItem> actionItems = commerceTeaser.getActions();

        Assert.assertTrue(commerceTeaser.isActionsEnabled());
        Assert.assertTrue(actionItems.size() == 4);

        // Product slug is configured and there is a dedicated specific subpage for that product
        Assert.assertEquals(PRODUCT_SPECIFIC_PAGE + ".beaumont-summit-kit.html", actionItems.get(0).getURL());
        Assert.assertEquals("A product", actionItems.get(0).getTitle());

        // Category id is configured
        Assert.assertEquals(CATEGORY_PAGE + ".30.html", actionItems.get(1).getURL());
        Assert.assertEquals("A category", actionItems.get(1).getTitle());

        // Both are configured, category links "wins"
        Assert.assertEquals(CATEGORY_PAGE + ".30.html", actionItems.get(2).getURL());
        Assert.assertEquals("A category", actionItems.get(2).getTitle());

        // Some text is entered, current page is used
        Assert.assertEquals(PAGE + ".html", actionItems.get(3).getURL());
        Assert.assertEquals("Some text", actionItems.get(3).getTitle());
    }
}
