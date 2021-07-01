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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.MockLaunch;
import com.adobe.cq.launches.api.Launch;
import com.day.cq.wcm.api.Page;
import com.google.common.base.Function;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SiteNavigationTest {

    @Test
    public void testGetNavigationRootPage() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SiteNavigation.PN_NAV_ROOT, true);
        Page navRootPage = mockPage(properties);
        Page page0 = mockPage(null);
        Page page1 = mockPage(null);
        when(page0.getParent()).thenReturn(page1);
        when(page1.getParent()).thenReturn(navRootPage);
        Page page2 = mockPage(null);
        Page page3 = mockPage(null);
        when(page2.getParent()).thenReturn(page3);

        // returns null for null
        Assert.assertNull(SiteNavigation.getNavigationRootPage(null));

        // returns navigation root for navigation root
        Assert.assertSame(navRootPage, SiteNavigation.getNavigationRootPage(navRootPage));

        // returns navigation root for navigation root child
        Assert.assertSame(navRootPage, SiteNavigation.getNavigationRootPage(page1));

        // returns navigation root for child of navigation root child
        Assert.assertSame(navRootPage, SiteNavigation.getNavigationRootPage(page0));

        // returns null for page with no parent
        Assert.assertNull(SiteNavigation.getNavigationRootPage(page3));

        // returns null for page with no navigation root parent
        Assert.assertNull(SiteNavigation.getNavigationRootPage(page2));
    }

    private static Page mockPage(Map<String, Object> contentProperties) {
        Page page = mock(Page.class);
        Resource contentResource = mock(Resource.class);
        ValueMap properties = new ValueMapDecorator(contentProperties == null ? new HashMap<>() : contentProperties);
        when(contentResource.getValueMap()).thenReturn(properties);
        when(page.getContentResource()).thenReturn(contentResource);
        return page;
    }

    @Rule
    public final AemContext context = createContext("/context/jcr-content-breadcrumb.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                context.load().json(contentPath, "/content");
                context.registerAdapter(Resource.class, Launch.class, (Function<Resource, Launch>) resource -> new MockLaunch(resource));
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Test
    public void testNavigationWithLaunchAndLandingPage() {
        String launchPagePath = "/content/launches/2020/09/14/mylaunch/content/venia/us/en";
        Page page = context.pageManager().getPage(launchPagePath);

        // The cq:cifProductPage property is configured on the Launch page itself
        Page productPage = SiteNavigation.getProductPage(page);
        Assert.assertEquals(launchPagePath + "/products/product-page", productPage.getPath());

        // The cq:cifProductPage property is configured on the production page and has a corresponding page in the Launch
        Page categoryPage = SiteNavigation.getCategoryPage(page);
        Assert.assertEquals(launchPagePath + "/products/category-page", categoryPage.getPath());

        // The cq:cifSearchResultsPage property is configured on the production page and does not have a corresponding page in the Launch
        Page searchPage = SiteNavigation.getSearchResultsPage(page);
        Assert.assertEquals("/content/venia/us/en/products/search-page", searchPage.getPath());

        // That property is missing
        Page propertyNotFound = SiteNavigation.getGenericPage("cq:unknown", page);
        Assert.assertNull(propertyNotFound);

        // The page does not exist
        Page pageNotFound = SiteNavigation.getGenericPage("cq:notfound", page);
        Assert.assertNull(pageNotFound);
    }

    @Test
    public void testNavigationWithLaunchAndNewlyCreatedContent() {
        Page launchPage = context.pageManager().getPage("/content/launches/2020/09/14/mylaunch/content/venia/us/en/another-page");
        Page productionPage = context.pageManager().getPage("/content/venia/us/en/another-page");
        Assert.assertEquals(productionPage, SiteNavigation.toLaunchProductionPage(launchPage));

        launchPage = context.pageManager().getPage("/content/launches/2020/09/14/mylaunch/content/venia/us/en/new-content-page");
        Assert.assertSame(launchPage, SiteNavigation.toLaunchProductionPage(launchPage));
    }
}
