/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestPathInfo;

import static com.adobe.cq.commerce.core.components.services.UrlProvider.PAGE_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.SKU_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.URL_KEY_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.URL_PATH_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.VARIANT_SKU_PARAM;

public interface UrlFormat {
    Map<String, UrlFormat> DEFAULT_PRODUCTURL_FORMATS = new HashMap<String, UrlFormat>() {
        {
            put(ProductPageWithSku.PATTERN, ProductPageWithSku.INSTANCE);
            put(ProductPageWithUrlKey.PATTERN, ProductPageWithUrlKey.INSTANCE);
            put(ProductPageWithUrlPath.PATTERN, ProductPageWithUrlPath.INSTANCE);
        }
    };

    Map<String, UrlFormat> DEFAULT_CATEGORYURL_FORMATS = new HashMap<String, UrlFormat>() {
        {
            put(CategoryPageWithUrlPath.PATTERN, CategoryPageWithUrlPath.INSTANCE);
        }
    };

    /**
     * Formats an URL with the given parameters.
     * 
     * @param parameters the URL parameters to be applied to the URL according to the internal format
     * @return the formated URL
     */
    String format(Map<String, String> parameters);

    /**
     * Parses a givven request URI using the internal configured pattern.
     * 
     * @param requestPathInfo the request path info object used to extra the URL information from
     * @return a map containing the parsed URL elements
     */
    Map<String, String> parse(RequestPathInfo requestPathInfo);

    abstract class AbstractUrlFormat implements UrlFormat {
        static final String HTML_EXTENSION = ".html";

        String format(Map<String, String> parameters, String identifier) {
            return parameters.getOrDefault(PAGE_PARAM, "{{page}}") + HTML_EXTENSION + "/" + parameters.getOrDefault(identifier,
                "{{" + identifier + "}}")
                + HTML_EXTENSION + (StringUtils.isNotBlank(parameters.get(VARIANT_SKU_PARAM)) ? "#" + parameters.get(VARIANT_SKU_PARAM)
                    : "");
        }
    }

    class ProductPageWithSku extends AbstractUrlFormat {
        public static final ProductPageWithSku INSTANCE = new ProductPageWithSku();
        public static final String PATTERN = "{{page}}.html/{{sku}}.html#{{variant_sku}}";

        @Override
        public String format(Map<String, String> parameters) {
            return super.format(parameters, SKU_PARAM);
        }

        @Override
        public Map<String, String> parse(RequestPathInfo requestPathInfo) {
            if (requestPathInfo == null) {
                return Collections.emptyMap();
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put(PAGE_PARAM, requestPathInfo.getResourcePath());
            String suffix = StringUtils.removeStart(StringUtils.removeEnd(requestPathInfo.getSuffix(), HTML_EXTENSION), "/");
            if (StringUtils.isNotBlank(suffix)) {
                parameters.put(SKU_PARAM, suffix);
            }
            return parameters;
        }
    }

    class ProductPageWithUrlKey extends AbstractUrlFormat {
        public static final ProductPageWithUrlKey INSTANCE = new ProductPageWithUrlKey();
        public static final String PATTERN = "{{page}}.html/{{url_key}}.html#{{variant_sku}}";

        @Override
        public String format(Map<String, String> parameters) {
            return super.format(parameters, URL_KEY_PARAM);
        }

        @Override
        public Map<String, String> parse(RequestPathInfo requestPathInfo) {
            if (requestPathInfo == null) {
                return Collections.emptyMap();
            }

            return new HashMap<String, String>() {
                {
                    put(PAGE_PARAM, requestPathInfo.getResourcePath());
                    String suffix = StringUtils.removeStart(StringUtils.removeEnd(requestPathInfo.getSuffix(), HTML_EXTENSION), "/");
                    if (StringUtils.isNotBlank(suffix)) {
                        put(URL_KEY_PARAM, suffix);
                    }
                }
            };
        }
    }

    class ProductPageWithUrlPath extends AbstractUrlFormat {
        public static final ProductPageWithUrlKey INSTANCE = new ProductPageWithUrlKey();
        public static final String PATTERN = "{{page}}.html/{{url_path}}.html#{{variant_sku}}";

        @Override
        public String format(Map<String, String> parameters) {
            return super.format(parameters, URL_PATH_PARAM);
        }

        @Override
        public Map<String, String> parse(RequestPathInfo requestPathInfo) {
            if (requestPathInfo == null) {
                return Collections.emptyMap();
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put(PAGE_PARAM, requestPathInfo.getResourcePath());
            String suffix = StringUtils.removeStart(StringUtils.removeEnd(requestPathInfo.getSuffix(), HTML_EXTENSION), "/");
            if (StringUtils.isNotBlank(suffix)) {
                parameters.put(URL_PATH_PARAM, suffix);
                parameters.put(URL_KEY_PARAM, suffix.indexOf("/") > 0 ? StringUtils.substringAfterLast(suffix, "/") : suffix);
            }
            return parameters;
        }
    }

    class CategoryPageWithUrlPath extends AbstractUrlFormat {
        public static final CategoryPageWithUrlPath INSTANCE = new CategoryPageWithUrlPath();
        public static final String PATTERN = "{{page}}.html/{{url_path}}.html";

        @Override
        public String format(Map<String, String> parameters) {
            return super.format(parameters, URL_PATH_PARAM);
        }

        @Override
        public Map<String, String> parse(RequestPathInfo requestPathInfo) {
            if (requestPathInfo == null) {
                return Collections.emptyMap();
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put(PAGE_PARAM, requestPathInfo.getResourcePath());
            String suffix = StringUtils.removeStart(StringUtils.removeEnd(requestPathInfo.getSuffix(), HTML_EXTENSION), "/");
            if (StringUtils.isNotBlank(suffix)) {
                parameters.put(URL_PATH_PARAM, suffix);
            }
            return parameters;
        }
    }
}
