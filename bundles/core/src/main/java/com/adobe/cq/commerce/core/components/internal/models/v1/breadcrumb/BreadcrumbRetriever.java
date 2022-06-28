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

import org.apache.commons.collections4.CollectionUtils;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractRetriever;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
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

    private String productIdentifier;

    private String categoryIdentifier;

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
     * This assumes that {@link #setProductIdentifier(String)} has been called before.
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
     * @param productIdentifier The product sku.
     */
    protected void setProductIdentifier(String productIdentifier) {
        this.productIdentifier = productIdentifier;
    }

    /**
     * Set the category uid of the category that should be fetched. Setting the a new category, removes any cached
     * data.
     *
     * @param categoryIdentifier The category uid.
     */
    protected void setCategoryIdentifier(String categoryIdentifier) {
        this.categoryIdentifier = categoryIdentifier;
    }

    @Override
    protected void populate() {
        if (productIdentifier == null && categoryIdentifier == null) {
            categories = Collections.emptyList();
            product = Optional.empty();
            return;
        }

        GraphqlResponse<Query, Error> response = executeQuery();

        if (CollectionUtils.isNotEmpty(response.getErrors())) {
            categories = Collections.emptyList();
            product = Optional.empty();
            return;
        }

        Query rootQuery = response.getData();

        if (productIdentifier != null) {
            List<ProductInterface> products = rootQuery
                .getProducts()
                .getItems();

            if (products.size() > 0) {
                product = Optional.of(products.get(0));
                categories = product.get().getCategories();
            }
        } else {
            categories = rootQuery.getCategoryList();
        }
    }

    @Override
    protected GraphqlResponse<Query, Error> executeQuery() {
        if (query == null) {
            if (productIdentifier != null) {
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
        FilterEqualTypeInput identifierFilter = new FilterEqualTypeInput().setEq(productIdentifier);
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setSku(identifierFilter);

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
        FilterEqualTypeInput identifierFilter = new FilterEqualTypeInput().setEq(categoryIdentifier);
        CategoryFilterInput filter = new CategoryFilterInput().setCategoryUid(identifierFilter);

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
