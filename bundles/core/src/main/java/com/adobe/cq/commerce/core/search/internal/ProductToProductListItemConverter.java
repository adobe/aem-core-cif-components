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

package com.adobe.cq.commerce.core.search.internal;

import java.util.Locale;
import java.util.function.Function;

import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

/**
 * Converts a {@link ProductInterface} object into a {@link ProductListItem}.
 */
public class ProductToProductListItemConverter implements Function<ProductInterface, ProductListItem> {

    private final Page productPage;
    private final Locale locale;

    private final SlingHttpServletRequest request;

    public ProductToProductListItemConverter(final Page productPage, final SlingHttpServletRequest request) {
        this.productPage = productPage;
        this.locale = productPage.getLanguage(false);
        this.request = request;
    }

    @Override
    public ProductListItem apply(final ProductInterface product) {

        Price price = new PriceImpl(product.getPriceRange(), locale);

        ProductListItem productListItem = new ProductListItemImpl(product.getSku(),
            product.getUrlKey(),
            product.getName(),
            price,
            product.getSmallImage()
                .getUrl(),
            productPage,
            null, // search results aren't targeting specific variant
            request);

        return productListItem;
    }
}
