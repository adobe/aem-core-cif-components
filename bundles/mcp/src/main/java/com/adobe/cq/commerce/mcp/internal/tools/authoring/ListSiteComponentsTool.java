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

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP read tool ({@code list_site_components}, authoring-only) listing the site's own components under
 * {@code /apps/<site>/components} -- primarily the site's <em>proxy</em> components (a {@code cq:Component} whose
 * {@code sling:resourceSuperType} points at a core/CIF component), which are what pages of the site should
 * reference as {@code sling:resourceType} so the site's policies/styles apply. For each component:
 * {@code resourceType} (the {@code /apps/}-relative path, i.e. the value to write as {@code sling:resourceType}),
 * {@code title}, {@code group} ({@code componentGroup}), and {@code resourceSuperType}.
 * <p>
 * The {@code appsPath} argument is optional: when absent, the site's {@code /apps/<site>/components} root is
 * derived from the endpoint's own nav-root page ({@link SiteAppsSupport#appsComponentsPathFor}). Components whose
 * {@code componentGroup} starts with {@code .} (e.g. {@code .hidden}) are structural/hidden and are skipped unless
 * {@code includeHidden} is {@code true}; the optional {@code group} argument filters to one exact
 * {@code componentGroup}.
 */
@Component(service = McpTool.class)
public class ListSiteComponentsTool implements McpTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "list_site_components";
    }

    @Override
    public String description() {
        return "List the site's own components under /apps/<site>/components (typically proxy components whose "
            + "sling:resourceSuperType points at a core/CIF component). Returns for each: resourceType (the value "
            + "to use as sling:resourceType in add_components/update_component so site policies and styles apply), "
            + "title, group (componentGroup), and resourceSuperType. Optional appsPath (e.g. "
            + "/apps/venia/components); when absent it is derived from the endpoint's own nav-root page. Hidden "
            + "components (componentGroup starting with '.') are skipped unless includeHidden is true; optional "
            + "group filters to one exact componentGroup.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("appsPath").put("type", "string");
        properties.putObject("group").put("type", "string");
        properties.putObject("includeHidden").put("type", "boolean");
        return schema;
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        String appsPath = args.path("appsPath").asText(null);
        String groupFilter = args.path("group").asText(null);
        boolean includeHidden = args.path("includeHidden").asBoolean(false);

        if (StringUtils.isBlank(appsPath)) {
            appsPath = SiteAppsSupport.appsComponentsPathFor(ctx.getLandingPage());
            if (appsPath == null) {
                throw new IllegalArgumentException(
                    "appsPath is required: no site-specific /apps/<site>/components root could be derived from the endpoint's nav-root page");
            }
        } else if (!appsPath.startsWith("/apps/")) {
            throw new IllegalArgumentException("appsPath must be under /apps: " + appsPath);
        }

        Resource root = resolver.getResource(appsPath);
        if (root == null) {
            throw new IllegalArgumentException("appsPath not found: " + appsPath);
        }

        ObjectNode out = mapper.createObjectNode();
        out.put("appsPath", appsPath);
        ArrayNode components = out.putArray("components");

        Deque<Resource> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Resource current = stack.pop();
            for (Resource child : current.getChildren()) {
                if (!"cq:Component".equals(child.getValueMap().get("jcr:primaryType", String.class))) {
                    stack.push(child); // component folder (sling:Folder etc.) -- recurse
                    continue;
                }
                String group = child.getValueMap().get("componentGroup", String.class);
                if (groupFilter != null ? !groupFilter.equals(group)
                    : (!includeHidden && group != null && group.startsWith("."))) {
                    continue;
                }
                ObjectNode entry = components.addObject();
                entry.put("resourceType", child.getPath().substring("/apps/".length()));
                entry.put("title", child.getValueMap().get("jcr:title", String.class));
                if (group != null) {
                    entry.put("group", group);
                }
                String superType = child.getValueMap().get("sling:resourceSuperType", String.class);
                if (superType != null) {
                    entry.put("resourceSuperType", superType);
                }
            }
        }
        return out;
    }
}
