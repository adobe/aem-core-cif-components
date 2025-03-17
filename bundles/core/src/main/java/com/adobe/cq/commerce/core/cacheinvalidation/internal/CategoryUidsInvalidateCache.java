/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.*;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;

@Component(
    service = DispatcherCacheInvalidationStrategy.class)
public class CategoryUidsInvalidateCache extends InvalidateDispatcherCacheBase implements DispatcherCacheInvalidationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryUidsInvalidateCache.class);

    @Reference
    private UrlProviderImpl urlProvider;

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    @Override
    public String getPattern() {
        return "\"uid\"\\s*:\\s*\\{\"id\"\\s*:\\s*\"";
    }

    @Override
    public String getInvalidationRequestType() {
        return "categoryUids";
    }

    @Override
    public List<String> getPathsToInvalidate(DispatcherCacheInvalidationContext context) {
        try {
            List<String> categoryUids = extractCategoryUidsFromContext(context);
            if (!isValidCategoryUids(categoryUids)) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> categories = fetchCategories(context, categoryUids.toArray(new String[0]));
            if (categories.isEmpty()) {
                return Collections.emptyList();
            }

            Set<String> allPaths = new HashSet<>();

            // Add paths from JCR query
            addJcrPaths(context, categoryUids.toArray(new String[0]), allPaths);

            // Add paths from GraphQL response
            addGraphqlPaths(context, categories, allPaths);

            return new ArrayList<>(allPaths);

        } catch (Exception e) {
            LOGGER.error("Error getting paths to invalidate for storePath={}", context.getStorePath(), e);
            return Collections.emptyList();
        }
    }

    private List<String> extractCategoryUidsFromContext(DispatcherCacheInvalidationContext context) {
        List<String> attributeData = context.getAttributeData();
        return attributeData != null ? attributeData : Collections.emptyList();
    }

    private boolean isValidCategoryUids(List<String> categoryUids) {
        if (categoryUids == null || categoryUids.isEmpty()) {
            LOGGER.warn("No category UIDs provided for cache invalidation");
            return false;
        }
        return true;
    }

    private List<Map<String, Object>> fetchCategories(DispatcherCacheInvalidationContext context, String[] categoryUids) {
        String query = getGraphqlQuery(categoryUids);
        if (query == null) {
            return Collections.emptyList();
        }

        Query data = getGraphqlResponseData(context.getGraphqlClient(), query);
        if (data == null) {
            return Collections.emptyList();
        }

        List<CategoryTree> categories = data.getCategoryList();
        if (categories == null || categories.isEmpty()) {
            LOGGER.debug("No categories found for UIDs: {}", (Object) categoryUids);
            return Collections.emptyList();
        }

        return categories.stream()
            .map(category -> {
                Map<String, Object> map = new HashMap<>();
                map.put("uid", category.getUid());
                map.put("urlKey", category.getUrlKey());
                map.put("urlPath", category.getUrlPath());
                return map;
            })
            .collect(Collectors.toList());
    }

    private String getGraphqlQuery(String[] data) {
        CategoryFilterInput filter = new CategoryFilterInput();
        FilterEqualTypeInput identifiersFilter = new FilterEqualTypeInput().setIn(Arrays.asList(data));
        filter.setCategoryUid(identifiersFilter);
        QueryQuery.CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);

        CategoryTreeQueryDefinition queryArgs = q -> q.uid().name().urlKey().urlPath();

        return Operations.query(query -> query
            .categoryList(searchArgs, queryArgs)).toString();
    }

    private void addJcrPaths(DispatcherCacheInvalidationContext context, String[] categoryUids, Set<String> allPaths) {
        Session session = context.getResourceResolver().adaptTo(Session.class);
        if (session == null) {
            LOGGER.error("Failed to adapt ResourceResolver to Session");
            return;
        }

        try {
            String dataList = formatList(categoryUids, ", ", "'%s'");
            String[] correspondingPaths = getCorrespondingPagePaths(session, context.getStorePath(), dataList);
            if (correspondingPaths != null) {
                allPaths.addAll(Arrays.asList(correspondingPaths));
            }
        } catch (CacheInvalidationException e) {
            LOGGER.error("Failed to get corresponding paths for category UIDs: {}", Arrays.toString(categoryUids), e);
        }
    }

    private void addGraphqlPaths(DispatcherCacheInvalidationContext context, List<Map<String, Object>> categories, Set<String> allPaths) {
        Page page = context.getPage();
        categories.stream()
            .filter(Objects::nonNull)
            .forEach(category -> {
                Set<String> categoryPaths = getCategoryPaths(page, urlProvider, Collections.singletonList(category));
                if (!categoryPaths.isEmpty()) {
                    allPaths.addAll(categoryPaths);
                }
            });
    }

    private String[] getCorrespondingPagePaths(Session session, String storePath, String dataList) throws CacheInvalidationException {
        String sqlQuery = getQuery(storePath, dataList);
        return getQueryResult(getSqlQuery(session, sqlQuery));
    }

    private String getQuery(String storePath, String dataList) {
        return "SELECT content.[jcr:path] " +
            "FROM [nt:unstructured] AS content " +
            "WHERE ISDESCENDANTNODE(content,'" + storePath + "' ) " +
            "AND (" +
            "(content.[categoryId] in (" + dataList + ") AND content.[categoryIdType] in ('uid')) " +
            "OR (content.[category] in (" + dataList + ") AND content.[categoryType] in ('uid'))" +
            ")";
    }
}
