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

import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool browsing the category tree; returns a category and its immediate children.
 */
@Component(service = McpTool.class)
public class BrowseCategoriesTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @Reference
    UrlProvider urlProvider;

    @Override
    public String name() {
        return "browse_categories";
    }

    @Override
    public String description() {
        return "Browse the category tree; returns a category and its children.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("uid").put("type", "string");
        return schema;
    }

    protected CategoryInterface fetch(StoreContext ctx, String uid) {
        McpCategoryRetriever retriever = new McpCategoryRetriever(ctx.getClient());
        if (uid != null) {
            retriever.setIdentifier(uid);
        }
        return retriever.fetchCategory();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String uid = args.hasNonNull("uid") ? args.get("uid").asText() : null;
        CategoryInterface category = fetch(ctx, uid);
        ObjectNode out = mapper.createObjectNode();
        out.set("category", category == null ? mapper.nullNode()
            : DtoMapper.category(mapper, category, true, cat -> plpUrl(ctx, cat)));
        return out;
    }

    /**
     * Resolves the product-listing (category) page URL for the given category via CIF's {@link UrlProvider}, using the
     * endpoint's own nav-root page so the provider resolves the store's configured catalog page.
     */
    private String plpUrl(StoreContext ctx, CategoryInterface category) {
        return urlProvider.formatCategoryUrl(ctx.getRequest(), ctx.getLandingPage(), new CategoryUrlFormat.Params(category));
    }
}
