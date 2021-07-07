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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestPathInfo;

import com.google.common.collect.Sets;

import static com.adobe.cq.commerce.core.components.services.UrlProvider.PAGE_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.SKU_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.URL_KEY_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.URL_PATH_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.VARIANT_SKU_PARAM;

public interface UrlFormat {

    static final String HTML_EXTENSION = ".html";
    Map<String, UrlFormat> DEFAULT_PRODUCTURL_FORMATS = new HashMap<String, UrlFormat>() {
        {
            put(ProductPageWithSku.PATTERN, ProductPageWithSku.INSTANCE);
            put(ProductPageWithUrlKey.PATTERN, ProductPageWithUrlKey.INSTANCE);
            put(ProductPageWithSkuAndUrlKey.PATTERN, ProductPageWithSkuAndUrlKey.INSTANCE);
            put(ProductPageWithUrlPath.PATTERN, ProductPageWithUrlPath.INSTANCE);
            put(ProductPageWithSkuAndUrlPath.PATTERN, ProductPageWithSkuAndUrlPath.INSTANCE);
        }
    };

    Map<String, UrlFormat> DEFAULT_CATEGORYURL_FORMATS = new HashMap<String, UrlFormat>() {
        {
            put(CategoryPageWithUrlPath.PATTERN, CategoryPageWithUrlPath.INSTANCE);
            put(CategoryPageWithUrlKey.PATTERN, CategoryPageWithUrlKey.INSTANCE);
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

    /**
     * Returns a set of all parameter names the url format implementation supports.
     * 
     * @return all supported parameter names.
     */
    Set<String> getParameterNames();

    class ProductPageWithSku implements UrlFormat {
        public static final ProductPageWithSku INSTANCE = new ProductPageWithSku();
        public static final String PATTERN = "{{page}}.html/{{sku}}.html#{{variant_sku}}";

        @Override
        public String format(Map<String, String> parameters) {
            return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
                parameters.getOrDefault(SKU_PARAM, "{{" + SKU_PARAM + "}}") + HTML_EXTENSION +
                (StringUtils.isNotBlank(parameters.get(VARIANT_SKU_PARAM)) ? "#" + parameters.get(VARIANT_SKU_PARAM) : "");
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
                        put(SKU_PARAM, suffix);
                    }
                }
            };
        }

        @Override
        public Set<String> getParameterNames() {
            return Sets.newHashSet(PAGE_PARAM, SKU_PARAM, VARIANT_SKU_PARAM);
        }
    }

    class ProductPageWithUrlKey implements UrlFormat {
        public static final ProductPageWithUrlKey INSTANCE = new ProductPageWithUrlKey();
        public static final String PATTERN = "{{page}}.html/{{url_key}}.html#{{variant_sku}}";

        @Override
        public String format(Map<String, String> parameters) {
            return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
                parameters.getOrDefault(URL_KEY_PARAM, "{{" + URL_KEY_PARAM + "}}") + HTML_EXTENSION +
                (StringUtils.isNotBlank(parameters.get(VARIANT_SKU_PARAM)) ? "#" + parameters.get(VARIANT_SKU_PARAM) : "");
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

        @Override
        public Set<String> getParameterNames() {
            return Sets.newHashSet(PAGE_PARAM, URL_KEY_PARAM, VARIANT_SKU_PARAM);
        }
    }

    class ProductPageWithSkuAndUrlKey implements UrlFormat {
        public static final ProductPageWithSkuAndUrlKey INSTANCE = new ProductPageWithSkuAndUrlKey();
        public static final String PATTERN = "{{page}}.html/{{sku}}/{{url_key}}.html#{{variant_sku}}";

        @Override
        public String format(Map<String, String> parameters) {
            return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
                parameters.getOrDefault(SKU_PARAM, "{{" + SKU_PARAM + "}}") + "/" +
                parameters.getOrDefault(URL_KEY_PARAM, "{{" + URL_KEY_PARAM + "}}") + HTML_EXTENSION +
                (StringUtils.isNotBlank(parameters.get(VARIANT_SKU_PARAM)) ? "#" + parameters.get(VARIANT_SKU_PARAM) : "");
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
                        if (suffix.indexOf("/") > 0) {
                            put(SKU_PARAM, StringUtils.substringBefore(suffix, "/"));
                            put(URL_KEY_PARAM, StringUtils.substringAfter(suffix, "/"));
                        } else {
                            put(URL_KEY_PARAM, suffix);
                        }
                    }
                }
            };
        }

        @Override
        public Set<String> getParameterNames() {
            return Sets.newHashSet(PAGE_PARAM, SKU_PARAM, URL_KEY_PARAM, VARIANT_SKU_PARAM);
        }
    }

    class ProductPageWithUrlPath implements UrlFormat {
        public static final ProductPageWithUrlPath INSTANCE = new ProductPageWithUrlPath();
        public static final String PATTERN = "{{page}}.html/{{url_path}}.html#{{variant_sku}}";

        @Override
        public String format(Map<String, String> parameters) {
            return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
                parameters.getOrDefault(URL_PATH_PARAM, "{{" + URL_PATH_PARAM + "}}") + HTML_EXTENSION +
                (StringUtils.isNotBlank(parameters.get(VARIANT_SKU_PARAM)) ? "#" + parameters.get(VARIANT_SKU_PARAM) : "");
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
                        put(URL_PATH_PARAM, suffix);
                        put(URL_KEY_PARAM, suffix.indexOf("/") > 0 ? StringUtils.substringAfterLast(suffix, "/") : suffix);
                    }
                }
            };
        }

        @Override
        public Set<String> getParameterNames() {
            return Sets.newHashSet(PAGE_PARAM, URL_PATH_PARAM, VARIANT_SKU_PARAM);
        }
    }

    class ProductPageWithSkuAndUrlPath implements UrlFormat {
        public static final ProductPageWithSkuAndUrlPath INSTANCE = new ProductPageWithSkuAndUrlPath();
        public static final String PATTERN = "{{page}}.html/{{sku}}/{{url_path}}.html#{{variant_sku}}";

        @Override
        public String format(Map<String, String> parameters) {
            return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
                parameters.getOrDefault(SKU_PARAM, "{{" + SKU_PARAM + "}}") + "/" +
                parameters.getOrDefault(URL_PATH_PARAM, "{{" + URL_PATH_PARAM + "}}") + HTML_EXTENSION +
                (StringUtils.isNotBlank(parameters.get(VARIANT_SKU_PARAM)) ? "#" + parameters.get(VARIANT_SKU_PARAM) : "");
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
                        if (suffix.indexOf("/") > 0) {
                            put(SKU_PARAM, StringUtils.substringBefore(suffix, "/"));
                            String urlPath = StringUtils.substringAfter(suffix, "/");
                            put(URL_PATH_PARAM, urlPath);
                            put(URL_KEY_PARAM, urlPath.indexOf("/") > 0 ? StringUtils.substringAfterLast(urlPath, "/") : urlPath);
                        } else {
                            put(URL_PATH_PARAM, suffix);
                        }
                    }
                }
            };
        }

        @Override
        public Set<String> getParameterNames() {
            return Sets.newHashSet(PAGE_PARAM, SKU_PARAM, URL_KEY_PARAM, URL_PATH_PARAM, VARIANT_SKU_PARAM);
        }
    }

    class CategoryPageWithUrlPath implements UrlFormat {
        public static final CategoryPageWithUrlPath INSTANCE = new CategoryPageWithUrlPath();
        public static final String PATTERN = "{{page}}.html/{{url_path}}.html";

        @Override
        public String format(Map<String, String> parameters) {
            return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
                parameters.getOrDefault(URL_PATH_PARAM, "{{" + URL_PATH_PARAM + "}}") + HTML_EXTENSION;
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
                        put(URL_PATH_PARAM, suffix);
                        put(URL_KEY_PARAM, suffix.indexOf("/") > 0 ? StringUtils.substringAfterLast(suffix, "/") : suffix);
                    }
                }
            };
        }

        @Override
        public Set<String> getParameterNames() {
            return Sets.newHashSet(PAGE_PARAM, URL_PATH_PARAM, URL_KEY_PARAM);
        }
    }

    class CategoryPageWithUrlKey implements UrlFormat {
        public static final CategoryPageWithUrlKey INSTANCE = new CategoryPageWithUrlKey();
        public static final String PATTERN = "{{page}}.html/{{url_key}}.html";

        @Override
        public String format(Map<String, String> parameters) {
            return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
                parameters.getOrDefault(URL_KEY_PARAM, "{{" + URL_KEY_PARAM + "}}") + HTML_EXTENSION;
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

        @Override
        public Set<String> getParameterNames() {
            return Sets.newHashSet(PAGE_PARAM, URL_KEY_PARAM);
        }
    }
}
