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

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.EntityUrl;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool resolving a storefront URL (relative or url_key path) to the catalog/CMS entity it points at, via
 * Magento's {@code urlResolver} query.
 */
@Component(service = McpTool.class)
public class ResolveUrlToEntityTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "resolve_url_to_entity";
    }

    @Override
    public String description() {
        return "Resolve a storefront URL to the catalog/CMS entity it points at (type, id, uid, canonical and "
            + "relative URLs, redirect code).";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("url").put("type", "string");
        schema.putArray("required").add("url");
        return schema;
    }

    protected EntityUrl fetch(StoreContext ctx, String url) {
        String query = Operations
            .query(q -> q.urlResolver(url, u -> u.type().id().entityUid().canonicalUrl().relativeUrl().redirectCode()))
            .toString();
        GraphqlResponse<Query, Error> response = ctx.getClient().execute(query);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new IllegalArgumentException(response.getErrors().get(0).getMessage());
        }
        return response.getData().getUrlResolver();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String url = args.path("url").asText(null);
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url is required");
        }

        EntityUrl entityUrl = fetch(ctx, url);

        ObjectNode out = mapper.createObjectNode();
        out.put("url", url);
        if (entityUrl == null) {
            out.put("resolves", false);
            return out;
        }

        out.put("type", entityUrl.getType() != null ? entityUrl.getType().toString() : null);
        out.put("id", entityUrl.getId());
        out.put("uid", entityUrl.getEntityUid() != null ? entityUrl.getEntityUid().toString() : null);
        out.put("canonicalUrl", entityUrl.getCanonicalUrl());
        out.put("relativeUrl", entityUrl.getRelativeUrl());
        out.put("redirectCode", entityUrl.getRedirectCode());
        return out;
    }
}
