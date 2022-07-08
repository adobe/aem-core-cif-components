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
package com.adobe.cq.commerce.core.components.models.common;

import javax.annotation.Nullable;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.wcm.core.components.models.ListItem;

@ConsumerType
public interface ProductListItem extends ListItem {

    /**
     * Returns the product SKU of this {@code ProductListItem}. This is always the SKU of the base product even for a variant of a
     * configurable product. Use {@link ProductListItem#getCombinedSku()} to get access to the variant's sku if needed.
     *
     * @return the product SKU of this list item or {@code null}
     */
    @Nullable
    String getSKU();

    /**
     * Returns the SKU of this {@code ProductListItem} as {@link CombinedSku}
     *
     * @return a {@link CombinedSku} representing the SKU
     */
    default CombinedSku getCombinedSku() {
        return new CombinedSku(getSKU(), null);
    }

    /**
     * Returns the product slug of this {@code ProductListItem}.
     *
     * @return the product slug of this list item or {@code null}
     */
    @Nullable
    String getSlug();

    /**
     * Returns the product image URL of this {@code ProductListItem}.
     *
     * @return the product image URL of this list item or {@code null}
     */
    @Nullable
    String getImageURL();

    /**
     * Returns the product image alt text of this {@code ProductListItem}.
     *
     * @return the product image alt text of this list item or {@code null}
     */
    @Nullable
    String getImageAlt();

    /**
     * Returns the identifier of this product. If the returned {@link CommerceIdentifier} is of type
     * {@link CommerceIdentifier.IdentifierType#SKU} and the product is a configurable product the value of the identifier is the
     * variant's sku. Otherwise, it is the base product's sku.
     * 
     * @return a {@link CommerceIdentifier} object representing the identifier of this product.
     */
    CommerceIdentifier getCommerceIdentifier();

    /**
     * Returns the price range of this {@code ProductListItem}.
     *
     * @return Price range instance.
     */
    Price getPriceRange();

    /**
     * @return <code>true</code> if the product data contains staged changes, <code>false</code> otherwise.
     * @since com.adobe.cq.commerce.core.components.models.common 1.9.0
     */
    default Boolean isStaged() {
        return false;
    };

    /**
     * Returns the backend product using the GraphQL {@code ProductInterface}.
     *
     * @return The product.
     */
    ProductInterface getProduct();

    /**
     * Returns the call to action command for the 'Add to Cart' button of the product list item.
     *
     * @return {@code add-to-cart} if the related product can be added to the shopping cart, {@code details} otherwise
     */
    default String getCallToAction() {
        return "";
    }
}
