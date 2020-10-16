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

package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Variant;

public class VariantImpl implements Variant {
    private String id;

    private String name;

    private String description;

    private String sku;

    private String currency;

    private Double price;

    private Boolean inStock;

    private Integer color;

    private List<Asset> assets;

    private String formattedPrice;

    private Map<String, Integer> variantAttributes = new HashMap<>();

    private Price priceRange;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    @Override
    public String getCurrency() {
        return getPriceRange().getCurrency();
    }

    @Deprecated
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public Double getPrice() {
        return getPriceRange().getFinalPrice();
    }

    @Deprecated
    public void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public String getFormattedPrice() {
        return getPriceRange().getFormattedFinalPrice();
    }

    @Deprecated
    public void setFormattedPrice(String formattedPrice) {
        this.formattedPrice = formattedPrice;
    };

    @Override
    public Price getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(Price priceRange) {
        this.priceRange = priceRange;
    }

    @Override
    public Boolean getInStock() {
        return inStock;
    }

    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }

    @Override
    public Integer getColor() {
        return color;
    }

    @Override
    public Map<String, Integer> getVariantAttributes() {
        return variantAttributes;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    @Override
    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }
}
