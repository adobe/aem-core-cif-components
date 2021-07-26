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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerListItem;
import com.adobe.cq.commerce.core.components.internal.datalayer.ProductDataImpl;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider.ParamsBuilder;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProductListItemImpl extends DataLayerListItem implements ProductListItem {

    private String sku;
    private String slug;
    private String name;
    private String imageURL;
    private String imageAlt;
    private Price price;
    private String activeVariantSku;
    private Page productPage;
    private SlingHttpServletRequest request;
    private UrlProvider urlProvider;
    private CommerceIdentifier identifier;
    private Boolean isStaged;

    public ProductListItemImpl(String sku, String slug, String name, Price price, String imageURL, String imageAlt, Page productPage,
                               String activeVariantSku, SlingHttpServletRequest request, UrlProvider urlProvider, String parentId,
                               Boolean isStaged) {
        super(parentId, productPage.getContentResource());
        this.sku = sku;
        this.slug = slug;
        this.name = name;
        this.imageURL = imageURL;
        this.imageAlt = StringUtils.defaultIfBlank(imageAlt, name);
        this.price = price;
        this.productPage = productPage;
        this.activeVariantSku = activeVariantSku;
        this.request = request;
        this.urlProvider = urlProvider;
        this.isStaged = isStaged;
    }

    public ProductListItemImpl(CommerceIdentifier identifier, String parentId, Page productPage) {
        super(parentId, productPage.getContentResource());
        this.identifier = identifier;
        this.productPage = productPage;
        switch (identifier.getType()) {
            case SKU:
                this.sku = identifier.getValue();
                break;
            case URL_KEY:
                this.slug = identifier.getValue();
        }
    }

    @Override
    @JsonIgnore
    public String getSKU() {
        return sku;
    }

    @Override
    @JsonIgnore
    public String getSlug() {
        return slug;
    }

    @Nullable
    @Override
    @JsonIgnore
    public String getImageURL() {
        return imageURL;
    }

    @Nullable
    @Override
    @JsonIgnore
    public String getImageAlt() {
        return imageAlt;
    }

    @Nullable
    @Override
    @JsonIgnore
    public String getURL() {

        if (urlProvider == null) {
            return "";
        }
        Map<String, String> params = new ParamsBuilder()
            .sku(sku)
            .urlKey(slug)
            .variantSku(activeVariantSku)
            .map();

        return urlProvider.toProductUrl(request, productPage, params);
    }

    @Nullable
    @Override
    @JsonIgnore
    public String getTitle() {
        return name;
    }

    @Override
    @JsonIgnore
    public Price getPriceRange() {
        return price;
    }

    @Override
    @JsonIgnore
    public Boolean isStaged() {
        return Boolean.TRUE.equals(isStaged);
    }

    // DataLayer methods

    @Override
    protected ComponentData getComponentData() {
        return new ProductDataImpl(this, this.productPage.getContentResource());
    }

    @Override
    protected String generateId() {
        String prefix = StringUtils.join(parentId, ID_SEPARATOR, ITEM_ID_PREFIX);
        return ComponentUtils.generateId(prefix, getSKU());
    }

    @Override
    public String getDataLayerType() {
        return "core/cif/components/commerce/productlistitem";
    }

    @Override
    public String getDataLayerTitle() {
        return this.getTitle();
    }

    @Override
    public String getDataLayerSKU() {
        return this.getSKU();
    }

    @Override
    public Double getDataLayerPrice() {
        return this.getPriceRange() != null ? this.getPriceRange().getFinalPrice() : null;
    }

    @Override
    public String getDataLayerCurrency() {
        return this.getPriceRange() != null ? this.getPriceRange().getCurrency() : null;
    }

    @Override
    public CommerceIdentifier getCommerceIdentifier() {
        return identifier;
    }
}
