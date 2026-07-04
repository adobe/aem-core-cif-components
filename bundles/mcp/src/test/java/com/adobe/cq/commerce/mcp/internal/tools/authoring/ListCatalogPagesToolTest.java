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
import com.google.common.base.Function;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListCatalogPagesToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Registers a real {@code Page -> SiteStructure} adaptation (mirroring {@code StoreContextResolverTest}) whose
     * {@link SiteStructure#getCategoryPages()} is backed by real aem-mock {@link Page}/{@link Resource} objects
     * loaded from the shared {@code catalog-pages.json} fixture, so the tool's mapping of real property values
     * (not a mock echo) is exercised end to end.
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
    public void defaultsToEndpointNavRootAndListsInOrderWithFallbackLast() {
        context.load().json("/context/catalog-pages.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page outlet = context.pageManager().getPage("/content/site/outlet");

        SiteStructure site = siteStructureFor(navRoot, tops, outlet);
        context.registerAdapter(Page.class, SiteStructure.class, site);

        StoreContext ctx = ctxWithLandingPage(navRoot);

        JsonNode out = new ListCatalogPagesTool().call(ctx, mapper.createObjectNode());

        assertEquals("/content/site", out.get("siteRoot").asText());
        JsonNode pages = out.get("catalogPages");
        assertTrue(pages.isArray());
        assertEquals(3, pages.size());

        JsonNode topsNode = pages.get(0);
        assertEquals("/content/site/tops", topsNode.get("path").asText());
        assertEquals("venia-tops", topsNode.get("rootCategoryId").asText());
        assertEquals("urlPath", topsNode.get("idType").asText());
        assertFalse(topsNode.get("genericFallback").asBoolean());

        JsonNode outletNode = pages.get(1);
        assertEquals("/content/site/outlet", outletNode.get("path").asText());
        assertEquals("MjA=", outletNode.get("rootCategoryId").asText());
        assertEquals("uid", outletNode.get("idType").asText());
        assertTrue(outletNode.get("genericFallback").asBoolean());

        JsonNode fallbackNode = pages.get(2);
        assertEquals("/content/site", fallbackNode.get("path").asText());
        assertTrue(fallbackNode.get("genericFallback").asBoolean());
        // the nav-root fallback entry has no owning catalog page (Entry.getCatalogPage() == null), so its scope
        // fields are genuinely absent -- not guessed/defaulted.
        assertTrue(fallbackNode.get("rootCategoryId").isNull());
        assertTrue(fallbackNode.get("idType").isNull());
    }

    @Test
    public void resolvesExplicitSiteRootArgInsteadOfEndpointLandingPage() {
        context.load().json("/context/catalog-pages.json", "/content");
        Page navRoot = context.pageManager().getPage("/content/site");
        Page tops = context.pageManager().getPage("/content/site/tops");
        Page outlet = context.pageManager().getPage("/content/site/outlet");
        SiteStructure site = siteStructureFor(navRoot, tops, outlet);

        // A second, unrelated nav root that ONLY ctx.getLandingPage() points to -- adapting it to SiteStructure
        // must never be consulted once an explicit siteRoot arg is supplied.
        context.build().resource("/content/other/jcr:content", "sling:resourceType",
            "core/cif/components/structure/page/v3/page", "navRoot", true).commit();
        Page otherNavRoot = context.pageManager().getPage("/content/other");
        SiteStructure otherSite = mock(SiteStructure.class);
        when(otherSite.getCategoryPages()).thenThrow(new AssertionError(
            "siteRoot arg must take precedence over ctx.getLandingPage(); the endpoint nav root's SiteStructure "
                + "must not be consulted when an explicit siteRoot is supplied"));
        when(otherSite.getLandingPage()).thenReturn(otherNavRoot);

        context.registerAdapter(Page.class, SiteStructure.class, (Function<Page, SiteStructure>) page -> page.getPath()
            .startsWith("/content/site") ? site : otherSite);

        StoreContext ctx = ctxWithLandingPage(otherNavRoot);

        // siteRoot arg points INTO the /content/site tree (a non-root descendant), proving resolution walks up to
        // that site's own nav root/SiteStructure rather than using ctx.getLandingPage()'s.
        JsonNode out = new ListCatalogPagesTool().call(ctx, mapper.createObjectNode().put("siteRoot", "/content/site/tops"));

        // siteRoot in the output reports the RESOLVED nav-root path (site.getLandingPage()), not the raw arg.
        assertEquals("/content/site", out.get("siteRoot").asText());
        assertEquals(3, out.get("catalogPages").size());
    }

    @Test
    public void failsClosedWhenSiteRootNotUnderContent() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ListCatalogPagesTool().call(ctx, mapper.createObjectNode().put("siteRoot", "/etc/somewhere")));
    }

    @Test
    public void failsClosedWhenSiteRootDoesNotResolveToAPage() {
        context.build().resource("/content/not-a-page").commit();
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ListCatalogPagesTool().call(ctx, mapper.createObjectNode().put("siteRoot", "/content/not-a-page")));
    }

    @Test
    public void failsClosedWhenSiteRootPageDoesNotAdaptToSiteStructure() {
        context.build().resource("/content/lonely/jcr:content", "jcr:primaryType", "cq:PageContent").commit();
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ListCatalogPagesTool().call(ctx, mapper.createObjectNode().put("siteRoot", "/content/lonely")));
    }
}
