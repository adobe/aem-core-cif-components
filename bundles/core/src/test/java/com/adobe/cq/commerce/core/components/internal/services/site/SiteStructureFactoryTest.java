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
package com.adobe.cq.commerce.core.components.internal.services.site;

import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SiteStructureFactoryTest {

    @Rule
    public final AemContext aemContext = newAemContext("/context/SiteStructureImplTest/jcr-content.json");

    SiteStructureFactory subject;

    @Before
    public void setup() {
        subject = aemContext.getService(SiteStructureFactory.class);
    }

    @Test
    public void testAdaptionFromRequestCached() {
        // use AdapterManager instead of adaptTo() to bypass SlingAdaptable caching of the mock request object
        AdapterManager adapterManager = aemContext.getService(AdapterManager.class);
        aemContext.currentPage("/content/nav-root");

        SiteStructure siteStructure = adapterManager.getAdapter(aemContext.request(), SiteStructure.class);

        assertNotNull(siteStructure);

        SiteStructure newSiteStructure = adapterManager.getAdapter(aemContext.request(), SiteStructure.class);

        assertSame(siteStructure, newSiteStructure);
    }

    @Test
    public void testUnknownSiteStructureReturned() {
        Resource resource = aemContext.currentResource(aemContext.create().resource("/some/other/path"));

        assertSame(UnknownSiteStructure.INSTANCE, subject.getSiteStructure(resource));
        assertSame(UnknownSiteStructure.INSTANCE, subject.getSiteStructure(aemContext.request(), null));
    }

    @Test
    public void testExperienceFragmentWithPreviewPage() {
        // Create experience fragment structure with cq:cifPreviewPage property
        aemContext.create().page("/content/experience-fragments/venia/us/en/site/header/master");
        aemContext.create().page("/content/venia/us/en");

        // Add navRoot property to make it a landing page
        aemContext.create().resource("/content/venia/us/en/jcr:content", "navRoot", true);

        // Add cq:cifPreviewPage property
        aemContext.create().resource("/content/experience-fragments/venia/us/en/site/header", "cq:cifPreviewPage", "/content/venia/us/en");

        // Test with experience fragment page
        aemContext.currentPage("/content/experience-fragments/venia/us/en/site/header/master");
        AdapterManager adapterManager = aemContext.getService(AdapterManager.class);
        SiteStructure xfSiteStructure = adapterManager.getAdapter(aemContext.request(), SiteStructure.class);
        assertNotNull(xfSiteStructure);

        // Clear the request attributes to force a new adapter resolution
        aemContext.request().removeAttribute(SiteStructure.class.getName());

        // Change to the resolved page and test again
        aemContext.currentPage("/content/venia/us/en");
        SiteStructure resolvedSiteStructure = adapterManager.getAdapter(aemContext.request(), SiteStructure.class);
        assertNotNull(resolvedSiteStructure);

        // Both should resolve to the same landing page since they represent the same underlying page
        Page xfLandingPage = xfSiteStructure.getLandingPage();
        Page resolvedLandingPage = resolvedSiteStructure.getLandingPage();

        assertNotNull("XF SiteStructure landing page should not be null", xfLandingPage);
        assertNotNull("Resolved SiteStructure landing page should not be null", resolvedLandingPage);
        assertEquals("Both SiteStructure objects should resolve to the same landing page",
            xfLandingPage.getPath(), resolvedLandingPage.getPath());
    }

    @Test
    public void testExperienceFragmentWithProgressivePathLookup() {
        // Create experience fragment structure with cq:cifPreviewPage property
        aemContext.create().page("/content/experience-fragments/venia/us/en/site/header/master");
        aemContext.create().page("/content/venia/us/en");

        // Add navRoot property to make it a landing page
        aemContext.create().resource("/content/venia/us/en/jcr:content", "navRoot", true);

        // Test with experience fragment page
        aemContext.currentPage("/content/experience-fragments/venia/us/en/site/header/master");
        AdapterManager adapterManager = aemContext.getService(AdapterManager.class);
        SiteStructure xfSiteStructure = adapterManager.getAdapter(aemContext.request(), SiteStructure.class);
        assertNotNull(xfSiteStructure);

        // Clear the request attributes to force a new adapter resolution
        aemContext.request().removeAttribute(SiteStructure.class.getName());

        // Change to the resolved page and test again
        aemContext.currentPage("/content/venia/us/en");
        SiteStructure resolvedSiteStructure = adapterManager.getAdapter(aemContext.request(), SiteStructure.class);
        assertNotNull(resolvedSiteStructure);

        // Both should resolve to the same landing page since they represent the same underlying page
        Page xfLandingPage = xfSiteStructure.getLandingPage();
        Page resolvedLandingPage = resolvedSiteStructure.getLandingPage();

        assertNotNull("XF SiteStructure landing page should not be null", xfLandingPage);
        assertNotNull("Resolved SiteStructure landing page should not be null", resolvedLandingPage);
        assertEquals("Both SiteStructure objects should resolve to the same landing page",
            xfLandingPage.getPath(), resolvedLandingPage.getPath());
    }

    @Test
    public void testExperienceFragmentWithNoCorrespondingPage() {
        // Create experience fragment page without any corresponding content pages
        aemContext.create().page("/content/experience-fragments/venia/us/en/site/header/master");

        // Add navRoot property to make it a landing page
        aemContext.create().resource("/content/venia/us/en/jcr:content", "navRoot", true);

        // Test with experience fragment page
        aemContext.currentPage("/content/experience-fragments/venia/us/en/site/header/master");

        AdapterManager adapterManager = aemContext.getService(AdapterManager.class);
        SiteStructure xfSiteStructure = adapterManager.getAdapter(aemContext.request(), SiteStructure.class);
        assertNotNull(xfSiteStructure);
        assertNull("SiteStructure should not have a landing page for XF without corresponding content page",
            xfSiteStructure.getLandingPage());
    }

    @Test
    public void testFindCorrespondingPageIterativeEdgeCases() {
        PageManager pageManager = aemContext.pageManager();

        try {
            java.lang.reflect.Method findCorrespondingPageIterativeMethod = SiteStructureFactory.class.getDeclaredMethod(
                "findCorrespondingPageIterative", PageManager.class, String.class);
            findCorrespondingPageIterativeMethod.setAccessible(true);

            // Test null path
            Page result = (Page) findCorrespondingPageIterativeMethod.invoke(subject, pageManager, (String) null);
            assertNull(result);

            // Test empty path
            result = (Page) findCorrespondingPageIterativeMethod.invoke(subject, pageManager, "");
            assertNull(result);

            // Test non-existent path
            result = (Page) findCorrespondingPageIterativeMethod.invoke(subject, pageManager, "/content/non/existent/path");
            assertNull(result);

            // Test existing path
            aemContext.create().page("/content/venia");
            result = (Page) findCorrespondingPageIterativeMethod.invoke(subject, pageManager, "/content/venia/us/en/site/header/master");
            assertNotNull(result);
            assertEquals("/content/venia", result.getPath());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
