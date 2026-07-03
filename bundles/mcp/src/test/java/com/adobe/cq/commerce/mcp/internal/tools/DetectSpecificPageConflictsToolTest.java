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
package com.adobe.cq.commerce.mcp.internal.tools;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Rule;
import org.junit.Test;

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

public class DetectSpecificPageConflictsToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private Page routingRoot;
    private Page cleanRoot;

    private void load() {
        context.load().json("/context/specific-pages-conflicts.json", "/content");
        routingRoot = context.pageManager().getPage("/content/site/routing");
        cleanRoot = context.pageManager().getPage("/content/site/clean-site");
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
    public void detectsDuplicateScopesForBothCategoryAndUseForCategoriesBindings() {
        load();
        StoreContext ctx = ctxWithLandingPage(routingRoot);

        JsonNode out = new DetectSpecificPageConflictsTool().call(ctx, mapper.createObjectNode());

        assertEquals("/content/site/routing", out.get("siteRoot").asText());

        JsonNode duplicates = out.get("duplicates");
        assertTrue(duplicates.isArray());
        assertEquals(2, duplicates.size());

        boolean sawMenDuplicate = false;
        boolean sawAccessoriesDuplicate = false;
        for (JsonNode duplicate : duplicates) {
            String scope = duplicate.get("scope").asText();
            JsonNode pages = duplicate.get("pages");
            assertEquals(2, pages.size());
            if ("men".equals(scope)) {
                sawMenDuplicate = true;
                assertTrue(containsText(pages, "/content/site/routing/dup-cat-a"));
                assertTrue(containsText(pages, "/content/site/routing/dup-cat-b"));
            } else if ("accessories".equals(scope)) {
                sawAccessoriesDuplicate = true;
                assertTrue(containsText(pages, "/content/site/routing/dup-prod-a"));
                assertTrue(containsText(pages, "/content/site/routing/dup-prod-b"));
            } else {
                throw new AssertionError("unexpected duplicate scope: " + scope);
            }
        }
        assertTrue("expected a duplicate 'men' category scope", sawMenDuplicate);
        assertTrue("expected a duplicate 'accessories' useForCategories scope", sawAccessoriesDuplicate);
    }

    @Test
    public void singlePageWithNestedUseForCategoriesEntriesDoesNotSelfShadowOrSelfDuplicate() {
        load();
        StoreContext ctx = ctxWithLandingPage(routingRoot);

        JsonNode out = new DetectSpecificPageConflictsTool().call(ctx, mapper.createObjectNode());

        // "self-nested-scopes" binds BOTH "home" and "home/home-decor" via its own useForCategories -- an
        // ancestor/descendant pair, but on the SAME page, so it must not be reported as the page shadowing
        // itself, nor as a duplicate of itself.
        String selfPath = "/content/site/routing/self-nested-scopes";
        for (JsonNode entry : out.get("shadowing")) {
            boolean broaderIsSelf = selfPath.equals(entry.get("broader").asText());
            boolean narrowerIsSelf = selfPath.equals(entry.get("narrower").asText());
            assertTrue("self-nested-scopes must not appear as both broader and narrower of the same entry",
                !(broaderIsSelf && narrowerIsSelf));
        }
        for (JsonNode duplicate : out.get("duplicates")) {
            JsonNode pages = duplicate.get("pages");
            assertTrue("self-nested-scopes must not be duplicated against itself",
                !(containsText(pages, selfPath) && pages.get(0).asText().equals(pages.get(1).asText())));
        }
    }

    @Test
    public void detectsShadowingWhenNarrowerScopeIsNotDeeperThanBroaderScope() {
        load();
        StoreContext ctx = ctxWithLandingPage(routingRoot);

        JsonNode out = new DetectSpecificPageConflictsTool().call(ctx, mapper.createObjectNode());

        JsonNode shadowing = out.get("shadowing");
        assertTrue(shadowing.isArray());
        assertEquals(1, shadowing.size());

        JsonNode entry = shadowing.get(0);
        assertEquals("/content/site/routing/shadow-broad", entry.get("broader").asText());
        assertEquals("/content/site/routing/shadow-narrow", entry.get("narrower").asText());
        assertTrue(entry.get("reason").asText().length() > 0);
    }

    @Test
    public void wellNestedDeeperNarrowerScopeIsNotFlaggedAsShadowing() {
        load();
        StoreContext ctx = ctxWithLandingPage(routingRoot);

        JsonNode out = new DetectSpecificPageConflictsTool().call(ctx, mapper.createObjectNode());

        JsonNode shadowing = out.get("shadowing");
        for (JsonNode entry : shadowing) {
            assertTrue("clean-parent/clean-child is well-nested (narrower strictly deeper) and must not be flagged",
                !"/content/site/routing/clean-parent".equals(entry.get("broader").asText()));
        }
    }

    @Test
    public void cleanDisjointWellNestedTreeReportsNoDuplicatesOrShadowing() {
        load();
        StoreContext ctx = ctxWithLandingPage(cleanRoot);

        JsonNode out = new DetectSpecificPageConflictsTool().call(ctx, mapper.createObjectNode());

        assertEquals("/content/site/clean-site", out.get("siteRoot").asText());
        assertEquals(0, out.get("duplicates").size());
        assertEquals(0, out.get("shadowing").size());
    }

    @Test
    public void resolvesExplicitSiteRootArgInsteadOfEndpointLandingPage() {
        load();
        StoreContext ctx = ctxWithLandingPage(cleanRoot);

        JsonNode out = new DetectSpecificPageConflictsTool().call(ctx,
            mapper.createObjectNode().put("siteRoot", "/content/site/routing"));

        assertEquals("/content/site/routing", out.get("siteRoot").asText());
        assertEquals(2, out.get("duplicates").size());
    }

    @Test
    public void failsClosedWhenSiteRootNotUnderContent() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new DetectSpecificPageConflictsTool().call(ctx,
                mapper.createObjectNode().put("siteRoot", "/etc/somewhere")));
    }

    @Test
    public void failsClosedWhenSiteRootDoesNotResolveToPage() {
        load();
        context.build().resource("/content/not-a-page").commit();
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new DetectSpecificPageConflictsTool().call(ctx,
                mapper.createObjectNode().put("siteRoot", "/content/not-a-page")));
    }

    @Test
    public void failsClosedWhenNoSiteRootArgAndNullLandingPage() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new DetectSpecificPageConflictsTool().call(ctx, mapper.createObjectNode()));
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
