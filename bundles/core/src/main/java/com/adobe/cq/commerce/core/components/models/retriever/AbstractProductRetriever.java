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

import java.util.List;
import java.util.Optional;
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
import com.shopify.graphql.support.Input;

/**
 * Abstract implementation of product retriever that loads product data using GraphQL.
 */
public abstract class AbstractProductRetriever extends AbstractRetriever {

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
     * Product instance. Is only available after populate() was called.
     */
    protected Optional<ProductInterface> product;

    /**
     * SKU identifier of the product that should be fetched.
     */
    protected String identifier;

    public AbstractProductRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    /**
     * Executes the GraphQL query and returns a product. For subsequent calls of this method, a cached product is returned.
     *
     * @return Product
     */
    public ProductInterface fetchProduct() {
        if (this.product == null) {
            populate();
        }
        return this.product.orElse(null);
    }

    /**
     * Set the identifier of the product that should be fetched. Products are retrieved using the default identifier SKU.
     *
     * @param identifier Product SKU identifier
     */
    public void setIdentifier(String identifier) {
        product = null;
        query = null;
        this.identifier = identifier;
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
     * If called multiple times, each hook will be "appended" to the previously registered hook(s).
     *
     * @param productQueryHook Lambda that extends the product query
     */
    public void extendProductQueryWith(Consumer<ProductInterfaceQuery> productQueryHook) {
        if (this.productQueryHook == null) {
            this.productQueryHook = productQueryHook;
        } else {
            this.productQueryHook = this.productQueryHook.andThen(productQueryHook);
        }
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
     * If called multiple times, each hook will be "appended" to the previously registered hook(s).
     *
     * @param variantQueryHook Lambda that extends the product variant query
     */
    public void extendVariantQueryWith(Consumer<SimpleProductQuery> variantQueryHook) {
        if (this.variantQueryHook == null) {
            this.variantQueryHook = variantQueryHook;
        } else {
            this.variantQueryHook = this.variantQueryHook.andThen(variantQueryHook);
        }
    }

    /**
     * Extends or replaces the product attribute filter with a custom instance defined by a lambda hook.
     *
     * Example 1 (Extend):
     *
     * <pre>
     * {@code
     * productRetriever.extendProductFilterWith(f -> f
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * Example 2 (Replace):
     *
     * <pre>
     * {@code
     * productRetriever.extendProductFilterWith(f -> new ProductAttributeFilterInput()
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
     * Generate a complete product GraphQL query with a filter for the given product identifier.
     *
     * @param identifier Product identifier, usually SKU or slug
     * @return GraphQL query as string
     */
    protected String generateQuery(String identifier) {
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput();
        FilterEqualTypeInput identifierFilter = new FilterEqualTypeInput().setEq(identifier);
        filter.setSkuInput(Input.optional(identifierFilter));

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
            query = generateQuery(identifier);
        }
        return client.execute(query);
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

    @Override
    protected void populate() {
        // Get product list from response
        GraphqlResponse<Query, Error> response = executeQuery();
        errors = response.getErrors();
        if (CollectionUtils.isEmpty(errors)) {
            Query rootQuery = response.getData();
            List<ProductInterface> products = rootQuery.getProducts().getItems();

            // Return first product in list unless the identifier type is 'url_key',
            // then return the product whose 'url_key' matches the identifier
            if (products.size() > 0) {
                product = Optional.of(products.get(0));
            } else {
                product = Optional.empty();
            }
        } else {
            product = Optional.empty();
        }
    }

    /**
     * Generates the partial ProductInterface query part of the GraphQL product query.
     *
     * @return ProductInterface query definition
     */
    abstract protected ProductInterfaceQueryDefinition generateProductQuery();
}
