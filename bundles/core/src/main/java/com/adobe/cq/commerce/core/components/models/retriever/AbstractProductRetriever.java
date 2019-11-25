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

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.shopify.graphql.support.AbstractQuery;

/**
 * Abstract implementation of product retriever that loads product data using GraphQL.
 */
public abstract class AbstractProductRetriever extends AbstractRetriever {

    private Consumer<AbstractQuery<?>> productQueryHook;
    private Consumer<AbstractQuery<?>> variantQueryHook;
    private ProductInterface product;
    private String mediaBaseUrl;
    private String identifier;

    public AbstractProductRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    /**
     * Returns the product.
     *
     * @return Product
     */
    public ProductInterface getProduct() {
        if (this.product == null) {
            populate();
        }
        return this.product;
    }

    /**
     * Stores a product.
     *
     * @param product Product
     */
    protected void setProduct(ProductInterface product) {
        this.product = product;
    }

    /**
     * Returns the media base url from the store info.
     *
     * @return Media base url
     */
    public String getMediaBaseUrl() {
        if (this.mediaBaseUrl == null) {
            populate();
        }
        return this.mediaBaseUrl;
    }

    /**
     * Stores the media base url.
     *
     * @param mediaBaseUrl Media base url
     */
    protected void setMediaBaseUrl(String mediaBaseUrl) {
        this.mediaBaseUrl = mediaBaseUrl;
    }

    /**
     * Returns the product identifier. This is usually either the slug or SKU.
     *
     * @return Product identifer
     */
    protected String getIdentifier() {
        return this.identifier;
    }

    /**
     * Set the identifier of the product that should be fetched. This is usually either the slug or SKU.
     *
     * @param identifier Product identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the product query hook lambda.
     *
     * @return Lambda that extends the product query
     */
    protected Consumer<AbstractQuery<?>> getProductQueryHook() {
        return this.productQueryHook;
    }

    /**
     * Extend the product GraphQL query with a partial query provided by a lambda hook that sets additional fields.
     *
     * Example:
     * 
     * <pre>
     * {@code
     * productRetriever.extendProductQueryWith((ProductInterfaceQuery p) -> p
     *     .createdAt()
     *     .addCustomSimpleField("is_returnable"));
     * }
     * </pre>
     *
     * @param productQueryHook Lambda that extends the product query
     * @param <U> Query class that implements AbstractQuery
     */
    public <U extends AbstractQuery<?>> void extendProductQueryWith(Consumer<U> productQueryHook) {
        this.productQueryHook = (Consumer<AbstractQuery<?>>) productQueryHook;
    }

    /**
     * Returns the product variant query hook lambda.
     *
     * @return Lambda that extends the product variant query
     */
    protected Consumer<AbstractQuery<?>> getVariantQueryHook() {
        return this.variantQueryHook;
    }

    /**
     * Extend the product variant GraphQL query with a partial query provided by a lambda hook that sets additional fields.
     *
     * Example:
     * 
     * <pre>
     * {@code
     * productRetriever.extendVariantQueryWith((SimpleProductQuery s) -> s
     *     .createdAt()
     *     .addCustomSimpleField("is_returnable"));
     * }
     * </pre>
     *
     * @param variantQueryHook Lambda that extends the product variant query
     * @param <U> Query class that implements AbstractQuery
     */
    public <U extends AbstractQuery<?>> void extendVariantQueryWith(Consumer<U> variantQueryHook) {
        this.variantQueryHook = (Consumer<AbstractQuery<?>>) variantQueryHook;
    }

}
