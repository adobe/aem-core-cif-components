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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Session;
import javax.jcr.query.*;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.day.cq.wcm.api.Page;

public class InvalidateDispatcherCacheBase {

    public static final String URL_KEY = "url_key";
    public static final String URL_PATH = "url_path";
    public static final String SKU = "sku";
    public static final String UID = "uid";
    public static final String PRODUCT_SAMPLE_URL = "XXXXXX";
    public static final String DISPATCHER_CATEGORY_URL_PATH = "categoryUrlPath";
    public static final String DISPATCHER_PRODUCT_URL_PATH = "productUrlPath";
    public static final String DISPATCHER_PAGE_URL_PATH = "pageUrlPath";

    protected Set<String> getProductPaths(Page page, UrlProviderImpl urlProvider,
        Map<String, Object> item, List<PatternConfig> patternsConfig) {
        Set<String> uniquePagePaths = new HashSet<>();
        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();
        productParams.setSku((String) item.get(SKU));
        productParams.setUrlKey((String) item.get(URL_KEY));

        List<Map<String, String>> urlRewrites = (List<Map<String, String>>) item.get("url_rewrites");
        if (urlRewrites != null) {
            for (Map<String, String> urlRewrite : urlRewrites) {
                productParams.setUrlRewrites(Collections.singletonList(new UrlRewrite().setUrl(urlRewrite.get("url"))));
                String productUrlPath = urlProvider.toProductUrl(null, page, productParams);
                uniquePagePaths.add(convertUrlPath(patternsConfig, productUrlPath));
            }
        }
        return uniquePagePaths;
    }

    protected Set<String> getCategoryPaths(Page page, UrlProviderImpl urlProvider,
        List<Map<String, Object>> categories, List<PatternConfig> patternsConfig) {
        Set<String> uniquePagePaths = new HashSet<>();
        CategoryUrlFormat.Params categoryParams = new CategoryUrlFormat.Params();
        if (categories != null) {
            for (Map<String, Object> category : categories) {
                categoryParams.setUid((String) category.get(UID));
                categoryParams.setUrlKey((String) category.get(URL_KEY));
                categoryParams.setUrlPath((String) category.get(URL_PATH));
                String categoryUrlPath = urlProvider.toCategoryUrl(null, page, categoryParams);
                categoryUrlPath = removeUpToDelimiter(categoryUrlPath, InvalidateCacheSupport.HTML_SUFFIX, true);
                uniquePagePaths.add(convertUrlPath(patternsConfig, categoryUrlPath));
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
        List<Map<String, Object>> items, List<PatternConfig> patternsConfig) {
        Set<String> uniquePagePaths = new HashSet<>();
        for (Map<String, Object> item : items) {
            if (item != null) {
                String productUrlPath = processItem(page, urlProvider, item);
                if (productUrlPath != null) {
                    uniquePagePaths.add(convertUrlPath(patternsConfig, productUrlPath));
                }
            }
        }
        return uniquePagePaths;
    }

    protected List<PatternConfig> getPatternAndMatch(InvalidateCacheSupport invalidateCacheSupport, String storePath, String urlPathType) {
        List<PatternConfig> dispatcherUrlData = invalidateCacheSupport.getDispatcherUrlConfigurationBasedOnStorePathAndType(storePath,
            urlPathType);
        return dispatcherUrlData != null ? dispatcherUrlData : new ArrayList<>();
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
        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();
        productParams.setUrlKey(PRODUCT_SAMPLE_URL);
        List<UrlRewrite> urlRewrites = Arrays.asList(
            new UrlRewrite().setUrl(item.get(URL_PATH) + "/" + PRODUCT_SAMPLE_URL),
            new UrlRewrite().setUrl(item.get(URL_KEY) + "/" + PRODUCT_SAMPLE_URL));
        productParams.setUrlRewrites(urlRewrites);

        productParams.getCategoryUrlParams().setUid((String) item.get(UID));
        productParams.getCategoryUrlParams().setUrlKey((String) item.get(URL_KEY));
        productParams.getCategoryUrlParams().setUrlPath((String) item.get(URL_PATH));
        return productParams;
    }

    protected String[] getQueryResult(InvalidateCacheSupport invalidateCacheSupport, Query query, String storePath)
        throws CacheInvalidationException {
        try {
            Set<String> uniquePagePaths = new HashSet<>();
            QueryResult result = query.execute();
            if (result != null) {
                List<PatternConfig> patternsConfig = getPatternAndMatch(invalidateCacheSupport, storePath, DISPATCHER_PAGE_URL_PATH);
                RowIterator rows = result.getRows();
                while (rows.hasNext()) {
                    Row row = rows.nextRow();
                    String fullPath = row.getPath("content");
                    if (fullPath != null) {
                        String pagePath = invalidateCacheSupport.extractPagePath(fullPath) + InvalidateCacheSupport.HTML_SUFFIX;
                        uniquePagePaths.add(convertUrlPath(patternsConfig, pagePath));
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

    protected String convertUrlPath(List<PatternConfig> patternsConfig, String urlPath) {
        if (patternsConfig != null) {
            for (PatternConfig patternConfig : patternsConfig) {
                String pattern = patternConfig.getPattern();
                String match = patternConfig.getMatch();
                if (pattern != null && match != null) {
                    Pattern patternObj = Pattern.compile(pattern);
                    Matcher matcher = patternObj.matcher(urlPath);
                    if (matcher.matches()) {
                        urlPath = matcher.replaceAll(match);
                        break;
                    }
                }
            }
        }
        return urlPath;
    }
}
