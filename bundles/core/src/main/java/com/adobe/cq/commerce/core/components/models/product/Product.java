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

package com.adobe.cq.commerce.core.components.models.product;

import java.util.List;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.page.PageMetadata;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Product is the sling model interface for the CIF core product component.
 */
public interface Product extends Component, PageMetadata {

    /**
     * Name of the boolean resource property indicating if the product component should load prices on the client-side.
     */
    String PN_LOAD_CLIENT_PRICE = "loadClientPrice";

    Boolean getFound();

    String getName();

    String getDescription();

    String getSku();

    /**
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    String getCurrency();

    /**
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    Double getPrice();

    Price getPriceRange();

    /**
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    String getFormattedPrice();

    Boolean getInStock();

    Boolean isConfigurable();

    Boolean isGroupedProduct();

    Boolean isVirtualProduct();

    Boolean isBundleProduct();

    String getVariantsJson();

    List<Variant> getVariants();

    List<GroupItem> getGroupedProductItems();

    List<Asset> getAssets();

    String getAssetsJson();

    List<VariantAttribute> getVariantAttributes();

    Boolean loadClientPrice();

    AbstractProductRetriever getProductRetriever();
}
