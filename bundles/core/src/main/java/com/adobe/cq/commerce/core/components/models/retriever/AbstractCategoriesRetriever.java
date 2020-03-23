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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQuery;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public abstract class AbstractCategoriesRetriever extends AbstractRetriever {

    public static final String CATEGORY_IMAGE_FOLDER = "catalog/category/";

    /**
     * Lambda that extends the category query.
     */
    protected Consumer<CategoryTreeQuery> categoryQueryHook;

    /**
     * List of category instances. Is only available after populate() was called.
     */
    protected List<CategoryTree> categories;

    /**
     * Identifiers of the categories that should be fetched. Which kind of identifier is used (usually id) is implementation
     * specific and should be checked in subclass implementations.
     */
    protected List<String> identifiers;

    public AbstractCategoriesRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    /**
     * Executes the GraphQL query and returns categories. For subsequent calls of this method, a cached list of categories is returned.
     *
     * @return List of categories
     */
    public List<CategoryTree> fetchCategories() {
        if (this.categories == null) {
            populate();
        }
        return this.categories;
    }

    /**
     * Set the identifiers of the categories that should be fetched. Which kind of identifier is used (usually id) is implementation
     * specific and should be checked in subclass implementations. Setting the identifiers, removes any cached data.
     *
     * @param identifiers Category identifiers
     */
    public void setIdentifiers(List<String> identifiers) {
        categories = null;
        query = null;
        this.identifiers = identifiers;
    }

    /**
     * Extend the category query part of the category GraphQL query with a partial query provided by a lambda hook that sets additional
     * fields.
     *
     * Example:
     *
     * <pre>
     * {@code
     * categoryRetriever.extendCategoryQueryWith(p -> p
     *     .level());
     * }
     * </pre>
     *
     * @param categoryQueryHook Lambda that extends the category query
     */
    public void extendCategoryQueryWith(Consumer<CategoryTreeQuery> categoryQueryHook) {
        this.categoryQueryHook = categoryQueryHook;
    }

    /**
     * Generates the partial CategoryTree query part of the GraphQL category query.
     *
     * @return CategoryTree query definition
     */
    abstract protected CategoryTreeQueryDefinition generateCategoryQuery();

    /**
     * Generates a complete category GraphQL query with a selection of the given category identifiers.
     *
     * @param identifiers Category identifiers, usually the category id
     * @return GraphQL query as string
     */
    protected String generateQuery(List<String> identifiers) {
        CategoryTreeQueryDefinition queryArgs = generateCategoryQuery();
        return Operations.query(query -> {
            for (String identifier : identifiers) {
                try {
                    int value = Integer.parseInt(identifier);
                    String alias = "category_" + identifier;
                    query.withAlias(alias).category(q -> q.id(value), queryArgs);
                } catch (NumberFormatException x) {
                    // ignore
                }
            }
        }).toString();
    }

    /**
     * Execute the GraphQL query with the GraphQL client.
     *
     * @return GraphqlResponse object
     */
    protected GraphqlResponse<Query, Error> executeQuery() {
        if (query == null) {
            setQuery(generateQuery(identifiers));
        }
        return client.execute(query);
    }

    @Override
    protected void populate() {
        GraphqlResponse<Query, Error> response = executeQuery();
        Query rootQuery = response.getData();

        categories = new ArrayList<>();
        for (String identifier : identifiers) {
            String alias = "category__category_" + identifier;
            CategoryTree category = (CategoryTree) rootQuery.get(alias);
            if (category != null) {
                categories.add(category);
            }
        }
    }
}
