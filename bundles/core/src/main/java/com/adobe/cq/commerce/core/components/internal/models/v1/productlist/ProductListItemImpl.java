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

package com.adobe.cq.commerce.core.components.internal.models.v1.productlist;

import java.text.NumberFormat;
import java.util.Locale;

import javax.annotation.Nullable;

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.day.cq.wcm.api.Page;

public class ProductListItemImpl implements ProductListItem {

    private final String sku;
    private final String slug;
    private final String name;
    private final String imageURL;
    private final Double price;
    private final String currency;

    private NumberFormat priceFormatter;
    private Page productPage;

    public ProductListItemImpl(String sku, String slug, String name, Double price, String currency, String imageURL, Page productPage) {
        this.sku = sku;
        this.slug = slug;
        this.name = name;
        this.imageURL = imageURL;
        this.price = price;
        this.currency = currency;
        this.productPage = productPage;

        // Initialize NumberFormatter with locale from current page.
        // Alternatively, the locale can potentially be retrieved via
        // the storeConfig query introduced with Magento 2.3.1
        final Locale locale = this.productPage.getLanguage(false);
        this.priceFormatter = Utils.buildPriceFormatter(locale, this.getCurrency());
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
        return Utils.constructUrlfromSlug(productPage.getPath(), this.getSlug());
    }

    @Nullable
    @Override
    public String getTitle() {
        return name;
    }

    @Nullable
    @Override
    public Double getPrice() {
        return price;
    }

    @Nullable
    @Override
    public String getCurrency() {
        return currency;
    }

    @Nullable
    @Override
    public String getFormattedPrice() {
        return priceFormatter.format(price.doubleValue());
    }
}
