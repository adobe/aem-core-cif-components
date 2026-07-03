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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.SpecificPageRouting;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP read tool resolving a specific PDP/PLP page's structure-component version and reporting which specific-page
 * binding fields (catalog §8.1) are available at that version, thinly wrapping
 * {@link SpecificPageRouting#readBinding(Page)} for the (best-effort) {@code pageType} heuristic.
 * <p>
 * <b>Version derivation:</b> the structure component resourceType base
 * ({@code core/cif/components/structure/page/vN/page}, verified against {@code PageImpl} v1/v2/v3 in
 * {@code bundles/core}) is identical across product and category pages, so the version is derived purely from
 * which {@code vN} literal the page's {@code jcr:content} resource type matches — checked via
 * {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType}, so a proxied project
 * component (e.g. Venia's) that super-types one of these literals still resolves to the correct version.
 * <p>
 * <b>pageType is a heuristic, not a precise read.</b> As documented on {@link SpecificPageRouting}, the structure
 * resourceType cannot distinguish product vs. category roles (both use the same RT); this tool reuses
 * {@link SpecificPageRouting#readBinding(Page)}'s field-presence heuristic (a present {@code useForCategories} means
 * "product"; a present {@code selectorFilterType}/{@code includesSubCategories} with no {@code useForCategories}
 * means "category"; otherwise it defaults to "product") — report {@code pageType} as best-effort, not authoritative.
 * <p>
 * <b>Field-availability table (catalog §8.1 "Version differences"):</b> {@code selectorFilter} and
 * {@code selectorFilterType} exist at every version, for both page types. {@code includesSubCategories} exists on
 * category pages at every version (v1/v2/v3); its *product-page* applicability was only broadened to v2+ (it is
 * paired with {@code useForCategories}, itself v2+, so it is moot — and reported unavailable — on a v1 product
 * page). {@code useForCategories} exists only on product pages, and only from v2 onward (absent in v1).
 */
@Component(service = McpTool.class)
public class CheckSpecificPageCapabilityTool implements McpTool {

    /**
     * Known CIF specific-page structure component resource types, keyed by version, in v1/v2/v3 iteration order
     * (see {@code PageImpl.RESOURCE_TYPE} in {@code bundles/core}'s {@code v1}/{@code v2}/{@code v3}
     * {@code …structure.page} packages). Matching is done via {@link Resource#isResourceType(String)}, which
     * follows {@code sling:resourceSuperType}, so proxied project components (e.g. Venia's) that super-type one of
     * these are also accepted.
     */
    private static final Map<String, String> STRUCTURE_PAGE_VERSIONS_BY_TYPE = new LinkedHashMap<>();
    static {
        STRUCTURE_PAGE_VERSIONS_BY_TYPE.put("core/cif/components/structure/page/v1/page", "v1");
        STRUCTURE_PAGE_VERSIONS_BY_TYPE.put("core/cif/components/structure/page/v2/page", "v2");
        STRUCTURE_PAGE_VERSIONS_BY_TYPE.put("core/cif/components/structure/page/v3/page", "v3");
    }

    private static final String PAGE_TYPE_PRODUCT = "product";

    private final ObjectMapper mapper = new ObjectMapper();
    private final SpecificPageRouting specificPageRouting = new SpecificPageRouting();

    @Override
    public String name() {
        return "check_specific_page_capability";
    }

    @Override
    public String description() {
        return "Resolve a specific PDP/PLP page's structure-component version and report which specific-page "
            + "binding fields (selectorFilter, selectorFilterType, includesSubCategories, useForCategories) are "
            + "available at that version -- useForCategories and product-page includesSubCategories are v2+ only.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("path").put("type", "string").put("description",
            "Path of the specific page to check (under /content).");
        schema.putArray("required").add("path");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Page page = PathArgs.resolvePage(resolver, "path", path);

        Resource content = resolver.getResource(path + "/jcr:content");
        if (content == null) {
            throw new IllegalArgumentException("page has no jcr:content: " + path);
        }
        String version = null;
        for (Map.Entry<String, String> candidate : STRUCTURE_PAGE_VERSIONS_BY_TYPE.entrySet()) {
            if (content.isResourceType(candidate.getKey())) {
                version = candidate.getValue();
                break;
            }
        }
        if (version == null) {
            throw new IllegalArgumentException("resource is not a CIF specific-page structure component: " + path);
        }

        SpecificPageRouting.Binding binding = specificPageRouting.readBinding(page);
        String pageType = binding.getPageType();
        boolean isV1 = "v1".equals(version);
        boolean isProduct = PAGE_TYPE_PRODUCT.equals(pageType);

        // useForCategories: product-page-only field, v2+ only (absent in v1).
        boolean useForCategoriesAvailable = isProduct && !isV1;
        // includesSubCategories: exists on category pages at every version; on product pages its applicability
        // was only broadened to v2+ (paired with useForCategories, itself v2+), so it is unavailable on a v1
        // product page.
        boolean includesSubCategoriesAvailable = !(isProduct && isV1);

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("pageType", pageType);
        out.put("componentVersion", version);

        ObjectNode fields = out.putObject("fields");
        fields.put("selectorFilter", true);
        fields.put("selectorFilterType", true);
        fields.put("includesSubCategories", includesSubCategoriesAvailable);
        fields.put("useForCategories", useForCategoriesAvailable);

        return out;
    }
}
