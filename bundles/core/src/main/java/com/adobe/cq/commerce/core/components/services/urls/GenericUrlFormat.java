/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.core.components.services.urls;

import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Consumers may implement this interface to provide a custom {@link GenericUrlFormat} to the {@link UrlProvider} implementation.
 * <p>
 * This is a generic service type that may not be implemented directly. Instead, implement any of the specific subtypes of this service and
 * register the implementation as OSGI service.
 * <p>
 * If any {@link GenericUrlFormat} is registered as described above, it overrides the configured behaviour of the {@link UrlProvider}
 * implementation. Implementing a {@link GenericUrlFormat} is optional.
 *
 * @param <ParameterType> The type which this GenericUrlFormat uses to format and parse urls.
 */
@ConsumerType
public interface GenericUrlFormat<ParameterType> {

    /**
     * Formats an URL with the given parameters.
     *
     * @param parameters the URL parameters to be applied to the URL according to the internal format
     * @return the formated URL
     */
    String format(ParameterType parameters);

    /**
     * Parses a given request URI using the internal configured pattern.
     * <p>
     * Passing the returned instance of {@link ParameterType} into {@code format()} must return the same
     * path info as used as input before.
     *
     * @param requestPathInfo the request path info object used to extra the URL information from
     * @param parameterMap the request parameters the implementation may consider when parsing the url
     * @return a map containing the parsed URL elements
     */
    ParameterType parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap);

    /**
     * Implementations may return a subset of parameters from the given parameters when they parse less
     * or other parameters then available for formatting.
     * <p>
     * An example would be a category url format that uses only the category uid but gets also url_key
     * and url_path for formatting.
     * <p>
     * The default implementation simply returns the parameters given.
     *
     * @param parameters all URL parameters available for formatting
     * @return a copy of the given parameters, reduced to those that would be available after parsing
     */
    default ParameterType retainParsableParameters(ParameterType parameters) {
        return parameters;
    }
}
