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

package com.adobe.cq.commerce.core.components.models.retriever;

import java.util.function.Consumer;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.shopify.graphql.support.AbstractQuery;

/**
 * Interface for product retriever which loads product data using GraphQL.
 */
public interface ProductRetriever {

    /**
     * Replace the product query with your own fully customized query.
     *
     * @param query GraphQL query for retrieving a product
     */
    void setQuery(String query);

    /**
     * Returns the product.
     *
     * @return Product
     */
    ProductInterface getProduct();

    /**
     * Returns the media base url from the store info.
     *
     * @return Media base url
     */
    String getMediaBaseUrl();

    /**
     * Set the slug of the product that should be fetched.
     *
     * @param slug Product slug
     */
    void setSlug(String slug);

    /**
     * Set a hook which adds an optional partial query to the product GraphQL query.
     *
     * Example:
     * 
     * <pre>
     * {@code
     * productRetriever.setProductQueryHook((ProductInterfaceQuery p) -> p
     *     .createdAt()
     *     .addCustomSimpleField("is_returnable"));
     * }
     * </pre>
     *
     * @param productQueryHook Lambda that extends the product query
     * @param <U> Query class that implements AbstractQuery
     */
    <U extends AbstractQuery<?>> void setProductQueryHook(Consumer<U> productQueryHook);

    /**
     * Set a hook which adds an optional partial query to the product variant GraphQL query.
     *
     * Example:
     * 
     * <pre>
     * {@code
     * productRetriever.setVariantQueryHook((SimpleProductQuery s) -> s
     *     .createdAt()
     *     .addCustomSimpleField("is_returnable"));
     * }
     * </pre>
     *
     * @param variantQueryHook Lambda that extends the product variant query
     * @param <U> Query class that implements AbstractQuery
     */
    <U extends AbstractQuery<?>> void setVariantQueryHook(Consumer<U> variantQueryHook);

}
