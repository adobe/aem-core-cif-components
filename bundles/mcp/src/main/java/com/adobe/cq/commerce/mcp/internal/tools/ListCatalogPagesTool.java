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
import org.apache.sling.api.resource.Resource;
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
 * MCP read tool listing every catalog page of a site (catalog §7), in {@link SiteStructure#getCategoryPages()}
 * order (the nav-root/landing page's generic-fallback entry always last), thinly wrapping
 * {@link CatalogPageRouting#listCatalogPages(SiteStructure)}.
 */
@Component(service = McpTool.class)
public class ListCatalogPagesTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CatalogPageRouting catalogPageRouting = new CatalogPageRouting();

    @Override
    public String name() {
        return "list_catalog_pages";
    }

    @Override
    public String description() {
        return "List every catalog (PLP) page of a site and its root-category scope, in routing order (the "
            + "generic fallback page always last). Optional siteRoot (any page under /content within the site; "
            + "defaults to the endpoint's own nav root).";
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

        Page siteRootPage;
        if (StringUtils.isNotBlank(siteRootArg)) {
            if (!siteRootArg.startsWith("/content/") && !"/content".equals(siteRootArg)) {
                throw new IllegalArgumentException("siteRoot must be under /content: " + siteRootArg);
            }
            ResourceResolver resolver = ctx.getRequest().getResourceResolver();
            Resource resource = resolver.getResource(siteRootArg);
            if (resource == null) {
                throw new IllegalArgumentException("siteRoot not found: " + siteRootArg);
            }
            siteRootPage = resource.adaptTo(Page.class);
            if (siteRootPage == null) {
                throw new IllegalArgumentException("siteRoot does not resolve to a page: " + siteRootArg);
            }
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
        ArrayNode catalogPagesNode = out.putArray("catalogPages");
        for (CatalogPageRouting.CatalogPageInfo page : catalogPages) {
            ObjectNode entry = catalogPagesNode.addObject();
            entry.put("path", page.getPath());
            entry.put("rootCategoryId", page.getRootCategoryId());
            entry.put("idType", page.getIdType());
            entry.put("genericFallback", page.isGenericFallback());
        }
        return out;
    }
}
