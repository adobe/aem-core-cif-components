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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.SpecificPageRouting;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP read tool flagging structural conflicts among a site's specific PDP/PLP page bindings (catalog §8): pages
 * that bind the exact same scope ({@code duplicates}), and narrower-scoped pages that are not deeper in the tree
 * than a broader (ancestor-scope) page ({@code shadowing}), which risks the broader page structurally shadowing
 * the narrower one under {@link SpecificPageRouting#resolveSpecificPage(Page, String, String) the deepest-wins
 * resolution algorithm} (catalog §8.2's "structural shadowing risk").
 * <p>
 * Only bindings with a comparable url-path <em>scope</em> participate: a category page's parsed {@code
 * selectorFilter} url-path (see {@link SpecificPageRouting#parseSelectorFilter(SpecificPageRouting.Binding)}), and
 * a product page's {@code useForCategories} entries (each of which scopes a whole category subtree to that PDP,
 * per §8.1's third binding row). A plain product-page {@code selectorFilter} binds individual SKUs/URL keys, not a
 * category subtree, so it has no ancestor/descendant relationship to compare and is excluded from both checks —
 * matching the brief's duplicate/shadowing definitions exactly.
 * <p>
 * This is a structural check only: it compares the specific pages' own binding properties and tree positions
 * against each other. It does <b>not</b> fetch the live category tree (no GraphQL round trip).
 */
@Component(service = McpTool.class)
public class DetectSpecificPageConflictsTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SpecificPageRouting specificPageRouting = new SpecificPageRouting();

    /**
     * One scoped specific-page binding: the page, its parsed url-path scope, and its tree depth relative to the
     * search root (matching {@link SpecificPageRouting}'s depth convention: the search root itself is depth 0).
     */
    private static final class ScopedPage {
        private final Page page;
        private final String scope;
        private final int depth;

        ScopedPage(Page page, String scope, int depth) {
            this.page = page;
            this.scope = scope;
            this.depth = depth;
        }
    }

    @Override
    public String name() {
        return "detect_specific_page_conflicts";
    }

    @Override
    public String description() {
        return "Flag structural conflicts among a site's specific PDP/PLP page bindings (catalog §8): pages that "
            + "bind the exact same scope (duplicates), and narrower-scoped pages that are not deeper in the tree "
            + "than a broader ancestor-scope page (shadowing risk under the deepest-wins resolution algorithm). "
            + "Structural comparison of the pages' own binding properties and tree positions only -- does not "
            + "fetch the live category tree. Optional siteRoot (any page under /content; defaults to the "
            + "endpoint's own nav root).";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("siteRoot").put("type", "string");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String siteRootArg = args.path("siteRoot").asText(null);

        Page searchRoot;
        if (StringUtils.isNotBlank(siteRootArg)) {
            if (!siteRootArg.startsWith("/content/") && !"/content".equals(siteRootArg)) {
                throw new IllegalArgumentException("siteRoot must be under /content: " + siteRootArg);
            }
            ResourceResolver resolver = ctx.getRequest().getResourceResolver();
            Resource resource = resolver.getResource(siteRootArg);
            if (resource == null) {
                throw new IllegalArgumentException("siteRoot not found: " + siteRootArg);
            }
            searchRoot = resource.adaptTo(Page.class);
            if (searchRoot == null) {
                throw new IllegalArgumentException("siteRoot does not resolve to a page: " + siteRootArg);
            }
        } else {
            searchRoot = ctx.getLandingPage();
        }

        if (searchRoot == null) {
            throw new IllegalArgumentException("could not resolve a site root page");
        }

        List<ScopedPage> scopedPages = collectScopedPages(searchRoot);

        ObjectNode out = mapper.createObjectNode();
        out.put("siteRoot", searchRoot.getPath());

        ArrayNode duplicates = out.putArray("duplicates");
        ArrayNode shadowing = out.putArray("shadowing");

        for (int i = 0; i < scopedPages.size(); i++) {
            ScopedPage a = scopedPages.get(i);
            for (int j = i + 1; j < scopedPages.size(); j++) {
                ScopedPage b = scopedPages.get(j);

                // A single page can carry multiple scope entries (e.g. two useForCategories categories, or two
                // selectorFilter entries). Comparing a page's own entries against each other is not a conflict
                // between two competing pages, so skip same-page pairs entirely.
                if (a.page.getPath().equals(b.page.getPath())) {
                    continue;
                }

                if (a.scope.equals(b.scope)) {
                    ObjectNode duplicate = duplicates.addObject();
                    duplicate.put("scope", a.scope);
                    ArrayNode pages = duplicate.putArray("pages");
                    pages.add(a.page.getPath());
                    pages.add(b.page.getPath());
                    continue;
                }

                addShadowingIfApplicable(shadowing, a, b);
                addShadowingIfApplicable(shadowing, b, a);
            }
        }

        return out;
    }

    /**
     * Emits a {@code shadowing} entry when {@code broader}'s scope is a strict ancestor url-path of
     * {@code narrower}'s scope, but the narrower page is not strictly deeper in the tree than the broader page --
     * the structural shadowing risk from catalog §8.2 ("narrower binding at ≤ depth of a broader one"). A
     * well-nested pair (narrower strictly deeper) is not flagged.
     */
    private void addShadowingIfApplicable(ArrayNode shadowing, ScopedPage broader, ScopedPage narrower) {
        if (!narrower.scope.startsWith(broader.scope + "/")) {
            return;
        }
        if (narrower.depth > broader.depth) {
            return;
        }

        ObjectNode entry = shadowing.addObject();
        entry.put("broader", broader.page.getPath());
        entry.put("narrower", narrower.page.getPath());
        entry.put("reason", "narrower scope \"" + narrower.scope + "\" is at tree depth " + narrower.depth
            + ", not deeper than broader scope \"" + broader.scope + "\" at depth " + broader.depth
            + " -- the broader page can structurally shadow the narrower one under deepest-wins resolution");
    }

    /**
     * Every descendant of {@code searchRoot} with a comparable url-path scope: a category page's parsed {@code
     * selectorFilter} entries, and a product page's {@code useForCategories} entries. A malformed (pipe-less)
     * category filter entry has no parsed url-path ({@code ParsedFilter.getUrlPath() == null}); mirroring {@link
     * SpecificPageRouting}'s own ambiguous fallback (used in its matching logic), its raw string is used as the
     * scope instead of dropping the entry from conflict detection.
     */
    private List<ScopedPage> collectScopedPages(Page searchRoot) {
        List<ScopedPage> result = new ArrayList<>();
        int rootDepth = depthOf(searchRoot);

        for (Page page : specificPageRouting.specificPages(searchRoot)) {
            SpecificPageRouting.Binding binding = specificPageRouting.readBinding(page);
            int depth = depthOf(page) - rootDepth;

            for (String categoryUrlPath : binding.getUseForCategories()) {
                result.add(new ScopedPage(page, categoryUrlPath, depth));
            }

            // A category page's selectorFilter binds a category url-path scope (comparable below). A product
            // page's selectorFilter binds individual SKUs/URL keys -- not a category subtree -- so it has no
            // ancestor/descendant relationship to compare and is intentionally excluded here (the brief's
            // duplicate/shadowing definitions are scoped to url-path scopes only).
            if ("category".equals(binding.getPageType())) {
                for (SpecificPageRouting.ParsedFilter filter : specificPageRouting.parseSelectorFilter(binding)) {
                    String scope = filter.isValid() ? filter.getUrlPath() : filter.getRaw();
                    result.add(new ScopedPage(page, scope, depth));
                }
            }
        }

        return result;
    }

    private int depthOf(Page page) {
        return page.getPath().split("/").length;
    }
}
