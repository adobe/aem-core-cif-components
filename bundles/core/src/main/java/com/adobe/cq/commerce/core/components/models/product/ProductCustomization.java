/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
package com.adobe.cq.commerce.core.components.models.product;

import java.text.NumberFormat;
import java.util.List;

import com.adobe.cq.commerce.core.components.models.retriever.ProductRetriever;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.MediaGalleryEntry;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPricesQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.StoreConfigQueryDefinition;

public interface ProductCustomization extends Product {

    default public ProductRetriever getProductRetriever() {
        throw new UnsupportedOperationException();
    }

    default public String generateQuery(String slug) {
        throw new UnsupportedOperationException();
    }

    default public ProductPricesQueryDefinition generatePriceQuery() {
        throw new UnsupportedOperationException();
    }

    default public SimpleProductQueryDefinition generateSimpleProductQuery() {
        throw new UnsupportedOperationException();
    }

    default public ProductInterfaceQueryDefinition generateProductQuery() {
        throw new UnsupportedOperationException();
    }

    default public StoreConfigQueryDefinition generateStoreConfigQuery() {
        throw new UnsupportedOperationException();
    }

    default public Variant mapVariant(ConfigurableVariant variant) {
        throw new UnsupportedOperationException();
    }

    default public List<Asset> filterAndSortAssets(List<MediaGalleryEntry> assets) {
        throw new UnsupportedOperationException();
    }

    default public Asset mapAsset(MediaGalleryEntry entry) {
        throw new UnsupportedOperationException();
    }

    default public VariantValue mapVariantValue(ConfigurableProductOptionsValues value) {
        throw new UnsupportedOperationException();
    }

    default public VariantAttribute mapVariantAttribute(ConfigurableProductOptions option) {
        throw new UnsupportedOperationException();
    }

    default public String parseProductSlug() {
        throw new UnsupportedOperationException();
    }

    default public NumberFormat getPriceFormatter() {
        throw new UnsupportedOperationException();
    }

    default public String safeDescription(ProductInterface product) {
        throw new UnsupportedOperationException();
    }
}
