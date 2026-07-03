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
package com.adobe.cq.commerce.mcp.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.ValueMap;

import com.day.cq.wcm.api.Page;

/**
 * Shared read-only logic for CIF specific-PDP/PLP binding diagnostics (catalog §8.1). REIMPLEMENTS the
 * depth-first traversal and matching algorithm of {@code SpecificPageStrategy}
 * ({@code com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy}, `bundles/core`) because
 * that class and its binding-property constants live in a non-exported {@code …internal.services} package and
 * cannot be imported from {@code bundles/mcp}. The binding constants below are redeclared literals, verified
 * against {@code SpecificPageStrategy.java:49-56}.
 * <p>
 * <b>Resolution algorithm (verified against {@code SpecificPageStrategy.traverse}/{@code getSpecificPage} and
 * {@code SpecificPageStrategyTest#testNestedSpecificProductPage}/{@code #testNestedSpecificCategory}):</b> the
 * oracle's {@code traverse(Page)} recursively emits {@code traverse(child)} for every child stream-concatenated
 * <em>before</em> {@code Stream.of(child)} itself, so a full depth-first walk lists a page's descendants (in
 * document order, each descendant's own descendants first) before the page. {@code findFirst()} over that stream
 * therefore returns the <em>deepest</em> matching page in the tree — by tree depth, not by which candidate's
 * filter is more "specific". {@link #resolveSpecificPage(Page, String, String)} mirrors this exactly: it builds a
 * candidate list in the same descendants-before-ancestors order and returns the first match in that order.
 * <p>
 * <b>pageType is a property of the search root, not independently inferred per candidate page.</b> In the oracle,
 * {@code getSpecificPage(Page, ProductUrlFormat.Params)} and {@code getSpecificPage(Page, CategoryUrlFormat.Params)}
 * are separate overloads, and {@code UrlProviderImpl.getSpecificPageAndFormat} always resolves the root page from
 * exactly one of {@code SiteStructure.getProductPages()}/{@code getCategoryPages()}, paired with exactly one
 * params type — a single traversal never mixes product-type and category-type candidates. The product/category
 * structure page resourceType ({@code core/cif/components/structure/page/vN/page}) is identical for both roles
 * (verified against {@code PageImpl} v1/v2/v3 in `bundles/core`) so it cannot be used to tell them apart; the real
 * signal in `core` is whether a page is a descendant of the site's {@code cq:cifProductPage} vs.
 * {@code cq:cifCategoryPage} reference ({@code SiteStructureImpl#isProductPage}/{@code #isCategoryPage}), which
 * requires a resolved {@link com.adobe.cq.commerce.core.components.models.common.SiteStructure} that this
 * search-root-scoped helper deliberately does not depend on (that dependency belongs to the calling tool /
 * {@link CatalogPageRouting}, per the plan's reuse-vs-reimplement boundary). Callers therefore pass the type
 * explicitly to {@link #resolveSpecificPage(Page, String, String)}. {@link #readBinding(Page)}, which reads a
 * single arbitrary page with no root/type context, derives {@code pageType} from which binding fields are
 * present: {@code useForCategories} is a product-page-only field (catalog §8.1's binding table), so its presence
 * unambiguously means {@code "product"}; a bare {@code selectorFilter} is ambiguous in isolation (both product
 * and category pages use it) and defaults to {@code "product"}, the simpler/more common shape, unless
 * {@code selectorFilterType} or {@code includesSubCategories} is also present — both of which are the standard
 * category-page dialog fields (see the v2 dialog {@code categoryFilter}/{@code includesSubCategories} render
 * conditions gated on {@code pageType="category"}) — in which case it is reported as {@code "category"}.
 */
public final class SpecificPageRouting {

    // Redeclared literals (SpecificPageStrategy.SELECTOR_FILTER_PROPERTY et al.): that class lives in a
    // non-exported …internal.services package and cannot be imported from bundles/mcp.
    static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";
    static final String SELECTOR_FILTER_TYPE_PROPERTY = "selectorFilterType";
    static final String SELECTOR_FILTER_TYPE_DEFAULT = "uidAndUrlPath";
    static final String INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories";
    static final String PN_USE_FOR_CATEGORIES = "useForCategories";
    static final String UID_AND_URL_PATH_SEPARATOR = "|";

    static final String PAGE_TYPE_PRODUCT = "product";
    static final String PAGE_TYPE_CATEGORY = "category";

    /**
     * The raw specific-page binding of one page, as read off its {@code jcr:content} by {@link #readBinding(Page)}.
     */
    public static final class Binding {
        private final String pageType;
        private final String[] selectorFilter;
        private final String selectorFilterType;
        private final boolean includesSubCategories;
        private final String[] useForCategories;

        public Binding(String pageType, String[] selectorFilter, String selectorFilterType, boolean includesSubCategories,
                       String[] useForCategories) {
            this.pageType = pageType;
            this.selectorFilter = selectorFilter;
            this.selectorFilterType = selectorFilterType;
            this.includesSubCategories = includesSubCategories;
            this.useForCategories = useForCategories;
        }

        public String getPageType() {
            return pageType;
        }

        public String[] getSelectorFilter() {
            return selectorFilter;
        }

        public String getSelectorFilterType() {
            return selectorFilterType;
        }

        public boolean isIncludesSubCategories() {
            return includesSubCategories;
        }

        public String[] getUseForCategories() {
            return useForCategories;
        }
    }

    /**
     * One {@code selectorFilter} entry, parsed by {@link #parseSelectorFilter(Binding)}.
     */
    public static final class ParsedFilter {
        private final String raw;
        private final boolean valid;
        private final String uid;
        private final String urlPath;
        private final String issue;

        ParsedFilter(String raw, boolean valid, String uid, String urlPath, String issue) {
            this.raw = raw;
            this.valid = valid;
            this.uid = uid;
            this.urlPath = urlPath;
            this.issue = issue;
        }

        public String getRaw() {
            return raw;
        }

        public boolean isValid() {
            return valid;
        }

        public String getUid() {
            return uid;
        }

        public String getUrlPath() {
            return urlPath;
        }

        public String getIssue() {
            return issue;
        }
    }

    /**
     * One step of {@link Resolution#getTrace()}: a candidate specific page, its tree depth relative to the
     * search root ({@code searchRoot} itself = depth 0, its direct children = depth 1, and so on), whether it
     * matched, and why / why not.
     */
    public static final class Candidate {
        private final String path;
        private final int depth;
        private final boolean matched;
        private final String why;

        Candidate(String path, int depth, boolean matched, String why) {
            this.path = path;
            this.depth = depth;
            this.matched = matched;
            this.why = why;
        }

        public String getPath() {
            return path;
        }

        public int getDepth() {
            return depth;
        }

        public boolean isMatched() {
            return matched;
        }

        public String getWhy() {
            return why;
        }
    }

    /**
     * The result of {@link #resolveSpecificPage(Page, String, String)}: the winning page (or {@code null} if
     * none matched), its tree depth relative to the search root (see {@link Candidate}), and the ordered
     * candidate trace (descendants before ancestors, matching the oracle's traversal order).
     */
    public static final class Resolution {
        private final Page winningPage;
        private final int depth;
        private final List<Candidate> trace;

        Resolution(Page winningPage, int depth, List<Candidate> trace) {
            this.winningPage = winningPage;
            this.depth = depth;
            this.trace = trace;
        }

        public Page getWinningPage() {
            return winningPage;
        }

        public int getDepth() {
            return depth;
        }

        public List<Candidate> getTrace() {
            return trace;
        }
    }

    /**
     * Lists every descendant of {@code searchRoot} that carries a present {@code selectorFilter} or
     * {@code useForCategories} binding (the oracle's {@code isSpecificPage} candidate check), in depth-first
     * order with descendants preceding ancestors — i.e. the same order {@code findFirst()} would consume in
     * {@link #resolveSpecificPage(Page, String, String)}. {@code searchRoot} itself is never included, matching
     * {@code SpecificPageStrategy.traverse}, which only visits {@code page.listChildren()} and below.
     * <p>
     * Matches the oracle's {@code isSpecificPage} exactly: a candidate qualifies when the property is
     * <em>present</em> (non-{@code null}), regardless of array length — a page whose {@code selectorFilter} (or
     * {@code useForCategories}) was cleared to {@code []} is still listed here, even though such a candidate can
     * never {@link #resolveSpecificPage(Page, String, String) match} (it has no slugs/paths to compare against).
     *
     * @param searchRoot the page to search descendants of (typically a product-page or category-page root)
     * @return the ordered list of candidate specific pages
     */
    public List<Page> specificPages(Page searchRoot) {
        List<Page> result = new ArrayList<>();
        collectDepthFirst(searchRoot, result);
        return result;
    }

    private void collectDepthFirst(Page page, List<Page> out) {
        if (page == null) {
            return;
        }
        Iterator<Page> children = page.listChildren();
        while (children.hasNext()) {
            Page child = children.next();
            collectDepthFirst(child, out);
            if (isSpecificPage(child)) {
                out.add(child);
            }
        }
    }

    private boolean isSpecificPage(Page candidate) {
        // Oracle-faithful (SpecificPageStrategy.isSpecificPage, bundles/core, line ~124-129): a candidate
        // qualifies when the property is present (non-null), regardless of array length. A present-but-empty
        // ([]) selectorFilter/useForCategories still counts as a candidate here, even though it can never match
        // in resolveSpecificPage (an empty array has no slugs/paths to compare against).
        ValueMap properties = candidate.getProperties();
        String[] selectorFilter = properties.get(SELECTOR_FILTER_PROPERTY, String[].class);
        String[] useForCategories = properties.get(PN_USE_FOR_CATEGORIES, String[].class);
        return selectorFilter != null || useForCategories != null;
    }

    /**
     * Reads the raw specific-page binding off {@code page}'s {@code jcr:content}. {@code selectorFilterType}
     * defaults to {@code "uidAndUrlPath"} when absent, matching
     * {@code SpecificPageStrategy.isSpecificPageFor(Page, CategoryUrlFormat.Params)}'s
     * {@code properties.get(SELECTOR_FILTER_TYPE_PROPERTY, "uidAndUrlPath")} read. See the class javadoc for how
     * {@code pageType} is derived (this method has no access to the site's product/category root pages, only to
     * {@code page} itself).
     *
     * @param page the page to read the binding of
     * @return the page's binding (all-empty/false if unbound)
     */
    public Binding readBinding(Page page) {
        ValueMap properties = page.getProperties();
        String[] selectorFilter = properties.get(SELECTOR_FILTER_PROPERTY, new String[0]);
        String selectorFilterType = properties.get(SELECTOR_FILTER_TYPE_PROPERTY, SELECTOR_FILTER_TYPE_DEFAULT);
        boolean includesSubCategories = properties.get(INCLUDES_SUBCATEGORIES_PROPERTY, false);
        String[] useForCategories = properties.get(PN_USE_FOR_CATEGORIES, new String[0]);

        String pageType;
        if (useForCategories.length > 0) {
            pageType = PAGE_TYPE_PRODUCT;
        } else if (properties.get(SELECTOR_FILTER_TYPE_PROPERTY, String.class) != null || includesSubCategories) {
            pageType = PAGE_TYPE_CATEGORY;
        } else {
            pageType = PAGE_TYPE_PRODUCT;
        }

        return new Binding(pageType, selectorFilter, selectorFilterType, includesSubCategories, useForCategories);
    }

    /**
     * Parses {@code binding}'s {@code selectorFilter} entries. When {@code selectorFilterType} is
     * {@code "uidAndUrlPath"} (the default), each entry is split on {@code "|"} into {@code uid|urlPath}; an
     * entry with no pipe cannot be unambiguously split and is flagged {@code valid=false} with
     * {@code issue="missing '|' separator"} (the oracle itself falls back to treating such a malformed entry as
     * <em>both</em> uid and url-path — see {@code SpecificPageStrategy.isSpecificPageFor(Page,
     * CategoryUrlFormat.Params)}'s "weird data, should not happen but is used in tests" branch — which is exactly
     * the ambiguity this diagnostic surfaces instead of silently tolerating). For any other
     * {@code selectorFilterType} (e.g. {@code "urlPath"}, used by the v2/v3 category dialog), every entry is
     * treated as a plain url-path.
     *
     * @param binding the binding to parse {@code selectorFilter} from
     * @return one {@link ParsedFilter} per {@code selectorFilter} entry, in order
     */
    public List<ParsedFilter> parseSelectorFilter(Binding binding) {
        List<ParsedFilter> result = new ArrayList<>();
        boolean uidAndUrlPath = SELECTOR_FILTER_TYPE_DEFAULT.equals(binding.getSelectorFilterType());

        for (String raw : binding.getSelectorFilter()) {
            if (!uidAndUrlPath) {
                result.add(new ParsedFilter(raw, true, null, raw, null));
                continue;
            }

            int separatorIndex = raw.indexOf(UID_AND_URL_PATH_SEPARATOR);
            if (separatorIndex < 0) {
                result.add(new ParsedFilter(raw, false, null, null, "missing '|' separator"));
                continue;
            }

            String uid = raw.substring(0, separatorIndex);
            String urlPath = raw.substring(separatorIndex + 1);
            result.add(new ParsedFilter(raw, true, uid, urlPath, null));
        }

        return result;
    }

    /**
     * Mirrors {@code SpecificPageStrategy.getSpecificPage}: depth-first traversal of {@code searchRoot}'s
     * descendants (descendants before ancestors, matching {@link #specificPages(Page)}'s order), returning the
     * <em>first</em> candidate in that order whose binding matches {@code urlPath} for the given {@code type} —
     * i.e. the <em>deepest</em> matching page in the tree, by tree depth, not by which candidate's filter is more
     * specific (proven by the oracle's {@code testNestedSpecificProductPage}/{@code testNestedSpecificCategory}).
     * <p>
     * Matching: a {@code "product"} candidate matches when {@code urlPath} equals one of its
     * {@code selectorFilter} slugs, or falls within one of its {@code useForCategories} scopes (subtree predicate
     * below). A {@code "category"} candidate matches when {@code urlPath} falls within one of its parsed
     * {@code selectorFilter} url-path scopes (subtree predicate below); a malformed filter entry (no pipe) is
     * still checked as a plain url-path, matching the oracle's ambiguous fallback. Subtree predicate (verified
     * against {@code SpecificPageStrategy.matchesUrlPath}):
     * {@code categoryUrlPath.equals(given) || (includesSubCategories && given.startsWith(categoryUrlPath + "/"))}.
     *
     * @param searchRoot the page to search descendants of
     * @param urlPath the category url_path or product slug/url_key to resolve a winning specific page for
     * @param type {@code "product"} or {@code "category"} — which kind of {@code urlPath} this is
     * @return the winning page (or {@code null}), its depth, and the ordered candidate trace
     */
    public Resolution resolveSpecificPage(Page searchRoot, String urlPath, String type) {
        List<Page> candidates = specificPages(searchRoot);
        int rootDepth = depthOf(searchRoot);

        List<Candidate> trace = new ArrayList<>();
        Page winningPage = null;
        int winningDepth = -1;
        boolean winnerFound = false;

        for (Page candidate : candidates) {
            int depth = depthOf(candidate) - rootDepth;

            if (winnerFound) {
                trace.add(new Candidate(candidate.getPath(), depth, false, "not evaluated: a deeper candidate already won"));
                continue;
            }

            boolean matches = matches(candidate, urlPath, type);
            if (matches) {
                trace.add(new Candidate(candidate.getPath(), depth, true, "matched"));
                winningPage = candidate;
                winningDepth = depth;
                winnerFound = true;
            } else {
                trace.add(new Candidate(candidate.getPath(), depth, false, "binding does not match \"" + urlPath + "\""));
            }
        }

        return new Resolution(winningPage, winningDepth, trace);
    }

    private int depthOf(Page page) {
        return page.getPath().split("/").length;
    }

    private boolean matches(Page candidate, String urlPath, String type) {
        Binding binding = readBinding(candidate);

        if (PAGE_TYPE_PRODUCT.equals(type)) {
            // Note: a product page's useForCategories scope is intentionally NOT consulted here. It is only
            // ever evaluated under the PAGE_TYPE_CATEGORY branch below (see productPageMatchesUseForCategoriesScope
            // in the test), because this helper flattens the oracle's two separate (urlPath, type) params into a
            // single call, and useForCategories always represents a *category* url_path scope regardless of
            // which page carries the binding. Do not expect it to fire when type="product".
            for (String slug : binding.getSelectorFilter()) {
                if (slug.equals(urlPath)) {
                    return true;
                }
            }
            return false;
        }

        if (PAGE_TYPE_CATEGORY.equals(type)) {
            // a product page's useForCategories scopes a whole category tree to this custom PDP
            for (String categoryUrlPath : binding.getUseForCategories()) {
                if (matchesUrlPath(urlPath, categoryUrlPath, binding.isIncludesSubCategories())) {
                    return true;
                }
            }

            for (ParsedFilter filter : parseSelectorFilter(binding)) {
                // a well-formed entry contributes its parsed urlPath; a malformed (pipe-less) entry has no parsed
                // urlPath (ParsedFilter.getUrlPath() == null) but the oracle's ambiguous fallback still tries the
                // raw string itself as a url-path candidate (SpecificPageStrategy.isSpecificPageFor: "consider
                // the filter to be both" -- it adds the raw filter to categoryUrlPaths verbatim), so replicate
                // that here instead of silently dropping the malformed entry from matching.
                String categoryUrlPath = filter.isValid() ? filter.getUrlPath() : filter.getRaw();
                if (matchesUrlPath(urlPath, categoryUrlPath, binding.isIncludesSubCategories())) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    private static boolean matchesUrlPath(String givenUrlPath, String categoryUrlPath, boolean includesSubCategories) {
        return categoryUrlPath.equals(givenUrlPath)
            || (includesSubCategories && givenUrlPath.startsWith(categoryUrlPath + "/"));
    }
}
