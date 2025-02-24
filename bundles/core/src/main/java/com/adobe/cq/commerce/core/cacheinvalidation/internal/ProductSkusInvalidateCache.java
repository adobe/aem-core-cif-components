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

import com.adobe.cq.commerce.core.cacheinvalidation.internal.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;

@Component(
    service = DispatcherCacheInvalidationStrategy.class,
    property = { InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER + "=productSkus" })
public class ProductSkusInvalidateCache extends InvalidateDispatcherCacheBase implements DispatcherCacheInvalidationStrategy {

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
        String sqlQuery = getQuery(storePath, dataList);
        return getQueryResult(invalidateCacheSupport, getSqlQuery(session, sqlQuery), storePath);
    }

    private String getQuery(String storePath, String dataList) {
        return "SELECT content.[jcr:path] " +
            "FROM [nt:unstructured] AS content " +
            "WHERE ISDESCENDANTNODE(content, '" + storePath + "') " +
            "AND ( " +
            "    (content.[product] IN (" + dataList + ") AND content.[productType] = 'combinedSku') " +
            "    OR (content.[selection] IN (" + dataList + ") AND content.[selectionType] IN ('combinedSku', 'sku')) " +
            ")";
    }

    @Override
    public String getGraphqlQuery(String[] data) {
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
    }

    @Override
    public String[] getPathsToInvalidate(Page page, ResourceResolver resourceResolver, Map<String, Object> data, String storePath) {
        Set<String> uniquePagePaths = new HashSet<>();
        Map<String, Object> productsData = (Map<String, Object>) data.get("products");
        if (productsData != null) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) productsData.get("items");
            if (items != null) {
                Map<String, String> productPatternAndMatch = getPatternAndMatch(invalidateCacheSupport, storePath,
                    DISPATCHER_PRODUCT_URL_PATH);
                Map<String, String> categoryPatternAndMatch = getPatternAndMatch(invalidateCacheSupport, storePath,
                    DISPATCHER_CATEGORY_URL_PATH);
                for (Map<String, Object> item : items) {
                    if (item != null) {
                        Set<String> productPaths = getProductPaths(page, invalidateCacheSupport, urlProvider, item, productPatternAndMatch);
                        List<Map<String, Object>> categories = (List<Map<String, Object>>) item.get("categories");
                        if (categories != null) {
                            Set<String> categoryPaths = getCategoryPaths(page, invalidateCacheSupport, urlProvider, categories,
                                categoryPatternAndMatch);
                            uniquePagePaths.addAll(categoryPaths);
                        }
                        uniquePagePaths.addAll(productPaths);
                    }
                }
            }
        }
        return uniquePagePaths.toArray(new String[0]);
    }
}
