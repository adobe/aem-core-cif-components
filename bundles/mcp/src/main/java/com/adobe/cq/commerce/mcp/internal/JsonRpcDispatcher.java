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

import com.adobe.cq.commerce.mcp.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public class JsonRpcDispatcher {
    private final ObjectMapper mapper;
    private final ToolRegistry registry;

    public JsonRpcDispatcher(ObjectMapper mapper, ToolRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    public ObjectNode dispatch(String selector, McpCallContext ctx, JsonRpc.Request req) {
        if (!"2.0".equals(req.jsonrpc) || req.method == null) {
            return JsonRpc.error(req.id, JsonRpc.INVALID_REQUEST, "invalid request");
        }
        switch (req.method) {
            case "initialize":
                return JsonRpc.result(req.id, initialize());
            case "tools/list":
                return JsonRpc.result(req.id, toolsList(selector));
            case "tools/call":
                return toolsCall(selector, ctx, req);
            default:
                return JsonRpc.error(req.id, JsonRpc.METHOD_NOT_FOUND, "unknown method: " + req.method);
        }
    }

    private ObjectNode initialize() {
        ObjectNode r = mapper.createObjectNode();
        r.put("protocolVersion", "2025-06-18");
        r.putObject("capabilities").putObject("tools");
        ObjectNode info = r.putObject("serverInfo");
        info.put("name", "cif-commerce-mcp");
        info.put("version", "1.0.0");
        return r;
    }

    private ObjectNode toolsList(String selector) {
        ObjectNode r = mapper.createObjectNode();
        ArrayNode arr = r.putArray("tools");
        for (McpTool t : registry.forSelector(selector)) {
            ObjectNode n = arr.addObject();
            n.put("name", t.name());
            n.put("description", t.description());
            n.set("inputSchema", t.inputSchema());
        }
        return r;
    }

    private ObjectNode toolsCall(String selector, McpCallContext ctx, JsonRpc.Request req) {
        JsonNode params = req.params == null ? mapper.createObjectNode() : req.params;
        String name = params.path("name").asText(null);
        JsonNode args = params.has("arguments") ? params.get("arguments") : mapper.createObjectNode();
        McpTool tool = name == null ? null : registry.byName(selector, name);
        if (tool == null) {
            return JsonRpc.error(req.id, JsonRpc.METHOD_NOT_FOUND, "unknown tool: " + name);
        }
        try {
            JsonNode structured = tool.call(ctx, args);
            ObjectNode result = mapper.createObjectNode();
            ObjectNode text = result.putArray("content").addObject();
            text.put("type", "text");
            text.put("text", mapper.writeValueAsString(structured));
            result.set("structuredContent", structured);
            return JsonRpc.result(req.id, result);
        } catch (Exception e) {
            return JsonRpc.error(req.id, JsonRpc.TOOL_ERROR, name + ": " + e.getMessage());
        }
    }
}
