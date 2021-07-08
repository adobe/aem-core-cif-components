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
package com.adobe.cq.commerce.core.components.internal.services.urlformats;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceUtil;

import com.adobe.cq.commerce.core.components.internal.services.UrlFormat;

import static com.adobe.cq.commerce.core.components.services.UrlProvider.URL_KEY_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.URL_PATH_PARAM;

abstract class AbstractUrlFormat implements UrlFormat {
    protected static final String HTML_EXTENSION = ".html";

    /**
     * Returns the url_key from the given paramters. If not present but the url_path is, the last segment of the url_path os returned
     * instead.
     * <p>
     * Returns {@code "{{" + URL_KEY_PARAM + "}}"} when the url_key is empty
     *
     * @param parameters
     * @return
     */
    protected static String getUrlKey(Map<String, String> parameters) {
        String urlKey = null;
        if (StringUtils.isEmpty(parameters.get(URL_KEY_PARAM))) {
            if (StringUtils.isNotEmpty(parameters.get(URL_PATH_PARAM))) {
                urlKey = StringUtils.substringAfterLast(parameters.get(URL_PATH_PARAM), "/");
                if (StringUtils.isEmpty(urlKey)) {
                    urlKey = parameters.get(URL_PATH_PARAM);
                }
            }
        } else {
            urlKey = parameters.get(URL_KEY_PARAM);
        }
        return StringUtils.isNotEmpty(urlKey) ? urlKey : "{{" + URL_KEY_PARAM + "}}";
    }

    protected static String removeJcrContent(String path) {
        if (path == null) {
            return null;
        } else if (JcrConstants.JCR_CONTENT.equals(ResourceUtil.getName(path))) {
            return ResourceUtil.getParent(path);
        } else {
            return path;
        }
    }
}
