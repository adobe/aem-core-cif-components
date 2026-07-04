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
package com.adobe.cq.commerce.mcp.internal;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Shared validate/adapt scaffold for MCP <em>write</em> tools (authoring-only, {@code writesContent() == true}).
 * <p>
 * Every write tool must resolve its target under the <strong>caller's</strong> {@link ResourceResolver} (never a
 * service/admin resolver, so JCR ACLs are enforced), and must fail closed with {@link IllegalArgumentException} on
 * a blank/missing path, a path outside {@code /content/}, a resource that does not exist, or a resource that is not
 * one of the CIF component/page types the tool understands. This class factors that scaffold out of the individual
 * {@code Configure*Tool}s so new write tools don't re-copy it.
 * <p>
 * This class is a plain helper, not an {@code McpTool} — it is not an OSGi component and is not discovered by
 * {@code ToolRegistry}.
 */
public final class CommerceWriteSupport {

    private CommerceWriteSupport() {}

    /**
     * Resolves {@code path} to its {@link Resource} under the caller's {@code resolver}, gated to one of
     * {@code allowedResourceTypes} via the super-type-aware {@link Resource#isResourceType(String)} (so proxied
     * project components, e.g. Venia's, that super-type one of the allowed types are also accepted).
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param argName the tool argument name {@code path} was read from, used in error messages
     * @param path the resource path to resolve; must be non-blank and under {@code /content/}
     * @param allowedResourceTypes the CIF component/page resource-type literals this tool understands; at least
     *            one must match, checked via {@code resource.isResourceType(type)}
     * @return the resolved target {@link Resource} (its own node, not a page's {@code jcr:content})
     * @throws IllegalArgumentException if {@code path} is blank, not under {@code /content/}, does not resolve to
     *             an existing resource, or the resource is not one of {@code allowedResourceTypes}
     */
    public static Resource resolveComponent(ResourceResolver resolver, String argName, String path,
        List<String> allowedResourceTypes) {
        if (StringUtils.isBlank(path) || !path.startsWith("/content/")) {
            throw new IllegalArgumentException(argName + " (under /content) is required");
        }

        Resource target = resolver.getResource(path);
        if (target == null) {
            throw new IllegalArgumentException("resource not found: " + path);
        }
        if (allowedResourceTypes == null || allowedResourceTypes.stream().noneMatch(target::isResourceType)) {
            throw new IllegalArgumentException("resource is not one of the expected CIF component/page types (" + argName
                + "): " + path);
        }
        return target;
    }

    /**
     * Adapts {@code resource} to a {@link ModifiableValueMap}, failing closed if the resource is not modifiable
     * (e.g. it does not resolve to a real JCR node under the caller's resolver).
     *
     * @param resource the resource to adapt
     * @param argName the tool argument name the resource was resolved from, used in the error message
     * @return the resource's {@link ModifiableValueMap}
     * @throws IllegalArgumentException if the resource is not adaptable to {@link ModifiableValueMap}
     */
    public static ModifiableValueMap mutableMap(Resource resource, String argName) {
        ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new IllegalArgumentException(argName + " resource not modifiable: " + resource.getPath());
        }
        return properties;
    }
}
