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

import java.util.List;
import java.util.Map;

import com.adobe.cq.commerce.core.components.models.common.Price;

/**
 * Variant is a view model interface representing a product variant that
 * contains properties specific to a variant in comparison to its base product.
 */
public interface Variant {
    String getId();

    String getName();

    String getDescription();

    String getSku();

    /**
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    String getCurrency();

    /**
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    Double getPrice();

    /**
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    String getFormattedPrice();

    Price getPriceRange();

    Boolean getInStock();

    Integer getColor();

    Map<String, Integer> getVariantAttributes();

    List<Asset> getAssets();
}
