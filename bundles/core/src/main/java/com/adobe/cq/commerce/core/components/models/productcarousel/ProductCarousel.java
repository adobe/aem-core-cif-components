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

package com.adobe.cq.commerce.core.components.models.productcarousel;

import java.util.List;

import javax.annotation.Nonnull;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.wcm.core.components.models.Component;

public interface ProductCarousel extends Component {

    /**
     * Returns the list of products to be displayed in the carousel.
     * 
     * @return {@link List} of {@link ProductListItem}s
     */
    @Nonnull
    List<ProductListItem> getProducts();

    /**
     * Returns in instance of the products retriever for fetching product data via GraphQL.
     *
     * @return products retriever instance
     */
    AbstractProductsRetriever getProductsRetriever();

    /**
     * Returns true if the component is correctly configured, false otherwise.
     *
     * @return true or false
     */
    boolean isConfigured();

    /**
     * Should return the HTML tag type for the component title.
     * 
     * @return The HTML tag type that should be used to display the component title.
     */
    String getTitleType();
}
