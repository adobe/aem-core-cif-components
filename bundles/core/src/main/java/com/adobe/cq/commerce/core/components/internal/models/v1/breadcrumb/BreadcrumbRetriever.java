/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.models.v1.breadcrumb;

import java.util.List;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider.CategoryIdentifierType;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
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

public class BreadcrumbRetriever extends AbstractRetriever {

    private List<? extends CategoryInterface> categories;
    private String productName;

    private String productIdentifier;
    private ProductIdentifierType productIdentifierType;

    private String categoryIdentifier;
    private CategoryIdentifierType categoryIdentifierType;

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
     * This assumes that {@link #setProductIdentifier(ProductIdentifierType, String)} has been called before.
     * For subsequent calls of this method, a cached response is returned.
     *
     * @return The product name.
     */
    protected String fetchProductName() {
        if (productName == null) {
            populate();
        }
        return productName;
    }

    /**
     * Set the identifier and the identifier type of the product that should be fetched. Setting the identifier, removes any cached data.
     *
     * @param productIdentifierType The product identifier type.
     * @param productIdentifier The product identifier.
     */
    protected void setProductIdentifier(ProductIdentifierType productIdentifierType, String productIdentifier) {
        this.productIdentifier = productIdentifier;
        this.productIdentifierType = productIdentifierType;
    }

    /**
     * Set the identifier and the identifier type of the category that should be fetched. Setting the identifier, removes any cached data.
     *
     * @param categoryIdentifierType The category identifier type.
     * @param identifier The category identifier.
     */
    protected void setCategoryIdentifier(CategoryIdentifierType categoryIdentifierType, String categoryIdentifier) {
        this.categoryIdentifier = categoryIdentifier;
        this.categoryIdentifierType = categoryIdentifierType;
    }

    @Override
    protected void populate() {
        if (productIdentifier == null && categoryIdentifier == null) {
            return;
        }

        GraphqlResponse<Query, Error> response = executeQuery();
        Query rootQuery = response.getData();

        if (productIdentifier != null) {
            List<ProductInterface> products = rootQuery
                .getProducts()
                .getItems();

            if (products.size() > 0) {
                ProductInterface product = products.get(0);
                productName = product.getName();
                categories = product.getCategories();
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
        ProductAttributeFilterInput filter;
        if (ProductIdentifierType.URL_KEY.equals(productIdentifierType)) {
            filter = new ProductAttributeFilterInput().setUrlKey(identifierFilter);
        } else if (ProductIdentifierType.SKU.equals(productIdentifierType)) {
            filter = new ProductAttributeFilterInput().setSku(identifierFilter);
        } else {
            throw new RuntimeException("Product identifier type is not supported");
        }

        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        ProductsQueryDefinition queryArgs = q -> q.items(i -> i
            .sku()
            .urlKey()
            .name()
            .categories(c -> c
                .id()
                .urlPath()
                .name()
                .breadcrumbs(b -> b
                    .categoryId()
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
        CategoryFilterInput filter;
        if (CategoryIdentifierType.ID.equals(categoryIdentifierType)) {
            filter = new CategoryFilterInput().setIds(identifierFilter);
        } else {
            throw new RuntimeException("Category identifier type is not supported");
        }

        CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);

        CategoryTreeQueryDefinition queryArgs = q -> q
            .id()
            .urlPath()
            .name()
            .breadcrumbs(b -> b
                .categoryId()
                .categoryUrlPath()
                .categoryName());

        return Operations.query(query -> query.categoryList(searchArgs, queryArgs)).toString();
    }

}
