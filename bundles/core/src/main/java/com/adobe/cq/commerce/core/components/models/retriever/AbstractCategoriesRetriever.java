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

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQuery;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public abstract class AbstractCategoriesRetriever extends AbstractRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCategoriesRetriever.class);
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

    /**
     * Identifiers of the categories that should be fetched. Which kind of identifier is used (usually id) is implementation
     * specific and should be checked in subclass implementations.
     */
    protected UrlProvider.CategoryIdentifierType identifierType;

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
        setIdentifiers(identifiers, UrlProvider.CategoryIdentifierType.ID);
    }

    /**
     * Set the identifiers and identifier types of the categories that should be fetched
     * Setting the identifiers, removes any cached data.
     *
     * @param identifiers Category identifiers
     * @param identifierType Which kind of identifier is used: ID, UID
     */
    public void setIdentifiers(List<String> identifiers, UrlProvider.CategoryIdentifierType identifierType) {
        categories = null;
        query = null;
        this.identifiers = identifiers;
        this.identifierType = identifierType;
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
            FilterEqualTypeInput identifiersFilter = new FilterEqualTypeInput().setIn(identifiers);
            CategoryFilterInput filter = AbstractCategoryRetriever.generateCategoryFilter(identifiersFilter, identifierType);

            QueryQuery.CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);
            query.categoryList(searchArgs, queryArgs);
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
        categories = rootQuery.getCategoryList();
        categories.sort(Comparator.comparing(c -> identifiers.indexOf(getCategoryIdentifierValue(c))));
    }

    private String getCategoryIdentifierValue(CategoryTree category) {
        switch (identifierType) {
            case ID:
                return category.getId().toString();
            case UID:
                return category.getUid().toString();
            default:
                return "";
        }
    }
}
