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
 * MCP read tool replaying {@code SpecificPageStrategy.getSpecificPage} for a given product slug/url_key or category
 * {@code url_path} (catalog §8), reporting which specific PDP/PLP page wins, its tree depth, and why, thinly
 * wrapping {@link SpecificPageRouting#resolveSpecificPage(Page, String, String)}.
 */
@Component(service = McpTool.class)
public class ExplainPageResolutionTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SpecificPageRouting specificPageRouting = new SpecificPageRouting();

    @Override
    public String name() {
        return "explain_page_resolution";
    }

    @Override
    public String description() {
        return "Explain which specific PDP/PLP page wins for a product slug/url_key or category url_path, and its "
            + "tree depth, replaying the site's deepest-wins specific-page routing with a full evaluation trace. "
            + "Optional siteRoot (any page under /content; defaults to the endpoint's own nav root).";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("identifier").put("type", "string").put("description",
            "The category url_path or product slug/url_key to resolve a winning specific page for.");
        ObjectNode type = properties.putObject("type").put("type", "string");
        ArrayNode typeEnum = type.putArray("enum");
        typeEnum.add("product");
        typeEnum.add("category");
        properties.putObject("siteRoot").put("type", "string");
        ArrayNode required = schema.putArray("required");
        required.add("identifier");
        required.add("type");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String identifier = args.path("identifier").asText(null);
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("identifier is required");
        }

        String type = args.path("type").asText(null);
        if (!"product".equals(type) && !"category".equals(type)) {
            throw new IllegalArgumentException("type must be \"product\" or \"category\"");
        }

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

        SpecificPageRouting.Resolution resolution = specificPageRouting.resolveSpecificPage(searchRoot, identifier, type);

        ObjectNode out = mapper.createObjectNode();
        out.put("identifier", identifier);
        out.put("type", type);
        Page winningPage = resolution.getWinningPage();
        out.put("winningPage", winningPage != null ? winningPage.getPath() : null);
        out.put("depth", resolution.getDepth());

        ArrayNode candidatesNode = out.putArray("candidates");
        for (SpecificPageRouting.Candidate candidate : resolution.getTrace()) {
            ObjectNode entry = candidatesNode.addObject();
            entry.put("path", candidate.getPath());
            entry.put("depth", candidate.getDepth());
            entry.put("matched", candidate.isMatched());
            entry.put("why", candidate.getWhy());
        }
        return out;
    }
}
