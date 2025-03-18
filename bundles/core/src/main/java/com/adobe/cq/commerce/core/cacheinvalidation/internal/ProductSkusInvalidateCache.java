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

/**
 * Implementation of {@link DispatcherCacheInvalidationStrategy} that handles cache invalidation
 * for product SKUs. This class manages the invalidation of cache entries related to product pages
 * and their associated category pages when product SKUs are modified.
 */
@Component(
    service = DispatcherCacheInvalidationStrategy.class)
public class ProductSkusInvalidateCache extends InvalidateDispatcherCacheBase implements DispatcherCacheInvalidationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSkusInvalidateCache.class);

    // Constants for product types and selection types
    private static final String PRODUCT_TYPE_COMBINED_SKU = "combinedSku";
    private static final String SELECTION_TYPE_SKU = "sku";

    // SQL query template for finding content paths
    private static final String SQL_QUERY_TEMPLATE = "SELECT content.[jcr:path] " +
        "FROM [nt:unstructured] AS content " +
        "WHERE ISDESCENDANTNODE(content, '%s') " +
        "AND ( " +
        "    (content.[product] IN (%s) AND content.[productType] = '%s') " +
        "    OR (content.[selection] IN (%s) AND content.[selectionType] IN ('%s', '%s')) " +
        ")";

    @Reference
    private UrlProviderImpl urlProvider;

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    @Override
    public String getPattern() {
        return "\"sku\":\\s*\"";
    }

    @Override
    public String getInvalidationRequestType() {
        return "productSkus";
    }

    @Override
    public List<String> getPathsToInvalidate(DispatcherCacheInvalidationContext context) {
        try {
            List<String> skus = context.getAttributeData();
            if (skus == null) {
                return Collections.emptyList();
            }

            String[] skusArray = skus.toArray(new String[0]);

            List<Map<String, Object>> products = fetchProducts(context, skusArray);
            if (products.isEmpty()) {
                return Collections.emptyList();
            }

            Set<String> allPaths = new HashSet<>();

            // Add paths from JCR query
            addJcrPaths(context, skusArray, allPaths);

            // Add paths from GraphQL response
            addGraphqlPaths(context, products, allPaths);

            return new ArrayList<>(allPaths);

        } catch (Exception e) {
            LOGGER.error("Error getting paths to invalidate for storePath={}", context.getStorePath(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves the corresponding page paths for given SKUs using JCR query.
     *
     * @param session The JCR session
     * @param storePath The store path to search in
     * @param dataList The formatted list of SKUs
     * @return Array of page paths
     * @throws CacheInvalidationException if query execution fails
     */
    private String[] getCorrespondingPagePaths(Session session, String storePath, String dataList)
        throws CacheInvalidationException {
        if (session == null || storePath == null || dataList == null) {
            LOGGER.error("Invalid parameters: session={}, storePath={}, dataList={}", session, storePath, dataList);
            return new String[0];
        }

        try {
            String sqlQuery = getQuery(storePath, dataList);
            return getQueryResult(getSqlQuery(session, sqlQuery));
        } catch (Exception e) {
            throw new CacheInvalidationException("Failed to get corresponding page paths", e);
        }
    }

    private String getQuery(String storePath, String dataList) {
        return String.format(SQL_QUERY_TEMPLATE,
            storePath,
            dataList,
            PRODUCT_TYPE_COMBINED_SKU,
            dataList,
            PRODUCT_TYPE_COMBINED_SKU,
            SELECTION_TYPE_SKU);
    }

    /**
     * Generates a GraphQL query for fetching product information.
     *
     * @param data Array of SKUs to query
     * @return GraphQL query string
     */
    private String getGraphqlQuery(String[] data) {
        try {
            ProductAttributeFilterInput filter = new ProductAttributeFilterInput();
            FilterEqualTypeInput skuFilter = new FilterEqualTypeInput().setIn(Arrays.asList(data));
            filter.setSku(skuFilter);

            QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);
            ProductsQueryDefinition queryArgs = q -> q.items(item -> item.sku()
                .urlKey()
                .urlRewrites(uq -> uq.url())
                .categories(c -> c.uid().urlKey().urlPath()));

            return Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
        } catch (Exception e) {
            LOGGER.error("Error generating GraphQL query for data={}", Arrays.toString(data), e);
            return null;
        }
    }

    /**
     * Fetches product information using GraphQL query.
     *
     * @param context The cache invalidation context
     * @param skus Array of SKUs to query
     * @return List of product data maps
     */
    private List<Map<String, Object>> fetchProducts(DispatcherCacheInvalidationContext context, String[] skus) {
        String query = getGraphqlQuery(skus);
        if (query == null) {
            return Collections.emptyList();
        }

        Query data = getGraphqlResponseData(context.getGraphqlClient(), query);
        if (data == null || data.getProducts() == null || data.getProducts().getItems() == null) {
            LOGGER.debug("No products found for SKUs: {}", (Object) skus);
            return Collections.emptyList();
        }

        return data.getProducts().getItems().stream()
            .map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("sku", item.getSku());
                map.put("urlKey", item.getUrlKey());
                map.put("urlRewrites", item.getUrlRewrites());
                map.put("categories", item.getCategories());
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * Adds paths from JCR query to the set of paths.
     *
     * @param context The cache invalidation context
     * @param skus Array of SKUs
     * @param allPaths Set to add paths to
     */
    private void addJcrPaths(DispatcherCacheInvalidationContext context, String[] skus, Set<String> allPaths) {
        Session session = context.getResourceResolver().adaptTo(Session.class);
        try {
            String dataList = formatList(skus, ", ", "'%s'");
            String[] correspondingPaths = getCorrespondingPagePaths(session, context.getStorePath(), dataList);
            if (correspondingPaths != null) {
                allPaths.addAll(Arrays.asList(correspondingPaths));
            }
        } catch (CacheInvalidationException e) {
            LOGGER.error("Failed to get corresponding paths for SKUs: {}", Arrays.toString(skus), e);
        }
    }

    /**
     * Adds paths from GraphQL response to the set of paths.
     *
     * @param context The cache invalidation context
     * @param products List of product data maps
     * @param allPaths Set to add paths to
     */
    private void addGraphqlPaths(DispatcherCacheInvalidationContext context, List<Map<String, Object>> products, Set<String> allPaths) {
        Page page = context.getPage();
        LOGGER.debug("Processing {} products for path invalidation", products.size());

        for (Map<String, Object> item : products) {
            if (item == null) {
                continue;
            }

            LOGGER.debug("Processing product with SKU: {}", item.get("sku"));

            // Add product paths
            addProductPaths(page, item, allPaths);

            // Add category paths
            addCategoryPaths(page, item, allPaths);
        }
    }

    private void addProductPaths(Page page, Map<String, Object> item, Set<String> allPaths) {
        Set<String> productPaths = getProductPaths(page, urlProvider, item);
        LOGGER.debug("Found product paths: {}", productPaths);
        allPaths.addAll(productPaths);
    }

    private void addCategoryPaths(Page page, Map<String, Object> item, Set<String> allPaths) {
        List<CategoryTree> categories = (List<CategoryTree>) item.get("categories");
        if (categories != null) {
            LOGGER.debug("Found {} categories for product", categories.size());
            List<Map<String, Object>> transformedCategories = new ArrayList<>();

            for (CategoryTree category : categories) {
                LOGGER.debug("Category data: {}", category);
                Map<String, Object> transformed = new HashMap<>();
                transformed.put("uid", category.getUid());
                transformed.put("urlKey", category.getName());
                transformed.put("urlPath", category.getUrlPath());
                transformedCategories.add(transformed);
            }

            Set<String> categoryPaths = getCategoryPaths(page, urlProvider, transformedCategories);
            LOGGER.debug("Found category paths: {}", categoryPaths);
            allPaths.addAll(categoryPaths);
        } else {
            LOGGER.debug("No categories found for product");
        }
    }
}
