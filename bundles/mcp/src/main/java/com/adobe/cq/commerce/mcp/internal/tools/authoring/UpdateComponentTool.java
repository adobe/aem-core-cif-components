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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool ({@code update_component}) updating the flat properties of an existing component instance inside
 * a page's {@code jcr:content} subtree, via the caller's {@link ResourceResolver} so JCR ACLs are enforced.
 * <p>
 * The generic counterpart to the CIF-specific {@code configure_*} tools: any property key with a JSON {@code null}
 * value is removed, scalars/arrays are converted via {@link SiteAppsSupport#toJcrValue}, {@code jcr:primaryType}
 * is rejected, and a {@code sling:resourceType} update is allowed only when the new value resolves to a
 * {@code cq:Component} definition ({@link SiteAppsSupport#resolveComponentDefinition}) so a component cannot be
 * retyped to an arbitrary repository path. All properties are validated before anything is written, the commit is
 * verified by re-reading each value, and {@code dryRun} previews the would-be changes without persisting anything.
 */
@Component(service = McpTool.class)
public class UpdateComponentTool implements McpTool {

    private static final String RESOURCE_TYPE_PROPERTY = "sling:resourceType";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "update_component";
    }

    @Override
    public String description() {
        return "Update the properties of an existing component inside a page's jcr:content subtree. properties is "
            + "a flat JSON object: strings, numbers, booleans, and string arrays are set; a null value removes "
            + "the property. sling:resourceType may be updated only to a resolvable cq:Component (use "
            + "list_site_components); jcr:primaryType cannot be changed. Supports dryRun to preview the would-be "
            + "changes without persisting anything.";
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
        properties.putObject("properties").put("type", "object");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("path").add("properties");
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

        JsonNode propsNode = args.path("properties");
        if (!propsNode.isObject() || propsNode.size() == 0) {
            throw new IllegalArgumentException("properties is required and must be a non-empty object");
        }

        // Validate everything before writing anything (all-or-nothing).
        Map<String, Object> toSet = new LinkedHashMap<String, Object>();
        Map<String, Object> toRemove = new LinkedHashMap<String, Object>();
        Iterator<Map.Entry<String, JsonNode>> fields = propsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            if ("jcr:primaryType".equals(key)) {
                throw new IllegalArgumentException("reserved property cannot be changed: " + key);
            }
            Object value = SiteAppsSupport.toJcrValue(field.getValue());
            if (value == null) {
                toRemove.put(key, null);
                continue;
            }
            if (RESOURCE_TYPE_PROPERTY.equals(key)
                && SiteAppsSupport.resolveComponentDefinition(resolver, value.toString()) == null) {
                throw new IllegalArgumentException(
                    "sling:resourceType does not resolve to a cq:Component: " + value);
            }
            toSet.put(key, value);
        }

        ObjectNode out = mapper.createObjectNode();
        out.put("path", target.getPath());
        ArrayNode set = out.putArray("set");
        for (String key : toSet.keySet()) {
            set.add(key);
        }
        ArrayNode removed = out.putArray("removed");
        for (String key : toRemove.keySet()) {
            removed.add(key);
        }

        if (dryRun) {
            out.put("dryRun", true);
            return out;
        }

        ModifiableValueMap map = CommerceWriteSupport.mutableMap(target, "path");
        for (Map.Entry<String, Object> entry : toSet.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        for (String key : toRemove.keySet()) {
            map.remove(key);
        }
        resolver.commit();

        // Post-write verification: re-read every changed property so we never report success for a write that did
        // not take.
        ValueMap persisted = resolver.getResource(target.getPath()).getValueMap();
        for (Map.Entry<String, Object> entry : toSet.entrySet()) {
            if (!valueEquals(entry.getValue(), persisted.get(entry.getKey()))) {
                throw new IllegalStateException("failed to verify updated property: " + entry.getKey());
            }
        }
        for (String key : toRemove.keySet()) {
            if (persisted.containsKey(key)) {
                throw new IllegalStateException("failed to verify removed property: " + key);
            }
        }

        out.put("dryRun", false);
        out.put("updated", true);
        return out;
    }

    private static boolean valueEquals(Object expected, Object actual) {
        if (expected instanceof String[] && actual instanceof String[]) {
            return Arrays.equals((String[]) expected, (String[]) actual);
        }
        return Objects.equals(expected, actual);
    }
}
