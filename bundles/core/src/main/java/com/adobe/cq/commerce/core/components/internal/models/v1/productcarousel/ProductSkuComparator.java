/*
 * Copyright 2019 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.util.Comparator;
import java.util.Map;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;

public class ProductSkuComparator implements Comparator<ProductInterface> {

    private Map<String, Integer> sortOrder;

    public ProductSkuComparator(Map<String, Integer> sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public int compare(ProductInterface product1, ProductInterface product2) {
        Integer productSkuPos1 = sortOrder.get(product1.getSku());
        if (productSkuPos1 == null) {
            throw new IllegalArgumentException("Sorting error -> Can not get Index value for :" + product1.getSku());
        }

        Integer productSkuPos2 = sortOrder.get(product2.getSku());
        if (productSkuPos2 == null) {
            throw new IllegalArgumentException("Sorting error -> Can not get Index value for :" + product2.getSku());
        }
        return productSkuPos1.compareTo(productSkuPos2);
    }
}