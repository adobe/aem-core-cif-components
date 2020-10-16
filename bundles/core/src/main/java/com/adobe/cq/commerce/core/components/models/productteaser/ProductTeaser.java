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

package com.adobe.cq.commerce.core.components.models.productteaser;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Product Teaser is the sling model interface for the CIF Teaser component.
 */
public interface ProductTeaser extends Component {

    /**
     * Returns name of the configured Product for this {@code ProductTeaser}
     *
     * @return name of the configured Product for this Teaser of {@code null}
     */
    String getName();

    /**
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    String getFormattedPrice();

    /**
     * Returns url of swatch image of the product for display for this {@code ProductTeaser}
     *
     * @return url of the swatch image for the product or {@code null}
     */
    String getImage();

    /**
     * Returns the url of the product page for this {@code ProductTeaser}
     *
     * @return the url of the product page of the configured product or {@code null}
     */
    String getUrl();

    /**
     * Returns the SKU of the product displayed by this {@code ProductTeaser}
     * 
     * @return a String value representing the SKU
     */
    String getSku();

    /**
     * Returns the "call to action" configured for this teaser.
     * 
     * @return the value of the "call to action" option. This can be "add-to-cart" or "details". If no CTA is configured then this methods
     *         returns {@link null}
     */
    String getCallToAction();

    /**
     * Returns the price range.
     *
     * @return Price range instance.
     */
    Price getPriceRange();

    /**
     * Returns true if the product is a virtual product.
     *
     * @return Boolean
     */
    Boolean isVirtualProduct();

    /**
     * Returns in instance of the product retriever for fetching product data via GraphQL.
     *
     * @return product retriever instance
     */
    AbstractProductRetriever getProductRetriever();

}
