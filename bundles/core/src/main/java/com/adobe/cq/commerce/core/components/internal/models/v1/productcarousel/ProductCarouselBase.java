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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerListItem;
import com.adobe.cq.commerce.core.components.internal.datalayer.ProductDataImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.CommerceIdentifierImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProductCarouselBase extends DataLayerComponent {

    @Self
    protected SlingHttpServletRequest request;

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
