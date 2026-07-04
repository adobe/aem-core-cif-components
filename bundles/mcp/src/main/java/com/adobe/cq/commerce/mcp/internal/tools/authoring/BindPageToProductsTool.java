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
 * MCP write tool that binds a CIF structure page (typed as a <strong>product</strong> page) to a specific set of
 * products by writing its {@code selectorFilter} property, via the caller's {@link ResourceResolver} so that JCR
 * ACLs are enforced.
 * <p>
 * This is the write counterpart of the shipped §8.1 specific-page diagnostics (backed by
 * {@code com.adobe.cq.commerce.mcp.internal.tools.authoring.SpecificPageRouting}, which redeclares the binding-field constants from
 * the non-exported {@code SpecificPageStrategy}). For a <em>product</em> page, {@code selectorFilter} entries use
 * dialog {@code selectionId="slug"} — i.e. plain product SKUs/URL keys/slugs, never pipe-encoded
 * ({@code uid|urlPath}) and never {@code CombinedSku}-encoded. That pipe/combinedSku encoding only applies to
 * <em>category</em>-page bindings (a separate tool, {@code bind_page_to_category}).
 * <p>
 * <strong>Type ambiguity:</strong> the structure page resource type
 * ({@code core/cif/components/structure/page/vN/page}) does not distinguish a product page from a category page —
 * both share the same component. This tool gates only on the structure-page type and writes the product-style
 * (plain-slug) {@code selectorFilter}; the caller/author is responsible for applying it to an actual product page.
 * Binding a category page instead is a separate concern, handled by {@code bind_page_to_category}.
 */
@Component(service = McpTool.class)
public class BindPageToProductsTool implements McpTool {

    // Redeclared from SpecificPageRouting/SpecificPageStrategy (non-exported): the property a product page's
    // specific-page binding is stored under.
    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";

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
        return "bind_page_to_products";
    }

    @Override
    public String description() {
        return "Bind a CIF structure page to a specific set of products by writing its selectorFilter property "
            + "(plain SKUs/URL keys/slugs, not pipe-encoded). Pass an empty skusOrUrlKeys array to clear an "
            + "existing binding. Note: the structure page type does not distinguish product vs. category pages -- "
            + "apply this tool only to a product page; use bind_page_to_category for a category page's binding.";
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
        ObjectNode skusOrUrlKeys = properties.putObject("skusOrUrlKeys");
        skusOrUrlKeys.put("type", "array");
        skusOrUrlKeys.putObject("items").put("type", "string");
        schema.putArray("required").add("path").add("skusOrUrlKeys");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);

        if (!args.has("skusOrUrlKeys") || !args.path("skusOrUrlKeys").isArray()) {
            throw new IllegalArgumentException("skusOrUrlKeys (array) is required");
        }

        List<String> entries = new ArrayList<>();
        for (JsonNode entry : (ArrayNode) args.path("skusOrUrlKeys")) {
            if (!entry.isNull() && !entry.asText().trim().isEmpty()) {
                entries.add(entry.asText());
            }
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource content = CommerceWriteSupport.resolvePageContent(resolver, "path", path, CIF_STRUCTURE_PAGE_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(content, "path");

        CommerceWriteSupport.putOrRemoveArray(properties, SELECTOR_FILTER_PROPERTY, entries);
        resolver.commit();

        // Post-write verification: re-read the persisted selectorFilter so we never report success for a write
        // that did not take (the resource-type check above already validated it consumes this property).
        Resource persisted = resolver.getResource(content.getPath());
        String[] persistedFilter = persisted == null ? null
            : persisted.getValueMap().get(SELECTOR_FILTER_PROPERTY, String[].class);
        boolean updated = persisted != null && selectorFilterMatches(persistedFilter, entries);

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        ArrayNode selectorFilterOut = out.putArray("selectorFilter");
        if (persistedFilter != null) {
            for (String value : persistedFilter) {
                selectorFilterOut.add(value);
            }
        }
        out.put("updated", updated);
        return out;
    }

    private static boolean selectorFilterMatches(String[] actual, List<String> expected) {
        if (expected == null || expected.isEmpty()) {
            return actual == null;
        }
        return actual != null && expected.equals(Arrays.asList(actual));
    }
}
