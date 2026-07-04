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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.AssociatedContentSupport;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool listing AEM content associated with a category UID: experience fragments, content fragments,
 * associated content pages, and DAM assets referenced from matched content fragments.
 */
@Component(service = McpTool.class)
public class GetCategoryAssociatedContentTool implements McpTool {

    @Reference
    AssociatedContentService associatedContentService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_category_associated_content";
    }

    @Override
    public String description() {
        return "List AEM experience fragments, content fragments, associated pages, and DAM assets linked to a category UID.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("categoryUid").put("type", "string");
        properties.putObject("fragmentLocation").put("type", "string");
        properties.putObject("contentFragmentModel").put("type", "string");
        properties.putObject("linkElement").put("type", "string");
        properties.putObject("limit").put("type", "integer");
        schema.putArray("required").add("categoryUid");
        return schema;
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String categoryUid = args.path("categoryUid").asText(null);
        if (StringUtils.isBlank(categoryUid)) {
            throw new IllegalArgumentException("categoryUid is required");
        }

        return AssociatedContentSupport.buildCategoryResult(associatedContentService, mapper,
            ctx.getRequest().getResourceResolver(), ctx.getLandingPage(), categoryUid,
            args.path("fragmentLocation").asText(null), args.path("contentFragmentModel").asText(null),
            args.path("linkElement").asText(null), AssociatedContentSupport.limit(args));
    }
}
