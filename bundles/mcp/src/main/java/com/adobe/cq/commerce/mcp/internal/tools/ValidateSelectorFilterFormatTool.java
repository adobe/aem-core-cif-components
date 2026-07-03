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
 * MCP read tool validating the {@code uid|urlPath} format of a specific page's {@code selectorFilter} entries
 * (catalog §8), thinly wrapping {@link SpecificPageRouting#readBinding(Page)} /
 * {@link SpecificPageRouting#parseSelectorFilter(SpecificPageRouting.Binding)}. Catches malformed entries (missing
 * {@code |} separator) that the oracle ({@code SpecificPageStrategy}) otherwise silently tolerates via an ambiguous
 * fallback (treating the raw string as both uid and url-path).
 */
@Component(service = McpTool.class)
public class ValidateSelectorFilterFormatTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SpecificPageRouting specificPageRouting = new SpecificPageRouting();

    @Override
    public String name() {
        return "validate_selector_filter_format";
    }

    @Override
    public String description() {
        return "Validate the uid|urlPath format of a specific PDP/PLP page's selectorFilter entries, flagging "
            + "entries missing the '|' separator (which the runtime otherwise resolves via an ambiguous fallback).";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("path").put("type", "string").put("description",
            "Path of the specific page to validate (under /content).");
        schema.putArray("required").add("path");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("path is required");
        }
        if (!path.startsWith("/content/") && !"/content".equals(path)) {
            throw new IllegalArgumentException("path must be under /content: " + path);
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("path not found: " + path);
        }
        Page page = resource.adaptTo(Page.class);
        if (page == null) {
            throw new IllegalArgumentException("path does not resolve to a page: " + path);
        }

        SpecificPageRouting.Binding binding = specificPageRouting.readBinding(page);
        List<SpecificPageRouting.ParsedFilter> parsedFilters = specificPageRouting.parseSelectorFilter(binding);

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("selectorFilterType", binding.getSelectorFilterType());

        ArrayNode entries = out.putArray("entries");
        for (SpecificPageRouting.ParsedFilter filter : parsedFilters) {
            ObjectNode entry = entries.addObject();
            entry.put("raw", filter.getRaw());
            entry.put("valid", filter.isValid());
            if (filter.isValid()) {
                entry.put("uid", filter.getUid());
                entry.put("urlPath", filter.getUrlPath());
            } else {
                entry.put("issue", filter.getIssue());
            }
        }

        return out;
    }
}
