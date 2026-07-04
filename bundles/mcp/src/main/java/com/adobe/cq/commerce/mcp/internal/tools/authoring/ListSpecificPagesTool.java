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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP read tool listing every descendant page under a site with a specific-page binding (catalog §8), and what it
 * binds, thinly wrapping {@link SpecificPageRouting#specificPages(Page)} / {@link SpecificPageRouting#readBinding(Page)}.
 */
@Component(service = McpTool.class)
public class ListSpecificPagesTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SpecificPageRouting specificPageRouting = new SpecificPageRouting();

    @Override
    public String name() {
        return "list_specific_pages";
    }

    @Override
    public String description() {
        return "List every descendant page under a site with a specific PDP/PLP binding (selectorFilter or "
            + "useForCategories), and what it binds. Optional siteRoot (any page under /content; defaults to the "
            + "endpoint's own nav root). siteRoot in the result is the traversal root used (the arg if given, else "
            + "the endpoint nav root). pageType is a best-effort field-presence heuristic (for parity with "
            + "check_specific_page_capability).";
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

        Page searchRoot;
        if (StringUtils.isNotBlank(siteRootArg)) {
            ResourceResolver resolver = ctx.getRequest().getResourceResolver();
            searchRoot = PathArgs.resolvePage(resolver, "siteRoot", siteRootArg);
        } else {
            searchRoot = ctx.getLandingPage();
        }

        if (searchRoot == null) {
            throw new IllegalArgumentException("could not resolve a site root page");
        }

        List<Page> pages = specificPageRouting.specificPages(searchRoot);

        ObjectNode out = mapper.createObjectNode();
        out.put("siteRoot", searchRoot.getPath());

        ArrayNode pagesNode = out.putArray("specificPages");
        for (Page page : pages) {
            SpecificPageRouting.Binding binding = specificPageRouting.readBinding(page);

            ObjectNode entry = pagesNode.addObject();
            entry.put("path", page.getPath());
            entry.put("pageType", binding.getPageType());

            if (binding.getSelectorFilter().length > 0) {
                ArrayNode selectorFilter = entry.putArray("selectorFilter");
                for (String filter : binding.getSelectorFilter()) {
                    selectorFilter.add(filter);
                }
                entry.put("selectorFilterType", binding.getSelectorFilterType());
            }

            if (binding.isIncludesSubCategories()) {
                entry.put("includesSubCategories", true);
            }

            if (binding.getUseForCategories().length > 0) {
                ArrayNode useForCategories = entry.putArray("useForCategories");
                for (String category : binding.getUseForCategories()) {
                    useForCategories.add(category);
                }
            }
        }

        return out;
    }
}
