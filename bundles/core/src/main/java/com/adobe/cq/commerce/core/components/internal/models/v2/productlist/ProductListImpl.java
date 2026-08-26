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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ProductList.class,
    resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl extends com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl implements
    ProductList {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/productlist/v2/productlist";

    /**
     * Name of the boolean configuration property controlling whether the Adobe Commerce Content Staging {@code staged}
     * field is requested. Defaults to {@code true} to keep existing Adobe Commerce deployments unchanged. Magento Open
     * Source backends, which do not support the {@code staged} field, must set this to {@code false}.
     */
    protected static final String PN_ENABLE_STAGING = "enableContentStaging";

    @PostConstruct
    protected void initModel() {
        super.initModel();
        if (categoryRetriever != null && isStagingEnabled()) {
            categoryRetriever.extendCategoryQueryWith(c -> c.staged());
            categoryRetriever.extendProductQueryWith(p -> p.staged());
        }
    }

    private boolean isStagingEnabled() {
        Resource contentResource = currentPage.adaptTo(Resource.class);
        ComponentsConfiguration configProperties = contentResource != null
            ? contentResource.adaptTo(ComponentsConfiguration.class)
            : null;
        return configProperties != null ? configProperties.get(PN_ENABLE_STAGING, Boolean.TRUE) : Boolean.TRUE;
    }

    @Override
    public Boolean isStaged() {
        return getCategory() != null ? Boolean.TRUE.equals(getCategory().getStaged()) : false;
    }
}
