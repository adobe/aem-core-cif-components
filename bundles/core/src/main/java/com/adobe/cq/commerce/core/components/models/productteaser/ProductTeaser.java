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
package com.adobe.cq.commerce.core.components.models.productteaser;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Product Teaser is the sling model interface for the CIF Teaser component.
 */
@ConsumerType
public interface ProductTeaser extends Component {

    /**
     * Returns the identifier of this product.
     *
     * @return a {@link CommerceIdentifier} object representing the identifier of this product.
     */
    CommerceIdentifier getCommerceIdentifier();

    /**
     * Returns name of the configured Product for this {@code ProductTeaser}
     *
     * @return name of the configured Product for this Teaser of {@code null}
     */
    String getName();

    /**
     * Returns url of swatch image of the product for display for this {@code ProductTeaser}
     *
     * @return url of the swatch image for the product or {@code null}
     */
    String getImage();

    /**
     * Returns alt text of swatch image of the product for display for this {@code ProductTeaser}
     *
     * @return alt text of the swatch image for the product or {@code null}
     */
    String getImageAlt();

    /**
     * Returns the url of the product page for this {@code ProductTeaser}
     *
     * @return the url of the product page of the configured product or {@code null}
     */
    String getUrl();

    /**
     * Returns the link target for the links generated on the product teaser.
     *
     * @return the link target or {@code null} if no link target is configured
     */
    default String getLinkTarget() {
        return null;
    }

    /**
     * Returns the effective SKU of the product displayed by this {@code ProductTeaser}. For a variant of a configurable product thi is
     * always the SKU of the configured variant.
     *
     * @return a String value representing the SKU
     */
    String getSku();

    /**
     * Returns the SKU of the product displayed by this {@code ProductTeaser} as {@link CombinedSku}
     *
     * @return a {@link CombinedSku} representing the SKU
     */
    CombinedSku getCombinedSku();

    /**
     * Returns the "call to action" configured for this teaser.
     * If the configured product cannot be added directly to the cart and for CTA
     * "add-to-cart" is configured then "details" is returned.
     * If no CTA is configured then {@code null} is returned.
     *
     * @return the value of the "call to action" option. This can be "add-to-cart" or "details".
     */
    String getCallToAction();

    /**
     * Returns the "call to action text" configured for this teaser.
     * If no CTA text is configured then this methods returns {@code null} unless the configured product
     * cannot be added directly to the cart, when "Add to Cart" is returned.
     *
     * @return the value of the "call to action text" option.
     */
    String getCallToActionText();

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

    /**
     * Returns true when the Add to Wish List button is enabled.
     *
     * @return
     */
    default boolean getAddToWishListEnabled() {
        return false;
    }

    /**
     * Returns true when the component should load the product price client side.
     *
     * @return
     */
    default boolean loadClientPrice() {
        return false;
    }

}
