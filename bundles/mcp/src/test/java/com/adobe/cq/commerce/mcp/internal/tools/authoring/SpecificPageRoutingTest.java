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

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SpecificPageRoutingTest {

    @Rule
    public final AemContext context = new AemContext();

    private final SpecificPageRouting subject = new SpecificPageRouting();

    private Page productPageRoot;
    private Page categoryPageRoot;

    private void load() {
        context.load().json("/context/specific-pages.json", "/content");
        productPageRoot = context.pageManager().getPage("/content/site/product-page");
        categoryPageRoot = context.pageManager().getPage("/content/site/category-page");
    }

    @Test
    public void deeperNestedProductPageWinsOverShallowerMatch() {
        load();

        // "sub-page" itself only matches "productId1"; "productId1.1" only matches the nested grandchild.
        // If the traversal were shallow-first or filter-specificity-based, the shallower "sub-page" could
        // wrongly report no match, or a broken implementation could stop too early. Prove the deepest
        // candidate wins per the oracle's testNestedSpecificProductPage.
        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(productPageRoot, "productId1.1", "product");

        assertNotNull(resolution.getWinningPage());
        assertEquals("/content/site/product-page/sub-page/nested-page", resolution.getWinningPage().getPath());
        assertEquals(2, resolution.getDepth());
    }

    @Test
    public void shallowerProductPageMatchesItsOwnFilter() {
        load();

        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(productPageRoot, "productId1", "product");

        assertNotNull(resolution.getWinningPage());
        assertEquals("/content/site/product-page/sub-page", resolution.getWinningPage().getPath());
        assertEquals(1, resolution.getDepth());
    }

    @Test
    public void deeperNestedCategoryPageWinsOverShallowerMatchWithinIncludedSubtree() {
        load();

        // "sub-page" scopes "men/tops" (+ sub-categories), so "men/tops/sweaters" matches BOTH "sub-page" (via
        // includesSubCategories) and the deeper "nested-page" (exact match on its own narrower scope). Deepest
        // wins regardless of which candidate's match is more "specific" -- mirrors testNestedSpecificCategory.
        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(categoryPageRoot, "men/tops/sweaters", "category");

        assertNotNull(resolution.getWinningPage());
        assertEquals("/content/site/category-page/sub-page/nested-page", resolution.getWinningPage().getPath());
        assertEquals(2, resolution.getDepth());
    }

    @Test
    public void includesSubCategoriesMatchesDescendantUrlPath() {
        load();

        // "men/tops/jackets" is not any candidate's exact scope, but falls under "sub-page"'s "men/tops" scope
        // because includesSubCategories=true there (and its nested-page's narrower "men/tops/sweaters" scope
        // does not contain it), so "sub-page" (depth 1) must win, not the nested page.
        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(categoryPageRoot, "men/tops/jackets", "category");

        assertNotNull(resolution.getWinningPage());
        assertEquals("/content/site/category-page/sub-page", resolution.getWinningPage().getPath());
        assertEquals(1, resolution.getDepth());
    }

    @Test
    public void resolutionTraceListsCandidatesDescendantsBeforeAncestorsWithMatchFlags() {
        load();

        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(categoryPageRoot, "men/tops/sweaters", "category");

        List<SpecificPageRouting.Candidate> trace = resolution.getTrace();
        // depth-first, descendants before ancestors: nested-page (depth 2) precedes sub-page (depth 1), which
        // precedes malformed-page (depth 1, a sibling of sub-page, visited after sub-page's whole subtree).
        assertEquals("/content/site/category-page/sub-page/nested-page", trace.get(0).getPath());
        assertTrue(trace.get(0).isMatched());
        assertEquals(2, trace.get(0).getDepth());

        assertEquals("/content/site/category-page/sub-page", trace.get(1).getPath());
        assertEquals(1, trace.get(1).getDepth());

        // findFirst semantics: once the deepest match (nested-page) is found, later (shallower) candidates --
        // even ones that would otherwise match, like "sub-page" here (its own "men/tops" scope also contains
        // "men/tops/sweaters") -- are still reported in the trace but flagged as NOT matched / not evaluated,
        // because an earlier (deeper) candidate already won. This is what "deepest wins" means operationally.
        assertFalse(trace.get(1).isMatched());
        assertFalse(trace.get(1).getWhy().isEmpty());
    }

    @Test
    public void noMatchReturnsNullWinnerWithFullTrace() {
        load();

        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(productPageRoot, "unknown-sku", "product");

        assertNull(resolution.getWinningPage());
        assertTrue(resolution.getTrace().stream().noneMatch(SpecificPageRouting.Candidate::isMatched));
    }

    @Test
    public void malformedFilterStillMatchesAsRawUrlPathAmbiguousFallback() {
        load();

        // "malformed-page" has one well-formed entry ("category-uid-3|women/women-tops") and one pipe-less entry
        // ("no-pipe-here"). parseSelectorFilter() flags the pipe-less entry invalid for diagnostics, but
        // resolveSpecificPage() must still mirror the oracle's ambiguous fallback (SpecificPageStrategy treats a
        // pipe-less filter as both uid and url-path verbatim), so it still participates in url-path matching.
        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(categoryPageRoot, "no-pipe-here", "category");

        assertNotNull(resolution.getWinningPage());
        assertEquals("/content/site/category-page/malformed-page", resolution.getWinningPage().getPath());
    }

    @Test
    public void productPageMatchesUseForCategoriesScope() {
        load();

        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(productPageRoot, "men/men-tops/men-sweaters", "category");

        assertNotNull(resolution.getWinningPage());
        assertEquals("/content/site/product-page/category-tree-page", resolution.getWinningPage().getPath());
    }

    @Test
    public void specificPagesFindsAllCandidatesDepthFirstDescendantsBeforeAncestors() {
        load();

        List<Page> pages = subject.specificPages(productPageRoot);
        List<String> paths = new java.util.ArrayList<>();
        for (Page page : pages) {
            paths.add(page.getPath());
        }

        assertTrue(paths.contains("/content/site/product-page/sub-page"));
        assertTrue(paths.contains("/content/site/product-page/sub-page/nested-page"));
        assertTrue(paths.contains("/content/site/product-page/sub-page-2"));
        assertTrue(paths.contains("/content/site/product-page/category-tree-page"));
        // the plain root and any non-bound page are excluded (isSpecificPage requires selectorFilter or
        // useForCategories to be present, i.e. non-null; a page with neither property at all is not a candidate)
        assertFalse(paths.contains("/content/site/product-page"));

        // descendants precede ancestors: nested-page comes before its parent sub-page
        assertTrue(paths.indexOf("/content/site/product-page/sub-page/nested-page") < paths.indexOf(
            "/content/site/product-page/sub-page"));
    }

    @Test
    public void specificPagesListsCandidateWithPresentButEmptySelectorFilter() {
        load();

        // Oracle-faithful (SpecificPageStrategy.isSpecificPage): a page whose selectorFilter was cleared to []
        // is still a candidate because the property is present (non-null), regardless of length. This matters
        // for the §8 listing/conflict tools (T-34/T-35), which must not silently under-report cleared bindings.
        List<Page> pages = subject.specificPages(productPageRoot);
        List<String> paths = new java.util.ArrayList<>();
        for (Page page : pages) {
            paths.add(page.getPath());
        }

        assertTrue(paths.contains("/content/site/product-page/cleared-filter-page"));
    }

    @Test
    public void presentButEmptySelectorFilterCandidateNeverWinsResolution() {
        load();

        // An empty-array candidate is listed (see specificPagesListsCandidateWithPresentButEmptySelectorFilter)
        // but has no slugs to compare against, so it can never MATCH -- resolution winners must be unchanged.
        // "cleared-filter-page" is a shallower sibling of "sub-page"/"sub-page-2"; the deepest genuine match
        // ("sub-page/nested-page" for "productId1.1") must still win.
        SpecificPageRouting.Resolution resolution = subject.resolveSpecificPage(productPageRoot, "productId1.1", "product");

        assertNotNull(resolution.getWinningPage());
        assertEquals("/content/site/product-page/sub-page/nested-page", resolution.getWinningPage().getPath());

        boolean clearedFilterPageMatched = resolution.getTrace().stream()
            .anyMatch(candidate -> "/content/site/product-page/cleared-filter-page".equals(candidate.getPath()) && candidate.isMatched());
        assertFalse(clearedFilterPageMatched);

        // Also prove it never wins even when nothing else matches: with an unknown urlPath, the winner stays
        // null instead of the empty-filter candidate spuriously "matching everything".
        SpecificPageRouting.Resolution noMatch = subject.resolveSpecificPage(productPageRoot, "unknown-sku", "product");
        assertNull(noMatch.getWinningPage());
    }

    @Test
    public void readBindingReadsProductPageSelectorFilter() {
        load();

        Page subPage = context.pageManager().getPage("/content/site/product-page/sub-page");
        SpecificPageRouting.Binding binding = subject.readBinding(subPage);

        assertEquals(1, binding.getSelectorFilter().length);
        assertEquals("productId1", binding.getSelectorFilter()[0]);
        assertEquals("uidAndUrlPath", binding.getSelectorFilterType());
        assertFalse(binding.isIncludesSubCategories());
        assertEquals(0, binding.getUseForCategories().length);
    }

    @Test
    public void readBindingReadsProductPageUseForCategories() {
        load();

        Page categoryTreePage = context.pageManager().getPage("/content/site/product-page/category-tree-page");
        SpecificPageRouting.Binding binding = subject.readBinding(categoryTreePage);

        assertEquals(2, binding.getUseForCategories().length);
        assertEquals("women", binding.getUseForCategories()[0]);
        assertEquals("men/men-tops", binding.getUseForCategories()[1]);
        assertTrue(binding.isIncludesSubCategories());
    }

    @Test
    public void readBindingReadsCategoryPageSelectorFilterTypeAndIncludesSubCategories() {
        load();

        Page subPage = context.pageManager().getPage("/content/site/category-page/sub-page");
        SpecificPageRouting.Binding binding = subject.readBinding(subPage);

        assertEquals(1, binding.getSelectorFilter().length);
        assertEquals("category-uid-1|men/tops", binding.getSelectorFilter()[0]);
        assertEquals("uidAndUrlPath", binding.getSelectorFilterType());
        assertTrue(binding.isIncludesSubCategories());
    }

    @Test
    public void parseSelectorFilterSplitsUidAndUrlPathOnPipe() {
        load();

        Page subPage = context.pageManager().getPage("/content/site/category-page/sub-page");
        SpecificPageRouting.Binding binding = subject.readBinding(subPage);

        List<SpecificPageRouting.ParsedFilter> parsed = subject.parseSelectorFilter(binding);

        assertEquals(1, parsed.size());
        SpecificPageRouting.ParsedFilter filter = parsed.get(0);
        assertTrue(filter.isValid());
        assertEquals("category-uid-1", filter.getUid());
        assertEquals("men/tops", filter.getUrlPath());
        assertEquals("category-uid-1|men/tops", filter.getRaw());
        assertNull(filter.getIssue());
    }

    @Test
    public void parseSelectorFilterFlagsMissingPipeAsInvalid() {
        load();

        Page malformedPage = context.pageManager().getPage("/content/site/category-page/malformed-page");
        SpecificPageRouting.Binding binding = subject.readBinding(malformedPage);

        List<SpecificPageRouting.ParsedFilter> parsed = subject.parseSelectorFilter(binding);

        assertEquals(2, parsed.size());

        SpecificPageRouting.ParsedFilter wellFormed = parsed.get(0);
        assertTrue(wellFormed.isValid());
        assertEquals("category-uid-3", wellFormed.getUid());
        assertEquals("women/women-tops", wellFormed.getUrlPath());

        SpecificPageRouting.ParsedFilter malformed = parsed.get(1);
        assertFalse(malformed.isValid());
        assertEquals("no-pipe-here", malformed.getRaw());
        assertEquals("missing '|' separator", malformed.getIssue());
    }

    @Test
    public void parseSelectorFilterTreatsEntriesAsPlainUrlPathsWhenTypeIsNotUidAndUrlPath() {
        SpecificPageRouting.Binding binding = new SpecificPageRouting.Binding(
            "category",
            new String[] { "women/women-tops" },
            "urlPath",
            true,
            new String[0]);

        List<SpecificPageRouting.ParsedFilter> parsed = subject.parseSelectorFilter(binding);

        assertEquals(1, parsed.size());
        SpecificPageRouting.ParsedFilter filter = parsed.get(0);
        assertTrue(filter.isValid());
        assertNull(filter.getUid());
        assertEquals("women/women-tops", filter.getUrlPath());
    }
}
