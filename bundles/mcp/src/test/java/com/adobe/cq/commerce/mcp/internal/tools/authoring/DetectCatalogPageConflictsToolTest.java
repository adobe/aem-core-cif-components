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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DetectCatalogPageConflictsToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Registers a real {@code Page -> SiteStructure} adaptation (mirroring
     * {@code ListCatalogPagesToolTest}/{@code ExplainCatalogPageRoutingToolTest}) whose
     * {@link SiteStructure#getCategoryPages()} is backed by real aem-mock {@link Page}/{@link Resource} objects
     * loaded from the {@code catalog-pages-conflicts.json} fixture, so the tool traces real property values
     * (not a mock echo) through {@code CatalogPageRouting.listCatalogPages} into the JSON output.
     */
    private SiteStructure siteStructureFor(Page navRoot, Page... catalogPages) {
        SiteStructure.Entry[] entries = new SiteStructure.Entry[catalogPages.length + 1];
        for (int i = 0; i < catalogPages.length; i++) {
            Page catalogPage = catalogPages[i];
            SiteStructure.Entry entry = mock(SiteStructure.Entry.class);
            when(entry.getPage()).thenReturn(catalogPage);
            when(entry.getCatalogPage()).thenReturn(catalogPage);
            entries[i] = entry;
        }

        SiteStructure.Entry fallbackEntry = mock(SiteStructure.Entry.class);
        when(fallbackEntry.getPage()).thenReturn(navRoot);
        when(fallbackEntry.getCatalogPage()).thenReturn(null);
        entries[catalogPages.length] = fallbackEntry;

        SiteStructure site = mock(SiteStructure.class);
        when(site.getCategoryPages()).thenReturn(Arrays.asList(entries));
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
    public void detectsDuplicateAndAncestorDescendantOverlapsAndExcludesGenericPages() {
        context.load().json("/context/catalog-pages-conflicts.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page blouses = context.pageManager().getPage("/content/site/blouses");
        Page topsDuplicate = context.pageManager().getPage("/content/site/tops-duplicate");
        Page bottoms = context.pageManager().getPage("/content/site/bottoms");
        Page outlet = context.pageManager().getPage("/content/site/outlet");

        SiteStructure site = siteStructureFor(navRoot, tops, blouses, topsDuplicate, bottoms, outlet);
        context.registerAdapter(Page.class, SiteStructure.class, site);

        StoreContext ctx = ctxWithLandingPage(navRoot);

        JsonNode out = new DetectCatalogPageConflictsTool().call(ctx, mapper.createObjectNode());

        assertEquals("/content/site", out.get("siteRoot").asText());
        JsonNode overlaps = out.get("overlaps");
        assertTrue(overlaps.isArray());
        // Three scoped pages pairwise-overlap: tops (venia-tops) <-> blouses (venia-tops/venia-blouses) is
        // ancestor-descendant; tops <-> tops-duplicate (also venia-tops) is duplicate-scope; and
        // tops-duplicate <-> blouses is ALSO ancestor-descendant (tops-duplicate has the same scope as tops).
        // bottoms (disjoint "venia-bottoms") and the generic outlet/nav-root fallback participate in none.
        assertEquals(3, overlaps.size());

        int duplicateCount = 0;
        int ancestorDescendantCount = 0;
        boolean sawTopsBlouses = false;
        boolean sawTopsTopsDuplicate = false;
        boolean sawTopsDuplicateBlouses = false;
        for (JsonNode overlap : overlaps) {
            String kind = overlap.get("kind").asText();
            JsonNode pages = overlap.get("pages");
            assertEquals(2, pages.size());
            if ("duplicate-scope".equals(kind)) {
                duplicateCount++;
                assertEquals("venia-tops", overlap.get("scope").asText());
                assertTrue(containsText(pages, "/content/site/tops") && containsText(pages, "/content/site/tops-duplicate"));
                sawTopsTopsDuplicate = true;
            } else if ("ancestor-descendant".equals(kind)) {
                ancestorDescendantCount++;
                assertEquals("venia-tops", overlap.get("scope").asText());
                if (containsText(pages, "/content/site/tops") && containsText(pages, "/content/site/blouses")) {
                    sawTopsBlouses = true;
                } else if (containsText(pages, "/content/site/tops-duplicate") && containsText(pages, "/content/site/blouses")) {
                    sawTopsDuplicateBlouses = true;
                } else {
                    throw new AssertionError("unexpected ancestor-descendant pair: " + pages);
                }
            } else {
                throw new AssertionError("unexpected kind: " + kind);
            }
        }
        assertEquals("expected exactly one duplicate-scope overlap", 1, duplicateCount);
        assertEquals("expected exactly two ancestor-descendant overlaps", 2, ancestorDescendantCount);
        assertTrue("expected tops<->blouses ancestor-descendant overlap", sawTopsBlouses);
        assertTrue("expected tops<->tops-duplicate duplicate-scope overlap", sawTopsTopsDuplicate);
        assertTrue("expected tops-duplicate<->blouses ancestor-descendant overlap", sawTopsDuplicateBlouses);
    }

    @Test
    public void reportsNoOverlapsWhenAllScopesAreDisjoint() {
        context.load().json("/context/catalog-pages.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page outlet = context.pageManager().getPage("/content/site/outlet");
        Page bottoms = context.pageManager().getPage("/content/site/bottoms");

        // tops (urlPath "venia-tops"), outlet (generic: idType "uid"), bottoms (urlPath "venia-bottoms") -- all
        // disjoint, so no overlaps.
        SiteStructure site = siteStructureFor(navRoot, tops, outlet, bottoms);
        context.registerAdapter(Page.class, SiteStructure.class, site);

        StoreContext ctx = ctxWithLandingPage(navRoot);

        JsonNode out = new DetectCatalogPageConflictsTool().call(ctx, mapper.createObjectNode());

        assertEquals("/content/site", out.get("siteRoot").asText());
        assertTrue(out.get("overlaps").isArray());
        assertEquals(0, out.get("overlaps").size());
    }

    @Test
    public void reportsNoOverlapsWithOnlyOneCatalogPage() {
        context.load().json("/context/catalog-pages.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");

        SiteStructure.Entry topsEntry = mock(SiteStructure.Entry.class);
        when(topsEntry.getPage()).thenReturn(tops);
        when(topsEntry.getCatalogPage()).thenReturn(tops);
        SiteStructure.Entry fallbackEntry = mock(SiteStructure.Entry.class);
        when(fallbackEntry.getPage()).thenReturn(navRoot);
        when(fallbackEntry.getCatalogPage()).thenReturn(null);

        SiteStructure site = mock(SiteStructure.class);
        when(site.getCategoryPages()).thenReturn(Arrays.asList(topsEntry, fallbackEntry));
        when(site.getLandingPage()).thenReturn(navRoot);
        context.registerAdapter(Page.class, SiteStructure.class, site);

        StoreContext ctx = ctxWithLandingPage(navRoot);

        JsonNode out = new DetectCatalogPageConflictsTool().call(ctx, mapper.createObjectNode());

        assertEquals(0, out.get("overlaps").size());
    }

    @Test
    public void resolvesExplicitSiteRootArgInsteadOfEndpointLandingPage() {
        context.load().json("/context/catalog-pages-conflicts.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page blouses = context.pageManager().getPage("/content/site/blouses");
        SiteStructure site = siteStructureFor(navRoot, tops, blouses);

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

        JsonNode out = new DetectCatalogPageConflictsTool().call(ctx,
            mapper.createObjectNode().put("siteRoot", "/content/site/tops"));

        assertEquals("/content/site", out.get("siteRoot").asText());
        assertEquals(1, out.get("overlaps").size());
    }

    @Test
    public void failsClosedWhenSiteRootNotUnderContent() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new DetectCatalogPageConflictsTool().call(ctx, mapper.createObjectNode().put("siteRoot", "/etc/somewhere")));
    }

    @Test
    public void failsClosedWhenSiteRootDoesNotResolveToAPage() {
        context.build().resource("/content/not-a-page").commit();
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new DetectCatalogPageConflictsTool().call(ctx, mapper.createObjectNode().put("siteRoot", "/content/not-a-page")));
    }

    @Test
    public void failsClosedWhenSiteRootPageDoesNotAdaptToSiteStructure() {
        context.build().resource("/content/lonely/jcr:content", "jcr:primaryType", "cq:PageContent").commit();
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new DetectCatalogPageConflictsTool().call(ctx, mapper.createObjectNode().put("siteRoot", "/content/lonely")));
    }

    private static boolean containsText(JsonNode arrayNode, String text) {
        for (JsonNode node : arrayNode) {
            if (text.equals(node.asText())) {
                return true;
            }
        }
        return false;
    }
}
