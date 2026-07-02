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

import org.junit.*;

import com.adobe.cq.commerce.mcp.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.*;

public class JsonRpcDispatcherTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonRpcDispatcher dispatcher;

    @Before
    public void setup() {
        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(new McpTool() {
            public String name() {
                return "ping";
            }

            public String description() {
                return "ping tool";
            }

            public ObjectNode inputSchema() {
                return mapper.createObjectNode().put("type", "object");
            }

            public JsonNode call(McpCallContext c, JsonNode a) {
                return mapper.createObjectNode().put("pong", true);
            }
        });
        dispatcher = new JsonRpcDispatcher(mapper, reg);
    }

    private JsonRpc.Request req(String method, String params) throws Exception {
        return JsonRpc.parse(mapper,
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}");
    }

    @Test
    public void initializeReportsToolsCapability() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null, req("initialize", "{}"));
        assertEquals("2025-06-18", out.get("result").get("protocolVersion").asText());
        assertTrue(out.get("result").get("capabilities").has("tools"));
    }

    @Test
    public void toolsListReturnsVisibleTools() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null, req("tools/list", "{}"));
        assertEquals(1, out.get("result").get("tools").size());
        assertEquals("ping", out.get("result").get("tools").get(0).get("name").asText());
    }

    @Test
    public void toolsCallInvokesTool() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null,
            req("tools/call", "{\"name\":\"ping\",\"arguments\":{}}"));
        assertTrue(out.get("result").get("structuredContent").get("pong").asBoolean());
    }

    @Test
    public void unknownToolReturnsMethodNotFound() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null,
            req("tools/call", "{\"name\":\"nope\",\"arguments\":{}}"));
        assertEquals(-32601, out.get("error").get("code").asInt());
    }

    @Test
    public void unknownMethodReturnsMethodNotFound() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null, req("frobnicate", "{}"));
        assertEquals(-32601, out.get("error").get("code").asInt());
    }
}
