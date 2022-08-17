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
package com.adobe.cq.commerce.core.components.internal.storefrontcontext;

import com.adobe.cq.commerce.core.components.storefrontcontext.Pricing;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

public class PricingImpl implements Pricing {
    private final ProductInterface product;

    public PricingImpl(ProductInterface product) {
        this.product = product;
    }

    @Override
    public Double getSpecialPrice() {
        return product.getPriceRange().getMinimumPrice().getFinalPrice().getValue();
    }

    @Override
    public Double getRegularPrice() {
        return product.getPriceRange().getMinimumPrice().getRegularPrice().getValue();
    }

    @Override
    public String getCurrencyCode() {
        return product.getPriceRange().getMinimumPrice().getFinalPrice().getCurrency().toString();
    }

}
