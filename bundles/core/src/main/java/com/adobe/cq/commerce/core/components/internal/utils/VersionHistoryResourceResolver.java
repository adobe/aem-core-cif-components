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
import org.apache.sling.api.resource.ValueMap;

/**
 * Utility methods to resolve source content resources from AEM timeline/version preview resources.
 */
public final class VersionHistoryResourceResolver {

    private static final String VERSION_HISTORY_ROOT = "/tmp/versionhistory/";

    private static final String[] SOURCE_PATH_PROPERTIES = {
        "cq:sourcePath",
        "sourcePath",
        "jcr:sourcePath"
    };

    private VersionHistoryResourceResolver() {}

    public static Resource resolveSourceResource(Resource resource) {
        if (resource == null) {
            return null;
        }

        Resource propertyBasedResource = resolveFromSourcePathProperty(resource);
        if (propertyBasedResource != null) {
            return propertyBasedResource;
        }

        if (!StringUtils.startsWith(resource.getPath(), VERSION_HISTORY_ROOT)) {
            return resource;
        }

        String relativePath = getRelativePath(resource.getPath());
        if (StringUtils.isBlank(relativePath)) {
            return resource;
        }

        ResourceResolver resolver = resource.getResourceResolver();
        Resource candidate = resolveCandidatePath(resolver, "/" + relativePath);
        if (candidate != null) {
            return candidate;
        }

        if (!StringUtils.startsWith(relativePath, "content/")) {
            candidate = resolveCandidatePath(resolver, "/content/" + relativePath);
            if (candidate != null) {
                return candidate;
            }
        }

        return resource;
    }

    private static Resource resolveFromSourcePathProperty(Resource resource) {
        ValueMap properties = resource.getValueMap();
        for (String propertyName : SOURCE_PATH_PROPERTIES) {
            String sourcePath = properties.get(propertyName, String.class);
            if (StringUtils.isNotBlank(sourcePath)) {
                Resource sourceResource = resource.getResourceResolver().getResource(sourcePath);
                if (sourceResource != null) {
                    return sourceResource;
                }
            }
        }
        return null;
    }

    private static Resource resolveCandidatePath(ResourceResolver resolver, String candidatePath) {
        String path = StringUtils.removeEnd(candidatePath, "/");
        while (StringUtils.isNotBlank(path)) {
            Resource candidate = resolver.getResource(path);
            if (candidate != null) {
                return candidate;
            }
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash <= 0) {
                break;
            }
            path = path.substring(0, lastSlash);
        }
        return null;
    }

    private static String getRelativePath(String path) {
        String suffix = StringUtils.substringAfter(path, VERSION_HISTORY_ROOT);
        if (StringUtils.isBlank(suffix)) {
            return null;
        }

        int firstSlash = suffix.indexOf('/');
        if (firstSlash < 0) {
            return null;
        }
        int secondSlash = suffix.indexOf('/', firstSlash + 1);
        if (secondSlash < 0 || secondSlash + 1 >= suffix.length()) {
            return null;
        }

        return suffix.substring(secondSlash + 1);
    }
}
