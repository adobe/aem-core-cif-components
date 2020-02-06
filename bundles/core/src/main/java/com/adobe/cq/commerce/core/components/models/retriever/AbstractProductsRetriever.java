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

import java.util.List;
import java.util.function.Consumer;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public abstract class AbstractProductsRetriever extends AbstractRetriever {

    /**
     * Lambda that extends the product query.
     */
    protected Consumer<ProductInterfaceQuery> productQueryHook;

    /**
     * Lambda that extends the product variant query.
     */
    protected Consumer<SimpleProductQuery> variantQueryHook;

    /**
     * List of product instances. Is only available after populate() was called.
     */
    protected List<ProductInterface> products;

    /**
     * Identifier of the product that should be fetched. Which kind of identifier is used (usually slug or SKU) is implementation
     * specific and should be checked in subclass implementations.
     */
    protected List<String> identifiers;

    public AbstractProductsRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    /**
     * Executes the GraphQL query and returns a product. For subsequent calls of this method, a cached product is returned.
     *
     * @return Product
     */
    public List<ProductInterface> fetchProducts() {
        if (this.products == null) {
            populate();
        }
        return this.products;
    }

    /**
     * Set the identifiers of the products that should be fetched. Which kind of identifier is used (usually slug or SKU) is implementation
     * specific and should be checked in subclass implementations. Setting the identifier, removes any cached data.
     *
     * @param identifiers Product identifier
     */
    public void setIdentifiers(List<String> identifiers) {
        products = null;
        query = null;
        this.identifiers = identifiers;
    }

    /**
     * Extend the product GraphQL query with a partial query provided by a lambda hook that sets additional fields.
     *
     * Example:
     *
     * <pre>
     * {@code
     * productRetriever.extendProductQueryWith(p -> p
     *     .createdAt()
     *     .addCustomSimpleField("is_returnable"));
     * }
     * </pre>
     *
     * @param productQueryHook Lambda that extends the product query
     */
    public void extendProductQueryWith(Consumer<ProductInterfaceQuery> productQueryHook) {
        this.productQueryHook = productQueryHook;
    }

    /**
     * Extend the product variant GraphQL query with a partial query provided by a lambda hook that sets additional fields.
     *
     * Example:
     *
     * <pre>
     * {@code
     * productRetriever.extendVariantQueryWith(s -> s
     *     .createdAt()
     *     .addCustomSimpleField("is_returnable"));
     * }
     * </pre>
     *
     * @param variantQueryHook Lambda that extends the product variant query
     */
    public void extendVariantQueryWith(Consumer<SimpleProductQuery> variantQueryHook) {
        this.variantQueryHook = variantQueryHook;
    }

    /**
     * Generate a complete product GraphQL query with a filter for the given product identifiers.
     *
     * @param identifiers Product identifiers, usually SKU or slug
     * @return GraphQL query as string
     */
    protected String generateQuery(List<String> identifiers) {
        FilterEqualTypeInput skuFilter = new FilterEqualTypeInput().setIn(identifiers);
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setSku(skuFilter);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        ProductsQueryDefinition queryArgs = q -> q.items(generateProductQuery());
        return Operations.query(query -> query
            .products(searchArgs, queryArgs)).toString();
    }

    /**
     * Execute the GraphQL query with the GraphQL client.
     *
     * @return GraphqlResponse object
     */
    protected GraphqlResponse<Query, Error> executeQuery() {
        if (query == null) {
            query = generateQuery(identifiers);
        }
        return client.execute(query);
    }

    @Override
    protected void populate() {
        // Get product list from response
        GraphqlResponse<Query, Error> response = executeQuery();
        Query rootQuery = response.getData();
        products = rootQuery.getProducts().getItems();
    }

    /**
     * Generates the partial ProductInterface query part of the GraphQL product query.
     *
     * @return ProductInterface query definition
     */
    abstract protected ProductInterfaceQueryDefinition generateProductQuery();

}
