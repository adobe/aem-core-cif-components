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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * MCP write tool binding a relation type (and optional product SKU override) to a CIF related-products component
 * resource, via the caller's {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * Unlike {@code ConfigureProductTeaserComponentTool} / {@code ConfigureProductComponentTool}, {@code product} here
 * is read by {@code RelatedProductsImpl} as a <strong>plain base SKU</strong> (the picker is configured with
 * {@code selectionId="sku"}), not a combinedSku, so it is deliberately not passed through {@code CombinedSku}. When
 * {@code product} is blank/omitted, the model falls back to the page-URL product, so this tool removes the
 * property rather than writing an empty value.
 */
@Component(service = McpTool.class)
public class ConfigureRelatedProductsComponentTool implements McpTool {

    private static final String PRODUCT_PROPERTY = "product";
    private static final String RELATION_TYPE_PROPERTY = "relationType";

    /**
     * Known CIF related-products component resource types (see {@code RelatedProductsImpl.RESOURCE_TYPE}, v1 only).
     * Matching is done via {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType},
     * so proxied project components (e.g. Venia's) that super-type this are also accepted.
     */
    private static final List<String> CIF_RELATEDPRODUCTS_COMPONENT_TYPES = Arrays.asList(
        "core/cif/components/commerce/relatedproducts/v1/relatedproducts");

    /**
     * Valid {@code relationType} values, redeclared from {@code RelatedProductsRetriever.RelationType} (internal
     * to {@code bundles/core}, not importable from here). Stored as the enum name, unchanged.
     */
    private static final Set<String> VALID_RELATION_TYPES = new HashSet<>(
        Arrays.asList("RELATED_PRODUCTS", "UPSELL_PRODUCTS", "CROSS_SELL_PRODUCTS"));

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_relatedproducts_component";
    }

    @Override
    public String description() {
        return "Set the relation type (related/upsell/cross-sell) and optional plain-SKU product override on a "
            + "CIF related-products component resource.";
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
        properties.putObject("product").put("type", "string");
        ObjectNode relationType = properties.putObject("relationType");
        relationType.put("type", "string");
        relationType.putArray("enum").add("RELATED_PRODUCTS").add("UPSELL_PRODUCTS").add("CROSS_SELL_PRODUCTS");
        schema.putArray("required").add("path").add("relationType");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        String relationType = args.path("relationType").asText(null);
        if (StringUtils.isBlank(relationType) || !VALID_RELATION_TYPES.contains(relationType)) {
            throw new IllegalArgumentException("relationType must be one of " + VALID_RELATION_TYPES + ": " + relationType);
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = CommerceWriteSupport.resolveComponent(resolver, "path", path,
            CIF_RELATEDPRODUCTS_COMPONENT_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(target, "path");

        properties.put(RELATION_TYPE_PROPERTY, relationType);
        String product = args.path("product").asText(null);
        CommerceWriteSupport.putOrRemove(properties, PRODUCT_PROPERTY, product);
        resolver.commit();

        // Post-write verification: re-read the persisted value so we never report success for a write that did
        // not take (the model consumes exactly this property, validated by the resource-type check above).
        Resource persisted = resolver.getResource(path);
        boolean updated = persisted != null
            && relationType.equals(persisted.getValueMap().get(RELATION_TYPE_PROPERTY, String.class));

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("relationType", relationType);
        out.put("updated", updated);
        return out;
    }
}
