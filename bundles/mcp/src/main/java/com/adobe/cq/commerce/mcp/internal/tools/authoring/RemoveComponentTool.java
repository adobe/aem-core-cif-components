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
package com.adobe.cq.commerce.mcp.internal.tools.authoring;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool ({@code remove_component}) deleting a component instance (and its entire subtree) inside a
 * page's {@code jcr:content} subtree, via the caller's {@link ResourceResolver} so JCR ACLs are enforced.
 * <p>
 * The removal boundary is enforced by {@link SiteAppsSupport#requireInsidePageContent}: the target must be
 * strictly inside a page's {@code jcr:content} subtree, so a page's {@code jcr:content} node itself, a page node,
 * or anything outside a page fails closed (page deletion is deliberately out of scope for this tool). Supports
 * {@code dryRun} to preview the would-be removal, and the commit is verified by re-reading the path.
 */
@Component(service = McpTool.class)
public class RemoveComponentTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "remove_component";
    }

    @Override
    public String description() {
        return "Remove a component (and its entire subtree) from inside a page's jcr:content subtree. The target "
            + "must be a component resource inside a page -- a page or a page's jcr:content node itself cannot be "
            + "removed with this tool. Supports dryRun to preview the removal without persisting anything.";
    }

    @Override
    public boolean writesContent() {
        return true;
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("path").put("type", "string");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("path");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        boolean dryRun = args.path("dryRun").asBoolean(false);

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = CommerceWriteSupport.resolveContainer(resolver, "path", path);
        SiteAppsSupport.requireInsidePageContent(target, "path");

        ObjectNode out = mapper.createObjectNode();
        out.put("path", target.getPath());
        out.put("resourceType", target.getResourceType());

        if (dryRun) {
            out.put("dryRun", true);
            out.put("removed", false);
            return out;
        }

        resolver.delete(target);
        resolver.commit();

        // Post-write verification: re-read the path so we never report success for a delete that did not take.
        if (resolver.getResource(path) != null) {
            throw new IllegalStateException("failed to verify removed component: " + path);
        }

        out.put("dryRun", false);
        out.put("removed", true);
        return out;
    }
}
