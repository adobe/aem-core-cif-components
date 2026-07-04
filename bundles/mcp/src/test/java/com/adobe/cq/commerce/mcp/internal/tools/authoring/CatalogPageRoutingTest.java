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
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogPageRoutingTest {

    @Rule
    public final AemContext context = new AemContext();

    private Page navRoot;
    private Page topsCatalogPage;
    private Page outletCatalogPage;

    private SiteStructure siteStructureWithFallbackLast() {
        context.load().json("/context/catalog-pages.json", "/content");
        navRoot = context.pageManager().getPage("/content/site");
        topsCatalogPage = context.pageManager().getPage("/content/site/tops");
        outletCatalogPage = context.pageManager().getPage("/content/site/outlet");

        // SiteStructure.getCategoryPages() returns Entry objects whose getPage() is the (resolved) category/PLP
        // page and whose getCatalogPage() is the owning catalog page carrying the magentoRootCategoryId(+Type)
        // scope properties. Mock only the SiteStructure/Entry seam; the underlying Page/ValueMap objects are real
        // aem-mock pages so property reads are exercised for real (see SpecificPageStrategy.isSpecificCatalogPageFor,
        // which reads these properties off Entry.getCatalogPage(), not Entry.getPage()).
        SiteStructure.Entry topsEntry = mock(SiteStructure.Entry.class);
        when(topsEntry.getPage()).thenReturn(topsCatalogPage);
        when(topsEntry.getCatalogPage()).thenReturn(topsCatalogPage);

        SiteStructure.Entry outletEntry = mock(SiteStructure.Entry.class);
        when(outletEntry.getPage()).thenReturn(outletCatalogPage);
        when(outletEntry.getCatalogPage()).thenReturn(outletCatalogPage);

        // The nav-root/landing page is always appended last by SiteStructureImpl.getCatalogPages(), and (per
        // EntryImpl) its Entry.getCatalogPage() is null -- it IS the generic fallback, not scoped by any catalog
        // page's magentoRootCategoryId(+Type).
        SiteStructure.Entry fallbackEntry = mock(SiteStructure.Entry.class);
        when(fallbackEntry.getPage()).thenReturn(navRoot);
        when(fallbackEntry.getCatalogPage()).thenReturn(null);

        SiteStructure site = mock(SiteStructure.class);
        when(site.getCategoryPages()).thenReturn(Arrays.asList(topsEntry, outletEntry, fallbackEntry));
        when(site.getLandingPage()).thenReturn(navRoot);
        return site;
    }

    @Test
    public void listsCatalogPagesInOrderWithGenericFallbackLast() {
        SiteStructure site = siteStructureWithFallbackLast();

        List<CatalogPageRouting.CatalogPageInfo> pages = new CatalogPageRouting().listCatalogPages(site);

        assertEquals(3, pages.size());

        CatalogPageRouting.CatalogPageInfo tops = pages.get(0);
        assertEquals("/content/site/tops", tops.getPath());
        assertEquals("venia-tops", tops.getRootCategoryId());
        assertEquals("urlPath", tops.getIdType());
        assertFalse(tops.isGenericFallback());

        CatalogPageRouting.CatalogPageInfo outlet = pages.get(1);
        assertEquals("/content/site/outlet", outlet.getPath());
        assertEquals("MjA=", outlet.getRootCategoryId());
        assertEquals("uid", outlet.getIdType());
        // idType != "urlPath" => generic, even though rootCategoryId is set
        assertTrue(outlet.isGenericFallback());

        CatalogPageRouting.CatalogPageInfo fallback = pages.get(2);
        assertEquals("/content/site", fallback.getPath());
        assertTrue(fallback.isGenericFallback());
    }

    @Test
    public void resolvesSubPathToUrlPathScopedPage() {
        SiteStructure site = siteStructureWithFallbackLast();

        CatalogPageRouting.CatalogPageResolution resolution = new CatalogPageRouting().resolveFor(site, "venia-tops/venia-blouses");

        assertEquals("/content/site/tops", resolution.getWinningPage().getPath());
        assertEquals("urlPath-scope-match", resolution.getReason());

        // full ordered trace: the winner (index 0) is matched; later candidates were never reached because the
        // first match already won (first-match-wins, no explicit mapping table).
        List<CatalogPageRouting.CandidateEvaluation> trace = resolution.getTrace();
        assertEquals(3, trace.size());
        assertEquals("/content/site/tops", trace.get(0).getPath());
        assertTrue(trace.get(0).isMatched());
        assertFalse(trace.get(1).isMatched());
        assertFalse(trace.get(2).isMatched());
    }

    @Test
    public void fallsThroughToGenericFallbackWhenNoScopeMatches() {
        SiteStructure site = siteStructureWithFallbackLast();

        // "venia-bottoms" matches neither the "tops" urlPath scope nor "outlet" (which is itself generic because
        // its idType is "uid", not "urlPath") -- so "outlet" (index 1) wins as the first generic candidate, before
        // ever reaching the nav-root fallback (index 2).
        CatalogPageRouting.CatalogPageResolution resolution = new CatalogPageRouting().resolveFor(site, "venia-bottoms");

        assertEquals("/content/site/outlet", resolution.getWinningPage().getPath());
        assertEquals("generic-fallback", resolution.getReason());

        List<CatalogPageRouting.CandidateEvaluation> trace = resolution.getTrace();
        assertEquals(3, trace.size());
        assertFalse(trace.get(0).isMatched());
        assertTrue(trace.get(1).isMatched());
        assertEquals("/content/site/outlet", trace.get(1).getPath());
        assertFalse(trace.get(2).isMatched());
    }

    @Test
    public void fallsThroughToNavRootWhenNoCatalogPageMatchesOrIsGeneric() {
        // a stricter fixture where BOTH catalog pages are urlPath-scoped (no non-scoped catalog page exists), so a
        // url path outside both scopes must fall through all the way to the nav-root/landing-page entry.
        context.load().json("/context/catalog-pages.json", "/content");
        navRoot = context.pageManager().getPage("/content/site");
        topsCatalogPage = context.pageManager().getPage("/content/site/tops");
        Page bottomsCatalogPage = context.pageManager().getPage("/content/site/bottoms");

        SiteStructure.Entry topsEntry = mock(SiteStructure.Entry.class);
        when(topsEntry.getPage()).thenReturn(topsCatalogPage);
        when(topsEntry.getCatalogPage()).thenReturn(topsCatalogPage);

        SiteStructure.Entry bottomsEntry = mock(SiteStructure.Entry.class);
        when(bottomsEntry.getPage()).thenReturn(bottomsCatalogPage);
        when(bottomsEntry.getCatalogPage()).thenReturn(bottomsCatalogPage);

        SiteStructure.Entry fallbackEntry = mock(SiteStructure.Entry.class);
        when(fallbackEntry.getPage()).thenReturn(navRoot);
        when(fallbackEntry.getCatalogPage()).thenReturn(null);

        SiteStructure site = mock(SiteStructure.class);
        when(site.getCategoryPages()).thenReturn(Arrays.asList(topsEntry, bottomsEntry, fallbackEntry));

        CatalogPageRouting.CatalogPageResolution resolution = new CatalogPageRouting().resolveFor(site, "venia-accessories");

        assertEquals("/content/site", resolution.getWinningPage().getPath());
        assertEquals("generic-fallback", resolution.getReason());

        List<CatalogPageRouting.CandidateEvaluation> trace = resolution.getTrace();
        assertEquals(3, trace.size());
        assertFalse(trace.get(0).isMatched());
        assertFalse(trace.get(1).isMatched());
        assertTrue(trace.get(2).isMatched());
        assertEquals("/content/site", trace.get(2).getPath());
    }
}
