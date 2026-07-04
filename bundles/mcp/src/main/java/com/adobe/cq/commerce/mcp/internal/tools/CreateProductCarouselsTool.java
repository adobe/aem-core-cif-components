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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.CommerceWriteSupport;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool that bulk-creates CIF product carousel component instances (Tier-3 node creation) as
 * uniquely-named children under a caller-supplied container resource, one per requested carousel spec, via the
 * caller's {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * The grid/container path a page's editable components live under is template-dependent (there is no exported API
 * that returns "the" responsive grid for a page), so the author supplies {@code parentPath} explicitly -- typically
 * a resource discovered via the shipped {@code suggest_template_for_page_type} tool or by inspecting the page.
 * <p>
 * Each carousel spec is a small object -- {@code selectionType}/{@code product}/{@code category}/
 * {@code productCount} -- rather than a single flat {@code (categoryUid, count)} pair, so one call can create
 * several differently-configured carousels (some manual-product, some category-driven) in one batch; this mirrors
 * the shipped {@code configure_productcarousel_component}'s argument shape, so create and configure never diverge.
 * Each created node gets exactly the same {@code sling:resourceType}/{@code selectionType}/{@code product}/
 * {@code category}/{@code productCount} property shape {@link ConfigureProductCarouselComponentTool} writes.
 * Supports {@code dryRun} (default {@code false}): when {@code true}, the would-be node paths are computed and
 * returned, but nothing is created or committed.
 */
@Component(service = McpTool.class)
public class CreateProductCarouselsTool implements McpTool {

    /**
     * CIF product carousel component resource type (v1 only, matches {@code ProductCarouselImpl.RESOURCE_TYPE}),
     * used as the {@code sling:resourceType} of every node this tool creates.
     */
    private static final String PRODUCTCAROUSEL_RESOURCE_TYPE = "core/cif/components/commerce/productcarousel/v1/productcarousel";
    private static final String BASE_CHILD_NAME = "productcarousel";

    private static final String SELECTION_TYPE_PROPERTY = "selectionType";
    private static final String PRODUCT_PROPERTY = "product";
    private static final String CATEGORY_PROPERTY = "category";
    private static final String PRODUCT_COUNT_PROPERTY = "productCount";

    /**
     * Valid {@code selectionType} values, redeclared from {@code ProductCarouselImpl}'s internal constants (not
     * importable from here) -- same set {@link ConfigureProductCarouselComponentTool} validates against.
     */
    private static final Set<String> VALID_SELECTION_TYPES = new HashSet<String>(Arrays.asList("product", "category"));

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "create_product_carousels";
    }

    @Override
    public String description() {
        return "Bulk-create CIF product carousel components as new children under a caller-supplied container "
            + "resource, one per carousel spec (a manual product list or a category selection). Each spec mirrors "
            + "configure_productcarousel_component's shape, so a single call can create several differently "
            + "configured carousels. Supports dryRun to preview the nodes that would be created without "
            + "persisting anything.";
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

        ObjectNode carousels = properties.putObject("carousels");
        carousels.put("type", "array");
        ObjectNode carouselItem = carousels.putObject("items");
        carouselItem.put("type", "object");
        ObjectNode carouselProps = carouselItem.putObject("properties");
        ObjectNode selectionType = carouselProps.putObject("selectionType");
        selectionType.put("type", "string");
        selectionType.putArray("enum").add("product").add("category");
        ObjectNode product = carouselProps.putObject("product");
        product.put("type", "array");
        product.putObject("items").put("type", "string");
        carouselProps.putObject("category").put("type", "string");
        carouselProps.putObject("productCount").put("type", "integer");

        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("parentPath").add("carousels");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String parentPath = args.path("parentPath").asText(null);
        boolean dryRun = args.path("dryRun").asBoolean(false);

        JsonNode carouselsNode = args.path("carousels");
        if (!carouselsNode.isArray() || carouselsNode.size() == 0) {
            throw new IllegalArgumentException("carousels is required and must be a non-empty array");
        }

        List<Map<String, Object>> specs = new ArrayList<Map<String, Object>>();
        for (JsonNode carouselNode : carouselsNode) {
            specs.add(toProps(carouselNode));
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource parent = CommerceWriteSupport.resolveContainer(resolver, "parentPath", parentPath);

        ObjectNode out = mapper.createObjectNode();
        out.put("parentPath", parentPath);
        ArrayNode created = out.putArray("created");

        if (dryRun) {
            // Preview-only: compute the would-be unique sibling names without creating anything. Names already
            // "reserved" earlier in this same loop are tracked locally so a multi-spec dry run previews N distinct
            // names, exactly matching what a real run would create.
            Set<String> reserved = new HashSet<String>();
            for (int i = 0; i < specs.size(); i++) {
                String uniqueName = nextPreviewName(parent, reserved);
                reserved.add(uniqueName);
                ObjectNode item = created.addObject();
                item.put("path", parent.getPath() + "/" + uniqueName);
            }
            out.put("dryRun", true);
            return out;
        }

        for (Map<String, Object> props : specs) {
            Resource child = CommerceWriteSupport.createChild(resolver, parent, BASE_CHILD_NAME, props);
            ObjectNode item = created.addObject();
            item.put("path", child.getPath());
        }
        resolver.commit();

        // Post-write verification: re-read each persisted node so we never report success for a create that did
        // not take.
        for (JsonNode item : created) {
            Resource persisted = resolver.getResource(item.get("path").asText());
            if (persisted == null || !PRODUCTCAROUSEL_RESOURCE_TYPE
                .equals(persisted.getValueMap().get("sling:resourceType", String.class))) {
                throw new IllegalStateException("failed to verify created product carousel: " + item.get("path").asText());
            }
        }

        out.put("dryRun", false);
        return out;
    }

    /**
     * Builds the property map for one carousel spec: validates {@code selectionType} when provided, normalizes
     * {@code product} entries to combinedSku, and integral-guards {@code productCount} (a fractional JSON number
     * must not silently truncate, per the same pitfall documented for {@code quantity} handling in AGENTS.md §8).
     *
     * @param carouselNode one element of the {@code carousels} array
     * @return the properties to write on the new child, including {@code sling:resourceType}
     */
    private Map<String, Object> toProps(JsonNode carouselNode) {
        String selectionType = carouselNode.path("selectionType").asText(null);
        if (selectionType != null && !VALID_SELECTION_TYPES.contains(selectionType)) {
            throw new IllegalArgumentException("selectionType must be one of " + VALID_SELECTION_TYPES + ": " + selectionType);
        }

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling:resourceType", PRODUCTCAROUSEL_RESOURCE_TYPE);

        if (StringUtils.isNotBlank(selectionType)) {
            props.put(SELECTION_TYPE_PROPERTY, selectionType);
        }

        JsonNode productNode = carouselNode.path("product");
        if (productNode.isArray() && productNode.size() > 0) {
            List<String> normalizedSkus = new ArrayList<String>();
            for (JsonNode sku : productNode) {
                if (sku.isNull() || StringUtils.isBlank(sku.asText())) {
                    continue;
                }
                normalizedSkus.add(CombinedSku.parse(sku.asText()).toString());
            }
            if (!normalizedSkus.isEmpty()) {
                props.put(PRODUCT_PROPERTY, normalizedSkus.toArray(new String[0]));
            }
        }

        String category = carouselNode.path("category").asText(null);
        if (StringUtils.isNotBlank(category)) {
            props.put(CATEGORY_PROPERTY, category);
        }

        JsonNode productCountNode = carouselNode.path("productCount");
        if (productCountNode.isIntegralNumber()) {
            props.put(PRODUCT_COUNT_PROPERTY, Integer.valueOf(productCountNode.asInt()));
        }

        return props;
    }

    /**
     * Computes the next dry-run preview name under {@code parent}, mirroring
     * {@link org.apache.sling.api.resource.ResourceUtil#createUniqueChildName(Resource, String)}'s numbering
     * ({@code productcarousel}, {@code productcarousel0}, {@code productcarousel1}, ...) but also skipping names
     * already "reserved" earlier in the same dry-run preview loop, since those aren't persisted yet and so
     * wouldn't otherwise collide against {@code parent.getChild(...)}.
     *
     * @param parent the container resource the preview name is computed under
     * @param reserved names already handed out earlier in this dry-run call
     * @return a name not currently used as a real child of {@code parent} and not already reserved
     */
    private String nextPreviewName(Resource parent, Set<String> reserved) {
        if (parent.getChild(BASE_CHILD_NAME) == null && !reserved.contains(BASE_CHILD_NAME)) {
            return BASE_CHILD_NAME;
        }
        int i = 0;
        String candidate;
        do {
            candidate = BASE_CHILD_NAME + i;
            i++;
        } while (parent.getChild(candidate) != null || reserved.contains(candidate));
        return candidate;
    }
}
