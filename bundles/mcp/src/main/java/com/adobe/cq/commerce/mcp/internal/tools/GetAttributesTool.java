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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool listing the filterable product attributes available for the store, backed by
 * {@link SearchFilterService}.
 */
@Component(service = McpTool.class)
public class GetAttributesTool implements McpTool {

    @Reference
    SearchFilterService searchFilterService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_attributes";
    }

    @Override
    public String description() {
        return "List filterable product attributes for the store.";
    }

    @Override
    public ObjectNode inputSchema() {
        return mapper.createObjectNode().put("type", "object");
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext storeContext = (StoreContext) context;
        ObjectNode out = mapper.createObjectNode();
        ArrayNode attributes = out.putArray("attributes");
        for (FilterAttributeMetadata metadata : searchFilterService.retrieveCurrentlyAvailableCommerceFilters(
            storeContext.getLandingPage())) {
            ObjectNode attribute = attributes.addObject();
            attribute.put("code", metadata.getAttributeCode());
            attribute.put("inputType", metadata.getFilterInputType());
        }
        return out;
    }
}
