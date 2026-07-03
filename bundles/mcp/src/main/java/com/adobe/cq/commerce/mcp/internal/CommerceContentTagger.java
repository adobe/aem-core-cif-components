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
package com.adobe.cq.commerce.mcp.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.day.cq.dam.api.DamConstants;

/**
 * Sets or removes CIF commerce reference tags ({@code cq:products}, {@code cq:categories}) on DAM assets, pages, and
 * experience fragment variations — the same metadata used by the Commerce Content References UI.
 */
public final class CommerceContentTagger {

    static final String PN_CQ_PRODUCTS_TYPE = "cq:productsType";
    static final String PRODUCT_TYPE_COMBINED_SKU = "combinedSku";

    public enum Action {
        ADD,
        REMOVE;

        public static Action from(String value) {
            if (StringUtils.isBlank(value) || "add".equalsIgnoreCase(value)) {
                return ADD;
            }
            if ("remove".equalsIgnoreCase(value)) {
                return REMOVE;
            }
            throw new IllegalArgumentException("action must be 'add' or 'remove'");
        }
    }

    private CommerceContentTagger() {}

    public static Resource resolveTagTarget(ResourceResolver resolver, String path) {
        if (resolver == null || StringUtils.isBlank(path)) {
            return null;
        }
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            return null;
        }

        String primaryType = resource.getValueMap().get("jcr:primaryType", String.class);
        if (DamConstants.NT_DAM_ASSET.equals(primaryType)) {
            Resource metadata = resource.getChild("jcr:content/metadata");
            if (metadata == null) {
                throw new IllegalArgumentException("asset metadata not found: " + path);
            }
            return metadata;
        }

        if (resource.getName().equals("jcr:content")) {
            return resource;
        }

        Resource jcrContent = resource.getChild("jcr:content");
        if (jcrContent != null) {
            return jcrContent;
        }

        throw new IllegalArgumentException("unsupported commerce tag target: " + path);
    }

    public static void apply(Resource target, String sku, String categoryUid, Action action) {
        ModifiableValueMap properties = target.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new IllegalArgumentException("resource not modifiable: " + target.getPath());
        }

        if (StringUtils.isNotBlank(sku)) {
            updateTagProperty(properties, target, CommerceExperienceFragment.PN_CQ_PRODUCTS, sku, action,
                isDamMetadata(target));
        }
        if (StringUtils.isNotBlank(categoryUid)) {
            updateTagProperty(properties, target, CommerceExperienceFragment.PN_CQ_CATEGORIES, categoryUid, action,
                isDamMetadata(target));
        }
    }

    private static void updateTagProperty(ModifiableValueMap properties, Resource target, String property,
        String value, Action action, boolean damMetadata) {
        Set<String> tags = readTags(properties, property);
        if (action == Action.ADD) {
            tags.add(value);
        } else {
            tags.remove(value);
        }
        writeTags(properties, property, tags);
        if (damMetadata && CommerceExperienceFragment.PN_CQ_PRODUCTS.equals(property)) {
            syncDamProductsType(properties, tags);
        }
    }

    static boolean isDamMetadata(Resource target) {
        return target.getPath().contains("/jcr:content/metadata");
    }

    private static void writeTags(ModifiableValueMap properties, String property, Set<String> tags) {
        properties.remove(property);
        if (tags.isEmpty()) {
            return;
        }
        if (tags.size() == 1) {
            properties.put(property, tags.iterator().next());
        } else {
            properties.put(property, tags.toArray(new String[0]));
        }
    }

    private static void syncDamProductsType(ModifiableValueMap properties, Set<String> productTags) {
        properties.remove(PN_CQ_PRODUCTS_TYPE);
        if (productTags.size() > 1) {
            properties.put(PN_CQ_PRODUCTS_TYPE, PRODUCT_TYPE_COMBINED_SKU);
        }
    }

    static Set<String> readTags(ValueMap properties, String property) {
        Set<String> tags = new LinkedHashSet<>();
        Object raw = properties.get(property);
        if (raw instanceof String[]) {
            for (String entry : (String[]) raw) {
                addTag(tags, entry);
            }
        } else if (raw instanceof List) {
            for (Object entry : (List<?>) raw) {
                addTag(tags, entry != null ? entry.toString() : null);
            }
        } else if (raw instanceof String) {
            String value = (String) raw;
            if (value.contains(",")) {
                for (String entry : value.split(",")) {
                    addTag(tags, entry);
                }
            } else {
                addTag(tags, value);
            }
        }
        return tags;
    }

    private static void addTag(Set<String> tags, String value) {
        if (StringUtils.isNotBlank(value)) {
            tags.add(value.trim());
        }
    }

    public static List<String> readTagList(ValueMap properties, String property) {
        return new ArrayList<>(readTags(properties, property));
    }
}
