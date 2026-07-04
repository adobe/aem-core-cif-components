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
 * MCP write tool that binds a CIF structure page (typed as a <strong>product</strong> page) as the dedicated custom
 * PDP for every product under a whole category/tree, by writing its {@code useForCategories}/
 * {@code includesSubCategories} properties, via the caller's {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * This is the write counterpart of the shipped §8.1 specific-page diagnostics (backed by
 * {@code com.adobe.cq.commerce.mcp.internal.SpecificPageRouting}, which redeclares the binding-field constants from
 * the non-exported {@code SpecificPageStrategy}).
 * <p>
 * <strong>Verified {@code useForCategories} entry format (source-confirmed, NOT the {@code uid|urlPath} pipe format
 * used by a category page's {@code selectorFilter}):</strong>
 * {@code SpecificPageStrategy.isSpecificPageFor(Page, ProductUrlFormat.Params)} (`bundles/core`,
 * {@code internal/services/SpecificPageStrategy.java:149-166}) reads {@code useForCategories} straight off the
 * property (
 * {@code String[] categoryUrlPaths = properties.get(PN_USE_FOR_CATEGORIES, new String[0]);}) and compares each
 * entry <em>directly</em> against the incoming category url-path/url-key via {@code matchesUrlPath}/
 * {@code matchesUrlKey} — there is no {@code "|"}-splitting step anywhere in that method, unlike
 * {@code isSpecificPageFor(Page, CategoryUrlFormat.Params)}'s {@code selectorFilter} handling (lines 192-225), which
 * explicitly parses {@code uidAndUrlPath}-typed filters on the pipe separator. So each {@code useForCategories}
 * entry this tool writes is the <strong>plain {@code urlPath}</strong> (e.g. {@code "venia-tops"}), never
 * {@code categoryUid + "|" + urlPath}. {@code categoryUid} is accepted as a required input for API symmetry with
 * {@code bind_page_to_category} and validated, but is intentionally not encoded into the persisted value because the
 * resolver never reads a uid out of this property.
 * <p>
 * <strong>Version note:</strong> {@code useForCategories} only takes effect on the v2+ structure-page component
 * (absent/ignored in v1) — see catalog §8.1's version-differences note. This tool does not gate on component
 * version; it writes regardless, and it is the author's responsibility to apply it to a v2+ product page for the
 * binding to actually take effect at render/resolution time.
 * <p>
 * <strong>Type ambiguity:</strong> the structure page resource type
 * ({@code core/cif/components/structure/page/vN/page}) does not distinguish a product page from a category page —
 * both share the same component. This tool gates only on the structure-page type and writes the
 * {@code useForCategories} product-page binding; the caller/author is responsible for applying it to an actual
 * product page. Binding a single category page instead is a separate concern, handled by {@code bind_page_to_category}.
 */
@Component(service = McpTool.class)
public class BindProductPageToCategoryTreeTool implements McpTool {

    // Redeclared from SpecificPageRouting/SpecificPageStrategy (non-exported): the property a product page's
    // whole-category-tree binding is stored under, and the includesSubCategories flag.
    private static final String PN_USE_FOR_CATEGORIES = "useForCategories";
    private static final String INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories";

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
        return "bind_product_page_to_category_tree";
    }

    @Override
    public String description() {
        return "Bind a CIF structure page (a PRODUCT page) as the dedicated custom PDP for every product under a "
            + "whole category tree, by writing its useForCategories (plain urlPath, v2+ only) and "
            + "includesSubCategories properties. Note: the structure page type does not distinguish product vs. "
            + "category pages -- apply this tool only to a product page; use bind_page_to_category for a single "
            + "category page's binding.";
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

        // VERIFIED format: plain urlPath, not categoryUid + "|" + urlPath -- see class javadoc.
        CommerceWriteSupport.putOrRemoveArray(properties, PN_USE_FOR_CATEGORIES, Collections.singletonList(urlPath));
        properties.put(INCLUDES_SUBCATEGORIES_PROPERTY, includesSubCategories);
        resolver.commit();

        // Post-write verification: re-read the persisted properties so we never report success for a write that
        // did not take (the resource-type check above already validated it consumes these properties).
        Resource persisted = resolver.getResource(content.getPath());
        String[] persistedUseForCategories = persisted == null ? null
            : persisted.getValueMap().get(PN_USE_FOR_CATEGORIES, String[].class);
        boolean persistedIncludesSubCategories = persisted != null
            && persisted.getValueMap().get(INCLUDES_SUBCATEGORIES_PROPERTY, false);
        boolean updated = persisted != null
            && Arrays.asList(urlPath).equals(persistedUseForCategories == null ? null : Arrays.asList(persistedUseForCategories))
            && persistedIncludesSubCategories == includesSubCategories;

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        ArrayNode useForCategoriesOut = out.putArray("useForCategories");
        if (persistedUseForCategories != null) {
            for (String value : persistedUseForCategories) {
                useForCategoriesOut.add(value);
            }
        }
        out.put("includesSubCategories", persistedIncludesSubCategories);
        out.put("updated", updated);
        return out;
    }
}
