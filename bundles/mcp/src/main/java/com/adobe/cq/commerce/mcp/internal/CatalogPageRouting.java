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
import java.util.List;

import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;

/**
 * Shared read-only logic for CIF multi-catalog-page diagnostics (catalog §7). Reuses the exported
 * {@link SiteStructure} API (in particular {@link SiteStructure#getCategoryPages()}) for the actual page-tree
 * traversal, and reimplements only the small generic-vs-{@code urlPath} first-match resolution algorithm that
 * {@code SpecificPageStrategy.getGenericPage(SiteStructure, CategoryUrlFormat.Params)} performs internally
 * (that class lives in a non-exported {@code …internal.services} package and cannot be imported here).
 * <p>
 * <b>Property location, verified against source:</b> {@link SiteStructure#getCategoryPages()} returns a
 * {@link SiteStructure.Entry} per catalog page whose {@link SiteStructure.Entry#getPage()} is the resolved
 * category/PLP page (the {@code cq:cifCategoryPage} reference, or the landing page itself for the generic
 * fallback entry) and whose {@link SiteStructure.Entry#getCatalogPage()} is the <em>owning catalog page</em> that
 * carries the {@code magentoRootCategoryId} / {@code magentoRootCategoryIdType} scope properties on its
 * {@code jcr:content} (see {@code SpecificPageStrategy.isSpecificCatalogPageFor}, which reads them off
 * {@code Entry.getCatalogPage()}, not {@code Entry.getPage()}). The generic fallback entry (the nav root /
 * landing page, always last per {@code SiteStructureImpl.getCatalogPages()}) has a {@code null} catalog page.
 */
public final class CatalogPageRouting {

    // Redeclared literals (SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER / _IDENTIFIER_TYPE): that class
    // lives in a non-exported …internal.services.site package and cannot be imported from bundles/mcp.
    static final String PN_MAGENTO_ROOT_CATEGORY_ID = "magentoRootCategoryId";
    static final String PN_MAGENTO_ROOT_CATEGORY_ID_TYPE = "magentoRootCategoryIdType";
    static final String ID_TYPE_URL_PATH = "urlPath";

    /**
     * One entry of {@link SiteStructure#getCategoryPages()}, flattened to the fields the catalog §7 tools need.
     */
    public static final class CatalogPageInfo {
        private final Page page;
        private final String rootCategoryId;
        private final String idType;
        private final boolean genericFallback;

        CatalogPageInfo(Page page, String rootCategoryId, String idType, boolean genericFallback) {
            this.page = page;
            this.rootCategoryId = rootCategoryId;
            this.idType = idType;
            this.genericFallback = genericFallback;
        }

        public Page getPage() {
            return page;
        }

        public String getPath() {
            return page.getPath();
        }

        public String getRootCategoryId() {
            return rootCategoryId;
        }

        public String getIdType() {
            return idType;
        }

        public boolean isGenericFallback() {
            return genericFallback;
        }
    }

    /**
     * One step of the {@link #resolveFor(SiteStructure, String)} evaluation trace: which candidate was
     * considered, whether it matched, and why.
     */
    public static final class CandidateEvaluation {
        private final String path;
        private final boolean matched;
        private final String why;

        CandidateEvaluation(String path, boolean matched, String why) {
            this.path = path;
            this.matched = matched;
            this.why = why;
        }

        public String getPath() {
            return path;
        }

        public boolean isMatched() {
            return matched;
        }

        public String getWhy() {
            return why;
        }
    }

    /**
     * The result of {@link #resolveFor(SiteStructure, String)}: the winning page, why it won
     * ({@code "generic-fallback"} or {@code "urlPath-scope-match"}), and the ordered evaluation trace.
     */
    public static final class CatalogPageResolution {
        private final Page winningPage;
        private final String reason;
        private final List<CandidateEvaluation> trace;

        CatalogPageResolution(Page winningPage, String reason, List<CandidateEvaluation> trace) {
            this.winningPage = winningPage;
            this.reason = reason;
            this.trace = trace;
        }

        public Page getWinningPage() {
            return winningPage;
        }

        public String getReason() {
            return reason;
        }

        public List<CandidateEvaluation> getTrace() {
            return trace;
        }
    }

    /**
     * Lists every catalog page of {@code site}, in {@link SiteStructure#getCategoryPages()} order (the nav-root /
     * landing page is always last, per {@code SiteStructureImpl}, and is reported with {@code genericFallback=true}).
     * An entry is generic when its scope id-type is not {@code "urlPath"} or its root category id is blank.
     *
     * @param site the site structure to read catalog pages from
     * @return the ordered list of catalog page info
     */
    public List<CatalogPageInfo> listCatalogPages(SiteStructure site) {
        List<CatalogPageInfo> result = new ArrayList<>();
        for (SiteStructure.Entry entry : site.getCategoryPages()) {
            result.add(toCatalogPageInfo(entry));
        }
        return result;
    }

    /**
     * Replays {@code SpecificPageStrategy.getGenericPage(SiteStructure, CategoryUrlFormat.Params)}: iterates
     * {@link SiteStructure#getCategoryPages()} in order and returns the first entry that is either generic, or
     * whose {@code urlPath} scope contains {@code categoryUrlPath} (equal to the scope, or a descendant of it).
     * First match wins; there is no explicit mapping table.
     *
     * @param site the site structure to resolve against
     * @param categoryUrlPath the category's {@code url_path} to resolve a winning catalog page for
     * @return the winning page plus the ordered evaluation trace
     */
    public CatalogPageResolution resolveFor(SiteStructure site, String categoryUrlPath) {
        List<CandidateEvaluation> trace = new ArrayList<>();
        List<CatalogPageInfo> candidates = listCatalogPages(site);

        Page winningPage = null;
        String reason = "no-match";
        boolean winnerFound = false;

        for (CatalogPageInfo candidate : candidates) {
            if (winnerFound) {
                trace.add(new CandidateEvaluation(candidate.getPath(), false, "not evaluated: an earlier candidate already won"));
                continue;
            }

            if (candidate.isGenericFallback()) {
                trace.add(new CandidateEvaluation(candidate.getPath(), true, "generic-fallback"));
                winningPage = candidate.getPage();
                reason = "generic-fallback";
                winnerFound = true;
                continue;
            }

            String scope = candidate.getRootCategoryId();
            boolean matches = categoryUrlPath.equals(scope) || categoryUrlPath.startsWith(scope + "/");
            if (matches) {
                trace.add(new CandidateEvaluation(candidate.getPath(), true, "urlPath-scope-match"));
                winningPage = candidate.getPage();
                reason = "urlPath-scope-match";
                winnerFound = true;
            } else {
                trace.add(new CandidateEvaluation(candidate.getPath(), false, "urlPath scope \"" + scope + "\" does not contain \""
                    + categoryUrlPath + "\""));
            }
        }

        return new CatalogPageResolution(winningPage, reason, trace);
    }

    private CatalogPageInfo toCatalogPageInfo(SiteStructure.Entry entry) {
        Page catalogPage = entry.getCatalogPage();
        String rootCategoryId = null;
        String idType = null;
        if (catalogPage != null) {
            ValueMap properties = catalogPage.getProperties();
            rootCategoryId = properties.get(PN_MAGENTO_ROOT_CATEGORY_ID, String.class);
            idType = properties.get(PN_MAGENTO_ROOT_CATEGORY_ID_TYPE, String.class);
        }
        boolean genericFallback = rootCategoryId == null || rootCategoryId.isEmpty() || !ID_TYPE_URL_PATH.equals(idType);
        return new CatalogPageInfo(entry.getPage(), rootCategoryId, idType, genericFallback);
    }
}
