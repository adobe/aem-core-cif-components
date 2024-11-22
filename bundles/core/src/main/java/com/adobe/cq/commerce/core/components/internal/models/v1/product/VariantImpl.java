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

    private String specialToDate;

    private String sku;

    private Boolean inStock;

    private Integer color;

    private List<Asset> assets;

    private Map<String, Integer> variantAttributes = new HashMap<>();

    private Map<String, String> variantAttributesUid = new HashMap<>();

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

    private Double specialPrice;

    @Override
    public Double getSpecialPrice() {
        return specialPrice;
    }

    public void setSpecialPrice(Double specialPrice) {
        this.specialPrice = specialPrice;
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

    @Override
    public String getSpecialToDate() {
        return specialToDate;
    }

    public void setSpecialToDate(String specialToDate) {
        this.specialToDate = specialToDate;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

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

    @Override
    public Map<String, String> getVariantAttributesUid() {
        return variantAttributesUid;
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
