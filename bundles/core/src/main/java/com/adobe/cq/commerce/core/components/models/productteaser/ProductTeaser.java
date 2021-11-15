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

import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Product Teaser is the sling model interface for the CIF Teaser component.
 */
public interface ProductTeaser extends Component {
    String CALL_TO_ACTION_TYPE_ADD_TO_CART = "add-to-cart";
    String CALL_TO_ACTION_TYPE_DETAILS = "details";
    String CALL_TO_ACTION_COMMAND_ADD_TO_CART = "addToCart";
    String CALL_TO_ACTION_COMMAND_DETAILS = CALL_TO_ACTION_TYPE_DETAILS;

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
     * Returns the SKU of the product displayed by this {@code ProductTeaser}
     * 
     * @return a String value representing the SKU
     */
    String getSku();

    /**
     * Returns the "call to action" configured for this teaser.
     * 
     * @return the value of the "call to action" option. This can be "add-to-cart" or "details". If no CTA is configured then this methods
     *         returns {@code null}
     */
    String getCallToAction();

    /**
     * Returns the "call to action text" configured for this teaser.
     * 
     * @return the value of the "call to action text" option.
     *         If no CTA text is configured then this methods returns
     *         {@code null}
     */
    String getCallToActionText();

    /**
     * Returns the call to action command for the configured call to action.
     *
     * @return "addToCart" or "details" or {@code null} if call to action is not configured
     */
    default String getCallToActionCommand() {
        if (CALL_TO_ACTION_TYPE_DETAILS.equals(getCallToAction())) {
            return CALL_TO_ACTION_COMMAND_DETAILS;
        } else if (CALL_TO_ACTION_TYPE_ADD_TO_CART.equals(getCallToAction())) {
            return CALL_TO_ACTION_COMMAND_ADD_TO_CART;
        }

        return null;
    }

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
