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
package com.adobe.cq.commerce.mcp.internal.tools.authoring;

import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Ports {@code SuggestTemplateForPageTypeToolTest}'s classification cases onto the hoisted
 * {@link PageTemplateSupport} helper, plus covers {@code resolveTemplate}'s explicit/auto-discovery paths.
 * <p>
 * The {@code conf-templates.json} fixture declares the pre-placed {@code product}/{@code productlist} nodes with
 * their {@code sling:resourceType} set directly to the core CIF type (not a Venia proxy) because the pinned
 * aem-mock's {@code Resource#isResourceType(String)} matches by exact identity only -- see the caveat on
 * {@code SuggestTemplateForPageTypeTool} / {@code CheckSpecificPageCapabilityTool}.
 */
public class PageTemplateSupportTest {

    @Rule
    public final AemContext context = new AemContext();

    private Resource template(String name) {
        context.load().json("/context/conf-templates.json", "/conf");
        return context.resourceResolver()
            .getResource("/conf/testsite/settings/wcm/templates/" + name);
    }

    @Test
    public void classifiesProductTemplateByComponent() {
        PageTemplateSupport.Classification c = PageTemplateSupport.classify(template("product-page"));
        assertEquals("product", c.getKind());
        assertEquals("Product page", c.getTitle());
        assertEquals("resourceSuperType", c.getSignal());
    }

    @Test
    public void classifiesCategoryTemplateByComponent() {
        PageTemplateSupport.Classification c = PageTemplateSupport.classify(template("category-page"));
        assertEquals("category", c.getKind());
        assertEquals("Category page", c.getTitle());
        assertEquals("resourceSuperType", c.getSignal());
    }

    @Test
    public void classifiesEmptyGridAsCatalog() {
        PageTemplateSupport.Classification c = PageTemplateSupport.classify(template("catalog-page"));
        assertEquals("catalog", c.getKind());
        assertEquals("Catalog Page", c.getTitle());
        assertEquals("resourceSuperType", c.getSignal());
    }

    @Test
    public void classifiesNonCommerceTemplateAsNull() {
        assertNull(PageTemplateSupport.classify(template("content-page")));
    }

    @Test
    public void fallsBackToTitleWhenInitialGridMissing() {
        context.build()
            .resource("/conf/titleonly/settings/wcm/templates/product-page/jcr:content",
                "jcr:primaryType", "cq:PageContent", "jcr:title", "Product page")
            .commit();
        Resource template = context.resourceResolver()
            .getResource("/conf/titleonly/settings/wcm/templates/product-page");

        PageTemplateSupport.Classification c = PageTemplateSupport.classify(template);
        assertEquals("product", c.getKind());
        assertEquals("Product page", c.getTitle());
        assertEquals("title", c.getSignal());
    }

    @Test
    public void resolveTemplateHonoursExplicitMatchingPath() {
        context.load().json("/context/conf-templates.json", "/conf");
        Resource resolved = PageTemplateSupport.resolveTemplate(context.resourceResolver(), "catalog",
            "/conf/testsite/settings/wcm/templates/catalog-page");
        assertEquals("/conf/testsite/settings/wcm/templates/catalog-page", resolved.getPath());
    }

    @Test
    public void resolveTemplateRejectsExplicitPathOfWrongKind() {
        context.load().json("/context/conf-templates.json", "/conf");
        // product-page classifies as "product", not "catalog" -> IAE
        assertThrows(IllegalArgumentException.class, () -> PageTemplateSupport.resolveTemplate(
            context.resourceResolver(), "catalog", "/conf/testsite/settings/wcm/templates/product-page"));
    }

    @Test
    public void resolveTemplateRejectsMissingExplicitPath() {
        context.load().json("/context/conf-templates.json", "/conf");
        assertThrows(IllegalArgumentException.class, () -> PageTemplateSupport.resolveTemplate(
            context.resourceResolver(), "catalog", "/conf/testsite/settings/wcm/templates/does-not-exist"));
    }

    @Test
    public void resolveTemplateAutoDiscoversByKind() {
        context.load().json("/context/conf-templates.json", "/conf");
        Resource resolved = PageTemplateSupport.resolveTemplate(context.resourceResolver(), "catalog", null);
        assertEquals("/conf/testsite/settings/wcm/templates/catalog-page", resolved.getPath());
    }

    @Test
    public void resolveTemplateAutoDiscoversProductByKind() {
        context.load().json("/context/conf-templates.json", "/conf");
        Resource resolved = PageTemplateSupport.resolveTemplate(context.resourceResolver(), "product", "  ");
        assertEquals("/conf/testsite/settings/wcm/templates/product-page", resolved.getPath());
    }

    @Test
    public void resolveTemplateThrowsWhenNoTemplateOfKind() {
        // Only a non-commerce template present -> no catalog/product/category match anywhere.
        context.build()
            .resource("/conf/empty/settings/wcm/templates/content-page/jcr:content",
                "jcr:primaryType", "cq:PageContent", "jcr:title", "Content page")
            .commit();
        assertThrows(IllegalArgumentException.class,
            () -> PageTemplateSupport.resolveTemplate(context.resourceResolver(), "catalog", null));
    }
}
