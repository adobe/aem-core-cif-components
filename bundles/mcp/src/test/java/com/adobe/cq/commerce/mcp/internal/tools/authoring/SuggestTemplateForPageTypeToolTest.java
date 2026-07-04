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
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Note on {@code conf-templates.json}'s {@code product}/{@code productlist} pre-placed nodes: they declare
 * {@code sling:resourceType} directly as the core CIF type (e.g.
 * {@code core/cif/components/commerce/product/v3/product}), not as a Venia proxy
 * ({@code venia/components/commerce/product}) with the core type as an {@code /apps}-registered super type. This
 * is because the pinned aem-mock's {@code Resource#isResourceType(String)} matches by exact identity only and does
 * not walk a proxy's super-type chain through {@code /apps} component definitions the way real AEM does -- see the
 * same caveat documented on {@link CheckSpecificPageCapabilityTool} and on
 * {@link SuggestTemplateForPageTypeTool}'s class javadoc ("aem-mock limitation"). The fixture therefore models the
 * end state real AEM would resolve to, and the real Venia proxy path can only be re-verified live, post-deploy.
 */
public class SuggestTemplateForPageTypeToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private StoreContext ctx() {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);
        return ctx;
    }

    @Test
    public void suggestsOnlyTheProductTemplateForKindProduct() {
        context.load().json("/context/conf-templates.json", "/conf");

        JsonNode out = new SuggestTemplateForPageTypeTool().call(ctx(), mapper.createObjectNode().put("kind", "product"));

        assertEquals("product", out.get("kind").asText());
        JsonNode templates = out.get("templates");
        assertEquals(1, templates.size());
        JsonNode entry = templates.get(0);
        assertEquals("/conf/testsite/settings/wcm/templates/product-page", entry.get("path").asText());
        assertEquals("Product page", entry.get("title").asText());
        assertEquals("resourceSuperType", entry.get("signal").asText());
    }

    @Test
    public void suggestsOnlyTheCategoryTemplateForKindCategory() {
        context.load().json("/context/conf-templates.json", "/conf");

        JsonNode out = new SuggestTemplateForPageTypeTool().call(ctx(), mapper.createObjectNode().put("kind", "category"));

        JsonNode templates = out.get("templates");
        assertEquals(1, templates.size());
        JsonNode entry = templates.get(0);
        assertEquals("/conf/testsite/settings/wcm/templates/category-page", entry.get("path").asText());
        assertEquals("Category page", entry.get("title").asText());
        assertEquals("resourceSuperType", entry.get("signal").asText());
    }

    @Test
    public void suggestsOnlyTheEmptyGridTemplateForKindCatalog() {
        context.load().json("/context/conf-templates.json", "/conf");

        JsonNode out = new SuggestTemplateForPageTypeTool().call(ctx(), mapper.createObjectNode().put("kind", "catalog"));

        JsonNode templates = out.get("templates");
        assertEquals(1, templates.size());
        JsonNode entry = templates.get(0);
        assertEquals("/conf/testsite/settings/wcm/templates/catalog-page", entry.get("path").asText());
        assertEquals("Catalog Page", entry.get("title").asText());
        assertEquals("resourceSuperType", entry.get("signal").asText());
    }

    @Test
    public void neverMatchesTheNonCommerceTemplate() {
        context.load().json("/context/conf-templates.json", "/conf");

        for (String kind : new String[] { "product", "category", "catalog" }) {
            JsonNode out = new SuggestTemplateForPageTypeTool().call(ctx(), mapper.createObjectNode().put("kind", kind));
            for (JsonNode entry : out.get("templates")) {
                assertEquals("content-page template must never match any kind",
                    false, entry.get("path").asText().endsWith("content-page"));
            }
        }
    }

    @Test
    public void fallsBackToTitleMatchWhenInitialGridIsMissing() {
        context.build()
            .resource("/conf/titleonly/settings/wcm/templates/product-page/jcr:content",
                "jcr:primaryType", "cq:PageContent", "jcr:title", "Product page")
            .commit();

        JsonNode out = new SuggestTemplateForPageTypeTool().call(ctx(), mapper.createObjectNode().put("kind", "product"));

        JsonNode templates = out.get("templates");
        assertEquals(1, templates.size());
        JsonNode entry = templates.get(0);
        assertEquals("/conf/titleonly/settings/wcm/templates/product-page", entry.get("path").asText());
        assertEquals("Product page", entry.get("title").asText());
        assertEquals("title", entry.get("signal").asText());
    }

    @Test
    public void throwsOnInvalidKind() {
        context.load().json("/context/conf-templates.json", "/conf");

        assertThrows(IllegalArgumentException.class,
            () -> new SuggestTemplateForPageTypeTool().call(ctx(), mapper.createObjectNode().put("kind", "bogus")));
    }

    @Test
    public void throwsWhenKindIsMissing() {
        context.load().json("/context/conf-templates.json", "/conf");

        assertThrows(IllegalArgumentException.class,
            () -> new SuggestTemplateForPageTypeTool().call(ctx(), mapper.createObjectNode()));
    }
}
