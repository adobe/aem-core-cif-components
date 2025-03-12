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

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;

@Component(
    service = DispatcherCacheInvalidationStrategy.class,
    property = { InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER + "=productSkus" })
public class ProductSkusInvalidateCache extends InvalidateDispatcherCacheBase implements DispatcherCacheInvalidationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSkusInvalidateCache.class);
    private static final String PRODUCT_TYPE_COMBINED_SKU = "combinedSku";
    private static final String SELECTION_TYPE_SKU = "sku";
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
    public String[] getCorrespondingPagePaths(Session session, String storePath, String dataList) throws CacheInvalidationException {
        if (session == null || storePath == null || dataList == null) {
            LOGGER.error("Invalid parameters: session={}, storePath={}, dataList={}", session, storePath, dataList);
            return new String[0];
        }

        try {
            String sqlQuery = getQuery(storePath, dataList);
            return getQueryResult(invalidateCacheSupport, getSqlQuery(session, sqlQuery));
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

    @Override
    public String getGraphqlQuery(String[] data) {
        if (data == null || data.length == 0) {
            LOGGER.warn("Empty data array provided for GraphQL query");
            return null;
        }

        try {
            ProductAttributeFilterInput filter = new ProductAttributeFilterInput();
            FilterEqualTypeInput skuFilter = new FilterEqualTypeInput().setIn(Arrays.asList(data));
            filter.setSku(skuFilter);
            QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

            ProductsQueryDefinition queryArgs = q -> q.items(item -> item.sku()
                .urlKey()
                .urlRewrites(uq -> uq.url())
                .categories(c -> c.uid().urlKey().urlPath()));
            return Operations.query(query -> query
                .products(searchArgs, queryArgs)).toString();
        } catch (Exception e) {
            LOGGER.error("Error generating GraphQL query for data={}", Arrays.toString(data), e);
            return null;
        }
    }

    @Override
    public String[] getPathsToInvalidate(Page page, ResourceResolver resourceResolver, Map<String, Object> data, String storePath) {
        if (page == null || resourceResolver == null || data == null || storePath == null) {
            LOGGER.error("Invalid parameters: page={}, resourceResolver={}, data={}, storePath={}",
                page, resourceResolver, data, storePath);
            return new String[0];
        }

        try {
            return processProductsData(page, data);
        } catch (Exception e) {
            LOGGER.error("Error getting paths to invalidate for storePath={}", storePath, e);
            return new String[0];
        }
    }

    private String[] processProductsData(Page page, Map<String, Object> data) {
        Set<String> uniquePagePaths = new HashSet<>();
        Map<String, Object> productsData = (Map<String, Object>) data.get("products");
        if (productsData == null) {
            return new String[0];
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) productsData.get("items");
        if (items == null) {
            return new String[0];
        }

        List<PatternConfig> categoryPatternsConfig = getPatternAndMatch(invalidateCacheSupport, DISPATCHER_CATEGORY_URL_PATH);
        List<PatternConfig> productPatternsConfig = getPatternAndMatch(invalidateCacheSupport, DISPATCHER_PRODUCT_URL_PATH);

        for (Map<String, Object> item : items) {
            if (item != null) {
                processItem(item, page, categoryPatternsConfig, productPatternsConfig, uniquePagePaths);
            }
        }
        return uniquePagePaths.toArray(new String[0]);
    }

    private void processItem(Map<String, Object> item, Page page, List<PatternConfig> categoryPatternsConfig,
        List<PatternConfig> productPatternsConfig, Set<String> uniquePagePaths) {
        Set<String> productPaths = getProductPaths(page, urlProvider, item, productPatternsConfig);
        List<Map<String, Object>> categories = (List<Map<String, Object>>) item.get("categories");
        if (categories != null) {
            Set<String> categoryPaths = getCategoryPaths(page, urlProvider, categories, categoryPatternsConfig);
            uniquePagePaths.addAll(categoryPaths);
        }
        uniquePagePaths.addAll(productPaths);
    }
}
