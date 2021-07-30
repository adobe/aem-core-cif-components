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
package com.adobe.cq.commerce.core.components.internal.models.v1.navigation;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

class GraphQLCategoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLCategoryProvider.class);

    private static final Function<CategoryTreeQuery, CategoryTreeQuery> CATEGORIES_QUERY = q -> q.uid().name().urlPath().position();
    private final MagentoGraphqlClient magentoGraphqlClient;

    GraphQLCategoryProvider(MagentoGraphqlClient magentoGraphqlClient) {
        this.magentoGraphqlClient = magentoGraphqlClient;
    }

    List<CategoryTree> getChildCategories(String categoryIdentifier, Integer depth) {
        if (magentoGraphqlClient == null) {
            LOGGER.debug("No Graphql client present, cannot retrieve top categories");
            return Collections.emptyList();
        }

        if (StringUtils.isBlank(categoryIdentifier)) {
            LOGGER.debug("Empty category identifier");
            return Collections.emptyList();
        }

        QueryQuery.CategoryListArgumentsDefinition searchArgs = d -> d.filters(new CategoryFilterInput().setCategoryUid(
            new FilterEqualTypeInput().setEq(
                categoryIdentifier)));

        String queryString = Operations.query(query -> query.categoryList(searchArgs, defineCategoriesQuery(depth))).toString();
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString);

        if (CollectionUtils.isNotEmpty(response.getErrors())) {
            return Collections.emptyList();
        }

        Query rootQuery = response.getData();
        List<CategoryTree> category = rootQuery.getCategoryList();
        if (category.isEmpty() || category.get(0) == null) {
            LOGGER.warn("Category not found for identifier: {}", categoryIdentifier);
            return Collections.emptyList();
        }

        List<CategoryTree> children = category.get(0).getChildren();
        if (children == null) {
            return Collections.emptyList();
        }

        return children;
    }

    static CategoryTreeQueryDefinition defineCategoriesQuery(int depth) {
        if (depth <= 0) {
            return CATEGORIES_QUERY::apply;
        } else {
            return t -> CATEGORIES_QUERY.apply(t).children(defineCategoriesQuery(depth - 1));
        }
    }
}
