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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.request.RequestPathInfo;

import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlPath;

public interface UrlFormat {

    /**
     * A {@link Map} of default patterns for product pages supported by the default implementation of
     * {@link com.adobe.cq.commerce.core.components.services.UrlProvider}.
     */
    Map<String, UrlFormat> DEFAULT_PRODUCT_URL_FORMATS = new HashMap<String, UrlFormat>() {
        {
            put(ProductPageWithSku.PATTERN, ProductPageWithSku.INSTANCE);
            put(ProductPageWithUrlKey.PATTERN, ProductPageWithUrlKey.INSTANCE);
            put(ProductPageWithSkuAndUrlKey.PATTERN, ProductPageWithSkuAndUrlKey.INSTANCE);
            put(ProductPageWithUrlPath.PATTERN, ProductPageWithUrlPath.INSTANCE);
            put(ProductPageWithSkuAndUrlPath.PATTERN, ProductPageWithSkuAndUrlPath.INSTANCE);
        }
    };

    /**
     * A {@link Map} of default patterns for category pages supported by the default implementation of
     * {@link com.adobe.cq.commerce.core.components.services.UrlProvider}.
     */
    Map<String, UrlFormat> DEFAULT_CATEGORY_URL_FORMATS = new HashMap<String, UrlFormat>() {
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
     * Parses a given request URI using the internal configured pattern.
     * <p>
     * Returns a {@link Map} with up to all the parameter names returned by {@link UrlFormat#getParameterNames()}.
     * <p>
     * Passing the returned {@link Map} of parameters into {@link UrlFormat#format(Map)} must return the same pathInfo as used as input
     * before.
     *
     * @param requestPathInfo the request path info object used to extra the URL information from
     * @return a map containing the parsed URL elements
     */
    Map<String, String> parse(RequestPathInfo requestPathInfo);

    /**
     * Returns a set of all parameter names the url format implementation supports when parsing a pathinfo.
     * <p>
     * This may return more parameters, than the url fromat uses in {@link UrlFormat#format(Map)}.
     *
     * @return all supported parameter names.
     */
    Set<String> getParameterNames();

}
