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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.adobe.cq.commerce.magento.graphql.BundleItem;
import com.adobe.cq.commerce.magento.graphql.BundleItemOption;
import com.adobe.cq.commerce.magento.graphql.BundleProduct;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.shopify.graphql.support.ID;

/**
 * Resolves human-readable bundle selections (e.g. {@code {"Necklace": "Carmina Necklace"}}) supplied to
 * {@code add_to_cart} into UIDs for {@code CartItemInput.selectedOptions} &mdash; the same field
 * {@link ConfigurableOptionResolver} resolves into, and the same unified {@code addProductsToCart} mutation handles
 * both: a bundle choice's own {@code uid} (verified live, e.g. {@code bundle/2/2/1}, base64-encoded) works exactly
 * like a configurable option-value UID, so no separate {@code addBundleProductsToCart} mutation is needed. Non-bundle
 * products resolve to an empty list. Matching is case-insensitive against the bundle item's title (bundle items have
 * no separate machine-readable code, unlike configurable options) and each choice's label. Optional bundle items
 * (not required) may be omitted from the supplied map.
 */
public class BundleOptionResolver {

    public List<ID> resolve(ProductInterface product, Map<String, String> suppliedOptions) {
        List<ID> resolved = new ArrayList<>();
        if (!(product instanceof BundleProduct)) {
            return resolved;
        }
        List<BundleItem> items = ((BundleProduct) product).getItems();
        if (items == null) {
            return resolved;
        }
        for (BundleItem item : items) {
            String suppliedValue = findSuppliedValue(suppliedOptions, item);
            if (suppliedValue == null) {
                if (Boolean.TRUE.equals(item.getRequired())) {
                    throw new IllegalArgumentException(item.getTitle() + " is required. Available values: " + availableValues(item));
                }
                continue;
            }
            BundleItemOption match = findMatchingOption(item, suppliedValue);
            if (match == null) {
                throw new IllegalArgumentException(item.getTitle() + " must be one of: " + availableValues(item));
            }
            resolved.add(match.getUid());
        }
        return resolved;
    }

    private String findSuppliedValue(Map<String, String> suppliedOptions, BundleItem item) {
        for (Map.Entry<String, String> entry : suppliedOptions.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(item.getTitle())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private BundleItemOption findMatchingOption(BundleItem item, String suppliedValue) {
        if (item.getOptions() == null) {
            return null;
        }
        for (BundleItemOption option : item.getOptions()) {
            if (suppliedValue.equalsIgnoreCase(option.getLabel())) {
                return option;
            }
        }
        return null;
    }

    private String availableValues(BundleItem item) {
        if (item.getOptions() == null) {
            return "";
        }
        return item.getOptions().stream().map(BundleItemOption::getLabel).collect(Collectors.joining(", "));
    }
}
