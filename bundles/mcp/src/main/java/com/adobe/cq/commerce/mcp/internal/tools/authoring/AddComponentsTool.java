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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
 * MCP write tool ({@code add_components}) creating one or more component instances as uniquely-named children
 * under a caller-supplied container inside a page's {@code jcr:content} subtree, via the caller's
 * {@link ResourceResolver} so JCR ACLs are enforced.
 * <p>
 * Unlike the CIF-specific {@code create_product_teasers}/{@code create_product_carousels} bulk-create tools, this
 * is the generic counterpart: each component spec carries its own {@code sling:resourceType} (typically a site
 * proxy component discovered via {@code list_site_components}), validated against its {@code cq:Component}
 * definition ({@link SiteAppsSupport#resolveComponentDefinition}) so arbitrary repository paths posing as
 * component types fail closed. All specs are validated before anything is written (all-or-nothing), and the
 * commit is verified by re-reading each created node. Supports {@code dryRun} to preview the would-be node paths.
 */
@Component(service = McpTool.class)
public class AddComponentsTool implements McpTool {

    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final String RESOURCE_TYPE_PROPERTY = "sling:resourceType";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "add_components";
    }

    @Override
    public String description() {
        return "Add one or more components as new children under a container inside a page's jcr:content subtree "
            + "(e.g. the page's responsive grid -- see list_page_templates' editableContainerPath). Each component "
            + "spec has: resourceType (required; must resolve to a cq:Component definition -- use "
            + "list_site_components to pick the site's proxy components), optional name (a numeric suffix is "
            + "appended on collision), and optional properties (flat JSON object; strings, numbers, booleans, and "
            + "string arrays). Supports dryRun to preview the would-be node paths without persisting anything.";
    }

    @Override
    public boolean writesContent() {
        return true;
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("parentPath").put("type", "string");
        ObjectNode components = properties.putObject("components");
        components.put("type", "array");
        ObjectNode item = components.putObject("items");
        item.put("type", "object");
        ObjectNode itemProps = item.putObject("properties");
        itemProps.putObject("resourceType").put("type", "string");
        itemProps.putObject("name").put("type", "string");
        itemProps.putObject("properties").put("type", "object");
        item.putArray("required").add("resourceType");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("parentPath").add("components");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String parentPath = args.path("parentPath").asText(null);
        boolean dryRun = args.path("dryRun").asBoolean(false);

        JsonNode componentsNode = args.path("components");
        if (!componentsNode.isArray() || componentsNode.size() == 0) {
            throw new IllegalArgumentException("components is required and must be a non-empty array");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource parent = CommerceWriteSupport.resolveContainer(resolver, "parentPath", parentPath);
        SiteAppsSupport.requireInsidePageContent(parent, "parentPath");

        // Validate every spec before writing anything (all-or-nothing).
        List<ComponentSpec> specs = new ArrayList<ComponentSpec>();
        for (JsonNode specNode : componentsNode) {
            specs.add(toSpec(resolver, specNode));
        }

        ObjectNode out = mapper.createObjectNode();
        out.put("parentPath", parent.getPath());
        ArrayNode created = out.putArray("created");

        if (dryRun) {
            // Preview-only: compute the would-be unique sibling names without creating anything. Names already
            // "reserved" earlier in this same loop are tracked locally so a multi-component dry run previews N
            // distinct names, exactly matching what a real run would create.
            Set<String> reserved = new HashSet<String>();
            for (ComponentSpec spec : specs) {
                String uniqueName = nextPreviewName(parent, spec.baseName, reserved);
                reserved.add(uniqueName);
                ObjectNode entry = created.addObject();
                entry.put("path", parent.getPath() + "/" + uniqueName);
                entry.put("resourceType", spec.resourceType);
            }
            out.put("dryRun", true);
            return out;
        }

        for (ComponentSpec spec : specs) {
            Map<String, Object> props = new LinkedHashMap<String, Object>(spec.properties);
            props.put(RESOURCE_TYPE_PROPERTY, spec.resourceType);
            Resource child = CommerceWriteSupport.createChild(resolver, parent, spec.baseName, props);
            ObjectNode entry = created.addObject();
            entry.put("path", child.getPath());
            entry.put("resourceType", spec.resourceType);
        }
        resolver.commit();

        // Post-write verification: re-read each persisted node so we never report success for a create that did
        // not take.
        for (JsonNode entry : created) {
            Resource persisted = resolver.getResource(entry.get("path").asText());
            if (persisted == null || !entry.get("resourceType").asText()
                .equals(persisted.getValueMap().get(RESOURCE_TYPE_PROPERTY, String.class))) {
                throw new IllegalStateException("failed to verify created component: " + entry.get("path").asText());
            }
        }

        out.put("dryRun", false);
        return out;
    }

    private ComponentSpec toSpec(ResourceResolver resolver, JsonNode specNode) {
        String resourceType = specNode.path("resourceType").asText(null);
        if (StringUtils.isBlank(resourceType)) {
            throw new IllegalArgumentException("each component spec requires a resourceType");
        }
        if (SiteAppsSupport.resolveComponentDefinition(resolver, resourceType) == null) {
            throw new IllegalArgumentException("resourceType does not resolve to a cq:Component: " + resourceType);
        }

        String name = specNode.path("name").asText(null);
        if (StringUtils.isBlank(name)) {
            name = resourceType.substring(resourceType.lastIndexOf('/') + 1);
        }
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid component name: " + name);
        }

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        JsonNode propsNode = specNode.path("properties");
        if (!propsNode.isMissingNode() && !propsNode.isNull()) {
            if (!propsNode.isObject()) {
                throw new IllegalArgumentException("properties must be an object");
            }
            Iterator<Map.Entry<String, JsonNode>> fields = propsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                if (RESOURCE_TYPE_PROPERTY.equals(key) || "jcr:primaryType".equals(key)) {
                    throw new IllegalArgumentException("reserved property cannot be set via properties: " + key);
                }
                Object value = SiteAppsSupport.toJcrValue(field.getValue());
                if (value == null) {
                    throw new IllegalArgumentException(
                        "null property values are not allowed when creating a component: " + key);
                }
                properties.put(key, value);
            }
        }
        return new ComponentSpec(name, resourceType, properties);
    }

    /**
     * Computes the next dry-run preview name under {@code parent}, mirroring
     * {@link org.apache.sling.api.resource.ResourceUtil#createUniqueChildName(Resource, String)}'s numbering
     * ({@code teaser}, {@code teaser0}, {@code teaser1}, ...) but also skipping names already "reserved" earlier
     * in the same dry-run preview loop, since those aren't persisted yet and so wouldn't otherwise collide
     * against {@code parent.getChild(...)}.
     */
    private String nextPreviewName(Resource parent, String baseName, Set<String> reserved) {
        if (parent.getChild(baseName) == null && !reserved.contains(baseName)) {
            return baseName;
        }
        int i = 0;
        String candidate;
        do {
            candidate = baseName + i;
            i++;
        } while (parent.getChild(candidate) != null || reserved.contains(candidate));
        return candidate;
    }

    private static final class ComponentSpec {
        private final String baseName;
        private final String resourceType;
        private final Map<String, Object> properties;

        private ComponentSpec(String baseName, String resourceType, Map<String, Object> properties) {
            this.baseName = baseName;
            this.resourceType = resourceType;
            this.properties = properties;
        }
    }
}
