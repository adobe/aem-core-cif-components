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
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

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

}
