/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.core.components.internal.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Utility methods to resolve source content resources from AEM timeline/version preview resources.
 */
public final class VersionHistoryUtils {

    private static final String VERSION_HISTORY_ROOT = "/tmp/versionhistory/";

    private VersionHistoryUtils() {}

    /**
     * Returns {@code true} when the resource is a synthetic version preview resource under /tmp/versionhistory.
     */
    public static boolean isVersionHistoryResource(Resource resource) {
        return resource != null && StringUtils.startsWith(resource.getPath(), VERSION_HISTORY_ROOT);
    }

    /**
     * Resolves a version preview resource back to its source page/resource so configuration lookups can work.
     */
    public static Resource resolveSourceResource(Resource resource) {
        if (!isVersionHistoryResource(resource)) {
            return resource;
        }

        String sourcePagePath = getSourcePagePath(resource.getPath());
        if (StringUtils.isBlank(sourcePagePath)) {
            return resource;
        }

        ResourceResolver resolver = resource.getResourceResolver();
        Resource sourcePageResource = resolver.getResource(sourcePagePath);
        if (sourcePageResource != null) {
            return sourcePageResource;
        }

        return resource;
    }

    private static String getSourcePagePath(String path) {
        String suffix = StringUtils.substringAfter(path, VERSION_HISTORY_ROOT);
        if (StringUtils.isBlank(suffix)) {
            return null;
        }

        int secondSlash = StringUtils.ordinalIndexOf(suffix, "/", 2);
        if (secondSlash < 0 || secondSlash == suffix.length() - 1) {
            return null;
        }

        String relativePath = suffix.substring(secondSlash + 1);
        return "/content/" + StringUtils.removeEnd(relativePath, "/");
    }
}
