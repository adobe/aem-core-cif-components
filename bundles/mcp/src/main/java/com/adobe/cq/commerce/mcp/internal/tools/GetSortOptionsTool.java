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

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.SortField;
import com.adobe.cq.commerce.magento.graphql.SortFields;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool listing the product listing sort fields available for the store, mirroring
 * {@code ProductSortFieldsDataSourceServlet} in {@code bundles/core}.
 * <p>
 * Note: {@code relevance} is injected by that servlet only for search results components and is
 * not part of the plain product-listing GraphQL response, so it is intentionally never
 * synthesized here.
 */
@Component(service = McpTool.class)
public class GetSortOptionsTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_sort_options";
    }

    @Override
    public String description() {
        return "List the available product sort fields (value/label) and the store's default sort field.";
    }

    @Override
    public ObjectNode inputSchema() {
        return mapper.createObjectNode().put("type", "object");
    }

    protected SortFields fetchSortFields(StoreContext ctx) {
        String query = "{products(filter:{}) {sort_fields {default options {label value}}}}";
        GraphqlResponse<Query, Error> response = ctx.getClient().execute(query);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new IllegalArgumentException(response.getErrors().get(0).getMessage());
        }
        return response.getData().getProducts().getSortFields();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        SortFields sortFields = fetchSortFields(ctx);

        ObjectNode out = mapper.createObjectNode();
        out.put("default", sortFields != null ? sortFields.getDefault() : null);

        ArrayNode options = out.putArray("options");
        List<SortField> sortFieldOptions = sortFields != null ? sortFields.getOptions() : null;
        if (sortFieldOptions != null) {
            for (SortField sortField : sortFieldOptions) {
                ObjectNode option = options.addObject();
                option.put("value", sortField.getValue());
                option.put("label", sortField.getLabel());
            }
        }
        return out;
    }
}
