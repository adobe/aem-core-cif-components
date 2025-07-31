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
        // Setup: Create XF structure with cq:cifPreviewPage property
        setupExperienceFragmentWithPreviewPage();

        // Test: Verify both XF and resolved pages produce same SiteStructure
        SiteStructure xfSiteStructure = getSiteStructureForPage("/content/experience-fragments/venia/us/en/site/header/master");
        SiteStructure resolvedSiteStructure = getSiteStructureForPage("/content/venia/us/en");

        // Assert: Both should resolve to the same landing page
        assertSameLandingPage(xfSiteStructure, resolvedSiteStructure, "/content/venia/us/en");
    }

    @Test
    public void testExperienceFragmentWithProgressivePathLookup() {
        // Setup: Create XF structure without cq:cifPreviewPage but with corresponding content page
        setupExperienceFragmentWithProgressiveLookup();

        // Test: Verify both XF and resolved pages produce same SiteStructure
        SiteStructure xfSiteStructure = getSiteStructureForPage("/content/experience-fragments/venia/us/en/site/header/master");
        SiteStructure resolvedSiteStructure = getSiteStructureForPage("/content/venia/us/en");

        // Assert: Both should resolve to the same landing page
        assertSameLandingPage(xfSiteStructure, resolvedSiteStructure, "/content/venia/us/en");
    }

    @Test
    public void testExperienceFragmentWithNoCorrespondingPage() {
        // Setup: Create XF structure without any corresponding content page
        setupExperienceFragmentWithoutCorrespondingPage();

        // Test: Verify XF page produces SiteStructure without landing page
        SiteStructure xfSiteStructure = getSiteStructureForPage("/content/experience-fragments/venia/us/en/site/header/master");

        // Assert: Should not have a landing page since no corresponding content page exists
        assertNull("SiteStructure should not have a landing page for XF without corresponding content page",
            xfSiteStructure.getLandingPage());
    }

    @Test
    public void testFindPreviewPageWithNullPage() {
        try {
            java.lang.reflect.Method findPreviewPageMethod = SiteStructureFactory.class.getDeclaredMethod(
                "findPreviewPage", Page.class);
            findPreviewPageMethod.setAccessible(true);

            // Test with null page
            Page result = (Page) findPreviewPageMethod.invoke(subject, (Page) null);
            assertNull("findPreviewPage should return null when given a null page", result);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========== HELPER METHODS ==========

    private void setupExperienceFragmentWithPreviewPage() {
        aemContext.create().page("/content/experience-fragments/venia/us/en/site/header/master");
        aemContext.create().page("/content/venia/us/en");

        // Add navRoot property to make it a landing page
        aemContext.create().resource("/content/venia/us/en/jcr:content", "navRoot", true);

        // Add cq:cifPreviewPage property
        aemContext.create().resource("/content/experience-fragments/venia/us/en/site/header", "cq:cifPreviewPage", "/content/venia/us/en");
    }

    private void setupExperienceFragmentWithProgressiveLookup() {
        aemContext.create().page("/content/experience-fragments/venia/us/en/site/header/master");
        aemContext.create().page("/content/venia/us/en");

        // Add navRoot property to make it a landing page
        aemContext.create().resource("/content/venia/us/en/jcr:content", "navRoot", true);
    }

    private void setupExperienceFragmentWithoutCorrespondingPage() {
        aemContext.create().page("/content/experience-fragments/venia/us/en/site/header/master");

        // Add navRoot property to make it a landing page
        aemContext.create().resource("/content/venia/us/en/jcr:content", "navRoot", true);
    }

    private SiteStructure getSiteStructureForPage(String pagePath) {
        aemContext.currentPage(pagePath);
        AdapterManager adapterManager = aemContext.getService(AdapterManager.class);
        SiteStructure siteStructure = adapterManager.getAdapter(aemContext.request(), SiteStructure.class);
        assertNotNull("SiteStructure should not be null for page: " + pagePath, siteStructure);

        // Clear the request attributes to force a new adapter resolution for next call
        aemContext.request().removeAttribute(SiteStructure.class.getName());

        return siteStructure;
    }

    private void assertSameLandingPage(SiteStructure siteStructure1, SiteStructure siteStructure2, String expectedPath) {
        Page landingPage1 = siteStructure1.getLandingPage();
        Page landingPage2 = siteStructure2.getLandingPage();

        assertNotNull("First SiteStructure landing page should not be null", landingPage1);
        assertNotNull("Second SiteStructure landing page should not be null", landingPage2);
        assertEquals("Both SiteStructure objects should resolve to the same landing page",
            expectedPath, landingPage1.getPath());
        assertEquals("Both SiteStructure objects should resolve to the same landing page",
            expectedPath, landingPage2.getPath());
    }
}
