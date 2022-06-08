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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.List;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.SiteNavigation;
import com.adobe.cq.launches.api.Launch;
import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.MockLaunch.MOCK_LAUNCH_ADAPTER;
import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SiteNavigationImplTest {

    @Rule
    public final AemContext aemContext = newAemContext("/context/SiteNavigationImplTest/jcr-content.json");

    final SiteNavigationImpl subject = new SiteNavigationImpl();
    Page navRootPage;

    @Before
    public void setup() {
        aemContext.registerInjectActivateService(subject);
        aemContext.registerAdapter(Resource.class, Launch.class, MOCK_LAUNCH_ADAPTER);

        navRootPage = aemContext.pageManager().getPage("/content/nav-root");
    }

    @Test
    public void testNullInputs() {
        assertNull(subject.getEntry(null));
        assertEquals(0, subject.getProductPages(null).size());
        assertEquals(0, subject.getCategoryPages(null).size());
        assertFalse(subject.isCatalogPage(null));
        assertFalse(subject.isCategoryPage(null));
        assertFalse(subject.isProductPage(null));
    }

    @Test
    public void testIsCatalogPage() {
        Page contentPage = aemContext.pageManager().getPage("/content/nav-root/content-page");
        Page catalogPage = aemContext.pageManager().getPage("/content/nav-root/shop");
        Page catalogPageV3 = aemContext.create().page("/content/nav-root/shop-v3", "catalogpagev3", ImmutableMap.of(
            "sling:resourceType", SiteNavigation.RT_CATALOG_PAGE));

        assertFalse(subject.isCatalogPage(contentPage));
        assertTrue(subject.isCatalogPage(catalogPage));
        assertTrue(subject.isCatalogPage(catalogPageV3));
    }

    @Test
    public void testIsProductPage() {
        Page anypage = aemContext.pageManager().getPage("/content/nav-root/shop");
        Page productPage = aemContext.pageManager().getPage("/content/nav-root/shop/product");
        Page nestedProductPage = aemContext.create().page("/content/nav-root/shop/product/specificpage", "productpage");

        assertFalse(subject.isProductPage(anypage));
        assertTrue(subject.isProductPage(productPage));
        assertTrue(subject.isProductPage(nestedProductPage));
    }

    @Test
    public void testIsCategoryPage() {
        Page anypage = aemContext.pageManager().getPage("/content/nav-root/shop");
        Page categoryPage = aemContext.pageManager().getPage("/content/nav-root/shop/category");
        Page nestedCategoryPage = aemContext.create().page("/content/nav-root/shop/category/specificpage", "categorypage");

        assertFalse(subject.isCategoryPage(anypage));
        assertTrue(subject.isCategoryPage(categoryPage));
        assertTrue(subject.isCategoryPage(nestedCategoryPage));
    }

    @Test
    public void testGetEntry() {
        aemContext.load().json("/context/SiteNavigationImplTest/jcr-content-launch-full.json", "/content/launches/1111/11/11/test");

        Page productPage = aemContext.pageManager().getPage("/content/nav-root/shop/product");
        Page contentPage = aemContext.pageManager().getPage("/content/nav-root/content-page");

        assertSiteNavigationEntry(subject.getEntry(contentPage), contentPage, null);
        assertSiteNavigationEntry(subject.getEntry(productPage), productPage, null);

        Page specificProductPage = aemContext.create().page("/content/nav-root/adventures", "catalogpage", ImmutableMap.of(
            "sling:resourceType", SiteNavigation.RT_CATALOG_PAGE_V3,
            SiteNavigationImpl.PN_CIF_PRODUCT_PAGE, "/content/nav-root/adventures"));

        assertSiteNavigationEntry(subject.getEntry(specificProductPage), specificProductPage, specificProductPage);
    }

    @Test
    public void testGetEntryNoProductOrCategoryPages() {
        Page navRoot = aemContext.pageManager().getPage("/content/no-nav-root");
        ValueMap properties = navRoot.getContentResource().adaptTo(ModifiableValueMap.class);
        properties.put(SiteNavigation.PN_NAV_ROOT, Boolean.TRUE);

        Page anypage = aemContext.pageManager().getPage("/content/no-nav-root/content-page");
        assertSiteNavigationEntry(subject.getEntry(anypage), anypage, null, navRoot);
    }

    @Test
    public void testGetEntryNoNavRoot() {
        Page anypage = aemContext.pageManager().getPage("/content/no-nav-root/content-page");
        assertNull(subject.getEntry(anypage));
    }

    @Test
    public void testGetProductPageFromLandingPage() {
        Page productPage = aemContext.pageManager().getPage("/content/nav-root/shop/product");
        Page contentPage = aemContext.pageManager().getPage("/content/nav-root/content-page");

        List<SiteNavigation.Entry> productPages = subject.getProductPages(contentPage);
        assertEquals(1, productPages.size());
        assertSiteNavigationEntry(productPages.get(0), productPage, null);
    }

    @Test
    public void testGetCategoryPageFromLandingPage() {
        Page categoryPage = aemContext.pageManager().getPage("/content/nav-root/shop/category");
        Page contentPage = aemContext.pageManager().getPage("/content/nav-root/content-page");

        List<SiteNavigation.Entry> categoryPages = subject.getCategoryPages(contentPage);
        assertEquals(1, categoryPages.size());
        assertSiteNavigationEntry(categoryPages.get(0), categoryPage, null);
    }

    @Test
    public void testGetProductPages() {
        Page anypage = aemContext.pageManager().getPage("/content/nav-root/content-page");
        Page defaultProductPage = aemContext.pageManager().getPage("/content/nav-root/shop/product");
        Page specificProductPage = aemContext.create().page("/content/nav-root/adventures", "catalogpage", ImmutableMap.of(
            "sling:resourceType", SiteNavigation.RT_CATALOG_PAGE_V3,
            SiteNavigationImpl.PN_CIF_PRODUCT_PAGE, "/content/nav-root/adventures"));

        List<SiteNavigation.Entry> productPages = subject.getProductPages(anypage);
        assertEquals(2, productPages.size());
        assertSiteNavigationEntry(productPages.get(0), specificProductPage, specificProductPage);
        assertSiteNavigationEntry(productPages.get(1), defaultProductPage, null);
    }

    @Test
    public void testGetProductPagesNoNavRoot() {
        Page anypage = aemContext.pageManager().getPage("/content/no-nav-root/content-page");
        List<SiteNavigation.Entry> productPages = subject.getProductPages(anypage);
        assertEquals(0, productPages.size());
    }

    @Test
    public void testGetProductPagesInLaunch() {
        aemContext.load().json("/context/SiteNavigationImplTest/jcr-content-launch-full.json", "/content/launches/1111/11/11/test");

        Page navRootPage = aemContext.pageManager().getPage("/content/launches/1111/11/11/test/content/nav-root");
        Page contentPage = aemContext.pageManager().getPage("/content/launches/1111/11/11/test/content/nav-root/content-page");
        Page productPage = aemContext.pageManager().getPage("/content/launches/1111/11/11/test/content/nav-root/shop/product");

        List<SiteNavigation.Entry> productPages = subject.getProductPages(contentPage);
        assertEquals(1, productPages.size());
        assertSiteNavigationEntry(productPages.get(0), productPage, null, navRootPage);
    }

    @Test
    public void testGetProductPagesInLaunchNoNavRoot() {
        aemContext.load().json("/context/SiteNavigationImplTest/jcr-content-launch-no-navroot.json", "/content/launches/1111/11/11/test");

        Page contentPage = aemContext.pageManager().getPage("/content/launches/1111/11/11/test/content/nav-root/content-page");
        Page productPage = aemContext.pageManager().getPage("/content/launches/1111/11/11/test/content/nav-root/shop/product");

        List<SiteNavigation.Entry> productPages = subject.getProductPages(contentPage);
        assertEquals(1, productPages.size());
        assertSiteNavigationEntry(productPages.get(0), productPage, null, navRootPage);
    }

    @Test
    public void testGetProductPagesInLaunchNoNavRootNewPage() {
        aemContext.load().json("/context/SiteNavigationImplTest/jcr-content-launch-no-navroot.json", "/content/launches/1111/11/11/test");

        Page contentPage = aemContext.create().page("/content/launches/1111/11/11/test/content/nav-root/new-content-page");
        Page productPage = aemContext.pageManager().getPage("/content/launches/1111/11/11/test/content/nav-root/shop/product");

        List<SiteNavigation.Entry> productPages = subject.getProductPages(contentPage);
        assertEquals(1, productPages.size());
        assertSiteNavigationEntry(productPages.get(0), productPage, null, navRootPage);
    }

    @Test
    public void testGetProductPagesInLaunchNewCatalogPageInProduction() {
        aemContext.load().json("/context/SiteNavigationImplTest/jcr-content-launch-no-navroot.json", "/content/launches/1111/11/11/test");

        Page contentPage = aemContext.pageManager().getPage("/content/launches/1111/11/11/test/content/nav-root/content-page");
        Page productPage = aemContext.pageManager().getPage("/content/launches/1111/11/11/test/content/nav-root/shop/product");

        // add a product page in production
        Page prodSpecificProductPage = aemContext.create().page("/content/nav-root/adventures", "catalogpage", ImmutableMap.of(
            "sling:resourceType", SiteNavigation.RT_CATALOG_PAGE_V3,
            SiteNavigationImpl.PN_CIF_PRODUCT_PAGE, "/content/nav-root/adventures"));

        List<SiteNavigation.Entry> productPages = subject.getProductPages(contentPage);
        assertEquals(2, productPages.size());
        assertSiteNavigationEntry(productPages.get(0), prodSpecificProductPage, prodSpecificProductPage, navRootPage);
        assertSiteNavigationEntry(productPages.get(1), productPage, null, navRootPage);
    }

    private void assertSiteNavigationEntry(SiteNavigation.Entry entry, Page page, Page catalogPage) {
        assertSiteNavigationEntry(entry, page, catalogPage, navRootPage);
    }

    private void assertSiteNavigationEntry(SiteNavigation.Entry entry, Page page, Page catalogPage, Page navRootPage) {
        assertNotNull(entry);
        assertEquals(page, entry.getPage());
        assertEquals(catalogPage, entry.getCatalogPage());
        assertEquals(navRootPage, entry.getNavigationRootPage());
    }

}
