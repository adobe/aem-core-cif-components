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

import com.fasterxml.jackson.databind.ObjectMapper;

@ProviderType
public final class CombinedSku {

    private static final String COMBINED_SKU_SEPARATOR = "#";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(CombinedSku.class);

    private final String baseSku;
    private final String variantSku;

    public CombinedSku(String baseSku, String variantSku) {
        this.baseSku = baseSku;
        this.variantSku = variantSku;
    }

    public static CombinedSku parse(String combinedSku) {
        String baseSku = StringUtils.substringBefore(combinedSku, COMBINED_SKU_SEPARATOR);
        String variantSku = StringUtils.substringAfter(combinedSku, COMBINED_SKU_SEPARATOR);
        return new CombinedSku(baseSku, variantSku.isEmpty() ? null : variantSku);
    }

    public String getBaseSku() {
        return baseSku;
    }

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
