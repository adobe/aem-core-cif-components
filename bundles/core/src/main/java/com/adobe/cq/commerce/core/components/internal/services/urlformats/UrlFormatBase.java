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

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceUtil;

import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.drew.lang.annotations.NotNull;

public class UrlFormatBase {

    protected static String HTML_EXTENSION = ".html";
    protected static String HTML_EXTENSION_AND_SUFFIX = HTML_EXTENSION + "/";

    /**
     * @see UrlFormatBase#getUrlKey(String, String)
     */
    public static String getUrlKey(ProductUrlFormat.Params params) {
        return getUrlKey(params.getUrlPath(), params.getUrlKey());
    }

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
    public static String getUrlKey(String urlPath, String urlKey) {
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
     * @see UrlFormatBase#selectUrlPath(String, List, String, String, String)
     */
    public static String selectUrlPath(String urlPath, List<String> alternatives, String urlKey) {
        return selectUrlPath(urlPath, alternatives, urlKey, null, null);
    }

    /**
     * @see UrlFormatBase#selectUrlPath(String, List, String, String, String)
     */
    public static String selectUrlPath(ProductUrlFormat.Params params) {
        return selectUrlPath(
            params.getUrlPath(),
            params.getUrlRewrites(),
            getUrlKey(params.getUrlPath(), params.getUrlKey()),
            params.getCategoryUrlParams().getUrlKey(),
            params.getCategoryUrlParams().getUrlPath());
    }

    /**
     * Selects the right url_path from the given parameters.
     * <p>
     * If a non-empty {@code urlPath} parameter is given it is simply returned. Otherwise one of the given {@code alternatives} will be
     * selected. The selection returns the first alternative with the most path segments, split by {@code "/"}. However, only the
     * alternatives which end with the given {@code urlKey} are considers.
     * <p>
     * If the optional {@code contextUrlPath} is given, the selection will prefer alternatives that use segments of this url_path. This
     * does not require an exact match, it will also rank partial matches higher.
     * <p>
     * TODO: In the future make this a Service that customers can override to decide what the "right" url_path would be.
     *
     * @param urlPath a given url_path. If not empty, this will immediately be returned
     * @param alternatives a list of alternative url_paths
     * @param urlKey an url_key to filter for. If not {@code null} only the alternatives that end with that url_key will be
     *            considered
     * @param contextUrlKey the url_key of a category defining the context for the current selection. If {@code null} it will be retrieved
     *            from the {@code contextUrlPath} if given.
     * @param contextUrlPath the url_path of a category defining the context for the current selection. If not given, the canonical url_path
     *            will be returned
     * @return
     */
    public static String selectUrlPath(@Nullable String urlPath, @NotNull List<String> alternatives, @Nullable String urlKey,
        @Nullable String contextUrlKey, @Nullable String contextUrlPath) {
        if (StringUtils.isNotEmpty(urlPath)) {
            return urlPath;
        }

        contextUrlKey = contextUrlKey == null && contextUrlPath != null
            ? StringUtils.substringAfterLast(contextUrlPath, "/")
            : contextUrlKey;
        String[] contextParts = StringUtils.isNotEmpty(contextUrlPath) ? contextUrlPath.split("/") : null;
        String[] candidateParts = new String[0];
        int candidateScore = 0;

        for (String alternative : alternatives) {
            if (StringUtils.isEmpty(alternative)) {
                // skip null or empty strings
                continue;
            }
            String[] alternativeParts = alternative.split("/");
            if (alternativeParts[alternativeParts.length - 1].equals(urlKey) || urlKey == null) {
                int alternativeScore = matchesContext(contextUrlKey, contextParts, alternativeParts);
                if (alternativeScore > candidateScore
                    || (alternativeScore == candidateScore && alternativeParts.length > candidateParts.length)) {
                    // only if the alternative matches more segments of the context, or when it matches the same number of times and
                    // has more segments than the current candidate.
                    candidateScore = alternativeScore;
                    candidateParts = alternativeParts;
                }
            }
        }

        return candidateParts.length > 0 ? StringUtils.join(candidateParts, '/') : urlKey;
    }

    private static int matchesContext(String contextUrlKey, String[] contextParts, String[] alternativeParts) {
        // prefix match first, counting the overlap with the contextParts
        // contextParts: venia-accessories/belts
        // alternativeParts:
        // * venia-accessories/product => 1
        // * venia-accessories/belts/product => 2 ...
        // * new-products/belts/product => 0
        int matches = 0;
        if (contextParts != null) {
            for (int i = 0; i < Math.min(contextParts.length, alternativeParts.length); i++) {
                if (contextParts[i].equals(alternativeParts[i])) {
                    matches++;
                } else {
                    break;
                }
            }
        }
        // if there is no overlap we try to find at least the contextUrlKey anywhere in the alternative parts
        if (matches == 0 && ArrayUtils.indexOf(alternativeParts, contextUrlKey) >= 0) {
            // if that is the case we seed the matches to one as the context is more relevant than no match but still would be less relevant
            // if we find another alternative that scores higher with the prefix match above.
            matches = 1;
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
