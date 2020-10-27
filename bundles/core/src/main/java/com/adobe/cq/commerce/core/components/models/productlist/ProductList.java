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

import javax.annotation.Nullable;

import com.adobe.cq.commerce.core.components.models.page.PageMetadata;
import com.adobe.cq.commerce.core.components.models.productcollection.ProductCollection;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;

public interface ProductList extends ProductCollection, PageMetadata {

    /**
     * Name of the boolean resource property indicating if the product list should render the category title.
     */
    String PN_SHOW_TITLE = "showTitle";

    /**
     * Name of the boolean resource property indicating if the product list should render the category image.
     */
    String PN_SHOW_IMAGE = "showImage";

    /**
     * Returns {@code true} if the category / product list title should be rendered.
     *
     * @return {@code true} if category / product list title should be shown, {@code false} otherwise
     */
    boolean showTitle();

    /**
     * Returns the title of this {@code ProductList}.
     *
     * @return the title of this list item or {@code null}
     */
    @Nullable
    String getTitle();

    String getImage();

    boolean showImage();

    /**
     * Returns in instance of the category retriever for fetching category data via GraphQL.
     *
     * @return category retriever instance
     */
    AbstractCategoryRetriever getCategoryRetriever();
}
