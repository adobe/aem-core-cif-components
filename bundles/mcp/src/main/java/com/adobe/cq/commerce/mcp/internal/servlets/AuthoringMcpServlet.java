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

import javax.servlet.Servlet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.mcp.internal.JsonRpcDispatcher;
import com.adobe.cq.commerce.mcp.internal.StoreContextResolver;
import com.adobe.cq.commerce.mcp.internal.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component(
    service = Servlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        "sling.servlet.resourceTypes=cq:Page",
        "sling.servlet.selectors=mcp-authoring",
        "sling.servlet.extensions=json",
        "sling.servlet.methods=POST"
    })
public class AuthoringMcpServlet extends AbstractMcpServlet {

    @Reference
    private transient ToolRegistry registry;

    @Reference
    private transient StoreContextResolver resolver;

    @Activate
    void activate() {
        init(new ObjectMapper(), new JsonRpcDispatcher(new ObjectMapper(), registry), resolver);
    }

    @Override
    protected String selector() {
        return "mcp-authoring";
    }
}
