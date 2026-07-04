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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.tools.authoring.CommerceContentTagger.Action;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool tagging DAM assets, pages, or experience fragment variations with product SKUs and/or category UIDs
 * using {@code cq:products} and {@code cq:categories}.
 */
@Component(service = McpTool.class)
public class TagContentWithCommerceTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "tag_content_with_commerce";
    }

    @Override
    public String description() {
        return "Tag a DAM asset, page, or experience fragment variation with product SKUs and/or category UIDs.";
    }

    @Override
    public boolean writesContent() {
        return true;
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("path").put("type", "string");
        properties.putObject("sku").put("type", "string");
        properties.putObject("categoryUid").put("type", "string");
        properties.putObject("action").put("type", "string");
        schema.putArray("required").add("path");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        String sku = args.path("sku").asText(null);
        String categoryUid = args.path("categoryUid").asText(null);
        Action action = Action.from(args.path("action").asText(null));

        if (StringUtils.isBlank(path) || !path.startsWith("/content/")) {
            throw new IllegalArgumentException("path (under /content) is required");
        }
        if (StringUtils.isBlank(sku) && StringUtils.isBlank(categoryUid)) {
            throw new IllegalArgumentException("sku and/or categoryUid is required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = CommerceContentTagger.resolveTagTarget(resolver, path);
        if (target == null) {
            throw new IllegalArgumentException("resource not found: " + path);
        }

        CommerceContentTagger.apply(target, sku, categoryUid, action);
        resolver.commit();

        ValueMap properties = target.getValueMap();
        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("targetPath", target.getPath());
        out.put("action", action.name().toLowerCase());
        if (StringUtils.isNotBlank(sku)) {
            out.put("sku", sku);
            out.set("products", toArray(properties, CommerceExperienceFragment.PN_CQ_PRODUCTS));
        }
        if (StringUtils.isNotBlank(categoryUid)) {
            out.put("categoryUid", categoryUid);
            out.set("categories", toArray(properties, CommerceExperienceFragment.PN_CQ_CATEGORIES));
        }
        out.put("updated", true);
        return out;
    }

    private ArrayNode toArray(ValueMap properties, String property) {
        ArrayNode array = mapper.createArrayNode();
        for (String value : CommerceContentTagger.readTagList(properties, property)) {
            array.add(value);
        }
        return array;
    }
}
