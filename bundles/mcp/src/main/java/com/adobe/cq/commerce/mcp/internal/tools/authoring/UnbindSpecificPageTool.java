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
import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.resource.ModifiableValueMap;
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
 * MCP write tool that clears a CIF structure page's specific-PDP/PLP binding, by removing all four binding
 * properties from its {@code jcr:content}, via the caller's {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * This is the write counterpart of the shipped §8.1 specific-page diagnostics (backed by
 * {@code com.adobe.cq.commerce.mcp.internal.tools.authoring.SpecificPageRouting}, which redeclares the binding-field constants from
 * the non-exported {@code SpecificPageStrategy}). No authoring-dialog affordance exists today to unset a binding
 * once set (catalog §8.2); this tool fills that gap.
 * <p>
 * <strong>Type ambiguity:</strong> the structure page resource type
 * ({@code core/cif/components/structure/page/vN/page}) does not distinguish a product page from a category page —
 * both share the same component. This tool gates only on the structure-page type; it removes whichever of the four
 * binding fields are present, regardless of whether the page was previously used as a product or category binding.
 * <p>
 * <strong>Idempotent:</strong> calling this tool on a page with no binding fields set removes nothing (an empty
 * {@code cleared} list) and reports {@code updated:false}, since nothing changed.
 */
@Component(service = McpTool.class)
public class UnbindSpecificPageTool implements McpTool {

    // Redeclared from SpecificPageRouting/SpecificPageStrategy (non-exported): the four properties a specific-page
    // binding (product or category) may be stored under.
    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";
    private static final String SELECTOR_FILTER_TYPE_PROPERTY = "selectorFilterType";
    private static final String INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories";
    private static final String PN_USE_FOR_CATEGORIES = "useForCategories";

    private static final List<String> BINDING_PROPERTIES = Arrays.asList(
        SELECTOR_FILTER_PROPERTY, SELECTOR_FILTER_TYPE_PROPERTY, INCLUDES_SUBCATEGORIES_PROPERTY, PN_USE_FOR_CATEGORIES);

    /**
     * Known CIF structure page resource types (see {@code v1/v2/v3 page.PageImpl.RESOURCE_TYPE}). Matching is done
     * via {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType}, so proxied
     * project components (e.g. Venia's) that super-type one of these are also accepted. The structure page type
     * does not distinguish product vs. category pages -- see the class javadoc.
     */
    private static final List<String> CIF_STRUCTURE_PAGE_TYPES = Arrays.asList(
        "core/cif/components/structure/page/v1/page",
        "core/cif/components/structure/page/v2/page",
        "core/cif/components/structure/page/v3/page");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "unbind_specific_page";
    }

    @Override
    public String description() {
        return "Clear a CIF structure page's specific-PDP/PLP binding by removing selectorFilter, "
            + "selectorFilterType, includesSubCategories, and useForCategories from its jcr:content. Idempotent: "
            + "calling it on an already-unbound page removes nothing.";
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
        schema.putArray("required").add("path");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource content = CommerceWriteSupport.resolvePageContent(resolver, "path", path, CIF_STRUCTURE_PAGE_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(content, "path");

        List<String> removed = new ArrayList<>();
        for (String propertyName : BINDING_PROPERTIES) {
            if (properties.containsKey(propertyName)) {
                properties.remove(propertyName);
                removed.add(propertyName);
            }
        }
        resolver.commit();

        // Post-write verification: re-read the persisted properties so we never report success for a write that
        // did not take (the resource-type check above already validated it consumes these properties).
        Resource persisted = resolver.getResource(content.getPath());
        List<String> stillPresent = new ArrayList<>();
        if (persisted != null) {
            for (String propertyName : BINDING_PROPERTIES) {
                if (persisted.getValueMap().containsKey(propertyName)) {
                    stillPresent.add(propertyName);
                }
            }
        }
        boolean updated = !removed.isEmpty() && stillPresent.isEmpty();

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        ArrayNode clearedOut = out.putArray("cleared");
        for (String propertyName : removed) {
            clearedOut.add(propertyName);
        }
        out.put("updated", updated);
        return out;
    }
}
