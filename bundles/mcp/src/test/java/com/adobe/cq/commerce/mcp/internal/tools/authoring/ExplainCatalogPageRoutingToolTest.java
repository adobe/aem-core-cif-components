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

import java.util.Arrays;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
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

public class ExplainCatalogPageRoutingToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Registers a real {@code Page -> SiteStructure} adaptation (mirroring {@code ListCatalogPagesToolTest}) whose
     * {@link SiteStructure#getCategoryPages()} is backed by real aem-mock {@link Page}/{@link Resource} objects
     * loaded from the shared {@code catalog-pages.json} fixture, so the tool traces real property values (not a
     * mock echo) through {@code CatalogPageRouting.resolveFor} into the JSON output.
     */
    private SiteStructure siteStructureFor(Page navRoot, Page topsCatalogPage, Page outletCatalogPage) {
        SiteStructure.Entry topsEntry = mock(SiteStructure.Entry.class);
        when(topsEntry.getPage()).thenReturn(topsCatalogPage);
        when(topsEntry.getCatalogPage()).thenReturn(topsCatalogPage);

        SiteStructure.Entry outletEntry = mock(SiteStructure.Entry.class);
        when(outletEntry.getPage()).thenReturn(outletCatalogPage);
        when(outletEntry.getCatalogPage()).thenReturn(outletCatalogPage);

        SiteStructure.Entry fallbackEntry = mock(SiteStructure.Entry.class);
        when(fallbackEntry.getPage()).thenReturn(navRoot);
        when(fallbackEntry.getCatalogPage()).thenReturn(null);

        SiteStructure site = mock(SiteStructure.class);
        when(site.getCategoryPages()).thenReturn(Arrays.asList(topsEntry, outletEntry, fallbackEntry));
        when(site.getLandingPage()).thenReturn(navRoot);
        return site;
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
    public void resolvesSubPathToUrlPathScopedPageWithOrderedTrace() {
        context.load().json("/context/catalog-pages.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page outlet = context.pageManager().getPage("/content/site/outlet");

        SiteStructure site = siteStructureFor(navRoot, tops, outlet);
        context.registerAdapter(Page.class, SiteStructure.class, site);

        StoreContext ctx = ctxWithLandingPage(navRoot);

        JsonNode out = new ExplainCatalogPageRoutingTool().call(ctx,
            mapper.createObjectNode().put("urlPath", "venia-tops/venia-blouses"));

        assertEquals("venia-tops/venia-blouses", out.get("identifier").asText());
        assertEquals("/content/site/tops", out.get("winningPage").asText());
        assertEquals("urlPath-scope-match", out.get("reason").asText());

        JsonNode candidates = out.get("candidates");
        assertTrue(candidates.isArray());
        assertEquals(3, candidates.size());

        JsonNode first = candidates.get(0);
        assertEquals("/content/site/tops", first.get("path").asText());
        assertTrue(first.get("matched").asBoolean());
        assertEquals("urlPath-scope-match", first.get("why").asText());

        assertFalse(candidates.get(1).get("matched").asBoolean());
        assertFalse(candidates.get(2).get("matched").asBoolean());
    }

    @Test
    public void fallsThroughToGenericFallbackWhenNoScopeMatches() {
        context.load().json("/context/catalog-pages.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page outlet = context.pageManager().getPage("/content/site/outlet");

        SiteStructure site = siteStructureFor(navRoot, tops, outlet);
        context.registerAdapter(Page.class, SiteStructure.class, site);

        StoreContext ctx = ctxWithLandingPage(navRoot);

        // "venia-bottoms" matches neither the "tops" urlPath scope nor "outlet" (generic because its idType is
        // "uid", not "urlPath") -- so "outlet" wins as the first generic candidate.
        JsonNode out = new ExplainCatalogPageRoutingTool().call(ctx,
            mapper.createObjectNode().put("urlPath", "venia-bottoms"));

        assertEquals("/content/site/outlet", out.get("winningPage").asText());
        assertEquals("generic-fallback", out.get("reason").asText());

        JsonNode candidates = out.get("candidates");
        assertEquals(3, candidates.size());
        assertFalse(candidates.get(0).get("matched").asBoolean());
        assertTrue(candidates.get(1).get("matched").asBoolean());
        assertEquals("/content/site/outlet", candidates.get(1).get("path").asText());
        assertFalse(candidates.get(2).get("matched").asBoolean());
    }

    @Test
    public void handlesNoMatchWithoutNpeWhenNoCatalogPageAndNoFallbackExists() {
        // A SiteStructure whose only entries are urlPath-scoped (no generic fallback entry at all), so a
        // non-matching urlPath must produce a NULL winning page -- resolveFor's no-match path -- and the tool must
        // not NPE, instead emitting winningPage as JSON null alongside the reason/trace.
        context.load().json("/context/catalog-pages.json", "/content");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page bottoms = context.pageManager().getPage("/content/site/bottoms");

        SiteStructure.Entry topsEntry = mock(SiteStructure.Entry.class);
        when(topsEntry.getPage()).thenReturn(tops);
        when(topsEntry.getCatalogPage()).thenReturn(tops);

        SiteStructure.Entry bottomsEntry = mock(SiteStructure.Entry.class);
        when(bottomsEntry.getPage()).thenReturn(bottoms);
        when(bottomsEntry.getCatalogPage()).thenReturn(bottoms);

        SiteStructure site = mock(SiteStructure.class);
        when(site.getCategoryPages()).thenReturn(Arrays.asList(topsEntry, bottomsEntry));
        when(site.getLandingPage()).thenReturn(tops);
        context.registerAdapter(Page.class, SiteStructure.class, site);

        StoreContext ctx = ctxWithLandingPage(tops);

        JsonNode out = new ExplainCatalogPageRoutingTool().call(ctx,
            mapper.createObjectNode().put("urlPath", "venia-accessories"));

        assertEquals("venia-accessories", out.get("identifier").asText());
        assertTrue("winningPage must be JSON null, not absent/throw, on no-match", out.get("winningPage").isNull());
        assertEquals("no-match", out.get("reason").asText());

        JsonNode candidates = out.get("candidates");
        assertEquals(2, candidates.size());
        assertFalse(candidates.get(0).get("matched").asBoolean());
        assertFalse(candidates.get(1).get("matched").asBoolean());
    }

    @Test
    public void failsClosedWhenUrlPathMissing() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainCatalogPageRoutingTool().call(ctx, mapper.createObjectNode()));
    }

    @Test
    public void failsClosedWhenUrlPathBlank() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainCatalogPageRoutingTool().call(ctx, mapper.createObjectNode().put("urlPath", "   ")));
    }

    @Test
    public void resolvesExplicitSiteRootArgInsteadOfEndpointLandingPage() {
        context.load().json("/context/catalog-pages.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page outlet = context.pageManager().getPage("/content/site/outlet");
        SiteStructure site = siteStructureFor(navRoot, tops, outlet);

        context.build().resource("/content/other/jcr:content", "sling:resourceType",
            "core/cif/components/structure/page/v3/page", "navRoot", true).commit();
        Page otherNavRoot = context.pageManager().getPage("/content/other");
        SiteStructure otherSite = mock(SiteStructure.class);
        when(otherSite.getCategoryPages()).thenThrow(new AssertionError(
            "siteRoot arg must take precedence over ctx.getLandingPage(); the endpoint nav root's SiteStructure "
                + "must not be consulted when an explicit siteRoot is supplied"));
        when(otherSite.getLandingPage()).thenReturn(otherNavRoot);

        context.registerAdapter(Page.class, SiteStructure.class,
            (com.google.common.base.Function<Page, SiteStructure>) page -> page.getPath().startsWith("/content/site") ? site
                : otherSite);

        StoreContext ctx = ctxWithLandingPage(otherNavRoot);

        JsonNode out = new ExplainCatalogPageRoutingTool().call(ctx,
            mapper.createObjectNode().put("urlPath", "venia-tops/venia-blouses").put("siteRoot", "/content/site/tops"));

        assertEquals("/content/site/tops", out.get("winningPage").asText());
        assertEquals("urlPath-scope-match", out.get("reason").asText());
    }

    @Test
    public void failsClosedWhenSiteRootNotUnderContent() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ExplainCatalogPageRoutingTool().call(ctx,
                mapper.createObjectNode().put("urlPath", "venia-tops").put("siteRoot", "/etc/somewhere")));
    }
}
