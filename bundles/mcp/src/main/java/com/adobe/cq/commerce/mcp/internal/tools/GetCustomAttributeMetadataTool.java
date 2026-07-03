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

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Attribute;
import com.adobe.cq.commerce.magento.graphql.AttributeInput;
import com.adobe.cq.commerce.magento.graphql.AttributeOption;
import com.adobe.cq.commerce.magento.graphql.CustomAttributeMetadata;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool exposing Magento's {@code customAttributeMetadata} query, which resolves the
 * attribute type, input type, and (for select/multiselect attributes) label/value options for a
 * caller-supplied list of {@code {attribute_code, entity_type}} pairs.
 */
@Component(service = McpTool.class)
public class GetCustomAttributeMetadataTool implements McpTool {

    private static final String DEFAULT_ENTITY_TYPE = "catalog_product";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_custom_attribute_metadata";
    }

    @Override
    public String description() {
        return "Look up metadata (attribute type, input type, and options) for one or more custom "
            + "product/entity attributes by code.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode attributes = properties.putObject("attributes");
        attributes.put("type", "array");
        ObjectNode items = attributes.putObject("items");
        items.put("type", "object");
        ObjectNode itemProperties = items.putObject("properties");
        itemProperties.putObject("code").put("type", "string");
        itemProperties.putObject("entityType").put("type", "string");
        items.putArray("required").add("code");
        schema.putArray("required").add("attributes");
        return schema;
    }

    protected CustomAttributeMetadata fetch(StoreContext ctx, List<AttributeInput> inputs) {
        String query = Operations.query(q -> q.customAttributeMetadata(inputs, m -> m.items(i -> i.attributeCode()
            .attributeType()
            .inputType()
            .entityType()
            .attributeOptions(o -> o.label().value())))).toString();
        GraphqlResponse<Query, Error> response = ctx.getClient().execute(query);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new IllegalArgumentException(response.getErrors().get(0).getMessage());
        }
        return response.getData().getCustomAttributeMetadata();
    }

    private List<AttributeInput> parseAttributeInputs(JsonNode args) {
        JsonNode attributesNode = args.get("attributes");
        if (attributesNode == null || !attributesNode.isArray() || attributesNode.isEmpty()) {
            throw new IllegalArgumentException("attributes must be a non-empty array of {code, entityType} objects");
        }
        List<AttributeInput> inputs = new ArrayList<>();
        for (JsonNode attributeNode : attributesNode) {
            JsonNode codeNode = attributeNode.get("code");
            if (codeNode == null || codeNode.isNull() || codeNode.asText().isEmpty()) {
                throw new IllegalArgumentException("each entry in attributes requires a non-empty 'code'");
            }
            JsonNode entityTypeNode = attributeNode.get("entityType");
            String entityType = (entityTypeNode != null && !entityTypeNode.isNull())
                ? entityTypeNode.asText()
                : DEFAULT_ENTITY_TYPE;
            inputs.add(new AttributeInput().setAttributeCode(codeNode.asText()).setEntityType(entityType));
        }
        return inputs;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        List<AttributeInput> inputs = parseAttributeInputs(args);
        CustomAttributeMetadata metadata = fetch(ctx, inputs);

        ObjectNode out = mapper.createObjectNode();
        ArrayNode items = out.putArray("items");
        List<Attribute> attributes = metadata != null ? metadata.getItems() : null;
        if (attributes != null) {
            for (Attribute attribute : attributes) {
                ObjectNode item = items.addObject();
                item.put("code", attribute.getAttributeCode());
                item.put("attributeType", attribute.getAttributeType());
                item.put("inputType", attribute.getInputType());
                item.put("entityType", attribute.getEntityType());
                ArrayNode options = item.putArray("options");
                List<AttributeOption> attributeOptions = attribute.getAttributeOptions();
                if (attributeOptions != null) {
                    for (AttributeOption option : attributeOptions) {
                        ObjectNode optionNode = options.addObject();
                        optionNode.put("label", option.getLabel());
                        optionNode.put("value", option.getValue());
                    }
                }
            }
        }
        return out;
    }
}
