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

public class ListSpecificPagesToolTest {

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
    public void listsProductPageBindingsFromEndpointLandingPage() {
        load();
        StoreContext ctx = ctxWithLandingPage(productPageRoot);

        JsonNode out = new ListSpecificPagesTool().call(ctx, mapper.createObjectNode());

        assertEquals("/content/site/product-page", out.get("siteRoot").asText());

        JsonNode pages = out.get("specificPages");
        assertTrue(pages.isArray());

        // real fixture values traced through, helper order: descendants before ancestors
        JsonNode nested = pages.get(0);
        assertEquals("/content/site/product-page/sub-page/nested-page", nested.get("path").asText());
        assertEquals("product", nested.get("pageType").asText());
        assertEquals(1, nested.get("selectorFilter").size());
        assertEquals("productId1.1", nested.get("selectorFilter").get(0).asText());

        JsonNode subPage = pages.get(1);
        assertEquals("/content/site/product-page/sub-page", subPage.get("path").asText());
        assertEquals("product", subPage.get("pageType").asText());
        assertEquals(1, subPage.get("selectorFilter").size());
        assertEquals("productId1", subPage.get("selectorFilter").get(0).asText());

        JsonNode subPage2 = pages.get(2);
        assertEquals("/content/site/product-page/sub-page-2", subPage2.get("path").asText());
        assertEquals("productId2", subPage2.get("selectorFilter").get(0).asText());

        JsonNode categoryTreePage = pages.get(3);
        assertEquals("/content/site/product-page/category-tree-page", categoryTreePage.get("path").asText());
        assertEquals("product", categoryTreePage.get("pageType").asText());
        assertEquals(2, categoryTreePage.get("useForCategories").size());
        assertEquals("women", categoryTreePage.get("useForCategories").get(0).asText());
        assertEquals("men/men-tops", categoryTreePage.get("useForCategories").get(1).asText());
        assertTrue(categoryTreePage.get("includesSubCategories").asBoolean());
        // no selectorFilter on this page -- must be omitted, not an empty array
        assertFalse(categoryTreePage.has("selectorFilter"));

        JsonNode clearedFilterPage = pages.get(4);
        assertEquals("/content/site/product-page/cleared-filter-page", clearedFilterPage.get("path").asText());
        // present-but-empty selectorFilter: page is still listed, but the empty field is omitted (compact)
        assertFalse(clearedFilterPage.has("selectorFilter"));
        assertFalse(clearedFilterPage.has("useForCategories"));

        assertEquals(5, pages.size());
    }

    @Test
    public void listsCategoryPageBindingsIncludingSelectorFilterTypeAndIncludesSubCategories() {
        load();
        StoreContext ctx = ctxWithLandingPage(categoryPageRoot);

        JsonNode out = new ListSpecificPagesTool().call(ctx, mapper.createObjectNode());

        JsonNode pages = out.get("specificPages");

        JsonNode nested = pages.get(0);
        assertEquals("/content/site/category-page/sub-page/nested-page", nested.get("path").asText());
        assertEquals("category", nested.get("pageType").asText());
        assertEquals("category-uid-2|men/tops/sweaters", nested.get("selectorFilter").get(0).asText());
        assertEquals("uidAndUrlPath", nested.get("selectorFilterType").asText());
        assertTrue(nested.get("includesSubCategories").asBoolean());
        assertFalse(nested.has("useForCategories"));

        JsonNode subPage = pages.get(1);
        assertEquals("/content/site/category-page/sub-page", subPage.get("path").asText());
        assertEquals("category-uid-1|men/tops", subPage.get("selectorFilter").get(0).asText());

        JsonNode malformedPage = pages.get(2);
        assertEquals("/content/site/category-page/malformed-page", malformedPage.get("path").asText());
        assertEquals(2, malformedPage.get("selectorFilter").size());
        assertEquals("category-uid-3|women/women-tops", malformedPage.get("selectorFilter").get(0).asText());
        assertEquals("no-pipe-here", malformedPage.get("selectorFilter").get(1).asText());

        assertEquals(3, pages.size());
    }

    @Test
    public void resolvesExplicitSiteRootArgInsteadOfEndpointLandingPage() {
        load();
        // Endpoint's own landing page is unrelated; the explicit siteRoot must be used instead.
        StoreContext ctx = ctxWithLandingPage(categoryPageRoot);

        JsonNode out = new ListSpecificPagesTool().call(ctx,
            mapper.createObjectNode().put("siteRoot", "/content/site/product-page"));

        assertEquals("/content/site/product-page", out.get("siteRoot").asText());
        JsonNode pages = out.get("specificPages");
        assertEquals(5, pages.size());
    }

    @Test
    public void failsClosedWhenSiteRootNotUnderContent() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ListSpecificPagesTool().call(ctx,
                mapper.createObjectNode().put("siteRoot", "/etc/somewhere")));
    }

    @Test
    public void failsClosedWhenSiteRootDoesNotResolveToPage() {
        load();
        context.build().resource("/content/not-a-page").commit();
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ListSpecificPagesTool().call(ctx,
                mapper.createObjectNode().put("siteRoot", "/content/not-a-page")));
    }

    @Test
    public void failsClosedWhenNoSiteRootArgAndNullLandingPage() {
        StoreContext ctx = ctxWithLandingPage(null);

        assertThrows(IllegalArgumentException.class,
            () -> new ListSpecificPagesTool().call(ctx, mapper.createObjectNode()));
    }
}
