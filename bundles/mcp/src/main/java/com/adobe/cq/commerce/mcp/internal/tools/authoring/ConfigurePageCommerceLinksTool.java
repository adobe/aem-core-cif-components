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
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool that writes the nav-config pagefields ({@code cq:cifProductPage}, {@code cq:cifCategoryPage},
 * {@code cq:cifSearchResultsPage}) on a CIF structure page's {@code jcr:content}, via the caller's
 * {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * These three properties are read by {@code SiteStructureImpl} (constants {@code PN_CIF_PRODUCT_PAGE} /
 * {@code PN_CIF_CATEGORY_PAGE} / {@code PN_CIF_SEARCH_RESULTS_PAGE}) and {@code SiteNavigation} to resolve the
 * site's generic product/category/search-results pages, and are dialog fields on the structure page's
 * {@code cq:dialog} (v1/v2/v3). Each is a content path: provide it to set, or pass an empty string to remove an
 * existing value (falling back to whatever an ancestor page configures).
 * <p>
 * <strong>Scope:</strong> this tool covers only the {@code cq:cif*Page} nav-config pagefields. The
 * {@code cq:products}/{@code cq:categories} associated-content markers on a page (a different, orthogonal concern)
 * are handled by the shipped {@code tag_content_with_commerce} tool — not by this one.
 */
@Component(service = McpTool.class)
public class ConfigurePageCommerceLinksTool implements McpTool {

    // Consumed by SiteStructureImpl / SiteNavigation (constants: PN_CIF_PRODUCT_PAGE / PN_CIF_CATEGORY_PAGE /
    // PN_CIF_SEARCH_RESULTS_PAGE); also the page cq:dialog field names (v1/v2/v3, "./cq:cif*Page").
    private static final String PRODUCT_PAGE_PROPERTY = "cq:cifProductPage";
    private static final String CATEGORY_PAGE_PROPERTY = "cq:cifCategoryPage";
    private static final String SEARCH_RESULTS_PAGE_PROPERTY = "cq:cifSearchResultsPage";

    /**
     * Known CIF structure page resource types (see {@code v1/v2/v3 page.PageImpl.RESOURCE_TYPE}). Matching is done
     * via {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType}, so proxied
     * project components (e.g. Venia's) that super-type one of these are also accepted.
     */
    private static final List<String> CIF_STRUCTURE_PAGE_TYPES = Arrays.asList(
        "core/cif/components/structure/page/v1/page",
        "core/cif/components/structure/page/v2/page",
        "core/cif/components/structure/page/v3/page");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_page_commerce_links";
    }

    @Override
    public String description() {
        return "Set the nav-config pagefields (cifProductPage, cifCategoryPage, cifSearchResultsPage) on a CIF "
            + "structure page's jcr:content, so generic product/category/search-results links resolve to the given "
            + "content paths. Pass an empty string to clear a field. Covers only these three nav-config pagefields; "
            + "the cq:products/cq:categories associated-content markers are handled by tag_content_with_commerce.";
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
        properties.putObject("cifProductPage").put("type", "string");
        properties.putObject("cifCategoryPage").put("type", "string");
        properties.putObject("cifSearchResultsPage").put("type", "string");
        schema.putArray("required").add("path");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);

        boolean hasProductPage = args.has("cifProductPage");
        boolean hasCategoryPage = args.has("cifCategoryPage");
        boolean hasSearchResultsPage = args.has("cifSearchResultsPage");
        if (!hasProductPage && !hasCategoryPage && !hasSearchResultsPage) {
            throw new IllegalArgumentException(
                "at least one of cifProductPage, cifCategoryPage, cifSearchResultsPage is required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource content = CommerceWriteSupport.resolvePageContent(resolver, "path", path, CIF_STRUCTURE_PAGE_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(content, "path");

        String productPage = args.path("cifProductPage").asText(null);
        String categoryPage = args.path("cifCategoryPage").asText(null);
        String searchResultsPage = args.path("cifSearchResultsPage").asText(null);
        if (hasProductPage) {
            CommerceWriteSupport.putOrRemove(properties, PRODUCT_PAGE_PROPERTY, productPage);
        }
        if (hasCategoryPage) {
            CommerceWriteSupport.putOrRemove(properties, CATEGORY_PAGE_PROPERTY, categoryPage);
        }
        if (hasSearchResultsPage) {
            CommerceWriteSupport.putOrRemove(properties, SEARCH_RESULTS_PAGE_PROPERTY, searchResultsPage);
        }
        resolver.commit();

        // Post-write verification: re-read the persisted values so we never report success for a write that did
        // not take (the model consumes exactly these properties, validated by the resource-type check above).
        Resource persisted = resolver.getResource(content.getPath());
        boolean updated = persisted != null
            && (!hasProductPage || propertyMatches(persisted, PRODUCT_PAGE_PROPERTY, productPage))
            && (!hasCategoryPage || propertyMatches(persisted, CATEGORY_PAGE_PROPERTY, categoryPage))
            && (!hasSearchResultsPage || propertyMatches(persisted, SEARCH_RESULTS_PAGE_PROPERTY, searchResultsPage));

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("updated", updated);
        return out;
    }

    private static boolean propertyMatches(Resource persisted, String property, String expected) {
        String actual = persisted.getValueMap().get(property, String.class);
        return StringUtils.isBlank(expected) ? actual == null : expected.equals(actual);
    }
}
