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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.Page;

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
     * Resolves {@code path} to a CIF <strong>page's</strong> {@code jcr:content} resource, for write tools that
     * operate on page-level fields (e.g. nav-config pagefields) rather than on a component instance.
     * <p>
     * Unlike {@link #resolveComponent}, which gates the resolved resource itself, this method requires {@code path}
     * to adapt to {@link Page} (failing closed if it does not — e.g. it is a component resource, not a page), then
     * gates the page's {@code jcr:content} resource (not the page resource itself) against
     * {@code allowedContentTypes} via the same super-type-aware {@link Resource#isResourceType(String)}.
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param argName the tool argument name {@code path} was read from, used in error messages
     * @param path the page path to resolve; must be non-blank and under {@code /content/}
     * @param allowedContentTypes the CIF structure-page resource-type literals this tool understands; at least one
     *            must match the page's {@code jcr:content} resource, checked via {@code content.isResourceType(type)}
     * @return the resolved page's {@code jcr:content} {@link Resource}
     * @throws IllegalArgumentException if {@code path} is blank, not under {@code /content/}, does not resolve to
     *             an existing resource, does not adapt to {@link Page}, or the page's {@code jcr:content} resource
     *             is not one of {@code allowedContentTypes}
     */
    public static Resource resolvePageContent(ResourceResolver resolver, String argName, String path,
        List<String> allowedContentTypes) {
        if (StringUtils.isBlank(path) || !path.startsWith("/content/")) {
            throw new IllegalArgumentException(argName + " (under /content) is required");
        }

        Resource target = resolver.getResource(path);
        if (target == null) {
            throw new IllegalArgumentException("resource not found: " + path);
        }
        Page page = target.adaptTo(Page.class);
        if (page == null) {
            throw new IllegalArgumentException(argName + " does not resolve to a page: " + path);
        }
        Resource content = page.getContentResource();
        if (content == null || allowedContentTypes == null
            || allowedContentTypes.stream().noneMatch(content::isResourceType)) {
            throw new IllegalArgumentException("page is not one of the expected CIF structure-page types (" + argName
                + "): " + path);
        }
        return content;
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

    /**
     * Writes an optional string property, clearing it instead of persisting a meaningless value: a blank
     * (null/empty/whitespace-only) {@code value} removes {@code propertyName} from {@code map} rather than storing
     * it, so callers can pass an empty string (or whitespace) to explicitly clear a previously-set value.
     *
     * @param map the {@link ModifiableValueMap} to update
     * @param propertyName the property to set or remove
     * @param value the value to write; {@link StringUtils#isBlank(CharSequence)} values remove the property instead
     */
    public static void putOrRemove(ModifiableValueMap map, String propertyName, String value) {
        if (StringUtils.isBlank(value)) {
            map.remove(propertyName);
        } else {
            map.put(propertyName, value);
        }
    }

    /**
     * Writes an optional multi-valued string property, clearing it instead of persisting a meaningless empty
     * array: a {@code null} or empty {@code values} removes {@code propertyName} from {@code map} rather than
     * storing it, so callers can pass an empty list to explicitly clear a previously-set value.
     *
     * @param map the {@link ModifiableValueMap} to update
     * @param propertyName the property to set or remove
     * @param values the values to write as a {@code String[]}; {@code null}/empty removes the property instead
     */
    public static void putOrRemoveArray(ModifiableValueMap map, String propertyName, List<String> values) {
        if (values == null || values.isEmpty()) {
            map.remove(propertyName);
        } else {
            map.put(propertyName, values.toArray(new String[0]));
        }
    }

    /**
     * Resolves {@code path} to a writable <strong>container</strong> resource for Tier-3 node creation (e.g. a
     * responsive grid a bulk-create tool stamps new component children under), failing closed on: a blank path, a
     * path outside {@code /content/}, a non-existent resource, a resource that is itself a {@code cq:Page} (the
     * caller must pass a container resource inside the page, e.g. the responsive grid, not the page itself), or a
     * resource not adaptable to {@link ModifiableValueMap} (not a real writable JCR node).
     * <p>
     * Unlike {@link #resolveComponent}, which gates the resolved resource against a fixed list of CIF
     * component/page resource types, this method has no resource-type allowlist -- any writable non-page container
     * under {@code /content/} is accepted, since the container itself (e.g. a responsive grid) is not a CIF
     * component.
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param argName the tool argument name {@code path} was read from, used in error messages
     * @param path the container resource path to resolve; must be non-blank and under {@code /content/}
     * @return the resolved container {@link Resource}
     * @throws IllegalArgumentException if {@code path} is blank, not under {@code /content/}, does not resolve to
     *             an existing resource, is itself a {@code cq:Page}, or is not adaptable to {@link ModifiableValueMap}
     */
    public static Resource resolveContainer(ResourceResolver resolver, String argName, String path) {
        if (StringUtils.isBlank(path) || !path.startsWith("/content/")) {
            throw new IllegalArgumentException(argName + " (under /content) is required");
        }

        Resource container = resolver.getResource(path);
        if (container == null) {
            throw new IllegalArgumentException("resource not found: " + path);
        }
        if (container.adaptTo(Page.class) != null) {
            throw new IllegalArgumentException(
                argName + " must be a container resource inside a page, not the page itself: " + path);
        }
        if (container.adaptTo(ModifiableValueMap.class) == null) {
            throw new IllegalArgumentException(argName + " resource not modifiable: " + path);
        }
        return container;
    }

    /**
     * Resolves {@code path} to its {@link Resource} under the caller's {@code resolver}, gated to a content
     * fragment: the path must be non-blank, under {@code /content/dam} (see {@link DamConstants#MOUNTPOINT_ASSETS}),
     * resolve to an existing resource, and adapt to {@link ContentFragment} (a DAM asset that is not a content
     * fragment adapts to {@code null} and is rejected).
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param argName the tool argument name {@code path} was read from, used in error messages
     * @param path the content-fragment resource path to resolve; must be non-blank and under {@code /content/dam}
     * @return the resolved content-fragment {@link Resource}
     * @throws IllegalArgumentException if {@code path} is blank, not under {@code /content/dam}, does not resolve
     *             to an existing resource, or does not adapt to {@link ContentFragment}
     */
    public static Resource resolveContentFragment(ResourceResolver resolver, String argName, String path) {
        if (StringUtils.isBlank(path) || !path.startsWith(DamConstants.MOUNTPOINT_ASSETS + "/")) {
            throw new IllegalArgumentException(argName + " (under " + DamConstants.MOUNTPOINT_ASSETS + ") is required");
        }

        Resource target = resolver.getResource(path);
        if (target == null) {
            throw new IllegalArgumentException("resource not found: " + path);
        }
        if (target.adaptTo(ContentFragment.class) == null) {
            throw new IllegalArgumentException(argName + " is not a content fragment: " + path);
        }
        return target;
    }

    /**
     * Creates a new, uniquely-named child resource under {@code parent} (Tier-3 node creation, e.g. a bulk-created
     * productteaser/productcarousel component instance), computing a collision-free sibling name from
     * {@code baseName} via {@link ResourceUtil#createUniqueChildName(Resource, String)}.
     * <p>
     * {@code jcr:primaryType} is forced to {@code nt:unstructured} in the written properties when the caller did not
     * already supply one, matching every other node this bundle creates (see {@link #writeComposite}). The caller
     * is responsible for {@link ResourceResolver#commit()} — this method only stages the create.
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced); used to compute the unique name and
     *            create the child
     * @param parent the container resource the new child is created under
     * @param baseName the preferred child name; a numeric suffix is appended if it already exists under
     *            {@code parent}
     * @param props the properties to write on the new child (e.g. {@code sling:resourceType}, {@code selection});
     *            not mutated -- a defensive copy is made before {@code jcr:primaryType} is added
     * @return the newly created, uncommitted {@link Resource}
     * @throws PersistenceException if a unique name cannot be computed or resource creation fails
     */
    public static Resource createChild(ResourceResolver resolver, Resource parent, String baseName,
        Map<String, Object> props) throws PersistenceException {
        String uniqueName = ResourceUtil.createUniqueChildName(parent, baseName);

        Map<String, Object> childProps = new HashMap<String, Object>(props);
        if (!childProps.containsKey("jcr:primaryType")) {
            childProps.put("jcr:primaryType", "nt:unstructured");
        }

        return resolver.create(parent, uniqueName, childProps);
    }

    /**
     * Rewrites a composite-multifield child node under {@code parent}: an existing {@code parent/childName} node
     * (and its entire subtree) is removed first, then -- when {@code items} is non-empty -- a fresh
     * {@code parent/childName} node (primary type {@code nt:unstructured}) is created with children named
     * {@code item0, item1, …} (also {@code nt:unstructured}), each carrying the properties from the corresponding
     * map in {@code items}, in list order.
     * <p>
     * This is the mechanism CIF composite multifields use on disk (e.g. featuredcategorylist/categorycarousel's
     * {@code items}, productlist's {@code fragments}): a container node whose children are read back in child order
     * by {@code resource.getChild(childName).getChildren()}, unlike a flat {@code String[]} property.
     * <p>
     * A {@code null} or empty {@code items} simply clears the container (no replacement node is created), so
     * callers can pass an empty list to explicitly remove a previously-configured composite multifield.
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced); used to delete the prior node and
     *            create the new container/children
     * @param parent the resource under which {@code childName} lives (e.g. a component instance resource)
     * @param childName the composite multifield's container node name (e.g. {@code items}, {@code fragments})
     * @param items the ordered list of per-child property maps; {@code null}/empty clears the container
     * @throws PersistenceException if resource creation/removal fails
     */
    public static void writeComposite(ResourceResolver resolver, Resource parent, String childName,
        List<Map<String, Object>> items) throws PersistenceException {
        Resource existing = parent.getChild(childName);
        if (existing != null) {
            resolver.delete(existing);
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        Map<String, Object> containerProps = new HashMap<String, Object>();
        containerProps.put("jcr:primaryType", "nt:unstructured");
        Resource container = resolver.create(parent, childName, containerProps);

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> childProps = new HashMap<String, Object>(items.get(i));
            childProps.put("jcr:primaryType", "nt:unstructured");
            resolver.create(container, "item" + i, childProps);
        }
    }
}
