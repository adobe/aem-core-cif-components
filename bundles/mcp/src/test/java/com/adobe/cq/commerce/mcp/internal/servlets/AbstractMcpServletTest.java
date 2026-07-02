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
package com.adobe.cq.commerce.mcp.internal.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.JsonRpcDispatcher;
import com.adobe.cq.commerce.mcp.internal.StoreContextResolver;
import com.adobe.cq.commerce.mcp.internal.ToolRegistry;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractMcpServletTest {
    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private TestServlet servletFor(String pagePath, String landing) {
        context.load().json("/context/venia-navroot.json", "/content");
        context.currentResource(pagePath + "/jcr:content");
        SiteStructure ss = mock(SiteStructure.class);
        when(ss.getLandingPage()).thenReturn(context.pageManager().getPage(landing));
        when(ss.getSearchResultsPage()).thenReturn(null);
        context.registerAdapter(Page.class, SiteStructure.class, ss);
        context.registerAdapter(SlingHttpServletRequest.class, MagentoGraphqlClient.class, mock(MagentoGraphqlClient.class));

        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(new McpTool() {
            public String name() {
                return "ping";
            }

            public String description() {
                return "ping";
            }

            public ObjectNode inputSchema() {
                return mapper.createObjectNode();
            }

            public JsonNode call(McpCallContext c, JsonNode a) {
                return mapper.createObjectNode().put("pong", true);
            }
        });
        return new TestServlet(mapper, new JsonRpcDispatcher(mapper, reg), new StoreContextResolver());
    }

    static class TestServlet extends AbstractMcpServlet {
        TestServlet(ObjectMapper m, JsonRpcDispatcher d, StoreContextResolver r) {
            super(m, d, r);
        }

        protected String selector() {
            return "mcp";
        }
    }

    @Test
    public void non_navroot_returns_404() throws Exception {
        TestServlet servlet = servletFor("/content/store-products", "/content/store");
        context.request().setContent("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}".getBytes("UTF-8"));
        MockSlingHttpServletResponse resp = context.response();
        servlet.doPost(context.request(), resp);
        assertEquals(404, resp.getStatus());
    }

    @Test
    public void navroot_dispatches_tools_call() throws Exception {
        TestServlet servlet = servletFor("/content/store", "/content/store");
        context.request().setContent(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"ping\",\"arguments\":{}}}".getBytes("UTF-8"));
        MockSlingHttpServletResponse resp = context.response();
        servlet.doPost(context.request(), resp);
        assertEquals(200, resp.getStatus());
        JsonNode out = mapper.readTree(resp.getOutputAsString());
        assertTrue(out.get("result").get("structuredContent").get("pong").asBoolean());
    }
}
