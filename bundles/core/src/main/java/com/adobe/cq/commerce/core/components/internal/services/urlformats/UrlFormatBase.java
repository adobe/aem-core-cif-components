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

    /**
     * @see UrlFormatBase#selectUrlPath(String, List, String, String)
     */
    protected static String selectUrlPath(String urlPath, List<String> alternatives, String urlKey) {
        return selectUrlPath(urlPath, alternatives, urlKey, null);
    }

    /**
     * Selects the right url_path from the given parameters.
     * <p>
     * If a non-empty {@code urlPath} parameter is given it is simply returned. Otherwise one of the given {@code alternatives} will be
     * selected. The selection returns the first alternative with the most path segments, split by {@code "/"}. However, only the
     * alternatives which end with the given {@code urlKey} are considers.
     * <p>
     * If the optional {@code contextUrlPath} is given, the selection will prefer alternatives that use segments of this url_path. This does
     * not require an exact match, it will also rank partial matches higher.
     *
     * @param urlPath
     * @param alternatives
     * @param urlKey
     * @param contextUrlPath
     * @return
     */
    protected static String selectUrlPath(String urlPath, List<String> alternatives, String urlKey, String contextUrlPath) {
        if (StringUtils.isNotEmpty(urlPath)) {
            return urlPath;
        }

        String[] contextParts = StringUtils.isNotEmpty(contextUrlPath) ? contextUrlPath.split("/") : null;
        String[] candidateParts = new String[0];
        int candidateMatches = -1;

        for (String alternative : alternatives) {
            String[] alternativeParts = alternative.split("/");
            if (alternativeParts.length >= candidateParts.length && alternativeParts[alternativeParts.length - 1].equals(urlKey)) {
                if (alternativeParts.length == candidateParts.length && contextParts != null) {
                    // check for contextPart matches also if the candidate and alternative have the same number of path segments
                    if (candidateMatches < 0) {
                        candidateMatches = countMatches(contextParts, candidateParts);
                    }
                    int alternativeMatches = countMatches(contextParts, alternativeParts);
                    if (alternativeMatches <= candidateMatches) {
                        // current candidate is ranked higher or equal already
                        continue;
                    }
                    candidateMatches = alternativeMatches;
                    candidateParts = alternativeParts;
                } else if (alternativeParts.length > candidateParts.length) {
                    // otherwise, only consider the alternative if it has more segments than the canidate (first match)
                    candidateParts = alternativeParts;
                }
            }
        }

        return candidateParts.length > 0 ? StringUtils.join(candidateParts, '/') : urlKey;
    }

    private static int countMatches(String[] left, String[] right) {
        int matches = 0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            if (left[i].equals(right[i])) {
                matches++;
            } else {
                break;
            }
        }
        return matches;
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
