/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.models.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.core.components.models.common.Price;

/**
 * Variant is a view model interface representing a product variant that
 * contains properties specific to a variant in comparison to its base product.
 */
@ConsumerType
public interface Variant {
    String getId();

    String getName();

    String getSpecialToDate();

    String getDescription();

    String getSku();

    Double getSpecialPrice();

    Price getPriceRange();

    Boolean getInStock();

    Integer getColor();

    Map<String, Integer> getVariantAttributes();

    default Map<String, String> getVariantAttributesUid() {
        return new HashMap<>();
    }

    List<Asset> getAssets();
}
