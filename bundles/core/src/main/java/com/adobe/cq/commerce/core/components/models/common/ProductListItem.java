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

package com.adobe.cq.commerce.core.components.models.common;

import javax.annotation.Nullable;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.wcm.core.components.models.ListItem;

@ConsumerType
public interface ProductListItem extends ListItem {

    /**
     * Returns the product SKU of this {@code ProductListItem}.
     *
     * @return the product SKU of this list item or {@code null}
     */
    default String getSKU() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the product slug of this {@code ProductListItem}.
     *
     * @return the product slug of this list item or {@code null}
     */
    default String getSlug() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the product image URL of this {@code ProductListItem}.
     *
     * @return the product image URL of this list item or {@code null}
     */
    @Nullable
    default String getImageURL() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the price of this {@code ProductListItem}.
     *
     * @return the price of this list item or {@code null}
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    @Nullable
    default Double getPrice() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the price currency of this {@code ProductListItem}.
     *
     * @return the currency of this list item or {@code null}
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    @Nullable
    String getCurrency();

    /**
     * Returns the formatted price of this {@code ProductListItem}.
     *
     * @return the formatted price of this list item or {@code null}
     * @deprecated Please use getPriceRange() instead.
     */
    @Deprecated
    @Nullable
    default String getFormattedPrice() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the price range of this {@code ProductListItem}.
     *
     * @return Price range instance.
     */
    Price getPriceRange();
}
