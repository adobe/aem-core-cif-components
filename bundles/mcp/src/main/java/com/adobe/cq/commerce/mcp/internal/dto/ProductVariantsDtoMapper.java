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
package com.adobe.cq.commerce.mcp.internal.dto;

import java.util.Collections;
import java.util.List;

import com.adobe.cq.commerce.magento.graphql.ConfigurableAttributeOption;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.PriceRange;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductPrice;
import com.adobe.cq.commerce.magento.graphql.ProductStockStatus;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Maps a Magento product (including configurable variants) to a compact MCP DTO.
 */
public final class ProductVariantsDtoMapper {

    private ProductVariantsDtoMapper() {}

    public static ObjectNode productVariants(ObjectMapper mapper, ProductInterface product) {
        ObjectNode node = mapper.createObjectNode();
        if (product == null) {
            node.putNull("sku");
            return node;
        }

        node.put("sku", product.getSku());
        node.put("name", product.getName());
        node.put("urlKey", product.getUrlKey());

        if (product instanceof ConfigurableProduct) {
            node.put("configurable", true);
            ConfigurableProduct configurable = (ConfigurableProduct) product;
            node.set("options", options(mapper, configurable.getConfigurableOptions()));
            node.set("variants", variants(mapper, configurable.getVariants()));
        } else {
            node.put("configurable", false);
            node.set("options", mapper.createArrayNode());
            node.set("variants", mapper.createArrayNode());
        }
        return node;
    }

    private static ArrayNode options(ObjectMapper mapper, List<ConfigurableProductOptions> options) {
        ArrayNode array = mapper.createArrayNode();
        if (options == null) {
            return array;
        }
        for (ConfigurableProductOptions option : options) {
            ObjectNode node = array.addObject();
            node.put("code", option.getAttributeCode());
            node.put("label", option.getLabel());
            ArrayNode values = node.putArray("values");
            List<ConfigurableProductOptionsValues> optionValues = option.getValues();
            if (optionValues != null) {
                for (ConfigurableProductOptionsValues value : optionValues) {
                    ObjectNode valueNode = values.addObject();
                    valueNode.put("valueIndex", value.getValueIndex());
                    valueNode.put("label", value.getLabel());
                }
            }
        }
        return array;
    }

    private static ArrayNode variants(ObjectMapper mapper, List<ConfigurableVariant> variants) {
        ArrayNode array = mapper.createArrayNode();
        if (variants == null) {
            return array;
        }
        for (ConfigurableVariant variant : variants) {
            SimpleProduct variantProduct = variant.getProduct();
            if (variantProduct == null) {
                continue;
            }
            ObjectNode node = array.addObject();
            node.put("sku", variantProduct.getSku());
            node.put("name", variantProduct.getName());
            node.put("inStock", ProductStockStatus.IN_STOCK.equals(variantProduct.getStockStatus()));
            if (variantProduct.getImage() != null) {
                node.put("imageUrl", variantProduct.getImage().getUrl());
                node.put("imageAlt", variantProduct.getImage().getLabel());
            }
            putPrice(node, variantProduct.getPriceRange());
            node.set("attributes", variantAttributes(mapper, variant.getAttributes()));
        }
        return array;
    }

    private static ArrayNode variantAttributes(ObjectMapper mapper, List<ConfigurableAttributeOption> attributes) {
        ArrayNode array = mapper.createArrayNode();
        List<ConfigurableAttributeOption> source = attributes != null ? attributes : Collections.emptyList();
        for (ConfigurableAttributeOption attribute : source) {
            ObjectNode node = array.addObject();
            node.put("code", attribute.getCode());
            node.put("valueIndex", attribute.getValueIndex());
        }
        return array;
    }

    private static void putPrice(ObjectNode node, PriceRange priceRange) {
        if (priceRange == null || priceRange.getMinimumPrice() == null) {
            return;
        }
        ProductPrice minimumPrice = priceRange.getMinimumPrice();
        Money finalPrice = minimumPrice.getFinalPrice();
        if (finalPrice != null) {
            node.put("price", finalPrice.getValue().doubleValue());
            node.put("currency", finalPrice.getCurrency().toString());
        }
    }
}
