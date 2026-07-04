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

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool resolving a single commerce content fragment by {@code linkElement} match (the same mechanism the
 * {@code contentfragment/v1} component's {@code findContentFragment()} uses) and returning its element values.
 */
@Component(service = McpTool.class)
public class GetCommerceContentFragmentTool implements McpTool {

    @Reference
    AssociatedContentService associatedContentService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_commerce_content_fragment";
    }

    @Override
    public String description() {
        return "Resolve a commerce content fragment matched to a product SKU or category UID via its linkElement field, and return the fragment's element values.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("identifier").put("type", "string");
        ObjectNode type = properties.putObject("type");
        type.put("type", "string");
        type.putArray("enum").add("product").add("category");
        properties.putObject("contentFragmentModel").put("type", "string");
        properties.putObject("linkElement").put("type", "string");
        ArrayNode required = schema.putArray("required");
        required.add("identifier");
        required.add("type");
        return schema;
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
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
            throw new IllegalArgumentException("type must be 'product' or 'category'");
        }
        String contentFragmentModel = args.path("contentFragmentModel").asText(null);
        String linkElement = args.path("linkElement").asText(null);

        ObjectNode out = mapper.createObjectNode();
        out.put("identifier", identifier);
        out.put("type", type);

        ContentFragment fragment = resolveContentFragment(ctx, type, identifier, contentFragmentModel, linkElement);
        if (fragment == null) {
            out.put("resolves", false);
            return out;
        }

        Resource resource = fragment.adaptTo(Resource.class);
        String modelPath = contentFragmentModel;
        String fragmentPath = null;
        if (resource != null) {
            fragmentPath = resource.getPath();
            if (StringUtils.isBlank(modelPath)) {
                Resource data = resource.getChild("jcr:content/data");
                if (data != null) {
                    String resolvedModel = data.getValueMap().get("cq:model", "");
                    if (StringUtils.isNotBlank(resolvedModel)) {
                        modelPath = resolvedModel;
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(modelPath)) {
            out.put("modelPath", modelPath);
        }
        if (fragmentPath != null) {
            out.put("fragmentPath", fragmentPath);
        }

        ObjectNode fields = out.putObject("fields");
        Iterator<ContentElement> elements = fragment.getElements();
        while (elements.hasNext()) {
            ContentElement element = elements.next();
            putFieldValue(fields, element.getName(), element.getValue() != null ? element.getValue().getValue() : null);
        }
        return out;
    }

    private void putFieldValue(ObjectNode fields, String name, Object value) {
        if (value instanceof Object[]) {
            ArrayNode array = fields.putArray(name);
            for (Object item : (Object[]) value) {
                array.add(item != null ? item.toString() : null);
            }
        } else if (value != null) {
            fields.put(name, value.toString());
        } else {
            fields.putNull(name);
        }
    }

    /**
     * Resolves the content fragment matched to {@code identifier} for the given {@code type}. Extracted as a
     * protected seam so unit tests can supply a canned {@link ContentFragment} without a live
     * {@link AssociatedContentService}.
     */
    protected ContentFragment resolveContentFragment(StoreContext ctx, String type, String identifier,
        String contentFragmentModel, String linkElement) {
        boolean isCategory = "category".equals(type);
        return AssociatedContentSupport.resolveSingleContentFragment(associatedContentService,
            ctx.getRequest().getResourceResolver(), isCategory, identifier, contentFragmentModel, linkElement);
    }
}
