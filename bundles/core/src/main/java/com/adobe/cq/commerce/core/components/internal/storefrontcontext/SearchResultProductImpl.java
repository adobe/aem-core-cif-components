/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.storefrontcontext;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultProduct;

public class SearchResultProductImpl implements SearchResultProduct {

    private final ProductListItem productListItem;

    public SearchResultProductImpl(ProductListItem productListItem) {
        this.productListItem = productListItem;
    }

    @Override
    public String getName() {
        return productListItem.getTitle();
    }

    @Override
    public String getSku() {
        return productListItem.getSKU();
    }

    @Override
    public String getUrl() {
        return productListItem.getURL();
    }

    @Override
    public String getImageUrl() {
        return productListItem.getImageURL();
    }

    @Override
    public Double getPrice() {
        return productListItem.getPriceRange().getFinalPrice();
    }

    @Override
    public int getRank() {
        return 0;
    }
}
