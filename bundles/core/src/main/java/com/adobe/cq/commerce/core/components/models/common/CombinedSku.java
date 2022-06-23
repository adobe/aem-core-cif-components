/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.models.common;

import org.apache.commons.lang3.StringUtils;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.lang.annotations.Nullable;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class represents a combined sku. For a configurable product variant the combined sku consists of a base sku and a variant sku. For
 * any other product types the variant sku will be {@code null}.
 * <p>
 * Combined skus are persisted as {@link String} where the base sku is separted from the variant sku (if available) using the
 * {@link CombinedSku#COMBINED_SKU_SEPARATOR}. The {@link CombinedSku#parse(String)} factory method allows to parse such a {@link String}
 * and create a {@link CombinedSku} object for it.
 */
@ProviderType
public final class CombinedSku {

    private static final String COMBINED_SKU_SEPARATOR = "#";

    private final String baseSku;
    private final String variantSku;

    public CombinedSku(String baseSku, String variantSku) {
        this.baseSku = baseSku;
        this.variantSku = variantSku;
    }

    /**
     * Parses a combined sku in its {@link String} representation.
     *
     * @param combinedSku
     * @return
     */
    public static CombinedSku parse(String combinedSku) {
        String baseSku = StringUtils.substringBefore(combinedSku, COMBINED_SKU_SEPARATOR);
        String variantSku = StringUtils.substringAfter(combinedSku, COMBINED_SKU_SEPARATOR);
        return new CombinedSku(baseSku, variantSku.isEmpty() ? null : variantSku);
    }

    /**
     * Returns the base product's sku.
     *
     * @return
     */
    public String getBaseSku() {
        return baseSku;
    }

    /**
     * For variants of a configurable product, this returns the variant's sku. For all other cases it returns {@code null}.
     *
     * @return
     */
    @Nullable
    public String getVariantSku() {
        return variantSku;
    }

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(variantSku)) {
            return baseSku + COMBINED_SKU_SEPARATOR + variantSku;
        } else {
            return baseSku;
        }
    }
}
