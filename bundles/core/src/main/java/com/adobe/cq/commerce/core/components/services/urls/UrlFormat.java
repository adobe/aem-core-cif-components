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
package com.adobe.cq.commerce.core.components.services.urls;

import java.util.Map;
import java.util.Set;

import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Consumers may implement this interface to provide a custom {@link UrlFormat} to the {@link UrlProvider} implementation.
 * <p>
 * The implementation(s) of this interface must be registered as OSGI service and must have the {@link UrlFormat#PROP_USE_AS} property set.
 * The {@link UrlFormat} with the {@link UrlFormat#PROP_USE_AS} set to {@link UrlFormat#PRODUCT_PAGE_URL_FORMAT} will be used by the
 * {@link UrlProvider} implementation to format and parse product urls, the {@link UrlFormat} registered with
 * {@link UrlFormat#CATEGORY_PAGE_URL_FORMAT} to format and parse category urls.
 * <p>
 * If any {@link UrlFormat} is registered as described above the override the configured behaviour of the {@link UrlProvider}
 * implementation. Implementing a {@link UrlFormat} is optional.
 */
@ConsumerType
public interface UrlFormat {

    /**
     * The service registration property used to identify the purpose of the {@link UrlFormat}. It can either be set to
     * {@link UrlFormat#PRODUCT_PAGE_URL_FORMAT} or {@link UrlFormat#CATEGORY_PAGE_URL_FORMAT}.When
     */
    String PROP_USE_AS = "useAs";

    /**
     * The value of the {@link UrlFormat#PROP_USE_AS} property to be set when the {@link UrlFormat} should be used to format and parse
     * product urls.
     */
    String PRODUCT_PAGE_URL_FORMAT = "productPageUrlFormat";

    /**
     * The value of the {@link UrlFormat#PROP_USE_AS} property to be set when the {@link UrlFormat} should be used to format and parse
     * category urls.
     */
    String CATEGORY_PAGE_URL_FORMAT = "categoryPageUrlFormat";

    /**
     * A {@link CharSequence} to be used to write defaults to the format when a mandatory parameter is missing.
     * <p>
     * Consumers can check formatted urls on the existence of this {@link CharSequence} to identify urls that failed formatting.
     * <p>
     * Example usage: {@code parameters.getOrDefault(key, OPENING_BRACKETS + key + CLOSING_BRACKETS)}
     */
    String OPENING_BRACKETS = "{{";

    /**
     * A {@link CharSequence} to be used to write defaults to the format when a mandatory parameter is missing.
     * <p>
     * Consumers can check formatted urls on the existence of this {@link CharSequence} to identify urls that failed formatting.
     * <p>
     * Example usage: {@code parameters.getOrDefault(key, OPENING_BRACKETS + key + CLOSING_BRACKETS)}
     */
    String CLOSING_BRACKETS = "}}";

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
    Map<String, String> parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap);

    /**
     * Returns a set of all parameter names the url format implementation supports when parsing a pathinfo.
     * <p>
     * This may return more parameters, than the url format uses in {@link UrlFormat#format(Map)}.
     *
     * @return all supported parameter names.
     */
    Set<String> getParameterNames();

}
