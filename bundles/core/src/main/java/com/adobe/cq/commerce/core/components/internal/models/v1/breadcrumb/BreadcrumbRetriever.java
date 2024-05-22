/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.breadcrumb;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.commons.collections4.CollectionUtils;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractRetriever;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.CategoryListArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

class BreadcrumbRetriever extends AbstractRetriever {

    private List<? extends CategoryInterface> categories;
    private Optional<ProductInterface> product;
    private UnaryOperator<ProductAttributeFilterInput> productIdentifierHook;
    private UnaryOperator<CategoryFilterInput> categoryIdentifierHook;

    BreadcrumbRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    /**
     * Executes the GraphQL query and returns an array of categories with breadcrumbs information.
     * For subsequent calls of this method, a cached response is returned.
     *
     * @return The list of categories with breadcrumbs information.
     */
    protected List<? extends CategoryInterface> fetchCategoriesBreadcrumbs() {
        if (categories == null) {
            populate();
        }
        return categories;
    }

    /**
     * Executes the GraphQL query and returns the name of the product.
     * This assumes that {@link #setProductIdentifierHook(UnaryOperator)} has been called before.
     * For subsequent calls of this method, a cached response is returned.
     *
     * @return The product name.
     */
    protected ProductInterface fetchProduct() {
        if (product == null) {
            populate();
        }
        return product.orElse(null);
    }

    /**
     * Set the sku of the product that should be fetched. Setting the a new product, removes any cached data.
     *
     * @param inputHook The product sku.
     */
    protected void setProductIdentifierHook(UnaryOperator<ProductAttributeFilterInput> inputHook) {
        this.productIdentifierHook = inputHook;
    }

    /**
     * Set the category uid of the category that should be fetched. Setting the a new category, removes any cached
     * data.
     *
     * @param inputHook The category uid.
     */
    protected void setCategoryIdentifierHook(UnaryOperator<CategoryFilterInput> inputHook) {
        this.categoryIdentifierHook = inputHook;
    }

    @Override
    protected void populate() {
        if (productIdentifierHook == null && categoryIdentifierHook == null) {
            categories = Collections.emptyList();
            product = Optional.empty();
            return;
        }

        GraphqlResponse<Query, Error> response = executeQuery();

        errors = response.getErrors();
        if (CollectionUtils.isNotEmpty(errors)) {
            categories = Collections.emptyList();
            product = Optional.empty();
            return;
        }

        Query rootQuery = response.getData();

        if (productIdentifierHook != null) {
            List<ProductInterface> products = rootQuery
                .getProducts()
                .getItems();

            if (products.size() > 0) {
                ProductInterface p = products.get(0);
                categories = p.getCategories();
                product = Optional.of(p);
            } else {
                categories = Collections.emptyList();
                product = Optional.empty();
            }
        } else {
            categories = rootQuery.getCategoryList();
            product = Optional.empty();
        }
    }

    @Override
    protected GraphqlResponse<Query, Error> executeQuery() {
        if (query == null) {
            if (productIdentifierHook != null) {
                query = generateProductQuery();
            } else {
                query = generateCategoryQuery();
            }
        }
        return client.execute(query);
    }

    /**
     * Generate a complete breadcrumbs GraphQL query with a filter for the product identifier.
     *
     * @return GraphQL query as string
     */
    protected String generateProductQuery() {
        ProductAttributeFilterInput filter = productIdentifierHook.apply(new ProductAttributeFilterInput());

        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        ProductsQueryDefinition queryArgs = q -> q.items(i -> i
            .sku()
            .urlKey()
            .urlPath()
            .urlRewrites(uq -> uq.url())
            .name()
            .categories(c -> c
                .uid()
                .urlPath()
                .name()
                .breadcrumbs(b -> b
                    .categoryUid()
                    .categoryUrlPath()
                    .categoryName())));

        return Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
    }

    /**
     * Generate a complete breadcrumbs GraphQL query with a filter for the category identifier.
     *
     * @return GraphQL query as string
     */
    protected String generateCategoryQuery() {
        CategoryFilterInput filter = categoryIdentifierHook.apply(new CategoryFilterInput());

        CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);

        CategoryTreeQueryDefinition queryArgs = q -> q
            .uid()
            .urlPath()
            .name()
            .breadcrumbs(b -> b
                .categoryUid()
                .categoryUrlPath()
                .categoryName());

        return Operations.query(query -> query.categoryList(searchArgs, queryArgs)).toString();
    }

}
