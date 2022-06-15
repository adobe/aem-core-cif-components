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

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerListItem;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.XfProductListItem;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

public class XfProductListItemImpl extends DataLayerListItem implements XfProductListItem {

    private Resource renderResource;

    public XfProductListItemImpl(Resource renderResource, String parentId, Page productPage) {
        super(parentId, productPage.getContentResource());
        this.renderResource = renderResource;
    }

    @Override
    public String getSKU() {
        return null;
    }

    @Override
    public String getSlug() {
        return null;
    }

    @Override
    public String getImageURL() {
        return null;
    }

    @Override
    public String getImageAlt() {
        return null;
    }

    @Override
    public CommerceIdentifier getCommerceIdentifier() {
        return null;
    }

    @Override
    public Price getPriceRange() {
        return null;
    }

    @Override
    public ProductInterface getProduct() {
        return null;
    }

    @Override
    public Resource getRenderResource() {
        return renderResource;
    }

}
