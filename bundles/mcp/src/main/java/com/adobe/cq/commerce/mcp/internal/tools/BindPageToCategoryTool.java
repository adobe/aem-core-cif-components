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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
 * MCP write tool that binds a CIF structure page (typed as a <strong>category</strong> page) as the dedicated PLP
 * for a specific category, by writing its {@code selectorFilter}/{@code selectorFilterType}/
 * {@code includesSubCategories} properties, via the caller's {@link ResourceResolver} so that JCR ACLs are
 * enforced.
 * <p>
 * This is the write counterpart of the shipped §8.1 specific-page diagnostics (backed by
 * {@code com.adobe.cq.commerce.mcp.internal.SpecificPageRouting}, which redeclares the binding-field constants from
 * the non-exported {@code SpecificPageStrategy}). For a <em>category</em> page, the single {@code selectorFilter}
 * entry is pipe-encoded as {@code categoryUid + "|" + urlPath} (the {@code uid|urlPath} format
 * {@code SpecificPageRouting.parseSelectorFilter} parses when {@code selectorFilterType == "uidAndUrlPath"}),
 * never a plain slug (that encoding is a <em>product</em>-page-only concern, handled by the separate
 * {@code bind_page_to_products} tool).
 * <p>
 * <strong>Type ambiguity:</strong> the structure page resource type
 * ({@code core/cif/components/structure/page/vN/page}) does not distinguish a product page from a category page —
 * both share the same component. This tool gates only on the structure-page type and writes the category-style
 * ({@code uid|urlPath}) {@code selectorFilter}; the caller/author is responsible for applying it to an actual
 * category page. Binding a product page instead is a separate concern, handled by {@code bind_page_to_products}.
 */
@Component(service = McpTool.class)
public class BindPageToCategoryTool implements McpTool {

    // Redeclared from SpecificPageRouting/SpecificPageStrategy (non-exported): the properties a category page's
    // specific-page binding is stored under, and the uid|urlPath separator/default filter type.
    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";
    private static final String SELECTOR_FILTER_TYPE_PROPERTY = "selectorFilterType";
    private static final String SELECTOR_FILTER_TYPE_DEFAULT = "uidAndUrlPath";
    private static final String INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories";
    private static final String UID_AND_URL_PATH_SEPARATOR = "|";

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
        return "bind_page_to_category";
    }

    @Override
    public String description() {
        return "Bind a CIF structure page to a specific category by writing its selectorFilter "
            + "(categoryUid|urlPath, pipe-encoded), selectorFilterType (uidAndUrlPath), and includesSubCategories "
            + "properties. Note: the structure page type does not distinguish product vs. category pages -- apply "
            + "this tool only to a category page; use bind_page_to_products for a product page's binding.";
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
        properties.putObject("categoryUid").put("type", "string");
        properties.putObject("urlPath").put("type", "string");
        properties.putObject("includesSubCategories").put("type", "boolean");
        schema.putArray("required").add("path").add("categoryUid").add("urlPath");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        String categoryUid = args.path("categoryUid").asText(null);
        String urlPath = args.path("urlPath").asText(null);
        boolean includesSubCategories = args.path("includesSubCategories").asBoolean(false);

        if (StringUtils.isBlank(categoryUid) || StringUtils.isBlank(urlPath)) {
            throw new IllegalArgumentException("categoryUid and urlPath are required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource content = CommerceWriteSupport.resolvePageContent(resolver, "path", path, CIF_STRUCTURE_PAGE_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(content, "path");

        String selectorFilterEntry = categoryUid + UID_AND_URL_PATH_SEPARATOR + urlPath;
        CommerceWriteSupport.putOrRemoveArray(properties, SELECTOR_FILTER_PROPERTY, Collections.singletonList(selectorFilterEntry));
        CommerceWriteSupport.putOrRemove(properties, SELECTOR_FILTER_TYPE_PROPERTY, SELECTOR_FILTER_TYPE_DEFAULT);
        properties.put(INCLUDES_SUBCATEGORIES_PROPERTY, includesSubCategories);
        resolver.commit();

        // Post-write verification: re-read the persisted properties so we never report success for a write that
        // did not take (the resource-type check above already validated it consumes these properties).
        Resource persisted = resolver.getResource(content.getPath());
        String[] persistedFilter = persisted == null ? null
            : persisted.getValueMap().get(SELECTOR_FILTER_PROPERTY, String[].class);
        String persistedFilterType = persisted == null ? null
            : persisted.getValueMap().get(SELECTOR_FILTER_TYPE_PROPERTY, String.class);
        boolean persistedIncludesSubCategories = persisted != null
            && persisted.getValueMap().get(INCLUDES_SUBCATEGORIES_PROPERTY, false);
        boolean updated = persisted != null
            && Arrays.asList(selectorFilterEntry).equals(persistedFilter == null ? null : Arrays.asList(persistedFilter))
            && SELECTOR_FILTER_TYPE_DEFAULT.equals(persistedFilterType)
            && persistedIncludesSubCategories == includesSubCategories;

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        ArrayNode selectorFilterOut = out.putArray("selectorFilter");
        if (persistedFilter != null) {
            for (String value : persistedFilter) {
                selectorFilterOut.add(value);
            }
        }
        out.put("includesSubCategories", persistedIncludesSubCategories);
        out.put("updated", updated);
        return out;
    }
}
