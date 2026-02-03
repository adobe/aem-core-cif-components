/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.GroupItem;
import com.adobe.cq.commerce.core.components.storefrontcontext.ProductStorefrontContext;

public class GroupItemImpl implements GroupItem {

    private String name;
    private String sku;
    private Price priceRange;
    private Double defaultQuantity;
    private Boolean virtualProduct;
    private ProductStorefrontContext context;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getSku() {
        return sku;
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
    public Double getDefaultQuantity() {
        return defaultQuantity;
    }

    public void setDefaultQuantity(Double defaultQuantity) {
        this.defaultQuantity = defaultQuantity;
    }

    @Override
    public Boolean isVirtualProduct() {
        return virtualProduct;
    }

    public void setVirtualProduct(Boolean virtualProduct) {
        this.virtualProduct = virtualProduct;
    }

    @Override
    public ProductStorefrontContext getStorefrontContext() {
        return context;
    }

    public void setStorefrontContext(ProductStorefrontContext context) {
        this.context = context;
    }
}
