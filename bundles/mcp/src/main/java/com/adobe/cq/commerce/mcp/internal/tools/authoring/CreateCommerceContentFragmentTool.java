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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.CommerceWriteSupport;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.day.cq.dam.api.DamConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool that creates a new commerce content fragment from a CF model under a DAM folder and seeds it with
 * the commerce {@code linkElement} value (a SKU or category UID) plus any extra scalar fields, via the caller's
 * {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * This is the "provision a new commerce CF" companion to {@code update_commerce_content_fragment_field} (which edits
 * an already-existing fragment): it creates the fragment from a model resource (the {@code modelPath}, which must
 * adapt to a {@link FragmentTemplate}), then seeds the {@code linkElement} model field with the {@code identifier}
 * so the resulting fragment is discoverable by SKU/category-UID match the same way
 * {@code get_commerce_content_fragment} resolves it. Only master (base) values are written; each seed is applied by
 * the element's own arity via {@link CommerceWriteSupport#applyElementValue} (single-value as text, multi-value as a
 * single-element list), so a multi-value {@code linkElement} such as the commerce {@code sku} field is supported.
 * <p>
 * Supports {@code dryRun} (default {@code false}): when {@code true}, the would-be fragment path and the list of
 * fields that would be seeded are computed and returned, but nothing is created or committed.
 * <p>
 * Like every write tool in this module, this never auto-publishes: it ends at {@link ResourceResolver#commit()},
 * leaving activation to the normal authoring/publish flow.
 */
@Component(service = McpTool.class)
public class CreateCommerceContentFragmentTool implements McpTool {

    private static final String PRODUCT = "product";
    private static final String CATEGORY = "category";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "create_commerce_content_fragment";
    }

    @Override
    public String description() {
        return "Create a new commerce content fragment from a CF model under a DAM folder and seed its linkElement "
            + "with a product SKU or category UID (plus optional extra fields), so the fragment is discoverable by "
            + "get_commerce_content_fragment. Seeds the master value only, applying each seed by the element's own "
            + "type: a single-value element takes the value as text, a multi-value element (e.g. the commerce sku "
            + "product-reference field) takes it as a single-element list. Supports dryRun to preview the fragment "
            + "path and seeded fields without persisting anything. Never auto-publishes -- ends at commit().";
    }

    @Override
    public boolean writesContent() {
        return true;
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("identifier").put("type", "string");
        ObjectNode type = properties.putObject("type");
        type.put("type", "string");
        type.putArray("enum").add(PRODUCT).add(CATEGORY);
        properties.putObject("modelPath").put("type", "string");
        properties.putObject("linkElement").put("type", "string");
        properties.putObject("fields").put("type", "object");
        properties.putObject("parentPath").put("type", "string");
        properties.putObject("name").put("type", "string");
        properties.putObject("title").put("type", "string");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("identifier").add("type").add("modelPath").add("linkElement");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;

        String identifier = args.path("identifier").asText(null);
        String type = args.path("type").asText(null);
        String modelPath = args.path("modelPath").asText(null);
        String linkElement = args.path("linkElement").asText(null);
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("identifier is required");
        }
        if (StringUtils.isBlank(modelPath)) {
            throw new IllegalArgumentException("modelPath is required");
        }
        if (StringUtils.isBlank(linkElement)) {
            throw new IllegalArgumentException("linkElement is required");
        }
        if (!PRODUCT.equals(type) && !CATEGORY.equals(type)) {
            throw new IllegalArgumentException("type must be 'product' or 'category'");
        }

        boolean dryRun = args.path("dryRun").asBoolean(false);

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();

        String parentPath = args.path("parentPath").asText(null);
        if (StringUtils.isBlank(parentPath)) {
            parentPath = DamConstants.MOUNTPOINT_ASSETS;
        }
        if (!parentPath.equals(DamConstants.MOUNTPOINT_ASSETS)
            && !parentPath.startsWith(DamConstants.MOUNTPOINT_ASSETS + "/")) {
            throw new IllegalArgumentException(
                "parentPath (under " + DamConstants.MOUNTPOINT_ASSETS + ") is required: " + parentPath);
        }
        Resource parent = resolver.getResource(parentPath);
        if (parent == null) {
            throw new IllegalArgumentException("parentPath resource not found: " + parentPath);
        }

        Resource model = resolver.getResource(modelPath);
        if (model == null) {
            throw new IllegalArgumentException("modelPath resource not found: " + modelPath);
        }

        // Collect the scalar fields to seed, in a deterministic order: linkElement first, then each extra field.
        Map<String, String> seed = new LinkedHashMap<String, String>();
        seed.put(linkElement, identifier);
        JsonNode fieldsNode = args.path("fields");
        if (fieldsNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = fieldsNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                JsonNode value = entry.getValue();
                if (!value.isNull()) {
                    seed.put(entry.getKey(), value.asText());
                }
            }
        }

        String explicitName = args.path("name").asText(null);
        String name = StringUtils.isNotBlank(explicitName) ? explicitName
            : ResourceUtil.createUniqueChildName(parent, sanitize(identifier));
        String title = args.path("title").asText(null);
        if (StringUtils.isBlank(title)) {
            title = identifier;
        }

        ObjectNode out = mapper.createObjectNode();
        out.put("modelPath", modelPath);
        ArrayNode seeded = mapper.createArrayNode();
        for (String field : seed.keySet()) {
            seeded.add(field);
        }

        if (dryRun) {
            out.put("fragmentPath", parent.getPath() + "/" + name);
            out.set("seeded", seeded);
            out.put("dryRun", true);
            return out;
        }

        ContentFragment fragment = createFragment(resolver, parent, model, name, title);
        for (Map.Entry<String, String> field : seed.entrySet()) {
            seedElement(fragment, field.getKey(), field.getValue());
        }
        resolver.commit();

        // Post-write verification: re-read each seeded element so we never report success for a seed that did not
        // take (AGENTS.md 4 -- not merely "no exception was thrown").
        for (Map.Entry<String, String> field : seed.entrySet()) {
            if (!readBack(fragment, field.getKey(), field.getValue())) {
                throw new IllegalStateException("failed to verify seeded field '" + field.getKey()
                    + "' on created content fragment");
            }
        }

        Resource fragmentResource = fragment.adaptTo(Resource.class);
        out.put("fragmentPath", fragmentResource != null ? fragmentResource.getPath() : parent.getPath() + "/" + name);
        out.set("seeded", seeded);
        out.put("dryRun", false);
        return out;
    }

    /**
     * Seeds a single element's master (base) value on the just-created fragment via the shared, arity-aware
     * {@link CommerceWriteSupport#applyElementValue}: a multi-value element (e.g. the commerce {@code sku}
     * product-reference field) receives the value as a single-element {@code String[]}, a single-value element as a
     * plain {@code String}. Fails closed if the element is unknown or the CF API rejects the value.
     *
     * @throws IllegalArgumentException if the element is unknown, or does not accept the value
     */
    private void seedElement(ContentFragment fragment, String elementName, String value) {
        ContentElement element = fragment.getElement(elementName);
        if (element == null) {
            throw new IllegalArgumentException("unknown element on model: " + elementName);
        }
        CommerceWriteSupport.applyElementValue(element, null, false, new String[] { value });
    }

    /**
     * Re-reads a just-seeded element (fresh from the fragment, after {@code commit()}) and confirms the value
     * actually round-tripped via the shared arity-aware {@link CommerceWriteSupport#elementValueRoundTrips}, so
     * success reflects a real per-field readback rather than merely "no exception was thrown".
     */
    private boolean readBack(ContentFragment fragment, String elementName, String expected) {
        ContentElement element = fragment.getElement(elementName);
        if (element == null) {
            return false;
        }
        return CommerceWriteSupport.elementValueRoundTrips(element, null, false, new String[] { expected });
    }

    /**
     * Sanitizes {@code identifier} into a JCR-node-name base: lowercased, with every non-alphanumeric character
     * replaced by {@code -}. The result is only a <em>base</em>; {@link ResourceUtil#createUniqueChildName} appends
     * a numeric suffix if it already exists under the parent.
     */
    private String sanitize(String identifier) {
        return identifier.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    /**
     * Creates the content fragment from {@code model} under {@code parent}, failing closed if {@code model} is not a
     * content-fragment model (its {@code adaptTo(FragmentTemplate.class)} is {@code null}). Extracted as a protected
     * seam so unit tests can supply a canned {@link ContentFragment} (Mockito) without needing aem-mock's uncertain
     * content-fragment write support.
     */
    protected ContentFragment createFragment(ResourceResolver resolver, Resource parent, Resource model, String name,
        String title) throws ContentFragmentException {
        FragmentTemplate template = model.adaptTo(FragmentTemplate.class);
        if (template == null) {
            throw new IllegalArgumentException("modelPath is not a content-fragment model: " + model.getPath());
        }
        return template.createFragment(parent, name, title);
    }
}
