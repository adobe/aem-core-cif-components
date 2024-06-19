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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
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
     * Lambda that allows to replace or extend the category filters.
     */
    protected Function<CategoryFilterInput, CategoryFilterInput> categoryFilterHook;

    /**
     * List of category instances. Is only available after populate() was called.
     */
    protected List<CategoryTree> categories;

    /**
     * Identifiers of the categories that should be fetched. Which kind of filter is used (usually uid) is implementation
     * specific and should be checked in subclass implementations.
     */
    protected List<String> identifiers;

    /**
     * filter type that should be used when fetched. usually uid but we can define it explicitly in the implementation
     * specific and should be checked in subclass implementations.
     */
    protected String filterType;

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
     * Set the identifiers of the categories that should be fetched. Categories are retrieved using the default identifier UID.
     *
     * @param identifiers Category identifiers
     */
    public void setIdentifiers(List<String> identifiers) {
        categories = null;
        query = null;
        this.identifiers = identifiers;
    }

    /**
     * Set the identifiers of the categories that should be fetched. Categories are retrieved using the default identifier UID.
     *
     * @param filterType Filter Type
     */
    public void setCategoryFilterType(String filterType) {
        this.filterType = filterType;
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
     * Extends or replaces the category filter with a custom instance defined by a lambda hook.
     *
     * Example 1 (Extend):
     *
     * <pre>
     * {@code
     * categoriesRetriever.extendCategoryFilterWith(f -> f
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * Example 2 (Replace):
     *
     * <pre>
     * {@code
     * categoriesRetriever.extendCategoryFilterWith(f -> new CategoryFilterInput()
     *     .setCategoryUid(new FilterEqualTypeInput()
     *         .setEq("custom-uid"))
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * @param categoryFilterHook Lambda that extends or replaces the category filter.
     */
    public void extendCategoryFilterWith(Function<CategoryFilterInput, CategoryFilterInput> categoryFilterHook) {
        if (this.categoryFilterHook == null) {
            this.categoryFilterHook = categoryFilterHook;
        } else {
            this.categoryFilterHook = this.categoryFilterHook.andThen(categoryFilterHook);
        }
    }

    /**
     * Generates the partial CategoryTree query part of the GraphQL category query.
     *
     * @return CategoryTree query definition
     */
    abstract protected CategoryTreeQueryDefinition generateCategoryQuery();

    /**
     * Generates a complete category GraphQL query with a selection of the given category UIDs.
     *
     * @param identifiers category UID identifiers
     * @return GraphQL query as string
     */
    protected String generateQuery(List<String> identifiers) {
        CategoryFilterInput filter = new CategoryFilterInput();
        FilterEqualTypeInput identifiersFilter = new FilterEqualTypeInput().setIn(identifiers);

        if ("urlPath".equals(this.filterType)) {
            filter.setUrlPath(identifiersFilter);
        } else if ("urlKey".equals(this.filterType)) {
            filter.setUrlKey(identifiersFilter);
        } else {
            filter.setCategoryUid(identifiersFilter);
        }

        // Apply category filter hook
        if (this.categoryFilterHook != null) {
            filter = this.categoryFilterHook.apply(filter);
        }

        CategoryFilterInput finalFilter = filter;
        QueryQuery.CategoryListArgumentsDefinition searchArgs = s -> s.filters(finalFilter);

        CategoryTreeQueryDefinition queryArgs = generateCategoryQuery();

        return Operations.query(query -> query
            .categoryList(searchArgs, queryArgs)).toString();
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
        errors = response.getErrors();
        if (CollectionUtils.isEmpty(errors)) {
            Query rootQuery = response.getData();
            categories = rootQuery.getCategoryList();
            categories.sort(Comparator.comparing(c -> identifiers.indexOf(c.getUid().toString())));
        } else {
            categories = Collections.emptyList();
        }
        logDuplicateCategories();
    }

    private void logDuplicateCategories() {
        if (categories == null) {
            return;
        }
        Set<String> categoryIds = new HashSet<>();
        for (CategoryTree category : categories) {
            String uid = category.getUid().toString();
            if (categoryIds.contains(uid)) {
                LOGGER.warn("Duplicate category detected: {}", uid);
            }
            categoryIds.add(uid);
        }
    }
}
