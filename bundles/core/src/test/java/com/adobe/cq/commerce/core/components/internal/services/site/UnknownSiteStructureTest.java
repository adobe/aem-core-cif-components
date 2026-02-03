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

import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class UnknownSiteStructureTest {

    @Rule
    public final AemContext aemContext = newAemContext("/context/SiteStructureImplTest/jcr-content.json");

    @Test
    public void testUnknownSiteStructure() {
        SiteStructure subject = UnknownSiteStructure.INSTANCE;
        Page catalogPage = aemContext.pageManager().getPage("/content/nav-root/shop");
        Page productPage = aemContext.pageManager().getPage("/content/nav-root/shop/product");
        Page categoryPage = aemContext.pageManager().getPage("/content/nav-root/shop/category");

        assertFalse(subject.isCatalogPage(catalogPage));
        assertFalse(subject.isProductPage(productPage));
        assertFalse(subject.isCategoryPage(categoryPage));
        assertEquals(0, subject.getProductPages().size());
        assertEquals(0, subject.getCategoryPages().size());
        assertNull(subject.getLandingPage());
        assertNull(subject.getSearchResultsPage());
        assertNull(subject.getEntry(productPage));
    }
}
