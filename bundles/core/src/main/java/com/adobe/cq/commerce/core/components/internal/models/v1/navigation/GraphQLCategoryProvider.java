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

package com.adobe.cq.commerce.core.components.internal.models.v1.navigation;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQuery;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

class GraphQLCategoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLCategoryProvider.class);
    private static final Function<CategoryTreeQuery, CategoryTreeQuery> CATEGORIES_QUERY = q -> q.id().name().urlPath().position();
    private MagentoGraphqlClient magentoGraphqlClient;

    GraphQLCategoryProvider(Resource resource, Page page) {
        magentoGraphqlClient = MagentoGraphqlClient.create(resource, page);
    }

    List<CategoryTree> getChildCategories(Integer categoryId, Integer depth) {
        if (magentoGraphqlClient == null || categoryId == null) {
            return Collections.emptyList();
        }

        QueryQuery.CategoryArgumentsDefinition searchArgs = q -> q.id(categoryId);
        String queryString = Operations.query(query -> query.category(searchArgs, defineCategoriesQuery(depth))).toString();
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString);

        Query rootQuery = response.getData();
        CategoryTree category = rootQuery.getCategory();
        if (category == null) {
            LOGGER.warn("Magento category not found for id: " + categoryId);
            return Collections.emptyList();
        }

        List<CategoryTree> children = category.getChildren();
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
