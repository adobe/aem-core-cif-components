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
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.StoreConfigQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.shopify.graphql.support.AbstractQuery;

public abstract class AbstractCategoryRetriever extends AbstractRetriever {

    private Consumer<AbstractQuery<?>> categoryQueryHook;
    private Consumer<AbstractQuery<?>> productQueryHook;
    private CategoryInterface category;
    private String mediaBaseUrl;
    private String identifier;
    private int currentPage = 0;
    private int pageSize = 6;

    public AbstractCategoryRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    /**
     * Returns the category.
     *
     * @return Category
     */
    public CategoryInterface getCategory() {
        if (this.category == null) {
            populate();
        }
        return this.category;
    }

    /**
     * Stores a category.
     *
     * @param category Category
     */
    protected void setCategory(CategoryInterface category) {
        this.category = category;
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

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    protected int getCurrentPage() {
        return this.currentPage;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    protected int getPageSize() {
        return this.pageSize;
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
     * Returns the category identifier. This is usually the category id.
     *
     * @return Product identifier
     */
    protected String getIdentifier() {
        return this.identifier;
    }

    /**
     * Set the identifier of the category that should be fetched. This is usually the category id.
     *
     * @param identifier Product identifier
     */
    public void setIdentifier(String identifier) {
        // Whenever the identifier is updated, clear the cache.
        setCategory(null);
        setQuery(null);
        this.identifier = identifier;
    }

    /**
     * Returns the category query hook lambda.
     *
     * @return Lambda that extends the category query
     */
    protected Consumer<AbstractQuery<?>> getCategoryQueryHook() {
        return this.categoryQueryHook;
    }

    /**
     * Extend the category query part of the category GraphQL query with a partial query provided by a lambda hook that sets additional
     * fields.
     *
     * Example:
     *
     * <pre>
     * {@code
     * categoryRetriever.extendCategoryQueryWith((CategoryInterfaceQuery p) -> p
     *     .level());
     * }
     * </pre>
     *
     * @param categoryQueryHook Lambda that extends the category query
     * @param <U> Query class that implements AbstractQuery
     */
    public <U extends AbstractQuery<?>> void extendCategoryQueryWith(Consumer<U> categoryQueryHook) {
        this.categoryQueryHook = (Consumer<AbstractQuery<?>>) categoryQueryHook;
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
     * Extend the product query part of the category GraphQL query with a partial query provided by a lambda hook that sets additional
     * fields.
     *
     * Example:
     *
     * <pre>
     * {@code
     * categoryRetriever.extendProductQueryWith((ProductInterfaceQuery p) -> p
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
     * Generates the partial StoreConfig query part of the GraphQL category query.
     *
     * @return StoreConfig query definition
     */
    protected StoreConfigQueryDefinition generateStoreConfigQuery() {
        return q -> q.secureBaseMediaUrl();
    }

    /**
     * Generates the partial CategoryTree query part of the GraphQL category query.
     *
     * @return CategoryTree query definition
     */
    protected CategoryTreeQueryDefinition generateCategoryQuery() {
        throw new UnsupportedOperationException();
    }

    /**
     * Generates a complete category GraphQL query with a selection of the given category identifier.
     *
     * @param identifier Category identifier, usually the category id
     * @return GraphQL query as string
     */
    protected String generateQuery(String identifier) {
        QueryQuery.CategoryArgumentsDefinition searchArgs = q -> q.id(Integer.parseInt(identifier));

        CategoryTreeQueryDefinition queryArgs = generateCategoryQuery();
        return Operations.query(query -> query
            .category(searchArgs, queryArgs)
            .storeConfig(generateStoreConfigQuery())).toString();
    }

    /**
     * Execute the GraphQL query with the GraphQL client.
     *
     * @return GraphqlResponse object
     */
    protected GraphqlResponse<Query, Error> executeQuery() {
        if (getQuery() == null) {
            setQuery(generateQuery(getIdentifier()));
        }
        return getClient().execute(getQuery());
    }

    @Override
    protected void populate() {
        GraphqlResponse<Query, Error> response = executeQuery();
        Query rootQuery = response.getData();

        setMediaBaseUrl(rootQuery.getStoreConfig().getSecureBaseMediaUrl());
        setCategory(rootQuery.getCategory());
    }

}
