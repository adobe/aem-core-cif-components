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

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckSpecificPageCapabilityToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private void load() {
        context.load().json("/context/specific-page-capability.json", "/content");
    }

    private StoreContext ctx() {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);
        return ctx;
    }

    private JsonNode call(String path) {
        return new CheckSpecificPageCapabilityTool().call(ctx(), mapper.createObjectNode().put("path", path));
    }

    @Test
    public void v1ProductPageReportsNoUseForCategoriesCapability() {
        load();

        JsonNode result = call("/content/site/product-page-v1");

        assertEquals("/content/site/product-page-v1", result.get("path").asText());
        assertEquals("product", result.get("pageType").asText());
        assertEquals("v1", result.get("componentVersion").asText());

        JsonNode fields = result.get("fields");
        assertTrue(fields.get("selectorFilter").asBoolean());
        assertTrue(fields.get("selectorFilterType").asBoolean());
        assertFalse("v1 product page cannot use includesSubCategories (v2+ applicability)", fields.get("includesSubCategories")
            .asBoolean());
        assertFalse("useForCategories is v2+ only", fields.get("useForCategories").asBoolean());
    }

    @Test
    public void v2ProductPageReportsUseForCategoriesCapability() {
        load();

        JsonNode result = call("/content/site/product-page-v2");

        assertEquals("product", result.get("pageType").asText());
        assertEquals("v2", result.get("componentVersion").asText());

        JsonNode fields = result.get("fields");
        assertTrue(fields.get("includesSubCategories").asBoolean());
        assertTrue("useForCategories is available from v2 onward", fields.get("useForCategories").asBoolean());
    }

    @Test
    public void v1CategoryPageReportsIncludesSubCategoriesCapability() {
        load();

        JsonNode result = call("/content/site/category-page-v1");

        assertEquals("category", result.get("pageType").asText());
        assertEquals("v1", result.get("componentVersion").asText());

        JsonNode fields = result.get("fields");
        assertTrue(fields.get("selectorFilter").asBoolean());
        assertTrue(fields.get("selectorFilterType").asBoolean());
        assertTrue("includesSubCategories exists on category pages in v1/v2/v3", fields.get("includesSubCategories").asBoolean());
        assertFalse("useForCategories is a product-page-only field", fields.get("useForCategories").asBoolean());
    }

    @Test
    public void v3CategoryPageReportsIncludesSubCategoriesCapability() {
        load();

        JsonNode result = call("/content/site/category-page-v3");

        assertEquals("category", result.get("pageType").asText());
        assertEquals("v3", result.get("componentVersion").asText());

        JsonNode fields = result.get("fields");
        assertTrue(fields.get("includesSubCategories").asBoolean());
        assertFalse(fields.get("useForCategories").asBoolean());
    }

    // NOTE: a proxied-resourceSuperType case (Venia-style: sling:resourceType is a project-local path,
    // sling:resourceSuperType chains to core/cif/components/structure/page/v3/page) cannot be exercised here.
    // This module's pinned aem-mock version (io.wcm.testing.aem-mock:2.2.6, pulling in
    // org.apache.sling.testing.resourceresolver-mock:1.1.18) implements MockResourceResolver#isResourceType as a
    // strict resource.getResourceType().equals(resourceType) check -- it does not walk sling:resourceSuperType at
    // all (verified against that version's source; later resourceresolver-mock versions, e.g. 1.1.22, do walk the
    // chain via getParentResourceType). The production code still uses Resource#isResourceType (see
    // CheckSpecificPageCapabilityTool's javadoc), matching every sibling tool's fail-closed type-check idiom
    // (ConfigureCatalogPageTool, ConfigureProductComponentTool, ConfigureProductListComponentTool), which IS
    // super-type-aware on a real Sling/AEM ResourceResolver -- this is a test-environment limitation, not a
    // production behavior gap.

    @Test
    public void failsClosedWhenPageIsNotACifStructurePage() {
        load();
        assertThrows(IllegalArgumentException.class, () -> call("/content/site/not-a-structure-page"));
    }

    @Test
    public void failsClosedWhenPathMissing() {
        assertThrows(IllegalArgumentException.class,
            () -> new CheckSpecificPageCapabilityTool().call(ctx(), mapper.createObjectNode()));
    }

    @Test
    public void failsClosedWhenPathBlank() {
        assertThrows(IllegalArgumentException.class, () -> call("   "));
    }

    @Test
    public void failsClosedWhenPathNotUnderContent() {
        assertThrows(IllegalArgumentException.class, () -> call("/etc/somewhere"));
    }

    @Test
    public void failsClosedWhenResourceNotFound() {
        load();
        assertThrows(IllegalArgumentException.class, () -> call("/content/site/does-not-exist"));
    }

    @Test
    public void failsClosedWhenResourceNotAPage() {
        load();
        context.build().resource("/content/not-a-page").commit();
        assertThrows(IllegalArgumentException.class, () -> call("/content/not-a-page"));
    }
}
