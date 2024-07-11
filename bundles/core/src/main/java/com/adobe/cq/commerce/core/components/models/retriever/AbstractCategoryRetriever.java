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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQuery;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.CategoryListArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public abstract class AbstractCategoryRetriever extends AbstractRetriever {

    /**
     * Lambda that extends the category query.
     */
    protected Consumer<CategoryTreeQuery> categoryQueryHook;

    /**
     * Lambda that extends the product query.
     */
    protected Consumer<ProductInterfaceQuery> productQueryHook;

    /**
     * Lambda that allows to replace or extend the category filters.
     */
    protected Function<CategoryFilterInput, CategoryFilterInput> categoryFilterHook;

    /**
     * Category instance. Is only available after populate() was called.
     */
    protected Optional<CategoryInterface> category;

    /**
     * Identifier of the category that should be fetched. Categories are identfied by UID.
     */
    protected String identifier;

    /**
     * Current page for pagination of products in a category.
     */
    protected int currentPage = 1;

    /**
     * Page size for pagination of products in a category.
     */
    protected int pageSize = 6;

    public AbstractCategoryRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    /**
     * Executes the GraphQL query and returns a category. For subsequent calls of this method, a cached category is returned.
     *
     * @return Category
     */
    public CategoryInterface fetchCategory() {
        if (this.category == null) {
            populate();
        }
        return this.category.orElse(null);
    }

    /**
     * Sets the current page for product pagination.
     *
     * @param currentPage The current AEM page.
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Sets the page size for product pagination.
     *
     * @param pageSize The page size.
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Set the identifier and the identifier type of the category that should be fetched. Setting the identifier, removes any cached data.
     *
     * @param identifier The category UID.
     */
    public void setIdentifier(String identifier) {
        category = null;
        query = null;
        this.identifier = identifier;
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
     * If called multiple times, each hook will be "appended" to the previously registered hook(s).
     *
     * @param categoryQueryHook Lambda that extends the category query
     */
    public void extendCategoryQueryWith(Consumer<CategoryTreeQuery> categoryQueryHook) {
        if (this.categoryQueryHook == null) {
            this.categoryQueryHook = categoryQueryHook;
        } else {
            this.categoryQueryHook = this.categoryQueryHook.andThen(categoryQueryHook);
        }
    }

    /**
     * Extends or replaces the category filter with a custom instance defined by a lambda hook.
     *
     * Example 1 (Extend):
     *
     * <pre>
     * {@code
     * categoryRetriever.extendCategoryFilterWith(f -> f
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * Example 2 (Replace):
     *
     * <pre>
     * {@code
     * categoryRetriever.extendCategoryFilterWith(f -> new CategoryFilterInput()
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
     * @return The extended category query part if it was set with {@link AbstractCategoryRetriever#extendCategoryQueryWith(Consumer)}
     */
    public Consumer<CategoryTreeQuery> getCategoryQueryHook() {
        return categoryQueryHook;
    }

    /**
     * Extend the product query part of the category GraphQL query with a partial query provided by a lambda hook that sets additional
     * fields.
     *
     * Example:
     *
     * <pre>
     * {@code
     * categoryRetriever.extendProductQueryWith(p -> p
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
     * @return The extended product query part if it was set with {@link AbstractCategoryRetriever#extendProductQueryWith(Consumer)}
     */
    public Consumer<ProductInterfaceQuery> getProductQueryHook() {
        return productQueryHook;
    }

    /**
     * Generates the partial CategoryTree query part of the GraphQL category query.
     *
     * @return CategoryTree query definition
     */
    abstract protected CategoryTreeQueryDefinition generateCategoryQuery();

    /**
     * Generates a complete category GraphQL query with a selection of the given category identifier.
     *
     * @param identifier Category uid identifier
     * @return GraphQL query as string
     */
    public String generateQuery(String identifier) {
        return Operations.query(query -> {
            Pair<CategoryListArgumentsDefinition, CategoryTreeQueryDefinition> categoryQueryArgs = generateCategoryQueryArgs(identifier);
            query.categoryList(categoryQueryArgs.getLeft(), categoryQueryArgs.getRight());
        }).toString();
    }

    /**
     * Generates a pair of args for the category query for a given category identifier;
     *
     * @param identifier Category uid identifier
     * @return GraphQL query as string
     */
    public Pair<CategoryListArgumentsDefinition, CategoryTreeQueryDefinition> generateCategoryQueryArgs(String identifier) {
        CategoryFilterInput filter = new CategoryFilterInput();
        FilterEqualTypeInput identifierFilter = new FilterEqualTypeInput().setEq(identifier);
        filter.setCategoryUid(identifierFilter);

        // Apply category filter hook
        if (this.categoryFilterHook != null) {
            filter = this.categoryFilterHook.apply(filter);
        }

        CategoryFilterInput finalFilter = filter;
        CategoryListArgumentsDefinition searchArgs = q -> q.filters(finalFilter);
        CategoryTreeQueryDefinition queryArgs = generateCategoryQuery();

        return new ImmutablePair<>(searchArgs, queryArgs);
    }

    /**
     * Generates a pair of args for the category query for the instance identifier;
     *
     * @return GraphQL query as string
     */
    public Pair<CategoryListArgumentsDefinition, CategoryTreeQueryDefinition> generateCategoryQueryArgs() {
        return generateCategoryQueryArgs(identifier);
    }

    /**
     * Execute the GraphQL query with the GraphQL client.
     *
     * @return GraphqlResponse object
     */
    protected GraphqlResponse<Query, Error> executeQuery() {
        if (query == null) {
            setQuery(generateQuery(identifier));
        }
        return client.execute(query);
    }

    @Override
    public String generateQuery() {
        return generateQuery(identifier);
    }

    @Override
    protected void populate() {
        GraphqlResponse<Query, Error> response = executeQuery();
        errors = response.getErrors();
        if (CollectionUtils.isEmpty(errors)) {
            Query rootQuery = response.getData();
            if (rootQuery.getCategoryList() != null && !rootQuery.getCategoryList().isEmpty()) {
                category = Optional.of(rootQuery.getCategoryList().get(0));
            } else {
                category = Optional.empty();
            }
        } else {
            category = Optional.empty();
        }
    }
}
