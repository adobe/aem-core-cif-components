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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Component(service = CategoryProvider.class, immediate = true)
@Designate(ocd = CategoryCacheConfig.class)
public class CategoryProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryProvider.class);
    private static final Function<CategoryTreeQuery, CategoryTreeQuery> CATEGORIES_QUERY = q -> q.id().name().urlPath().position();
    private static final String NO_GRAPHQL_CLIENT = "NoGraphqlClient";
    private Cache<CacheKey, Optional<List<CategoryTree>>> cache = null;

    public CategoryProvider() {

    }

    public List<CategoryTree> getChildCategories(Integer categoryId, Integer depth, Page page) {
        if (categoryId == null || depth == null)
            return Collections.emptyList();

        CacheKey key = new CacheKey(categoryId, depth);
        try {
            List<CategoryTree> list = cache.get(key, () -> loadCategories(categoryId, depth, page)).orElse(Collections.emptyList());
            return list;
        } catch (Exception x) {
            if (x.getCause() != null && NO_GRAPHQL_CLIENT.equals(x.getCause().getMessage())) {
                return Collections.emptyList();
            } else {
                throw new RuntimeException(x);
            }
        }
    }

    private Optional<List<CategoryTree>> loadCategories(Integer categoryId, Integer depth, Page page) {
        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(page.getContentResource());
        if (magentoGraphqlClient == null) {
            // avoid caching empty results when GraphQL client is not found
            throw new IllegalArgumentException(NO_GRAPHQL_CLIENT);
        }

        QueryQuery.CategoryArgumentsDefinition searchArgs = q -> q.id(categoryId);
        String queryString = Operations.query(query -> query.category(searchArgs, defineCategoriesQuery(depth))).toString();
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString);
        Query rootQuery = response.getData();
        CategoryTree category = rootQuery.getCategory();
        if (category == null) {
            LOGGER.warn("Magento category not found for id: " + categoryId);
            return Optional.empty();
        }

        List<CategoryTree> children = category.getChildren();
        if (children == null || children.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(children);
    }

    @Activate
    @SuppressWarnings("unused")
    protected void activate(CategoryCacheConfig conf) {
        cache = CacheBuilder.newBuilder().maximumSize(conf.enabled() ? conf.maxSize() : 0).expireAfterWrite(conf.expirationMinutes(),
            TimeUnit.MINUTES).build();
    }

    @Deactivate
    @SuppressWarnings("unused")
    protected void deactivate() {
        cache.invalidateAll();
    }

    static CategoryTreeQueryDefinition defineCategoriesQuery(int depth) {
        if (depth <= 0) {
            return CATEGORIES_QUERY::apply;
        } else {
            return t -> CATEGORIES_QUERY.apply(t).children(defineCategoriesQuery(depth - 1));
        }
    }

    private static class CacheKey {
        private Integer categoryId;
        private Integer depth;

        public CacheKey(Integer categoryId, Integer depth) {
            this.categoryId = categoryId;
            this.depth = depth;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            if (!categoryId.equals(cacheKey.categoryId)) {
                return false;
            }
            return depth.equals(cacheKey.depth);
        }

        @Override
        public int hashCode() {
            int result = categoryId.hashCode();
            result = 31 * result + depth.hashCode();
            return result;
        }
    }
}
