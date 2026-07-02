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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import com.adobe.cq.commerce.mcp.JsonRpc;
import com.adobe.cq.commerce.mcp.internal.JsonRpcDispatcher;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.StoreContextResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractMcpServlet extends SlingAllMethodsServlet {
    private static final int MAX_BODY = 65536;

    private transient ObjectMapper mapper;
    private transient JsonRpcDispatcher dispatcher;
    private transient StoreContextResolver resolver;

    protected AbstractMcpServlet() {}

    protected AbstractMcpServlet(ObjectMapper mapper, JsonRpcDispatcher dispatcher, StoreContextResolver resolver) {
        init(mapper, dispatcher, resolver);
    }

    protected final void init(ObjectMapper mapper, JsonRpcDispatcher dispatcher, StoreContextResolver resolver) {
        this.mapper = mapper;
        this.dispatcher = dispatcher;
        this.resolver = resolver;
    }

    protected abstract String selector();

    @Override
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws IOException {
        resp.sendError(405, "Use POST with a JSON-RPC body");
    }

    @Override
    protected void doPost(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws ServletException, IOException {
        String body = IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
        if (body.length() > MAX_BODY) {
            resp.sendError(413, "Request too large");
            return;
        }
        if (!resolver.isNavRoot(req)) {
            resp.sendError(404);
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        JsonRpc.Request rpc;
        try {
            rpc = JsonRpc.parse(mapper, body);
        } catch (Exception e) {
            write(resp, JsonRpc.error(null, JsonRpc.PARSE_ERROR, "parse error"));
            return;
        }
        StoreContext ctx = resolver.resolve(req);
        write(resp, dispatcher.dispatch(selector(), ctx, rpc));
    }

    private void write(SlingHttpServletResponse resp, ObjectNode node) throws IOException {
        resp.getWriter().write(mapper.writeValueAsString(node));
    }
}
