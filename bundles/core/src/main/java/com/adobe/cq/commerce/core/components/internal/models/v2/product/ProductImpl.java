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
package com.adobe.cq.commerce.core.components.internal.models.v2.product;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Product.class,
    resourceType = ProductImpl.RESOURCE_TYPE)
public class ProductImpl extends com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImpl implements Product {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v2/product";

    @PostConstruct
    protected void initModel() {
        super.initModel();
        if (productRetriever != null) {
            productRetriever.extendProductQueryWith(p -> p.staged());
            productRetriever.extendVariantQueryWith(v -> v.staged());
        }
    }

    @Override
    public Boolean isStaged() {
        // A product is considered "staged" if the product itself or one of its variant or item is "staged"
        ProductInterface product = productRetriever.fetchProduct();
        if (Boolean.TRUE.equals(product.getStaged())) {
            return true;
        } else if (isConfigurable()) {
            ConfigurableProduct cp = (ConfigurableProduct) product;
            return cp.getVariants().stream().anyMatch(v -> Boolean.TRUE.equals(v.getProduct().getStaged()));
        } else if (isGroupedProduct()) {
            GroupedProduct gp = (GroupedProduct) product;
            return gp.getItems().stream().anyMatch(i -> Boolean.TRUE.equals(i.getProduct().getStaged()));
        }
        return false;
    }
}
