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
package com.adobe.cq.commerce.mcp.internal.tools.authoring;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Shared site-derivation + generic-authoring scaffold for the site-aware MCP authoring tools
 * ({@code list_page_templates}, {@code list_site_components}, {@code create_content_page}, {@code add_components},
 * {@code update_component}, {@code remove_component}): derive the site's {@code /conf/<site>} and
 * {@code /apps/<site>/components} roots from the endpoint's own nav-root (landing) page, find a template's
 * editable drop zone, resolve {@code cq:Component} definitions, convert JSON argument values to JCR property
 * values, and enforce that generic component writes stay inside a page's {@code jcr:content} subtree.
 * <p>
 * This is a plain stateless helper, not an {@code McpTool} -- it is not an OSGi component and not discovered by
 * {@code ToolRegistry}.
 */
public final class SiteAppsSupport {

    private static final String TEMPLATES_SUFFIX = "/settings/wcm/templates/";
    private static final String JCR_CONTENT = "jcr:content";

    private SiteAppsSupport() {}

    /**
     * Derives the site's {@code /conf/<site>} root from the landing page's own {@code cq:template} property (e.g.
     * {@code /conf/venia/settings/wcm/templates/landing-page} &rarr; {@code /conf/venia}).
     *
     * @param landingPage the endpoint's nav-root page (from {@code StoreContext.getLandingPage()})
     * @return the conf root path, or {@code null} when the landing page carries no derivable {@code cq:template}
     */
    public static String confPathFor(Page landingPage) {
        Resource content = landingPage == null ? null : landingPage.getContentResource();
        String template = content == null ? null : content.getValueMap().get("cq:template", String.class);
        if (template == null) {
            return null;
        }
        int idx = template.indexOf(TEMPLATES_SUFFIX);
        return idx > 0 ? template.substring(0, idx) : null;
    }

    /**
     * Derives the site's {@code /apps/<site>/components} root from the landing page's {@code jcr:content}
     * {@code sling:resourceType} (e.g. {@code venia/components/structure/page} &rarr;
     * {@code /apps/venia/components}). Absolute resource types and the shared {@code core} namespace carry no
     * site signal and yield {@code null}.
     *
     * @param landingPage the endpoint's nav-root page (from {@code StoreContext.getLandingPage()})
     * @return the site's apps components root path, or {@code null} when no site-specific root can be derived
     */
    public static String appsComponentsPathFor(Page landingPage) {
        Resource content = landingPage == null ? null : landingPage.getContentResource();
        String resourceType = content == null ? null : content.getResourceType();
        if (StringUtils.isBlank(resourceType) || resourceType.startsWith("/")) {
            return null;
        }
        int slash = resourceType.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        String site = resourceType.substring(0, slash);
        if ("core".equals(site)) {
            return null;
        }
        return "/apps/" + site + "/components";
    }

    /**
     * Maps a core CIF component resource type (e.g. {@code core/cif/components/commerce/productteaser/v1/productteaser})
     * to a site-specific <em>proxy</em> component under {@code /apps/<site>/components} whose
     * {@code sling:resourceSuperType} chain resolves to that core type, returning the proxy's {@code /apps}-relative
     * resource type so a created node references the site's proxy (picking up its policies/styles) rather than the
     * shared core component. When no site apps root can be derived, the apps root does not resolve, or no proxy
     * super-types the core type, the {@code coreResourceType} is returned unchanged (fail-open to a still-valid type).
     * <p>
     * The super-type chain is walked manually (via {@link #resolveComponentDefinition}) rather than through
     * {@link Resource#isResourceType(String)}, because the pinned aem-mock matches {@code isResourceType} by identity
     * only and does not follow {@code sling:resourceSuperType} the way real AEM does.
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param landingPage the endpoint's nav-root page (from {@code StoreContext.getLandingPage()})
     * @param coreResourceType the core CIF resource type to map
     * @return the site proxy's {@code /apps}-relative resource type, or {@code coreResourceType} when no proxy applies
     */
    public static String resolveSiteResourceType(ResourceResolver resolver, Page landingPage, String coreResourceType) {
        String appsComponents = appsComponentsPathFor(landingPage);
        Resource root = (appsComponents == null || resolver == null) ? null : resolver.getResource(appsComponents);
        if (root == null) {
            return coreResourceType;
        }
        Deque<Resource> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Resource current = stack.pop();
            for (Resource child : current.getChildren()) {
                if (!"cq:Component".equals(child.getValueMap().get("jcr:primaryType", String.class))) {
                    stack.push(child); // component folder (sling:Folder etc.) -- recurse
                    continue;
                }
                if (superTypeChainReaches(resolver, child, coreResourceType)) {
                    return child.getPath().substring("/apps/".length());
                }
            }
        }
        return coreResourceType;
    }

    /**
     * True if {@code component}'s {@code sling:resourceSuperType} chain reaches {@code coreResourceType}. Walks the
     * chain manually, resolving each super-type to its {@code cq:Component} definition to read the next hop, and
     * guards against cycles via a visited set.
     */
    private static boolean superTypeChainReaches(ResourceResolver resolver, Resource component, String coreResourceType) {
        Set<String> visited = new HashSet<String>();
        String superType = component.getValueMap().get("sling:resourceSuperType", String.class);
        while (StringUtils.isNotBlank(superType) && visited.add(superType)) {
            if (superType.equals(coreResourceType)) {
                return true;
            }
            Resource superDefinition = resolveComponentDefinition(resolver, superType);
            if (superDefinition == null) {
                return false;
            }
            superType = superDefinition.getValueMap().get("sling:resourceSuperType", String.class);
        }
        return false;
    }

    /**
     * Finds a template's editable drop zone: the <strong>deepest</strong> descendant of
     * {@code <template>/structure/jcr:content} carrying {@code editable=true} (the responsive grid new page
     * content is authored into). The returned path is relative to the page's {@code jcr:content} (e.g.
     * {@code root/container}), so a page created from the template receives its components under
     * {@code <page>/jcr:content/<returned path>}.
     *
     * @param template the {@code /conf/*&#47;settings/wcm/templates/<name>} resource
     * @return the editable container's path relative to {@code jcr:content}, or {@code null} when the template's
     *         structure declares no editable container
     */
    public static String findEditableContainerPath(Resource template) {
        Resource structureContent = template == null ? null : template.getChild("structure/" + JCR_CONTENT);
        if (structureContent == null) {
            return null;
        }
        String base = structureContent.getPath() + "/";
        String deepest = null;
        int deepestSegments = -1;
        Deque<Resource> stack = new ArrayDeque<>();
        stack.push(structureContent);
        while (!stack.isEmpty()) {
            Resource current = stack.pop();
            for (Resource child : current.getChildren()) {
                stack.push(child);
                if (Boolean.TRUE.equals(child.getValueMap().get("editable", Boolean.class))) {
                    String relative = child.getPath().substring(base.length());
                    int segments = StringUtils.countMatches(relative, '/');
                    if (segments > deepestSegments) {
                        deepestSegments = segments;
                        deepest = relative;
                    }
                }
            }
        }
        return deepest;
    }

    /**
     * Resolves a {@code sling:resourceType} to its {@code cq:Component} definition: an absolute type is looked up
     * as-is; a relative type is searched under {@code /apps/} then {@code /libs/} (the Sling resource-type search
     * path order). A resolved node that is not primary-typed {@code cq:Component} yields {@code null}, so callers
     * fail closed on arbitrary repository paths posing as component types.
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param resourceType the resource type to resolve (relative like {@code venia/components/text}, or absolute)
     * @return the component definition resource, or {@code null} when it does not resolve to a {@code cq:Component}
     */
    public static Resource resolveComponentDefinition(ResourceResolver resolver, String resourceType) {
        if (StringUtils.isBlank(resourceType)) {
            return null;
        }
        Resource definition;
        if (resourceType.startsWith("/")) {
            definition = resolver.getResource(resourceType);
        } else {
            definition = resolver.getResource("/apps/" + resourceType);
            if (definition == null) {
                definition = resolver.getResource("/libs/" + resourceType);
            }
        }
        if (definition == null
            || !"cq:Component".equals(definition.getValueMap().get("jcr:primaryType", String.class))) {
            return null;
        }
        return definition;
    }

    /**
     * Converts a JSON argument value to the JCR property value the generic component write tools persist:
     * booleans and integral numbers keep their native type ({@code Boolean}/{@code Long}), other numbers become
     * {@code Double}, a JSON array of scalars becomes a {@code String[]} (the shape CIF multi-value properties
     * use), and everything else is stringified. JSON {@code null} yields Java {@code null}, which callers treat
     * as "remove the property" -- never the string {@code "null"} (see the {@code NullNode.asText()} pitfall).
     *
     * @param value the JSON value from the tool's {@code properties} argument
     * @return the JCR-typed value to write, or {@code null} when the property should be removed
     * @throws IllegalArgumentException if {@code value} is a nested JSON object (composite children are out of
     *             scope for a flat property write)
     */
    public static Object toJcrValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isIntegralNumber()) {
            return value.asLong();
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (value.isArray()) {
            String[] values = new String[value.size()];
            for (int i = 0; i < value.size(); i++) {
                JsonNode item = value.get(i);
                if (item == null || item.isNull() || item.isContainerNode()) {
                    throw new IllegalArgumentException("array property values must be non-null scalars");
                }
                values[i] = item.asText();
            }
            return values;
        }
        if (value.isObject()) {
            throw new IllegalArgumentException("nested object property values are not supported");
        }
        return value.asText();
    }

    /**
     * Enforces that a generic component write targets a node strictly <strong>inside</strong> a page's
     * {@code jcr:content} subtree: the resolved resource must have a {@code jcr:content} ancestor segment and must
     * not be the {@code jcr:content} node itself (page-level fields are the domain of the dedicated
     * {@code configure_*} tools, and deleting {@code jcr:content} would destroy the page). Fails closed with
     * {@link IllegalArgumentException} otherwise.
     *
     * @param resource the already-resolved target resource (under {@code /content/})
     * @param argName the tool argument name the resource was resolved from, used in error messages
     * @throws IllegalArgumentException if the resource is not strictly inside a page's {@code jcr:content} subtree
     */
    public static void requireInsidePageContent(Resource resource, String argName) {
        String path = resource.getPath();
        int idx = path.indexOf("/" + JCR_CONTENT + "/");
        if (idx < 0 || path.endsWith("/" + JCR_CONTENT)) {
            throw new IllegalArgumentException(
                argName + " must be a component inside a page's jcr:content subtree: " + path);
        }
    }
}
