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

package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import java.util.Map;

import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.day.cq.wcm.api.Page;

public class ProductListItemImpl implements ProductListItem {

    private final String sku;
    private final String slug;
    private final String name;
    private final String imageURL;
    private final Price price;
    private final String activeVariantSku;
    private final Page productPage;
    private final SlingHttpServletRequest request;
    private final UrlProvider urlProvider;

    public ProductListItemImpl(String sku, String slug, String name, Price price, String imageURL, Page productPage,
                               String activeVariantSku, SlingHttpServletRequest request, UrlProvider urlProvider) {
        this.sku = sku;
        this.slug = slug;
        this.name = name;
        this.imageURL = imageURL;
        this.price = price;
        this.productPage = productPage;
        this.activeVariantSku = activeVariantSku;
        this.request = request;
        this.urlProvider = urlProvider;
    }

    @Override
    public String getSKU() {
        return sku;
    }

    @Override
    public String getSlug() {
        return slug;
    }

    @Nullable
    @Override
    public String getImageURL() {
        return imageURL;
    }

    @Nullable
    @Override
    public String getURL() {
        Map<String, String> params = new ParamsBuilder()
            .sku(sku)
            .urlKey(slug)
            .variantSku(activeVariantSku)
            .map();

        return urlProvider.toProductUrl(request, productPage, params);
    }

    @Nullable
    @Override
    public String getTitle() {
        return name;
    }

    @Nullable
    @Override
    public Double getPrice() {
        return price.getFinalPrice();
    }

    @Nullable
    @Override
    public String getCurrency() {
        return price.getCurrency();
    }

    @Nullable
    @Override
    public String getFormattedPrice() {
        return price.getFormattedFinalPrice();
    }

    @Override
    public Price getPriceRange() {
        return price;
    }
}
