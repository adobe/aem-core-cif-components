/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.mcp.internal;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Rule;
import org.junit.Test;

import com.day.cq.wcm.api.PageManager;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageCreationSupportTest {

    @Rule
    public final AemContext context = new AemContext();

    @Test
    public void returnsExistingParentUnderContent() {
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();
        Resource parent = PageCreationSupport.validatePageParent(context.resourceResolver(), "parent", "/content/site/en");
        assertEquals("/content/site/en", parent.getPath());
    }

    @Test
    public void rejectsBlankParent() {
        assertThrows(IllegalArgumentException.class,
            () -> PageCreationSupport.validatePageParent(context.resourceResolver(), "parent", "  "));
    }

    @Test
    public void rejectsParentNotUnderContent() {
        context.build().resource("/conf/site/en", "jcr:primaryType", "cq:Page").commit();
        assertThrows(IllegalArgumentException.class,
            () -> PageCreationSupport.validatePageParent(context.resourceResolver(), "parent", "/conf/site/en"));
    }

    @Test
    public void rejectsMissingParent() {
        assertThrows(IllegalArgumentException.class,
            () -> PageCreationSupport.validatePageParent(context.resourceResolver(), "parent", "/content/does/not/exist"));
    }

    @Test
    public void createPageStagesAPageFromATemplate() throws Exception {
        // aem-mock provides a working PageManager; create() with a real template + parent stages a page under it.
        context.load().json("/context/conf-templates.json", "/conf");
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        String pagePath = PageCreationSupport.createPage(context.resourceResolver(), "/content/site/en", "shop",
            "/conf/testsite/settings/wcm/templates/catalog-page", "Shop");

        assertEquals("/content/site/en/shop", pagePath);
        // The page is staged in the caller's resolver (createPage uses autoSave=false and does not commit).
        assertNotNull(context.resourceResolver().getResource("/content/site/en/shop"));
    }

    @Test
    public void createPageRejectsResolverWithNoPageManager() {
        // Fail closed when the caller's resolver cannot provide a PageManager.
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.adaptTo(PageManager.class)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
            () -> PageCreationSupport.createPage(resolver, "/content/site/en", "shop", "/some/template", "Shop"));
    }
}
