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
 * MCP read tool replaying {@code SpecificPageStrategy.getGenericPage} for a given category {@code url_path}
 * (catalog §7), reporting which catalog page wins and why, thinly wrapping
 * {@link CatalogPageRouting#resolveFor(SiteStructure, String)}.
 */
@Component(service = McpTool.class)
public class ExplainCatalogPageRoutingTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CatalogPageRouting catalogPageRouting = new CatalogPageRouting();

    @Override
    public String name() {
        return "explain_catalog_page_routing";
    }

    @Override
    public String description() {
        return "Explain which catalog (PLP) page wins for a category url_path, and why (generic fallback or "
            + "urlPath-scope match), replaying the site's first-match routing with a full evaluation trace. "
            + "Optional siteRoot (any page under /content within the site; defaults to the endpoint's own nav "
            + "root). siteRoot in the result is the resolved store nav root.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("urlPath").put("type", "string").put("description",
            "The category's url_path to resolve a winning catalog page for.");
        properties.putObject("siteRoot").put("type", "string");
        schema.putArray("required").add("urlPath");
        return schema;
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String urlPath = args.path("urlPath").asText(null);
        if (StringUtils.isBlank(urlPath)) {
            throw new IllegalArgumentException("urlPath is required");
        }

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

        CatalogPageRouting.CatalogPageResolution resolution = catalogPageRouting.resolveFor(site, urlPath);

        ObjectNode out = mapper.createObjectNode();
        out.put("identifier", urlPath);
        Page winningPage = resolution.getWinningPage();
        out.put("winningPage", winningPage != null ? winningPage.getPath() : null);
        out.put("reason", resolution.getReason());

        ArrayNode candidatesNode = out.putArray("candidates");
        for (CatalogPageRouting.CandidateEvaluation candidate : resolution.getTrace()) {
            ObjectNode entry = candidatesNode.addObject();
            entry.put("path", candidate.getPath());
            entry.put("matched", candidate.isMatched());
            entry.put("why", candidate.getWhy());
        }
        return out;
    }
}
