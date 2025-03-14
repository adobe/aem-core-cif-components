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

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import javax.jcr.Session;
import javax.jcr.query.*;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.day.cq.wcm.api.Page;
import com.google.gson.reflect.TypeToken;

public class InvalidateDispatcherCacheBase {
    public static final String URL_KEY = "url_key";
    public static final String URL_PATH = "url_path";
    public static final String SKU = "sku";
    public static final String UID = "uid";
    public static final String PRODUCT_SAMPLE_URL = "XXXXXX";

    protected Set<String> getProductPaths(Page page, UrlProviderImpl urlProvider,
        Map<String, Object> item) {
        if (item == null || page == null || urlProvider == null) {
            return Collections.emptySet();
        }

        Set<String> uniquePagePaths = new HashSet<>();
        String sku = (String) item.get(SKU);
        String urlKey = (String) item.get(URL_KEY);

        if (sku == null || urlKey == null) {
            return uniquePagePaths;
        }

        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();
        productParams.setSku(sku);
        productParams.setUrlKey(urlKey);

        List<Map<String, String>> urlRewrites = (List<Map<String, String>>) item.get("url_rewrites");
        if (urlRewrites != null && !urlRewrites.isEmpty()) {
            for (Map<String, String> urlRewrite : urlRewrites) {
                String url = urlRewrite.get("url");
                if (url != null) {
                    productParams.setUrlRewrites(Collections.singletonList(new UrlRewrite().setUrl(url)));
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

        Set<String> uniquePagePaths = new HashSet<>();
        CategoryUrlFormat.Params categoryParams = new CategoryUrlFormat.Params();

        for (Map<String, Object> category : categories) {
            if (category != null) {
                String uid = (String) category.get(UID);
                String urlKey = (String) category.get(URL_KEY);
                String urlPath = (String) category.get(URL_PATH);

                if (uid != null && urlKey != null && urlPath != null) {
                    categoryParams.setUid(uid);
                    categoryParams.setUrlKey(urlKey);
                    categoryParams.setUrlPath(urlPath);

                    String categoryUrlPath = urlProvider.toCategoryUrl(null, page, categoryParams);
                    if (categoryUrlPath != null) {
                        uniquePagePaths.add(removeUpToDelimiter(categoryUrlPath, InvalidateCacheSupport.HTML_SUFFIX, true));
                    }
                }
            }
        }
        return uniquePagePaths;
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
                String productUrlPath = processItem(page, urlProvider, item);
                if (productUrlPath != null) {
                    uniquePagePaths.add(productUrlPath);
                }
            }
        }
        return uniquePagePaths;
    }

    protected String processItem(Page page, UrlProviderImpl urlProvider, Map<String, Object> item) {
        ProductUrlFormat.Params productParams = createProductParams(item);
        String productUrlPath = urlProvider.toProductUrl(null, page, productParams);
        if (productUrlPath != null) {
            productUrlPath = removeUpToDelimiter(productUrlPath, PRODUCT_SAMPLE_URL, false);
            if (productUrlPath != null) {
                productUrlPath = removeUpToDelimiter(productUrlPath, "/", true);
                if (!productUrlPath.endsWith("product-page.html")) {
                    return productUrlPath;
                }
            }
        }
        return null;
    }

    protected ProductUrlFormat.Params createProductParams(Map<String, Object> item) {
        if (item == null) {
            return null;
        }

        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();
        productParams.setUrlKey(PRODUCT_SAMPLE_URL);

        String urlPath = (String) item.get(URL_PATH);
        String urlKey = (String) item.get(URL_KEY);
        String uid = (String) item.get(UID);

        if (urlPath != null || urlKey != null) {
            List<UrlRewrite> urlRewrites = new ArrayList<>(2);
            if (urlPath != null) {
                urlRewrites.add(new UrlRewrite().setUrl(urlPath + "/" + PRODUCT_SAMPLE_URL));
            }
            if (urlKey != null) {
                urlRewrites.add(new UrlRewrite().setUrl(urlKey + "/" + PRODUCT_SAMPLE_URL));
            }
            productParams.setUrlRewrites(urlRewrites);
        }

        if (uid != null || urlKey != null || urlPath != null) {
            CategoryUrlFormat.Params categoryParams = productParams.getCategoryUrlParams();
            categoryParams.setUid(uid);
            categoryParams.setUrlKey(urlKey);
            categoryParams.setUrlPath(urlPath);
        }

        return productParams;
    }

    protected String[] getQueryResult(Query query)
        throws CacheInvalidationException {
        try {
            Set<String> uniquePagePaths = new HashSet<>();
            QueryResult result = query.execute();
            if (result != null) {
                RowIterator rows = result.getRows();
                while (rows.hasNext()) {
                    Row row = rows.nextRow();
                    String fullPath = row.getPath("content");
                    if (fullPath != null) {
                        uniquePagePaths.add(extractPagePath(fullPath) + InvalidateCacheSupport.HTML_SUFFIX);
                    }
                }
            }
            return uniquePagePaths.toArray(new String[0]);
        } catch (Exception e) {
            throw new CacheInvalidationException("Error getting query result", e);
        }
    }

    protected Query getSqlQuery(Session session, String sql2Query) throws CacheInvalidationException {
        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            return queryManager.createQuery(sql2Query, Query.JCR_SQL2);
        } catch (Exception e) {
            throw new CacheInvalidationException("Error creating SKU-based SQL2 query", e);
        }
    }

    protected Map<String, Object> getGraphqlResponseData(GraphqlClient client, String query) {
        if (client == null || query == null) {
            return Collections.emptyMap();
        }

        GraphqlRequest request = new GraphqlRequest(query);
        Type typeOfT = new TypeToken<Map<String, Object>>() {}.getType();
        Type typeOfU = new TypeToken<Map<String, Object>>() {}.getType();
        GraphqlResponse<Map<String, Object>, Map<String, Object>> response = client.execute(request, typeOfT, typeOfU);

        if (response == null || (response.getErrors() != null && !response.getErrors().isEmpty()) || response.getData() == null) {
            return Collections.emptyMap();
        }

        return response.getData();
    }

    protected String extractPagePath(String fullPath) {
        int jcrContentIndex = fullPath.indexOf("/jcr:content");
        return jcrContentIndex != -1 ? fullPath.substring(0, jcrContentIndex) : fullPath;
    }

    protected String formatList(String[] invalidCacheEntries, String delimiter, String pattern) {
        return Arrays.stream(invalidCacheEntries)
            .map(item -> String.format(pattern, item))
            .collect(Collectors.joining(delimiter));
    }
}
