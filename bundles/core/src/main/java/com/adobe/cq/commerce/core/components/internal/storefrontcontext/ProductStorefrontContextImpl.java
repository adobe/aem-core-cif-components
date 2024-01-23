/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.storefrontcontext.Pricing;
import com.adobe.cq.commerce.core.components.storefrontcontext.ProductStorefrontContext;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

public class ProductStorefrontContextImpl extends AbstractCommerceStorefrontContext
    implements ProductStorefrontContext {

    private final ProductInterface product;
    private Pricing pricing = null;

    public ProductStorefrontContextImpl(ProductInterface product, Resource resource) {
        super(resource);
        this.product = product;
    }

    @Override
    public Integer getId() {
        return 0;
    }

    @Override
    public String getSku() {
        return product.getSku();
    }

    @Override
    public String getName() {
        return product.getName();
    }

    @Override
    public Pricing getPricing() {
        if (pricing == null) {
            pricing = new PricingImpl(product);
        }
        return pricing;
    }
}
