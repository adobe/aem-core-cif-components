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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.CatalogPageRouting;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP read tool flagging catalog (PLP) pages whose {@code urlPath} root-category scopes overlap (catalog §7),
 * which makes {@code SpecificPageStrategy.getGenericPage}'s first-match routing ambiguous, thinly wrapping
 * {@link CatalogPageRouting#listCatalogPages(SiteStructure)}.
 * <p>
 * This is a structural check only: it compares the catalog pages' own {@code magentoRootCategoryId} /
 * {@code magentoRootCategoryIdType} scope properties against each other. It does <b>not</b> fetch the live
 * category tree, so it cannot detect categories that have <em>no</em> matching catalog page (dead links) —
 * that requires a GraphQL round trip and is out of scope for this tool.
 */
@Component(service = McpTool.class)
public class DetectCatalogPageConflictsTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CatalogPageRouting catalogPageRouting = new CatalogPageRouting();

    @Override
    public String name() {
        return "detect_catalog_page_conflicts";
    }

    @Override
    public String description() {
        return "Flag catalog (PLP) pages whose urlPath root-category scopes overlap (identical scopes, or one "
            + "scope nested inside another), which makes routing ambiguous. Structural comparison of the catalog "
            + "pages' own scope properties only -- does not fetch the live category tree, so it cannot detect "
            + "categories with no matching catalog page (dead-link detection is out of scope). Optional siteRoot "
            + "(any page under /content within the site; defaults to the endpoint's own nav root). siteRoot in the "
            + "result is the resolved store nav root.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("siteRoot").put("type", "string");
        return schema;
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String siteRootArg = args.path("siteRoot").asText(null);

        Page siteRootPage;
        if (StringUtils.isNotBlank(siteRootArg)) {
            ResourceResolver resolver = ctx.getRequest().getResourceResolver();
            siteRootPage = PathArgs.resolvePage(resolver, "siteRoot", siteRootArg);
        } else {
            siteRootPage = ctx.getLandingPage();
        }

        SiteStructure site = siteRootPage == null ? null : siteRootPage.adaptTo(SiteStructure.class);
        if (site == null) {
            throw new IllegalArgumentException("could not resolve a site structure for siteRoot: "
                + (siteRootPage == null ? siteRootArg : siteRootPage.getPath()));
        }

        List<CatalogPageRouting.CatalogPageInfo> catalogPages = catalogPageRouting.listCatalogPages(site);

        ObjectNode out = mapper.createObjectNode();
        Page landingPage = site.getLandingPage();
        out.put("siteRoot", landingPage != null ? landingPage.getPath() : siteRootPage.getPath());
        ArrayNode overlaps = out.putArray("overlaps");

        for (int i = 0; i < catalogPages.size(); i++) {
            CatalogPageRouting.CatalogPageInfo a = catalogPages.get(i);
            if (!isScoped(a)) {
                continue;
            }
            for (int j = i + 1; j < catalogPages.size(); j++) {
                CatalogPageRouting.CatalogPageInfo b = catalogPages.get(j);
                if (!isScoped(b)) {
                    continue;
                }

                String scopeA = a.getRootCategoryId();
                String scopeB = b.getRootCategoryId();
                String kind;
                String scope;
                if (scopeA.equals(scopeB)) {
                    kind = "duplicate-scope";
                    scope = scopeA;
                } else if (scopeA.startsWith(scopeB + "/") || scopeB.startsWith(scopeA + "/")) {
                    kind = "ancestor-descendant";
                    scope = scopeA.length() <= scopeB.length() ? scopeA : scopeB;
                } else {
                    continue;
                }

                ObjectNode overlap = overlaps.addObject();
                ArrayNode pages = overlap.putArray("pages");
                pages.add(a.getPath());
                pages.add(b.getPath());
                overlap.put("scope", scope);
                overlap.put("kind", kind);
            }
        }

        return out;
    }

    /**
     * Only non-generic, {@code urlPath}-typed catalog pages participate in overlap detection: a generic fallback
     * page has no scope to compare, and a {@code uid}-typed scope cannot be compared structurally as a url-path.
     */
    private boolean isScoped(CatalogPageRouting.CatalogPageInfo page) {
        return !page.isGenericFallback() && StringUtils.isNotBlank(page.getRootCategoryId());
    }
}
