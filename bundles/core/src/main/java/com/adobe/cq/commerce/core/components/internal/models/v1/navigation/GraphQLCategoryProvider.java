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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.GraphqlClientWrapper;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
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

    private static final CategoryTreeQueryDefinition categoryQueryDefinition = query -> {
        Function<CategoryTreeQuery, CategoryTreeQuery> categoriesQuery = q -> q.id().name().urlPath().position();
        categoriesQuery.apply(query).children(categoriesQuery::apply);
    };

    private GraphqlClientWrapper client;

    GraphQLCategoryProvider(Page page) {
        client = new GraphqlClientWrapper(page.getContentResource());
        if (client == null) {
            LOGGER.warn("GraphQL client not available for resource {}", page.getContentResource().getPath());
        }
    }

    List<CategoryTree> getChildCategories(Integer categoryId) {
        if (client == null || categoryId == null) {
            return Collections.emptyList();
        }

        QueryQuery.CategoryArgumentsDefinition searchArgs = q -> q.id(categoryId);
        String queryString = Operations.query(query -> query.category(searchArgs, categoryQueryDefinition)).toString();
        GraphqlResponse<Query, Error> response = client.execute(new GraphqlRequest(queryString), Query.class, Error.class);

        Query rootQuery = response.getData();
        CategoryTree category = rootQuery.getCategory();
        if (category == null) {
            LOGGER.warn("Magento category not found for id: " + categoryId);
            return Collections.emptyList();
        }
        return category.getChildren();
    }
}
