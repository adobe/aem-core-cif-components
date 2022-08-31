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
package com.adobe.cq.commerce.core.components.internal.datalayer;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.commerce.core.components.datalayer.ProductData;
import com.adobe.cq.wcm.core.components.models.datalayer.AssetData;

public class ProductDataImpl extends ComponentDataImpl implements ProductData {
    public ProductDataImpl(DataLayerComponent component, Resource resource) {
        super(component, resource);
    }

    @Override
    public String getSKU() {
        return component.getDataLayerSKU();
    }

    @Override
    public Double getPrice() {
        return component.getDataLayerPrice();
    }

    @Override
    public Double getDiscountAmount() {
        return component.getDataLayerDiscountAmount();
    }

    @Override
    public String getCurrency() {
        return component.getDataLayerCurrency();
    }

    @Override
    public CategoryData[] getCategories() {
        return component.getDataLayerCategories();
    }

    @Override
    public AssetData[] getAssets() {
        return component.getDataLayerAssets();
    }
}
