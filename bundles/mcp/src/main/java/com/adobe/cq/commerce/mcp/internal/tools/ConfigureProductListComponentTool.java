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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.models.productcollection.ProductCollection;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.CommerceWriteSupport;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool that configures a CIF product-list-family <em>component</em> (product list / product carousel):
 * pins it to a specific category via {@code category} (the "Manual Category Selection" read by
 * {@code ProductListImpl}/{@code ProductCarouselImpl}), and -- for productlist v1/v2, which is backed by
 * {@code ProductListImpl} extending {@code ProductCollectionImpl} -- also configures the rest of that component's
 * dialog: {@link ProductList#PN_SHOW_TITLE}, {@link ProductList#PN_SHOW_IMAGE}, {@link ProductCollection#PN_PAGE_SIZE},
 * {@link ProductCollection#PN_DEFAULT_SORT_FIELD}/{@link ProductCollection#PN_DEFAULT_SORT_ORDER}, and the
 * {@link ProductList#NN_FRAGMENTS} composite multifield (experience-fragment locations per page).
 * <p>
 * This is the category-to-component counterpart of {@code configure_product_component} (which pins a product
 * component to a SKU). It targets a <em>component resource</em>, not a page's {@code jcr:content}; to bind the
 * root category of a catalog (PLP) page use {@code configure_catalog_page} instead. Writes run under the
 * caller's {@link ResourceResolver} so JCR ACLs are enforced.
 * <p>
 * The new productlist-specific properties (everything but {@code category}) are harmless no-ops on
 * productcarousel v1: {@code ProductCarouselImpl} does not extend {@code ProductCollectionImpl}/{@code ProductListImpl}
 * and never reads any of them, so they may still be written to the carousel's node but have no effect at render
 * time. Use {@code configure_productcarousel_component} for carousel-specific fields instead.
 */
@Component(service = McpTool.class)
public class ConfigureProductListComponentTool implements McpTool {
    private static final String CATEGORY_PROPERTY = "category";
    private static final String SHOW_TITLE_PROPERTY = ProductList.PN_SHOW_TITLE;
    private static final String SHOW_IMAGE_PROPERTY = ProductList.PN_SHOW_IMAGE;
    private static final String PAGE_SIZE_PROPERTY = ProductCollection.PN_PAGE_SIZE;
    private static final String DEFAULT_SORT_FIELD_PROPERTY = ProductCollection.PN_DEFAULT_SORT_FIELD;
    private static final String DEFAULT_SORT_ORDER_PROPERTY = ProductCollection.PN_DEFAULT_SORT_ORDER;
    private static final String FRAGMENTS_CHILD_NAME = ProductList.NN_FRAGMENTS;

    // ProductListImpl's fragment child-node property names are internal (PN_FRAGMENT_*), redeclared here.
    private static final String FRAGMENT_LOCATION_PROPERTY = "fragmentLocation";
    private static final String FRAGMENT_PAGE_PROPERTY = "fragmentPage";
    private static final String FRAGMENT_CSS_CLASS_PROPERTY = "fragmentCssClass";

    /**
     * Valid {@code defaultSortOrder} values, matching {@code Sorter.Order} ({@code asc}/{@code desc},
     * case-insensitive at read time, but we only ever write the lower-case literal here).
     */
    private static final Set<String> VALID_SORT_ORDERS = new HashSet<>(Arrays.asList("asc", "desc"));

    /**
     * CIF component resource types that read the {@code category} manual-selection property (see
     * {@code ProductListImpl.RESOURCE_TYPE} v1/v2 and {@code ProductCarouselImpl.RESOURCE_TYPE}). Matching is
     * done via {@link Resource#isResourceType(String)}, which follows {@code sling:resourceSuperType}, so
     * proxied project components (e.g. Venia's {@code venia/components/...}) that super-type one of these are
     * also accepted.
     */
    private static final List<String> CIF_PRODUCTLIST_COMPONENT_TYPES = Arrays.asList(
        "core/cif/components/commerce/productlist/v1/productlist",
        "core/cif/components/commerce/productlist/v2/productlist",
        "core/cif/components/commerce/productcarousel/v1/productcarousel");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "configure_productlist_component";
    }

    @Override
    public String description() {
        return "Configure a CIF product list or product carousel component: pin it to a specific category (its "
            + "'category' manual selection) and, for product list components, the rest of its dialog -- showTitle, "
            + "showImage, pageSize, defaultSortField/defaultSortOrder (asc|desc), and the fragments composite "
            + "multifield (experience-fragment locations per page). The productlist-specific properties are "
            + "no-ops on a product carousel (it doesn't read them) -- only categoryUid has any effect there. "
            + "Targets the component resource; to set a catalog page's root category use configure_catalog_page.";
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
        properties.putObject("showTitle").put("type", "boolean");
        properties.putObject("showImage").put("type", "boolean");
        properties.putObject("pageSize").put("type", "integer");
        properties.putObject("defaultSortField").put("type", "string");
        ObjectNode defaultSortOrder = properties.putObject("defaultSortOrder");
        defaultSortOrder.put("type", "string");
        defaultSortOrder.putArray("enum").add("asc").add("desc");
        ObjectNode fragments = properties.putObject("fragments");
        fragments.put("type", "array");
        ObjectNode fragmentSchema = fragments.putObject("items");
        fragmentSchema.put("type", "object");
        ObjectNode fragmentProperties = fragmentSchema.putObject("properties");
        fragmentProperties.putObject(FRAGMENT_LOCATION_PROPERTY).put("type", "string");
        fragmentProperties.putObject(FRAGMENT_PAGE_PROPERTY).put("type", "integer");
        fragmentProperties.putObject(FRAGMENT_CSS_CLASS_PROPERTY).put("type", "string");
        schema.putArray("required").add("path");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String path = args.path("path").asText(null);

        String categoryUid = args.path("categoryUid").asText(null);
        boolean hasCategoryUid = args.has("categoryUid");

        boolean hasShowTitle = args.has("showTitle");
        Boolean showTitle = hasShowTitle ? Boolean.valueOf(args.path("showTitle").asBoolean()) : null;

        boolean hasShowImage = args.has("showImage");
        Boolean showImage = hasShowImage ? Boolean.valueOf(args.path("showImage").asBoolean()) : null;

        JsonNode pageSizeNode = args.path("pageSize");
        boolean hasPageSize = pageSizeNode.isIntegralNumber();
        Integer pageSize = hasPageSize ? Integer.valueOf(pageSizeNode.asInt()) : null;

        String defaultSortField = args.path("defaultSortField").asText(null);
        boolean hasDefaultSortField = args.has("defaultSortField");

        String defaultSortOrder = args.path("defaultSortOrder").asText(null);
        boolean hasDefaultSortOrder = args.has("defaultSortOrder");
        if (StringUtils.isNotBlank(defaultSortOrder) && !VALID_SORT_ORDERS.contains(defaultSortOrder)) {
            throw new IllegalArgumentException("defaultSortOrder must be one of " + VALID_SORT_ORDERS + ": " + defaultSortOrder);
        }

        JsonNode fragmentsNode = args.path("fragments");
        boolean hasFragments = args.has("fragments");
        List<Map<String, Object>> fragmentMaps = new ArrayList<Map<String, Object>>();
        if (fragmentsNode.isArray()) {
            for (JsonNode fragmentNode : fragmentsNode) {
                Map<String, Object> fragmentMap = new LinkedHashMap<String, Object>();
                String fragmentLocation = fragmentNode.path(FRAGMENT_LOCATION_PROPERTY).asText(null);
                if (StringUtils.isNotBlank(fragmentLocation)) {
                    fragmentMap.put(FRAGMENT_LOCATION_PROPERTY, fragmentLocation);
                }
                JsonNode fragmentPageNode = fragmentNode.path(FRAGMENT_PAGE_PROPERTY);
                if (fragmentPageNode.isIntegralNumber()) {
                    fragmentMap.put(FRAGMENT_PAGE_PROPERTY, Integer.valueOf(fragmentPageNode.asInt()));
                }
                String fragmentCssClass = fragmentNode.path(FRAGMENT_CSS_CLASS_PROPERTY).asText(null);
                if (StringUtils.isNotBlank(fragmentCssClass)) {
                    fragmentMap.put(FRAGMENT_CSS_CLASS_PROPERTY, fragmentCssClass);
                }
                fragmentMaps.add(fragmentMap);
            }
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = CommerceWriteSupport.resolveComponent(resolver, "path", path, CIF_PRODUCTLIST_COMPONENT_TYPES);
        ModifiableValueMap properties = CommerceWriteSupport.mutableMap(target, "path");

        if (hasCategoryUid) {
            CommerceWriteSupport.putOrRemove(properties, CATEGORY_PROPERTY, categoryUid);
        }
        if (hasShowTitle) {
            properties.put(SHOW_TITLE_PROPERTY, showTitle.booleanValue());
        }
        if (hasShowImage) {
            properties.put(SHOW_IMAGE_PROPERTY, showImage.booleanValue());
        }
        if (hasPageSize) {
            properties.put(PAGE_SIZE_PROPERTY, pageSize.intValue());
        }
        if (hasDefaultSortField) {
            CommerceWriteSupport.putOrRemove(properties, DEFAULT_SORT_FIELD_PROPERTY, defaultSortField);
        }
        if (hasDefaultSortOrder) {
            CommerceWriteSupport.putOrRemove(properties, DEFAULT_SORT_ORDER_PROPERTY, defaultSortOrder);
        }
        if (hasFragments) {
            CommerceWriteSupport.writeComposite(resolver, target, FRAGMENTS_CHILD_NAME, fragmentMaps);
        }

        resolver.commit();

        // Post-write verification: re-read the persisted resource and confirm every property that was actually
        // provided round-tripped, so we never report success for a write that did not take (the resource-type
        // check above already validated it's a CIF product list / carousel component).
        Resource persisted = resolver.getResource(path);
        boolean updated = persisted != null
            && (!hasCategoryUid || stringMatches(persisted, CATEGORY_PROPERTY, categoryUid))
            && (!hasShowTitle || booleanMatches(persisted, SHOW_TITLE_PROPERTY, showTitle))
            && (!hasShowImage || booleanMatches(persisted, SHOW_IMAGE_PROPERTY, showImage))
            && (!hasPageSize || integerMatches(persisted, PAGE_SIZE_PROPERTY, pageSize))
            && (!hasDefaultSortField || stringMatches(persisted, DEFAULT_SORT_FIELD_PROPERTY, defaultSortField))
            && (!hasDefaultSortOrder || stringMatches(persisted, DEFAULT_SORT_ORDER_PROPERTY, defaultSortOrder))
            && (!hasFragments || fragmentsMatch(persisted, fragmentMaps));

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path);
        if (hasCategoryUid) {
            out.put("categoryUid", categoryUid);
        }
        out.put("updated", updated);
        return out;
    }

    private static boolean stringMatches(Resource persisted, String property, String expected) {
        String actual = persisted.getValueMap().get(property, String.class);
        return StringUtils.isBlank(expected) ? actual == null : expected.equals(actual);
    }

    private static boolean booleanMatches(Resource persisted, String property, Boolean expected) {
        Boolean actual = persisted.getValueMap().get(property, Boolean.class);
        return expected.equals(actual);
    }

    private static boolean integerMatches(Resource persisted, String property, Integer expected) {
        Integer actual = persisted.getValueMap().get(property, Integer.class);
        return expected.equals(actual);
    }

    private static boolean fragmentsMatch(Resource persisted, List<Map<String, Object>> expected) {
        Resource fragments = persisted.getChild(FRAGMENTS_CHILD_NAME);
        if (expected.isEmpty()) {
            return fragments == null;
        }
        if (fragments == null) {
            return false;
        }
        for (int i = 0; i < expected.size(); i++) {
            Resource child = fragments.getChild("item" + i);
            if (child == null) {
                return false;
            }
            Map<String, Object> expectedProps = expected.get(i);
            if (!stringMatches(child, FRAGMENT_LOCATION_PROPERTY, (String) expectedProps.get(FRAGMENT_LOCATION_PROPERTY))) {
                return false;
            }
            if (!stringMatches(child, FRAGMENT_CSS_CLASS_PROPERTY, (String) expectedProps.get(FRAGMENT_CSS_CLASS_PROPERTY))) {
                return false;
            }
            Integer expectedPage = (Integer) expectedProps.get(FRAGMENT_PAGE_PROPERTY);
            Integer actualPage = child.getValueMap().get(FRAGMENT_PAGE_PROPERTY, Integer.class);
            if (expectedPage == null ? actualPage != null : !expectedPage.equals(actualPage)) {
                return false;
            }
        }
        return fragments.getChild("item" + expected.size()) == null;
    }
}
