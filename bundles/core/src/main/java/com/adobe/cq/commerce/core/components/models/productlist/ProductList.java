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

package com.adobe.cq.commerce.core.components.models.productlist;

import org.osgi.annotation.versioning.ProviderType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

@ProviderType
public interface ProductList {

    /**
     * Name of the boolean resource property indicating if the product list should render the category title.
     */
    String PN_SHOW_TITLE = "showTitle";

    /**
     * Name of the String resource property indicating number of products to render on front-end.
     */
    String PN_PAGE_SIZE = "pageSize";

    /**
     * Returns the product list's items collection, as {@link ProductListItem}s elements.
     *
     * @return {@link Collection} of {@link ProductListItem}s
     */
    @Nonnull
    default Collection<ProductListItem> getProducts() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if the category / product list title should be rendered.
     *
     * @return {@code true} if category / product list title should be shown, {@code false} otherwise
     */
    default boolean showTitle() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the title of this {@code ProductList}.
     *
     * @return the title of this list item or {@code null}
     */
    @Nullable
    default String getTitle() {
        throw new UnsupportedOperationException();
    }

    default int getTotalCount() {
        throw new UnsupportedOperationException();
    }

    default int getCurrentNavPage() {
        throw new UnsupportedOperationException();
    }

    default int getNextNavPage() {
        throw new UnsupportedOperationException();
    }

    default int getPreviousNavPage() {
        throw new UnsupportedOperationException();
    }
}
