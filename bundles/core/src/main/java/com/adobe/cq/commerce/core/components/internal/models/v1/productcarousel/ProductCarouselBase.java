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
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerListItem;
import com.adobe.cq.commerce.core.components.internal.datalayer.ProductDataImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.CommerceIdentifierImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ProductCarouselBase extends DataLayerComponent implements ProductCarousel {

    protected static final boolean ENABLE_ADD_TO_CART_DEFAULT = false;
    protected static final boolean ENABLE_ADD_TO_WISH_LIST_DEFAULT = false;
    static final String PN_ENABLE_ADD_TO_CART = "enableAddToCart";
    static final String PN_ENABLE_ADD_TO_WISH_LIST = "enableAddToWishList";
    static final String PN_CONFIG_ENABLE_WISH_LISTS = "enableWishLists";

    @Self
    protected SlingHttpServletRequest request;
    @ScriptVariable
    protected Page currentPage;
    @ScriptVariable
    protected Style currentStyle;
    protected boolean addToCartEnabled;
    protected boolean addToWishListEnabled;

    @ValueMapValue(
        name = Link.PN_LINK_TARGET,
        injectionStrategy = InjectionStrategy.OPTIONAL)
    protected String linkTarget;

    @PostConstruct
    private void initModel0() {
        ValueMap properties = resource.getValueMap();
        ComponentsConfiguration configProperties = currentPage.adaptTo(Resource.class).adaptTo(ComponentsConfiguration.class);
        addToCartEnabled = properties.get(PN_ENABLE_ADD_TO_CART, currentStyle.get(PN_ENABLE_ADD_TO_CART, ENABLE_ADD_TO_CART_DEFAULT));
        addToWishListEnabled = (configProperties != null ? configProperties.get(PN_CONFIG_ENABLE_WISH_LISTS, Boolean.TRUE) : Boolean.TRUE);
        addToWishListEnabled = addToWishListEnabled && properties.get(PN_ENABLE_ADD_TO_WISH_LIST, currentStyle.get(
            PN_ENABLE_ADD_TO_WISH_LIST, ENABLE_ADD_TO_WISH_LIST_DEFAULT));
    }

    @Override
    public boolean isAddToCartEnabled() {
        return addToCartEnabled;
    }

    @Override
    public boolean isAddToWishListEnabled() {
        return addToWishListEnabled;
    }

    @Override
    @JsonIgnore
    public String getLinkTarget() {
        return Utils.normalizeLinkTarget(linkTarget);
    }

    /**
     * And implementation of {@link CommerceIdentifier} that serializes to the same json format a {@link ProductListItemImpl} would
     * serialize to given only the {@link CommerceIdentifier}.
     */
    protected class ListItemIdentifier extends DataLayerListItem implements CommerceIdentifier {

        private final CommerceIdentifier commerceIdentifier;

        public ListItemIdentifier(String sku) {
            super(ProductCarouselBase.this.getId(), request.getResource());
            this.commerceIdentifier = new CommerceIdentifierImpl(sku, IdentifierType.SKU, EntityType.PRODUCT);
        }

        @Override
        protected String getIdentifier() {
            return getValue();
        }

        @Override
        @JsonIgnore
        public String getValue() {
            return commerceIdentifier.getValue();
        }

        @Override
        @JsonIgnore
        public IdentifierType getType() {
            return commerceIdentifier.getType();
        }

        @Override
        @JsonIgnore
        public EntityType getEntityType() {
            return commerceIdentifier.getEntityType();
        }

        @Override
        public String getDataLayerType() {
            return ProductListItemImpl.TYPE;
        }

        @Override
        public String getDataLayerSKU() {
            return this.getValue();
        }

        @Override
        protected ComponentData getComponentData() {
            return new ProductDataImpl(this, resource);
        }

        /**
         * Returns the instance itself, used by the JSON Exporter only.
         *
         * @return
         */
        public CommerceIdentifier getCommerceIdentifier() {
            return commerceIdentifier;
        }
    }
}
