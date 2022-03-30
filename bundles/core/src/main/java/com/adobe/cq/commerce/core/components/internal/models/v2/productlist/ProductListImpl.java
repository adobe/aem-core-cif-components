/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v2.productlist;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.models.productlist.ProductList;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ProductList.class,
    resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl extends com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl implements
    ProductList {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/productlist/v2/productlist";

    @PostConstruct
    protected void initModel() {
        super.initModel();
        if (categoryRetriever != null) {
            categoryRetriever.extendCategoryQueryWith(c -> c.staged());
            categoryRetriever.extendProductQueryWith(p -> p.staged());
        }
    }

    @Override
    public Boolean isStaged() {
        return getCategory() != null ? Boolean.TRUE.equals(getCategory().getStaged()) : false;
    }
}
