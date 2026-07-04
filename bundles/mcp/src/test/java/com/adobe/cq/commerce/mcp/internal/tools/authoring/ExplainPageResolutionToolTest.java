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
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExplainPageResolutionToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private Page productPageRoot;
    private Page categoryPageRoot;

    private void load() {
        context.load().json("/context/specific-pages.json", "/content");
        productPageRoot = context.pageManager().getPage("/content/site/product-page");
        categoryPageRoot = context.pageManager().getPage("/content/site/category-page");
    }

    private StoreContext ctxWithLandingPage(Page landingPage) {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);
        when(ctx.getLandingPage()).thenReturn(landingPage);
        return ctx;
    }

    @Test
    public void deeperNestedProductPageWinsAndReportsDepth() {
        load();
        StoreContext ctx = ctxWithLandingPage(productPageRoot);

        JsonNode out = new ExplainPageResolutionTool().call(ctx,
            mapper.createObjectNode().put("identifier", "productId1.1").put("type", "product"));

        assertEquals("productId1.1", out.get("identifier").asText());
        assertEquals("product", out.get("type").asText());
        assertEquals("/content/site/product-page/sub-page/nested-page", out.get("winningPage").asText());
        assertEquals(2, out.get("depth").asInt());

        JsonNode candidates = out.get("candidates");
        assertTrue(candidates.isArray());
        assertTrue(candidates.size() > 0);

        // real fixture values traced through: deepest match (nested-page) precedes its shallower parent
        JsonNode first = candidates.get(0);
        assertEquals("/content/site/product-page/sub-page/nested-page", first.get("path").asText());
        assertEquals(2, first.get("depth").asInt());
        assertTrue(first.get("matched").asBoolean());
        assertEquals("matched", first.get("why").asText());
    }

    @Test
    public void shallowerProductPageMatchesItsOwnFilter() {
        load();
        StoreContext ctx = ctxWithLandingPage(productPageRoot);

        JsonNode out = new ExplainPageResolutionTool().call(ctx,
            mapper.createObjectNode().put("identifier", "productId1").put("type", "product"));

        assertEquals("/content/site/product-page/sub-page", out.get("winningPage").asText());
        assertEquals(1, out.get("depth").asInt());
    }

    @Test
    public void deeperNestedCategoryPageWinsWithinIncludedSubtree() {
        load();
        StoreContext ctx = ctxWithLandingPage(categoryPageRoot);

        JsonNode out = new ExplainPageResolutionTool().call(ctx,
            mapper.createObjectNode().put("identifier", "men/tops/sweaters").put("type", "category"));

        assertEquals("/content/site/category-page/sub-page/nested-page", out.get("winningPage").asText());
        assertEquals(2, out.get("depth").asInt());

        JsonNode candidates = out.get("candidates");
        assertEquals("/content/site/category-page/sub-page/nested-page", candidates.get(0).get("path").asText());
        assertTrue(candidates.get(0).get("matched").asBoolean());
        assertEquals("/content/site/category-page/sub-page", candidates.get(1).get("path").asText());
        assertFalse(candidates.get(1).get("matched").asBoolean());
        assertFalse(candidates.get(1).get("why").asText().isEmpty());
    }

    @Test
    public void noMatchReturnsNullWinnerWithNegativeDepthAndFullTrace() {
        load();
        StoreContext ctx = ctxWithLandingPage(productPageRoot);

        JsonNode out = new ExplainPageResolutionTool().call(ctx,
            mapper.createObjectNode().put("identifier", "unknown-sku").put("type", "product"));

        assertTrue("winningPage must be JSON null, not absent/throw, on no-match", out.get("winningPage").isNull());
        assertEquals(-1, out.get("depth").asInt());

        JsonNode candidates = out.get("candidates");
        assertTrue(candidates.isArray());
        assertTrue(candidates.size() > 0);
        for (JsonNode candidate : candidates) {
            assertFalse(candidate.get("matched").asBoolean());
        }
    }

    @Test
    public void failsClosedWhenIdentifierMissing() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainPageResolutionTool().call(ctx, mapper.createObjectNode().put("type", "product")));
    }

    @Test
    public void failsClosedWhenIdentifierBlank() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainPageResolutionTool().call(ctx,
                mapper.createObjectNode().put("identifier", "   ").put("type", "product")));
    }

    @Test
    public void failsClosedWhenTypeMissing() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainPageResolutionTool().call(ctx,
                mapper.createObjectNode().put("identifier", "productId1")));
    }

    @Test
    public void failsClosedWhenTypeInvalid() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainPageResolutionTool().call(ctx,
                mapper.createObjectNode().put("identifier", "productId1").put("type", "bogus")));
    }

    @Test
    public void resolvesExplicitSiteRootArgInsteadOfEndpointLandingPage() {
        load();
        // Endpoint's own landing page is unrelated; the explicit siteRoot must be used instead.
        StoreContext ctx = ctxWithLandingPage(categoryPageRoot);

        JsonNode out = new ExplainPageResolutionTool().call(ctx,
            mapper.createObjectNode().put("identifier", "productId1.1").put("type", "product")
                .put("siteRoot", "/content/site/product-page"));

        assertEquals("/content/site/product-page/sub-page/nested-page", out.get("winningPage").asText());
        assertEquals(2, out.get("depth").asInt());
    }

    @Test
    public void failsClosedWhenSiteRootNotUnderContent() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainPageResolutionTool().call(ctx,
                mapper.createObjectNode().put("identifier", "productId1").put("type", "product")
                    .put("siteRoot", "/etc/somewhere")));
    }

    @Test
    public void failsClosedWhenSiteRootDoesNotResolveToPage() {
        load();
        context.build().resource("/content/not-a-page").commit();
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainPageResolutionTool().call(ctx,
                mapper.createObjectNode().put("identifier", "productId1").put("type", "product")
                    .put("siteRoot", "/content/not-a-page")));
    }

    @Test
    public void failsClosedWhenNoSiteRootArgAndNullLandingPage() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainPageResolutionTool().call(ctx,
                mapper.createObjectNode().put("identifier", "productId1").put("type", "product")));
    }
}
