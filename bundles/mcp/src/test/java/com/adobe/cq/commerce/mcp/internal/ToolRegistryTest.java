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

import java.util.*;

import org.junit.Test;

import com.adobe.cq.commerce.mcp.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.*;

public class ToolRegistryTest {
    private McpTool tool(String n, boolean writes) {
        return new McpTool() {
            public String name() {
                return n;
            }

            public String description() {
                return n;
            }

            public ObjectNode inputSchema() {
                return new ObjectMapper().createObjectNode();
            }

            public boolean writesContent() {
                return writes;
            }

            public JsonNode call(McpCallContext c, JsonNode a) {
                return a;
            }
        };
    }

    @Test
    public void shopperSelectorHidesWriteTools() {
        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(tool("search_products", false));
        reg.bindTool(tool("configure_product_component", true));

        List<McpTool> shopper = reg.forSelector("mcp");
        assertEquals(1, shopper.size());
        assertEquals("search_products", shopper.get(0).name());
        assertEquals(2, reg.forSelector("mcp-authoring").size());
        assertNull(reg.byName("mcp", "configure_product_component"));
        assertNotNull(reg.byName("mcp-authoring", "configure_product_component"));
    }
}
