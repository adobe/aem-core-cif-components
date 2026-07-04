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
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.CommerceWriteSupport;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool controlling which sections a CIF product component renders, via the caller's
 * {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * This operates on the same {@code product} component resource as the shipped
 * {@code ConfigureProductComponentTool}, but a different, orthogonal concern (section visibility rather than SKU
 * binding), hence its own operation-scoped tool name.
 * <p>
 * {@code visibleSections} (the model's {@code PN_VISIBLE_SECTIONS}) is read by {@code ProductImpl} (v1/v2/v3) as a
 * {@code String[]} of <strong>lowercase</strong> tokens mapped to the uppercase {@code Product.*_SECTION}
 * constants; note {@code images} is plural even though the underlying constant is {@code IMAGE_SECTION}. An empty
 * (or absent) property makes the model fall back to the style/policy default (all sections), so this tool removes
 * the property when given an empty array rather than persisting an empty one.
 */
@Component(service = McpTool.class)
public class ConfigureProductVisibleSectionsTool implements McpTool {

    private static final String VISIBLE_SECTIONS_PROPERTY = "visibleSections";

    /**
     * Known CIF product component resource types (see {@code ProductImpl.RESOURCE_TYPE}, v1/v2/v3). Matching is
     * done via {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType}, so proxied
     * project components (e.g. Venia's) that super-type one of these are also accepted.
     */
    private static final List<String> CIF_PRODUCT_COMPONENT_TYPES = Arrays.asList(
        "core/cif/components/commerce/product/v1/product",
        "core/cif/components/commerce/product/v2/product",
        "core/cif/components/commerce/product/v3/product");

    /**
     * Valid {@code visibleSections} dialog tokens, redeclared from {@code v3.product.ProductImpl.SECTIONS_MAP} keys
     * (internal to {@code bundles/core}, not importable from here). Lowercase; note {@code images} is plural.
     */
    private static final Set<String> VALID_SECTIONS = new LinkedHashSet<>(Arrays.asList(
        "title", "price", "sku", "images", "options", "quantity", "actions", "description", "details"));

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_product_visible_sections";
    }

    @Override
    public String description() {
        return "Set which sections (title, price, sku, images, options, quantity, actions, description, details) "
            + "a CIF product component renders; an empty array clears the override and falls back to the "
            + "style/policy default.";
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
        ObjectNode visibleSections = properties.putObject("visibleSections");
        visibleSections.put("type", "array");
        ObjectNode items = visibleSections.putObject("items");
        items.put("type", "string");
        ArrayNode enumNode = items.putArray("enum");
        for (String section : VALID_SECTIONS) {
            enumNode.add(section);
        }
        schema.putArray("required").add("path").add("visibleSections");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);

        JsonNode visibleSectionsArg = args.path("visibleSections");
        if (!visibleSectionsArg.isArray()) {
            throw new IllegalArgumentException("visibleSections (array of strings) is required");
        }
        List<String> visibleSections = new ArrayList<>();
        for (JsonNode sectionNode : visibleSectionsArg) {
            String section = sectionNode.asText(null);
            if (section == null || !VALID_SECTIONS.contains(section)) {
                throw new IllegalArgumentException("visibleSections must only contain " + VALID_SECTIONS + ": " + section);
            }
            visibleSections.add(section);
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = CommerceWriteSupport.resolveComponent(resolver, "path", path, CIF_PRODUCT_COMPONENT_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(target, "path");

        if (visibleSections.isEmpty()) {
            properties.remove(VISIBLE_SECTIONS_PROPERTY);
        } else {
            properties.put(VISIBLE_SECTIONS_PROPERTY, visibleSections.toArray(new String[0]));
        }
        resolver.commit();

        // Post-write verification: re-read the persisted value so we never report success for a write that did
        // not take (the model consumes exactly this property, validated by the resource-type check above).
        Resource persisted = resolver.getResource(path);
        String[] persistedSections = persisted == null ? null
            : persisted.getValueMap().get(VISIBLE_SECTIONS_PROPERTY, String[].class);
        boolean updated = persisted != null
            && (visibleSections.isEmpty() ? persistedSections == null
                : Arrays.equals(
                    visibleSections.toArray(new String[0]), persistedSections));

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        ArrayNode visibleSectionsOut = out.putArray("visibleSections");
        for (String section : visibleSections) {
            visibleSectionsOut.add(section);
        }
        out.put("updated", updated);
        return out;
    }
}
