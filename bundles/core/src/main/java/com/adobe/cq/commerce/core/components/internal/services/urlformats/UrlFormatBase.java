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

import java.util.List;

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
        return StringUtils.isNotEmpty(urlKey) ? urlKey : null;
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
            return ResourceUtil.normalize(path);
        }
    }

    protected static String selectUrlPath(String urlPath, List<String> alternatives, String urlKey) {
        if (StringUtils.isNotEmpty(urlPath)) {
            return urlPath;
        }

        String[] candidateParts = new String[0];

        for (String alternative : alternatives) {
            String[] optionParts = alternative.split("/");
            if (optionParts.length > candidateParts.length && optionParts[optionParts.length - 1].equals(urlKey)) {
                candidateParts = optionParts;
            }
        }

        return candidateParts.length > 0 ? StringUtils.join(candidateParts, '/') : urlKey;
    }

    /**
     * Extracts the category url_path and url_key encoded in a product's url_path.
     *
     * @param productUrlPath
     * @return a {@code String[]} with exactly 2 elements. The first is the url_key the second the url_path.
     */
    protected static String[] extractCategoryUrlFormatParams(String productUrlPath) {
        int lastSlash = productUrlPath != null ? productUrlPath.lastIndexOf('/') : -1;
        if (lastSlash < 0) {
            return new String[] { null, null };
        }

        String categoryUrlPath = productUrlPath.substring(0, lastSlash);
        lastSlash = categoryUrlPath.lastIndexOf("/");
        if (lastSlash > 0) {
            return new String[] { categoryUrlPath.substring(lastSlash + 1), categoryUrlPath };
        } else {
            return new String[] { categoryUrlPath, categoryUrlPath };
        }
    }
}
