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
package com.adobe.cq.commerce.core.components.internal.services.urlformats;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceUtil;

class UrlFormatBase {

    protected static String HTML_EXTENSION = ".html";
    protected static String HTML_EXTENSION_AND_SUFFIX = HTML_EXTENSION + "/";

    /**
     * Returns the url_key from the given parameters.
     * <p>
     * If no urlKey is given but a urlPath, the last path segment from the urlPath is extracted and returned as urlKey.
     * <p>
     * Returns {@code "{{url_key}}"} when the url_key is empty
     *
     * @param urlPath
     * @param urlKey
     * @return
     */
    protected static String getUrlKey(String urlPath, String urlKey) {
        if (StringUtils.isEmpty(urlKey)) {
            if (StringUtils.isNotEmpty(urlPath)) {
                urlKey = StringUtils.substringAfterLast(urlPath, "/");
                if (StringUtils.isEmpty(urlKey)) {
                    urlKey = urlPath;
                }
            }
        }
        return StringUtils.isNotEmpty(urlKey) ? urlKey : "{{url_key}}";
    }

    protected static String getOptionalAnchor(String anchor) {
        return StringUtils.isNotEmpty(anchor) ? "#" + anchor : "";
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
