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
package com.adobe.cq.commerce.mcp;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.*;

public class JsonRpcTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parsesRequest() throws Exception {
        JsonRpc.Request r = JsonRpc.parse(mapper,
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}");
        assertEquals("2.0", r.jsonrpc);
        assertEquals("tools/list", r.method);
        assertEquals(1, r.id.asInt());
    }

    @Test
    public void buildsErrorEnvelope() {
        ObjectNode err = JsonRpc.error(mapper.getNodeFactory().numberNode(7), JsonRpc.METHOD_NOT_FOUND,
            "no such method");
        assertEquals("2.0", err.get("jsonrpc").asText());
        assertEquals(7, err.get("id").asInt());
        assertEquals(-32601, err.get("error").get("code").asInt());
        assertEquals("no such method", err.get("error").get("message").asText());
    }

    @Test
    public void buildsResultEnvelope() {
        ObjectNode result = mapper.createObjectNode().put("ok", true);
        ObjectNode env = JsonRpc.result(mapper.getNodeFactory().numberNode(9), result);
        assertEquals(9, env.get("id").asInt());
        assertTrue(env.get("result").get("ok").asBoolean());
    }
}
