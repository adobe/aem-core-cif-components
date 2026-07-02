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

import java.util.Iterator;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.McpSearchOptions;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool searching the commerce catalog by keyword and optional attribute filters, backed by
 * {@link SearchResultsService}.
 */
@Component(service = McpTool.class)
public class SearchProductsTool implements McpTool {

    @Reference
    SearchResultsService searchResultsService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "search_products";
    }

    @Override
    public String description() {
        return "Search the commerce catalog by keyword and optional attribute filters.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("query").put("type", "string");
        properties.putObject("page").put("type", "integer");
        properties.putObject("pageSize").put("type", "integer");
        properties.putObject("filters").put("type", "object");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext storeContext = (StoreContext) context;
        McpSearchOptions options = new McpSearchOptions();
        if (args.hasNonNull("query")) {
            options.setSearchQuery(args.get("query").asText());
        }
        options.setCurrentPage(args.path("page").asInt(1));
        options.setPageSize(args.path("pageSize").asInt(20));
        if (args.has("filters") && args.get("filters").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = args.get("filters").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                options.putFilter(field.getKey(), field.getValue().asText());
            }
        }

        SearchResultsSet results = searchResultsService.performSearch(
            options, storeContext.getResource(), storeContext.getProductPage(), storeContext.getRequest());

        ObjectNode out = mapper.createObjectNode();
        out.put("total", results.getTotalResults() == null ? 0 : results.getTotalResults());
        ArrayNode items = out.putArray("items");
        for (ProductListItem item : results.getProductListItems()) {
            items.add(DtoMapper.product(mapper, item));
        }
        return out;
    }
}
