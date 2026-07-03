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

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool resolving a category's current details (name, url path, breadcrumb trail) by UID, verifying it still
 * resolves against the live catalog.
 */
@Component(service = McpTool.class)
public class ResolveCategoryDetailsTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "resolve_category_details";
    }

    @Override
    public String description() {
        return "Resolve a category's current details (name, url path, breadcrumbs) by UID, "
            + "confirming it still resolves against the live catalog.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("uid").put("type", "string");
        properties.putObject("urlPath").put("type", "string");
        schema.putArray("required").add("uid");
        return schema;
    }

    protected CategoryInterface fetch(StoreContext ctx, String identifier, String categoryIdType) {
        McpCategoryRetriever retriever = new McpCategoryRetriever(ctx.getClient());
        retriever.extendCategoryQueryWith(
            q -> q.breadcrumbs(b -> b.categoryUid().categoryName().categoryLevel().categoryUrlPath()));
        if (StringUtils.isNotBlank(categoryIdType)) {
            retriever.setCategoryIdType(categoryIdType);
        }
        retriever.setIdentifier(identifier);
        return retriever.fetchCategory();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String uid = args.path("uid").asText(null);
        if (StringUtils.isBlank(uid)) {
            throw new IllegalArgumentException("uid is required");
        }
        String urlPath = args.path("urlPath").asText(null);

        String identifier = StringUtils.isNotBlank(urlPath) ? urlPath : uid;
        String categoryIdType = StringUtils.isNotBlank(urlPath) ? "urlPath" : null;
        CategoryInterface category = fetch(ctx, identifier, categoryIdType);

        ObjectNode out = mapper.createObjectNode();
        if (category == null) {
            out.put("uid", uid);
            out.put("resolves", false);
            return out;
        }

        out.put("uid", category.getUid() != null ? category.getUid().toString() : uid);
        out.put("name", category.getName());
        out.put("urlPath", category.getUrlPath());
        out.set("breadcrumbs", DtoMapper.breadcrumbs(mapper, category.getBreadcrumbs()));
        return out;
    }
}
