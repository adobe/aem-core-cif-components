/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerListItem;
import com.adobe.cq.commerce.core.components.internal.datalayer.ProductDataImpl;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.drew.lang.annotations.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProductListItemImpl extends DataLayerListItem implements ProductListItem {

    public static final String TYPE = "core/cif/components/commerce/productlistitem";

    private String sku;
    private String urlKey;
    private String urlPath;
    private List<UrlRewrite> urlRewrites;
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
    private ProductInterface product;

    private ProductListItemImpl(ProductInterface product, String sku, String urlKey, String urlPath, List<UrlRewrite> urlRewrites,
                                String name, Price price, String imageURL, String imageAlt, Page productPage, String activeVariantSku,
                                SlingHttpServletRequest request, UrlProvider urlProvider, String parentId, Boolean isStaged) {
        super(parentId, productPage.getContentResource());
        this.product = product;
        this.sku = sku;
        this.urlKey = urlKey;
        this.urlPath = urlPath;
        this.urlRewrites = urlRewrites;
        this.name = name;
        this.imageURL = imageURL;
        this.imageAlt = StringUtils.defaultIfBlank(imageAlt, name);
        this.price = price;
        this.productPage = productPage;
        this.activeVariantSku = activeVariantSku;
        this.request = request;
        this.urlProvider = urlProvider;
        this.isStaged = isStaged;
        this.identifier = activeVariantSku != null
            ? new CommerceIdentifierImpl(activeVariantSku, CommerceIdentifier.IdentifierType.SKU, CommerceIdentifier.EntityType.PRODUCT)
            : new CommerceIdentifierImpl(sku, CommerceIdentifier.IdentifierType.SKU, CommerceIdentifier.EntityType.PRODUCT);
    }

    @Deprecated
    public ProductListItemImpl(CommerceIdentifier identifier, String parentId, Page productPage) {
        super(parentId, productPage.getContentResource());
        this.identifier = identifier;
        this.productPage = productPage;
        switch (identifier.getType()) {
            case SKU:
                this.sku = identifier.getValue();
                break;
            case URL_KEY:
                this.urlKey = identifier.getValue();
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
        return urlKey;
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

        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setSku(sku);
        params.setUrlKey(urlKey);
        params.setVariantSku(activeVariantSku);
        params.setUrlPath(urlPath);
        params.setUrlRewrites(urlRewrites);

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
    protected String getIdentifier() {
        String itemIdentifier = sku;
        if (StringUtils.isNotEmpty(activeVariantSku)) {
            itemIdentifier += "#" + activeVariantSku;
        }
        return StringUtils.defaultIfBlank(itemIdentifier, StringUtils.EMPTY);
    }

    @Override
    public String getDataLayerType() {
        return TYPE;
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

    @Override
    public ProductInterface getProduct() {
        return product;
    }

    public static class Builder {

        private final String parentId;
        private final Page productPage;
        private final SlingHttpServletRequest request;
        private final UrlProvider urlProvider;
        private ProductInterface product;
        private ProductImage image;
        private Price price;
        private String urlKey;
        private String sku;
        private String variantSku;
        private String name;
        private String urlPath;
        private List<UrlRewrite> urlRewrites;

        public Builder(String parentId, @NotNull Page productPage, SlingHttpServletRequest request, UrlProvider urlProvider) {
            this.parentId = parentId;
            this.productPage = Objects.requireNonNull(productPage, "product page is required");
            this.request = request;
            this.urlProvider = urlProvider;
        }

        public Builder product(ProductInterface product) {
            this.product = product;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder urlKey(String urlKey) {
            this.urlKey = urlKey;
            return this;
        }

        public Builder urlPath(String urlPath) {
            this.urlPath = urlPath;
            return this;
        }

        public Builder urlRewrites(List<UrlRewrite> urlRewrites) {
            this.urlRewrites = urlRewrites;
            return this;
        }

        public Builder variantSku(String variantSku) {
            this.variantSku = variantSku;
            return this;
        }

        public Builder image(ProductImage productImage) {
            this.image = productImage;
            return this;
        }

        public Builder price(Price price) {
            this.price = price;
            return this;
        }

        public ProductListItem build() {
            String sku = this.sku == null && product != null ? product.getSku() : this.sku;
            String urlKey = this.urlKey == null && product != null ? product.getUrlKey() : this.urlKey;
            String urlPath = this.urlPath == null && product != null ? product.getUrlPath() : this.urlPath;
            List<UrlRewrite> urlRewrites = this.urlRewrites == null && product != null ? product.getUrlRewrites() : this.urlRewrites;
            String name = this.name == null && product != null ? product.getName() : this.name;
            Price price = this.price == null && product != null
                ? new PriceImpl(product.getPriceRange(), productPage.getLanguage(false), product instanceof GroupedProduct)
                : this.price;
            ProductImage image = this.image == null && product != null ? product.getSmallImage() : this.image;
            String imageUrl = image != null ? image.getUrl() : null;
            String imageLabel = image != null ? StringUtils.defaultIfBlank(image.getLabel(), name) : null;
            boolean isStaged = product != null && Boolean.TRUE.equals(product.getStaged());

            return new ProductListItemImpl(product, sku, urlKey, urlPath, urlRewrites, name, price, imageUrl, imageLabel, productPage,
                variantSku, request, urlProvider, parentId, isStaged);
        }
    }
}
