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
package com.adobe.cq.commerce.core.components.models.retriever;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPriceQueryDefinition;
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
     * Lambda that allows to replace or extend the product attribute filters.
     */
    protected Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook;

    /**
     * List of product instances. Is only available after populate() was called.
     */
    protected List<ProductInterface> products;

    /**
     * Product SKU identifiers of the product that should be fetched.
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
     * Set the identifiers of the products that should be fetched. Products are retrieved using the default identifier SKU.
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
     * Extends or replaces the product attribute filter with a custom instance defined by a lambda hook.
     *
     * Example 1 (Extend):
     *
     * <pre>
     * {@code
     * productsRetriever.extendProductFilterWith(f -> f
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * Example 2 (Replace):
     *
     * <pre>
     * {@code
     * productsRetriever.extendProductFilterWith(f -> new ProductAttributeFilterInput()
     *     .setSku(new FilterEqualTypeInput()
     *         .setEq("custom-sku"))
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * @param productAttributeFilterHook Lambda that extends or replaces the product attribute filter.
     */
    public void extendProductFilterWith(Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook) {
        if (this.productAttributeFilterHook == null) {
            this.productAttributeFilterHook = productAttributeFilterHook;
        } else {
            this.productAttributeFilterHook = this.productAttributeFilterHook.andThen(productAttributeFilterHook);
        }
    }

    /**
     * Generate a complete product GraphQL query with a filter for the given product identifiers.
     *
     * @param identifiers product SKUs
     * @return GraphQL query as string
     */
    protected String generateQuery(List<String> identifiers) {
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput();
        FilterEqualTypeInput skuFilter = new FilterEqualTypeInput().setIn(identifiers);
        filter.setSku(skuFilter);

        // Apply product attribute filter hook
        if (this.productAttributeFilterHook != null) {
            filter = this.productAttributeFilterHook.apply(filter);
        }

        ProductAttributeFilterInput finalFilter = filter;
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(finalFilter);

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
        errors = response.getErrors();
        if (CollectionUtils.isEmpty(errors)) {
            Query rootQuery = response.getData();
            products = rootQuery.getProducts().getItems();
        } else {
            products = Collections.emptyList();
        }
    }

    protected ProductPriceQueryDefinition generatePriceQuery() {
        return q -> q
            .regularPrice(r -> r
                .value()
                .currency())
            .finalPrice(f -> f
                .value()
                .currency())
            .discount(d -> d
                .amountOff()
                .percentOff());
    }

    /**
     * Generates the partial ProductInterface query part of the GraphQL product query.
     *
     * @return ProductInterface query definition
     */
    abstract protected ProductInterfaceQueryDefinition generateProductQuery();

}
