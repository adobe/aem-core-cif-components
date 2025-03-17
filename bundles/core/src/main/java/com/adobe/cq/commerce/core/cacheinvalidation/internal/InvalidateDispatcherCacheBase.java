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
import javax.jcr.query.*;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

public class InvalidateDispatcherCacheBase {
    public static final String URL_KEY = "urlKey";
    public static final String URL_PATH = "urlPath";
    public static final String SKU = "sku";
    public static final String UID = "uid";

    protected Set<String> getProductPaths(Page page, UrlProviderImpl urlProvider,
        Map<String, Object> item) {
        if (item == null || page == null || urlProvider == null) {
            return Collections.emptySet();
        }

        Set<String> uniquePagePaths = new HashSet<>();

        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();
        productParams.setSku((String) item.get(SKU));
        productParams.setUrlKey((String) item.get(URL_KEY));

        @SuppressWarnings("unchecked")
        List<UrlRewrite> urlRewrites = (List<UrlRewrite>) item.get("urlRewrites");
        if (urlRewrites != null && !urlRewrites.isEmpty()) {
            for (UrlRewrite urlRewrite : urlRewrites) {
                String url = urlRewrite.getUrl();
                if (url != null) {
                    productParams.setUrlRewrites(Collections.singletonList(urlRewrite));
                    String productUrl = urlProvider.toProductUrl(null, page, productParams);
                    if (productUrl != null) {
                        uniquePagePaths.add(productUrl);
                    }
                }
            }
        }
        return uniquePagePaths;
    }

    protected Set<String> getCategoryPaths(Page page, UrlProviderImpl urlProvider,
        List<Map<String, Object>> categories) {
        if (categories == null || categories.isEmpty() || page == null || urlProvider == null) {
            return Collections.emptySet();
        }

        CategoryUrlFormat.Params categoryParams = new CategoryUrlFormat.Params();

        return categories.stream()
            .filter(Objects::nonNull)
            .map(category -> {
                categoryParams.setUid(getStringValue(category, UID));
                categoryParams.setUrlKey(getStringValue(category, URL_KEY));
                categoryParams.setUrlPath(getStringValue(category, URL_PATH));

                String categoryUrlPath = urlProvider.toCategoryUrl(null, page, categoryParams);
                return categoryUrlPath != null ? removeUpToDelimiter(categoryUrlPath, InvalidateCacheSupport.HTML_SUFFIX, true) : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : String.valueOf(value);
    }

    protected String removeUpToDelimiter(String input, String delimiter, boolean useLastIndex) {
        if (input == null || delimiter == null) {
            return input;
        }
        int index = useLastIndex ? input.lastIndexOf(delimiter) : input.indexOf(delimiter);
        if (index != -1) {
            input = input.substring(0, index);
        }
        return input;
    }

    protected Set<String> processItems(Page page, UrlProviderImpl urlProvider,
        List<Map<String, Object>> items) {
        Set<String> uniquePagePaths = new HashSet<>();
        for (Map<String, Object> item : items) {
            if (item != null) {
                String categoryUrlPath = processCategoryItem(page, urlProvider, item);
                if (categoryUrlPath != null) {
                    uniquePagePaths.add(categoryUrlPath);
                }
            }
        }
        return uniquePagePaths;
    }

    protected String processCategoryItem(Page page, UrlProviderImpl urlProvider, Map<String, Object> item) {
        CategoryUrlFormat.Params categoryParams = createCategoryParams(item);
        String categoryUrlPath = urlProvider.toCategoryUrl(null, page, categoryParams);
        if (categoryUrlPath != null) {
            return removeUpToDelimiter(categoryUrlPath, InvalidateCacheSupport.HTML_SUFFIX, true);
        }
        return null;
    }

    protected CategoryUrlFormat.Params createCategoryParams(Map<String, Object> item) {
        if (item == null) {
            return null;
        }

        CategoryUrlFormat.Params categoryParams = new CategoryUrlFormat.Params();
        String urlPath = (String) item.get(URL_PATH);
        String urlKey = (String) item.get(URL_KEY);
        String uid = (String) item.get(UID);

        categoryParams.setUrlPath(urlPath);
        categoryParams.setUrlKey(urlKey);
        categoryParams.setUid(uid);

        return categoryParams;
    }

    protected String[] getQueryResult(javax.jcr.query.Query query)
        throws CacheInvalidationException {
        try {
            Set<String> uniquePagePaths = new HashSet<>();
            QueryResult result = query.execute();
            if (result != null) {
                RowIterator rows = result.getRows();
                if (rows != null) {
                    while (rows.hasNext()) {
                        Row row = rows.nextRow();
                        String fullPath = row.getPath("content");
                        if (fullPath != null) {
                            uniquePagePaths.add(extractPagePath(fullPath) + InvalidateCacheSupport.HTML_SUFFIX);
                        }
                    }
                }
            }
            return uniquePagePaths.toArray(new String[0]);
        } catch (Exception e) {
            throw new CacheInvalidationException("Error getting query result", e);
        }
    }

    protected javax.jcr.query.Query getSqlQuery(Session session, String sql2Query) throws CacheInvalidationException {
        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            return queryManager.createQuery(sql2Query, javax.jcr.query.Query.JCR_SQL2);
        } catch (Exception e) {
            throw new CacheInvalidationException("Error creating SKU-based SQL2 query", e);
        }
    }

    protected Query getGraphqlResponseData(MagentoGraphqlClient client, String query) {
        if (client == null || query == null) {
            return null;
        }

        GraphqlResponse<com.adobe.cq.commerce.magento.graphql.Query, Error> response = client.execute(query);

        if (response == null || (response.getErrors() != null && !response.getErrors().isEmpty()) || response.getData() == null) {
            return null;
        }

        return response.getData();
    }

    protected String extractPagePath(String fullPath) {
        if (fullPath == null) {
            return null;
        }
        int jcrContentIndex = fullPath.indexOf("/jcr:content");
        return jcrContentIndex != -1 ? fullPath.substring(0, jcrContentIndex) : fullPath;
    }

    protected String formatList(String[] invalidCacheEntries, String delimiter, String pattern) {
        if (invalidCacheEntries == null) {
            return "";
        }
        return Arrays.stream(invalidCacheEntries)
            .map(item -> String.format(pattern, item))
            .collect(Collectors.joining(delimiter));
    }
}
