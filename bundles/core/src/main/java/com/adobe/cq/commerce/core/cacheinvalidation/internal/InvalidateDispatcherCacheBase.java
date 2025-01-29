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

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.day.cq.wcm.api.Page;

public class InvalidateDispatcherCacheBase extends InvalidateCacheBase {

    protected static final String URL_KEY = "url_key";
    protected static final String URL_PATH = "url_path";
    protected static final String HTML_SUFFIX = ".html";
    protected static final String PRODUCT_SAMPLE_URL = "XXXXXX";

    protected void addProductPaths(Page page, UrlProviderImpl urlProvider, Set<String> uniquePagePaths, Map<String, Object> item) {
        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();
        productParams.setSku((String) item.get("sku"));
        productParams.setUrlKey((String) item.get("url_key"));

        List<Map<String, String>> urlRewrites = (List<Map<String, String>>) item.get("url_rewrites");
        if (urlRewrites != null) {
            for (Map<String, String> urlRewrite : urlRewrites) {
                productParams.setUrlRewrites(Collections.singletonList(new UrlRewrite().setUrl(urlRewrite.get("url"))));
                String productUrlPath = urlProvider.toProductUrl(null, page, productParams);
                uniquePagePaths.add(productUrlPath);
            }
        }
    }

    protected void addCategoryPaths(Page page, UrlProviderImpl urlProvider, Set<String> uniquePagePaths,
        List<Map<String, Object>> categories) {
        CategoryUrlFormat.Params categoryParams = new CategoryUrlFormat.Params();
        if (categories != null) {
            for (Map<String, Object> category : categories) {
                categoryParams.setUid((String) category.get("uid"));
                categoryParams.setUrlKey((String) category.get("url_key"));
                categoryParams.setUrlPath((String) category.get("url_path"));
                String categoryUrlPath = urlProvider.toCategoryUrl(null, page, categoryParams);
                categoryUrlPath = removeUpToDelimiter(categoryUrlPath, ".html", true);
                uniquePagePaths.add(categoryUrlPath);
            }
        }
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

    protected void processItems(Page page, UrlProviderImpl urlProvider, Set<String> uniquePagePaths, List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            if (item != null) {
                processItem(page, urlProvider, uniquePagePaths, item);
            }
        }
    }

    protected void processItem(Page page, UrlProviderImpl urlProvider, Set<String> uniquePagePaths, Map<String, Object> item) {
        ProductUrlFormat.Params productParams = createProductParams(item);
        String productUrlPath = urlProvider.toProductUrl(null, page, productParams);
        if (productUrlPath != null) {
            productUrlPath = removeUpToDelimiter(productUrlPath, PRODUCT_SAMPLE_URL, false);
            if (productUrlPath != null) {
                productUrlPath = removeUpToDelimiter(productUrlPath, "/", true);
                if (!productUrlPath.endsWith("product-page.html")) {
                    uniquePagePaths.add(productUrlPath);
                }
            }
        }
    }

    protected ProductUrlFormat.Params createProductParams(Map<String, Object> item) {
        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();
        productParams.setUrlKey(PRODUCT_SAMPLE_URL);
        List<UrlRewrite> urlRewrites = Arrays.asList(
            new UrlRewrite().setUrl((String) item.get(URL_PATH) + "/" + PRODUCT_SAMPLE_URL),
            new UrlRewrite().setUrl((String) item.get(URL_KEY) + "/" + PRODUCT_SAMPLE_URL));
        productParams.setUrlRewrites(urlRewrites);

        productParams.getCategoryUrlParams().setUid((String) item.get("uid"));
        productParams.getCategoryUrlParams().setUrlKey((String) item.get(URL_KEY));
        productParams.getCategoryUrlParams().setUrlPath((String) item.get(URL_PATH));
        return productParams;
    }

}
