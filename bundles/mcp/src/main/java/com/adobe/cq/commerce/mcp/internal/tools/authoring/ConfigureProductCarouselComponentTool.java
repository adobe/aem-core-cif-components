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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool that configures a CIF product carousel component's selection: either a manual list of products
 * ({@code selectionType=product}) or a category ({@code selectionType=category}), via the caller's
 * {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * This is the fuller carousel-specific counterpart of the shipped {@code configure_productlist_component}, which
 * only writes the {@code category} property (accepted there for productcarousel v1 too, category-only). Use this
 * tool when a carousel needs manual product selection or an explicit {@code productCount}.
 * <p>
 * {@code product} entries are combinedSku-normalized via {@link CombinedSku} (base sku, or {@code base#variant}
 * for a configurable-product variant) before being written as a flat multi-valued {@code String[]} — matching
 * {@code ProductCarouselImpl}'s {@code product[]} manual-selection property. {@code enableAddToCart}/
 * {@code enableAddToWishList} are style/policy properties (read from {@code currentStyle}), not component-instance
 * dialog fields, and are intentionally not exposed here.
 */
@Component(service = McpTool.class)
public class ConfigureProductCarouselComponentTool implements McpTool {

    private static final String SELECTION_TYPE_PROPERTY = "selectionType";
    private static final String PRODUCT_PROPERTY = "product";
    private static final String CATEGORY_PROPERTY = "category";
    private static final String PRODUCT_COUNT_PROPERTY = "productCount";

    /**
     * Known CIF product carousel component resource types (see {@code ProductCarouselImpl.RESOURCE_TYPE}, v1
     * only). Matching is done via {@link Resource#isResourceType(String)}, which follows
     * {@code sling:resourceSuperType}, so proxied project components (e.g. Venia's) that super-type this are also
     * accepted.
     */
    private static final List<String> CIF_PRODUCTCAROUSEL_COMPONENT_TYPES = Arrays.asList(
        "core/cif/components/commerce/productcarousel/v1/productcarousel");

    /**
     * Valid {@code selectionType} values, redeclared from {@code ProductCarouselImpl}'s internal constants (not
     * importable from here).
     */
    private static final Set<String> VALID_SELECTION_TYPES = new HashSet<>(Arrays.asList("product", "category"));

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_productcarousel_component";
    }

    @Override
    public String description() {
        return "Configure a CIF product carousel component's selection: a manual list of product SKUs "
            + "(selectionType=product) or a category with an optional product count (selectionType=category).";
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
        ObjectNode selectionType = properties.putObject("selectionType");
        selectionType.put("type", "string");
        selectionType.putArray("enum").add("product").add("category");
        ObjectNode product = properties.putObject("product");
        product.put("type", "array");
        product.putObject("items").put("type", "string");
        properties.putObject("category").put("type", "string");
        properties.putObject("productCount").put("type", "integer");
        schema.putArray("required").add("path");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);

        String selectionType = args.path("selectionType").asText(null);
        if (selectionType != null && !VALID_SELECTION_TYPES.contains(selectionType)) {
            throw new IllegalArgumentException("selectionType must be one of " + VALID_SELECTION_TYPES + ": " + selectionType);
        }

        boolean hasSelectionType = args.has("selectionType");
        boolean hasProduct = args.has("product");
        boolean hasCategory = args.has("category");
        boolean hasProductCount = args.has("productCount");

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = CommerceWriteSupport.resolveComponent(resolver, "path", path,
            CIF_PRODUCTCAROUSEL_COMPONENT_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(target, "path");

        CommerceWriteSupport.putOrRemove(properties, SELECTION_TYPE_PROPERTY, selectionType);

        JsonNode productNode = args.path("product");
        List<String> normalizedSkus = new ArrayList<>();
        if (productNode.isArray()) {
            for (JsonNode sku : productNode) {
                if (sku.isNull() || StringUtils.isBlank(sku.asText())) {
                    continue;
                }
                normalizedSkus.add(CombinedSku.parse(sku.asText()).toString());
            }
            CommerceWriteSupport.putOrRemoveArray(properties, PRODUCT_PROPERTY, normalizedSkus);
        } else {
            CommerceWriteSupport.putOrRemoveArray(properties, PRODUCT_PROPERTY, null);
        }

        String category = args.path("category").asText(null);
        CommerceWriteSupport.putOrRemove(properties, CATEGORY_PROPERTY, category);

        JsonNode productCountNode = args.path("productCount");
        Integer productCount = productCountNode.isIntegralNumber() ? Integer.valueOf(productCountNode.asInt()) : null;
        if (productCount != null) {
            properties.put(PRODUCT_COUNT_PROPERTY, productCount.intValue());
        } else {
            properties.remove(PRODUCT_COUNT_PROPERTY);
        }

        resolver.commit();

        // Post-write verification: re-read the persisted resource and confirm every property that was actually
        // provided round-tripped, so we never report success for a write that did not take (the resource-type
        // check above already validated it consumes these properties).
        Resource persisted = resolver.getResource(path);
        boolean updated = persisted != null
            && (!hasSelectionType || stringMatches(persisted, SELECTION_TYPE_PROPERTY, selectionType))
            && (!hasProduct || productMatches(persisted, normalizedSkus))
            && (!hasCategory || stringMatches(persisted, CATEGORY_PROPERTY, category))
            && (!hasProductCount || integerMatches(persisted, productCount));

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("updated", updated);
        return out;
    }

    private static boolean stringMatches(Resource persisted, String property, String expected) {
        String actual = persisted.getValueMap().get(property, String.class);
        return StringUtils.isBlank(expected) ? actual == null : expected.equals(actual);
    }

    private static boolean productMatches(Resource persisted, List<String> expected) {
        String[] actual = persisted.getValueMap().get(PRODUCT_PROPERTY, String[].class);
        if (expected == null || expected.isEmpty()) {
            return actual == null;
        }
        return actual != null && expected.equals(Arrays.asList(actual));
    }

    private static boolean integerMatches(Resource persisted, Integer expected) {
        Integer actual = persisted.getValueMap().get(PRODUCT_COUNT_PROPERTY, Integer.class);
        return expected == null ? actual == null : expected.equals(actual);
    }
}
