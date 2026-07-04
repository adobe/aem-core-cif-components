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
 * MCP write tool that bulk-creates CIF product teaser component instances (Tier-3 node creation) as uniquely-named
 * children under a caller-supplied container resource, one per requested SKU, via the caller's
 * {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * The grid/container path a page's editable components live under is template-dependent (there is no exported API
 * that returns "the" responsive grid for a page), so the author supplies {@code parentPath} explicitly -- typically
 * a resource discovered via the shipped {@code suggest_template_for_page_type} tool or by inspecting the page.
 * <p>
 * Each created node gets exactly the same {@code sling:resourceType}/{@code selection}/{@code cta}/{@code ctaText}
 * property shape {@link ConfigureProductTeaserComponentTool} writes, so create and configure never diverge.
 * Supports {@code dryRun} (default {@code false}): when {@code true}, the would-be node paths and properties are
 * computed and returned, but nothing is created or committed.
 */
@Component(service = McpTool.class)
public class CreateProductTeasersTool implements McpTool {

    /**
     * CIF product teaser component resource type (v1 only, matches {@code ProductTeaserImpl.RESOURCE_TYPE}), used
     * as the {@code sling:resourceType} of every node this tool creates.
     */
    private static final String PRODUCTTEASER_RESOURCE_TYPE = "core/cif/components/commerce/productteaser/v1/productteaser";
    private static final String BASE_CHILD_NAME = "productteaser";

    private static final String SELECTION_PROPERTY = "selection";
    private static final String CTA_PROPERTY = "cta";
    private static final String CTA_TEXT_PROPERTY = "ctaText";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "create_product_teasers";
    }

    @Override
    public String description() {
        return "Bulk-create CIF product teaser components (one per SKU) as new children under a caller-supplied "
            + "container resource, optionally sharing a call-to-action. Supports dryRun to preview the nodes "
            + "that would be created without persisting anything.";
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
        ObjectNode skus = properties.putObject("skus");
        skus.put("type", "array");
        skus.putObject("items").put("type", "string");
        properties.putObject("cta").put("type", "string");
        properties.putObject("ctaText").put("type", "string");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("parentPath").add("skus");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String parentPath = args.path("parentPath").asText(null);
        boolean dryRun = args.path("dryRun").asBoolean(false);

        List<String> skus = new ArrayList<String>();
        JsonNode skusNode = args.path("skus");
        if (skusNode.isArray()) {
            for (JsonNode skuNode : skusNode) {
                if (!skuNode.isNull()) {
                    skus.add(skuNode.asText());
                }
            }
        }
        if (skus.isEmpty()) {
            throw new IllegalArgumentException("skus is required and must be a non-empty array");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource parent = CommerceWriteSupport.resolveContainer(resolver, "parentPath", parentPath);

        String cta = args.path("cta").asText(null);
        String ctaText = args.path("ctaText").asText(null);

        ObjectNode out = mapper.createObjectNode();
        out.put("parentPath", parentPath);
        ArrayNode created = out.putArray("created");

        if (dryRun) {
            // Preview-only: compute the would-be unique sibling names without creating anything. Names already
            // "reserved" earlier in this same loop are tracked locally so a multi-sku dry run previews N distinct
            // names, exactly matching what a real run would create.
            Set<String> reserved = new HashSet<String>();
            for (String sku : skus) {
                String selection = CombinedSku.parse(sku).toString();
                String uniqueName = nextPreviewName(parent, reserved);
                reserved.add(uniqueName);
                ObjectNode item = created.addObject();
                item.put("path", parent.getPath() + "/" + uniqueName);
                item.put("selection", selection);
            }
            out.put("dryRun", true);
            return out;
        }

        for (String sku : skus) {
            String selection = CombinedSku.parse(sku).toString();
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("sling:resourceType", PRODUCTTEASER_RESOURCE_TYPE);
            props.put(SELECTION_PROPERTY, selection);
            if (StringUtils.isNotBlank(cta)) {
                props.put(CTA_PROPERTY, cta);
            }
            if (StringUtils.isNotBlank(ctaText)) {
                props.put(CTA_TEXT_PROPERTY, ctaText);
            }

            Resource child = CommerceWriteSupport.createChild(resolver, parent, BASE_CHILD_NAME, props);
            ObjectNode item = created.addObject();
            item.put("path", child.getPath());
            item.put("selection", selection);
        }
        resolver.commit();

        // Post-write verification: re-read each persisted node so we never report success for a create that did
        // not take.
        for (JsonNode item : created) {
            Resource persisted = resolver.getResource(item.get("path").asText());
            if (persisted == null
                || !item.get("selection").asText().equals(persisted.getValueMap().get(SELECTION_PROPERTY, String.class))) {
                throw new IllegalStateException("failed to verify created product teaser: " + item.get("path").asText());
            }
        }

        out.put("dryRun", false);
        return out;
    }

    /**
     * Computes the next dry-run preview name under {@code parent}, mirroring
     * {@link org.apache.sling.api.resource.ResourceUtil#createUniqueChildName(Resource, String)}'s numbering
     * ({@code productteaser}, {@code productteaser0}, {@code productteaser1}, ...) but also skipping names already
     * "reserved" earlier in the same dry-run preview loop, since those aren't persisted yet and so wouldn't
     * otherwise collide against {@code parent.getChild(...)}.
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
