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

import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.shopify.graphql.support.ID;

/**
 * Resolves human-readable option values (e.g. {@code {"color": "Blue"}}) supplied to {@code add_to_cart} into the
 * Magento option-value UIDs a configurable product's {@code addProductsToCart} mutation needs. Simple products
 * (no configurable options) resolve to an empty list. Matching is case-insensitive against either the option's
 * attribute code or its label, since an agent may naturally supply either.
 */
public class ConfigurableOptionResolver {

    public List<ID> resolve(ProductInterface product, Map<String, String> suppliedOptions) {
        List<ID> resolved = new ArrayList<>();
        if (!(product instanceof ConfigurableProduct)) {
            return resolved;
        }
        List<ConfigurableProductOptions> configurableOptions = ((ConfigurableProduct) product).getConfigurableOptions();
        if (configurableOptions == null) {
            return resolved;
        }
        for (ConfigurableProductOptions option : configurableOptions) {
            String suppliedValue = findSuppliedValue(suppliedOptions, option);
            if (suppliedValue == null) {
                throw new IllegalArgumentException(optionName(option) + " is required. Available values: " + availableValues(option));
            }
            ConfigurableProductOptionsValues match = findMatchingValue(option, suppliedValue);
            if (match == null) {
                throw new IllegalArgumentException(optionName(option) + " must be one of: " + availableValues(option));
            }
            resolved.add(match.getUid());
        }
        return resolved;
    }

    private String findSuppliedValue(Map<String, String> suppliedOptions, ConfigurableProductOptions option) {
        for (Map.Entry<String, String> entry : suppliedOptions.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(option.getAttributeCode()) || entry.getKey().equalsIgnoreCase(option.getLabel())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private ConfigurableProductOptionsValues findMatchingValue(ConfigurableProductOptions option, String suppliedValue) {
        if (option.getValues() == null) {
            return null;
        }
        for (ConfigurableProductOptionsValues value : option.getValues()) {
            if (suppliedValue.equalsIgnoreCase(value.getLabel())) {
                return value;
            }
        }
        return null;
    }

    private String optionName(ConfigurableProductOptions option) {
        return option.getLabel() != null ? option.getLabel() : option.getAttributeCode();
    }

    private String availableValues(ConfigurableProductOptions option) {
        if (option.getValues() == null) {
            return "";
        }
        return option.getValues().stream().map(ConfigurableProductOptionsValues::getLabel).collect(Collectors.joining(", "));
    }
}
