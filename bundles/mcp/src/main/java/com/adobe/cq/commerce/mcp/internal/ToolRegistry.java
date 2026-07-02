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
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.*;

import com.adobe.cq.commerce.mcp.McpTool;

@Component(service = ToolRegistry.class)
public class ToolRegistry {
    private final List<McpTool> tools = new ArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindTool(McpTool tool) {
        synchronized (tools) {
            tools.add(tool);
        }
    }

    public void unbindTool(McpTool tool) {
        synchronized (tools) {
            tools.remove(tool);
        }
    }

    public List<McpTool> forSelector(String selector) {
        boolean authoring = "mcp-authoring".equals(selector);
        synchronized (tools) {
            return tools.stream().filter(t -> authoring || !t.writesContent()).collect(Collectors.toList());
        }
    }

    public McpTool byName(String selector, String name) {
        return forSelector(selector).stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }
}
