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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * MCP write tool that configures the {@code items} composite multifield shared by CIF's featured-category-list and
 * category-carousel components -- both are backed by the same {@code FeaturedCategoryListImpl} Sling model -- via
 * the caller's {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * Unlike a flat multi-valued property, {@code items} is a composite multifield: a container child node whose own
 * children ({@code item0}, {@code item1}, …) each carry {@code categoryId} (a category UID, required) and an
 * optional {@code asset} (a {@code /content/dam} path overriding the category's image). featuredcategorylist also
 * exposes {@code jcr:title}/{@code titleType}/{@code linkTarget}; categorycarousel does not render a title, but the
 * properties are harmless no-ops there since the same model backs both resource types.
 */
@Component(service = McpTool.class)
public class ConfigureFeaturedCategoryListComponentTool implements McpTool {

    private static final String CATEGORY_ID_PROPERTY = "categoryId";
    private static final String ASSET_PROPERTY = "asset";
    private static final String ITEMS_CHILD_NAME = "items";
    private static final String TITLE_PROPERTY = "jcr:title";
    private static final String TITLE_TYPE_PROPERTY = "titleType";
    private static final String LINK_TARGET_PROPERTY = "linkTarget";

    /**
     * Known CIF resource types backed by {@code FeaturedCategoryListImpl} (v1 only, for both components). Matching
     * is done via {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType}, so
     * proxied project components (e.g. Venia's) that super-type either of these are also accepted.
     */
    private static final List<String> CIF_FEATUREDCATEGORYLIST_COMPONENT_TYPES = Arrays.asList(
        "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist",
        "core/cif/components/commerce/categorycarousel/v1/categorycarousel");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_featuredcategorylist_component";
    }

    @Override
    public String description() {
        return "Configure a CIF featured-category-list or category-carousel component's category items "
            + "(the items composite multifield: categoryId + optional asset override per item) and title fields.";
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
        ObjectNode items = properties.putObject("items");
        items.put("type", "array");
        ObjectNode itemSchema = items.putObject("items");
        itemSchema.put("type", "object");
        ObjectNode itemProperties = itemSchema.putObject("properties");
        itemProperties.putObject("categoryId").put("type", "string");
        itemProperties.putObject("asset").put("type", "string");
        itemSchema.putArray("required").add("categoryId");
        properties.putObject("title").put("type", "string");
        properties.putObject("titleType").put("type", "string");
        properties.putObject("linkTarget").put("type", "string");
        schema.putArray("required").add("path").add("items");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = CommerceWriteSupport.resolveComponent(resolver, "path", path,
            CIF_FEATUREDCATEGORYLIST_COMPONENT_TYPES);

        JsonNode itemsNode = args.path("items");
        if (!itemsNode.isArray() || itemsNode.size() == 0) {
            throw new IllegalArgumentException("items (non-empty array) is required");
        }

        List<Map<String, Object>> itemMaps = new ArrayList<Map<String, Object>>();
        for (JsonNode itemNode : itemsNode) {
            String categoryId = itemNode.path(CATEGORY_ID_PROPERTY).asText(null);
            if (StringUtils.isBlank(categoryId)) {
                throw new IllegalArgumentException("each item requires a non-blank categoryId");
            }
            Map<String, Object> itemMap = new LinkedHashMap<String, Object>();
            itemMap.put(CATEGORY_ID_PROPERTY, categoryId);
            String asset = itemNode.path(ASSET_PROPERTY).asText(null);
            if (StringUtils.isNotBlank(asset)) {
                itemMap.put(ASSET_PROPERTY, asset);
            }
            itemMaps.add(itemMap);
        }

        CommerceWriteSupport.writeComposite(resolver, target, ITEMS_CHILD_NAME, itemMaps);

        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(target, "path");
        String title = args.path("title").asText(null);
        CommerceWriteSupport.putOrRemove(properties, TITLE_PROPERTY, title);
        String titleType = args.path("titleType").asText(null);
        CommerceWriteSupport.putOrRemove(properties, TITLE_TYPE_PROPERTY, titleType);
        String linkTarget = args.path("linkTarget").asText(null);
        CommerceWriteSupport.putOrRemove(properties, LINK_TARGET_PROPERTY, linkTarget);

        resolver.commit();

        // Post-write verification: re-read the persisted resource and confirm the items composite multifield and
        // any provided title fields round-tripped, so we never report success for a write that did not take.
        Resource persisted = resolver.getResource(path);
        boolean updated = persisted != null
            && itemsMatch(persisted, itemMaps)
            && stringMatches(persisted, TITLE_PROPERTY, title)
            && stringMatches(persisted, TITLE_TYPE_PROPERTY, titleType)
            && stringMatches(persisted, LINK_TARGET_PROPERTY, linkTarget);

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        out.put("itemCount", itemMaps.size());
        out.put("updated", updated);
        return out;
    }

    private static boolean itemsMatch(Resource persisted, List<Map<String, Object>> expected) {
        Resource items = persisted.getChild(ITEMS_CHILD_NAME);
        if (items == null) {
            return false;
        }
        for (int i = 0; i < expected.size(); i++) {
            Resource child = items.getChild("item" + i);
            if (child == null) {
                return false;
            }
            Map<String, Object> expectedProps = expected.get(i);
            if (!stringMatches(child, CATEGORY_ID_PROPERTY, (String) expectedProps.get(CATEGORY_ID_PROPERTY))) {
                return false;
            }
            if (!stringMatches(child, ASSET_PROPERTY, (String) expectedProps.get(ASSET_PROPERTY))) {
                return false;
            }
        }
        return items.getChild("item" + expected.size()) == null;
    }

    private static boolean stringMatches(Resource persisted, String property, String expected) {
        String actual = persisted.getValueMap().get(property, String.class);
        return StringUtils.isBlank(expected) ? actual == null : expected.equals(actual);
    }
}
