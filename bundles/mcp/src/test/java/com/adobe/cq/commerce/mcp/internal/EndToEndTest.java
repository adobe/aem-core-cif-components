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
package com.adobe.cq.commerce.mcp.internal;

import org.junit.Test;

import com.adobe.cq.commerce.mcp.JsonRpc;
import com.adobe.cq.commerce.mcp.internal.tools.ConfigureProductComponentTool;
import com.adobe.cq.commerce.mcp.internal.tools.GetCategoryAssociatedContentTool;
import com.adobe.cq.commerce.mcp.internal.tools.GetProductAssociatedContentTool;
import com.adobe.cq.commerce.mcp.internal.tools.GetProductVariantsTool;
import com.adobe.cq.commerce.mcp.internal.tools.SearchProductsTool;
import com.adobe.cq.commerce.mcp.internal.tools.TagContentWithCommerceTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EndToEndTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonRpc.Request listReq() {
        try {
            return JsonRpc.parse(mapper, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void writeToolHiddenFromShopperSelector() {
        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(new SearchProductsTool());
        reg.bindTool(new GetProductAssociatedContentTool());
        reg.bindTool(new GetCategoryAssociatedContentTool());
        reg.bindTool(new GetProductVariantsTool());
        reg.bindTool(new TagContentWithCommerceTool());
        reg.bindTool(new ConfigureProductComponentTool());
        JsonRpcDispatcher d = new JsonRpcDispatcher(mapper, reg);

        JsonNode shopperTools = d.dispatch("mcp", null, listReq()).get("result").get("tools");
        boolean hasWrite = false;
        for (JsonNode t : shopperTools) {
            String name = t.get("name").asText();
            if ("configure_product_component".equals(name) || "tag_content_with_commerce".equals(name)) {
                hasWrite = true;
            }
        }
        assertFalse(hasWrite);

        JsonNode authorTools = d.dispatch("mcp-authoring", null, listReq()).get("result").get("tools");
        assertEquals(6, authorTools.size());
    }
}
