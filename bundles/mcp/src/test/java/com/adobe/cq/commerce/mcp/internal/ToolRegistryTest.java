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

    /** A read-only tool (writesContent==false) that nonetheless declares itself authoring-only. */
    private McpTool authoringReadTool(String n) {
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

            public boolean authoringOnly() {
                return true;
            }

            public JsonNode call(McpCallContext c, JsonNode a) {
                return a;
            }
        };
    }

    /** A guest commerce-journey tool (cart/checkout/order) -- shopper-only, excluded from authoring. */
    private McpTool commerceTool(String n) {
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

            public boolean commerceJourney() {
                return true;
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

    @Test
    public void shopperSelectorHidesAuthoringOnlyReadTools() {
        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(tool("search_products", false));
        reg.bindTool(authoringReadTool("list_specific_pages")); // read-only, but authoring-only

        List<McpTool> shopper = reg.forSelector("mcp");
        assertEquals(1, shopper.size());
        assertEquals("search_products", shopper.get(0).name());
        assertNull(reg.byName("mcp", "list_specific_pages"));
        // the authoring endpoint still serves it
        assertEquals(2, reg.forSelector("mcp-authoring").size());
        assertNotNull(reg.byName("mcp-authoring", "list_specific_pages"));
    }

    @Test
    public void authoringOnlyDefaultsToWritesContent() {
        assertTrue(tool("configure_product_component", true).authoringOnly());
        assertFalse(tool("search_products", false).authoringOnly());
    }

    @Test
    public void authoringSelectorHidesCommerceJourneyTools() {
        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(tool("search_products", false));
        reg.bindTool(commerceTool("place_order")); // shopper-only cart/checkout/order tool

        // shopper sees both -- commerce-journey tools are not authoringOnly()
        List<McpTool> shopper = reg.forSelector("mcp");
        assertEquals(2, shopper.size());
        assertNotNull(reg.byName("mcp", "place_order"));

        // authoring sees the catalog-read tool but not the commerce-journey tool
        List<McpTool> authoring = reg.forSelector("mcp-authoring");
        assertEquals(1, authoring.size());
        assertEquals("search_products", authoring.get(0).name());
        assertNull(reg.byName("mcp-authoring", "place_order"));
    }
}
