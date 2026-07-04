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

import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool binding a product SKU (and optional call-to-action / link / id properties) to a CIF product
 * teaser component resource, via the caller's {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * Only {@code selection} (the model's {@code SELECTION_PROPERTY}, a combinedSku parsed via {@link CombinedSku})
 * is read by {@code ProductTeaserImpl}; it does not read a {@code selectionType} property, unlike the product
 * carousel, so this tool deliberately does not write one. {@code enableAddToCart}/{@code enableAddToWishList}
 * are style/policy properties (read from {@code currentStyle}), not component-instance dialog fields, and are
 * intentionally not exposed here.
 */
@Component(service = McpTool.class)
public class ConfigureProductTeaserComponentTool implements McpTool {

    private static final String SELECTION_PROPERTY = "selection";
    private static final String CTA_PROPERTY = "cta";
    private static final String CTA_TEXT_PROPERTY = "ctaText";
    private static final String LINK_TARGET_PROPERTY = "linkTarget";
    private static final String ID_PROPERTY = "id";

    /**
     * Known CIF product teaser component resource types (see {@code ProductTeaserImpl.RESOURCE_TYPE}, v1 only).
     * Matching is done via {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType},
     * so proxied project components (e.g. Venia's {@code venia/components/...}) that super-type this are also
     * accepted.
     */
    private static final List<String> CIF_PRODUCTTEASER_COMPONENT_TYPES = Arrays.asList(
        "core/cif/components/commerce/productteaser/v1/productteaser");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_productteaser_component";
    }

    @Override
    public String description() {
        return "Bind a product SKU (base or base#variant combinedSku) and optional call-to-action / link / id "
            + "properties to a CIF product teaser component resource.";
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
        properties.putObject("sku").put("type", "string");
        properties.putObject("cta").put("type", "string");
        properties.putObject("ctaText").put("type", "string");
        properties.putObject("linkTarget").put("type", "string");
        properties.putObject("id").put("type", "string");
        schema.putArray("required").add("path").add("sku");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);
        String sku = args.path("sku").asText(null);
        if (StringUtils.isBlank(sku)) {
            throw new IllegalArgumentException("sku is required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = CommerceWriteSupport.resolveComponent(resolver, "path", path,
            CIF_PRODUCTTEASER_COMPONENT_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(target, "path");

        String selection = CombinedSku.parse(sku).toString();
        properties.put(SELECTION_PROPERTY, selection);
        CommerceWriteSupport.putOrRemove(properties, CTA_PROPERTY, args.path("cta").asText(null));
        CommerceWriteSupport.putOrRemove(properties, CTA_TEXT_PROPERTY, args.path("ctaText").asText(null));
        CommerceWriteSupport.putOrRemove(properties, LINK_TARGET_PROPERTY, args.path("linkTarget").asText(null));
        CommerceWriteSupport.putOrRemove(properties, ID_PROPERTY, args.path("id").asText(null));
        resolver.commit();

        // Post-write verification: re-read the persisted value so we never report success for a write that did
        // not take (the teaser model consumes exactly this property, validated by the resource-type check above).
        Resource persisted = resolver.getResource(path);
        boolean updated = persisted != null && selection.equals(persisted.getValueMap().get(SELECTION_PROPERTY, String.class));

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("selection", selection);
        out.put("updated", updated);
        return out;
    }
}
