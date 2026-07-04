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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.FragmentData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool that sets a single element's value on a commerce content fragment, via the caller's
 * {@link ResourceResolver} so that JCR ACLs are enforced.
 * <p>
 * This is the "real editing capability" companion to the read-only {@code get_commerce_content_fragment}: it does
 * not resolve a fragment by SKU/category match, it operates directly on a {@code fragmentPath} the caller already
 * knows (e.g. from that tool's {@code fragmentPath} result). By default the element's <strong>master</strong>
 * (base) value is written; passing {@code variation} routes the write to that already-existing named variation
 * instead -- an unknown variation is rejected rather than silently auto-created.
 * <p>
 * <strong>Master-only lookup caveat:</strong> the CIF SDK's content-fragment lookup (used by
 * {@code get_commerce_content_fragment} and the associated-content tools) only matches against the fragment's
 * <strong>master</strong> variation. A value written here to a named variation is invisible to those read tools --
 * only a master-value write round-trips through them.
 * <p>
 * Like every write tool in this module, this never auto-publishes: it ends at {@link ResourceResolver#commit()},
 * leaving activation to the normal authoring/publish flow.
 */
@Component(service = McpTool.class)
public class UpdateCommerceContentFragmentFieldTool implements McpTool {

    private static final String RICHTEXT_CONTENT_TYPE = "text/html";
    private static final String MASTER = "master";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "update_commerce_content_fragment_field";
    }

    @Override
    public String description() {
        return "Set a single element's value on a commerce content fragment (master, or an already-existing named "
            + "variation). Richtext elements are written as HTML, scalar elements by type, multi-value elements "
            + "accept a JSON array. Never auto-publishes -- ends at commit(). Caveat: the CIF SDK content-fragment "
            + "lookup used by get_commerce_content_fragment only matches the master variation, so a write to a "
            + "named variation is invisible to that tool.";
    }

    @Override
    public boolean writesContent() {
        return true;
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("fragmentPath").put("type", "string");
        properties.putObject("elementName").put("type", "string");
        ObjectNode value = properties.putObject("value");
        ArrayNode valueOneOf = value.putArray("oneOf");
        valueOneOf.addObject().put("type", "string");
        valueOneOf.addObject().put("type", "array").putObject("items").put("type", "string");
        properties.putObject("variation").put("type", "string");
        schema.putArray("required").add("fragmentPath").add("elementName").add("value");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String fragmentPath = args.path("fragmentPath").asText(null);
        String elementName = args.path("elementName").asText(null);
        String variationName = args.path("variation").asText(null);
        JsonNode valueNode = args.path("value");
        if (StringUtils.isBlank(elementName)) {
            throw new IllegalArgumentException("elementName is required");
        }
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            throw new IllegalArgumentException("value is required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        ContentFragment fragment = resolveFragment(resolver, fragmentPath);

        ContentElement element = fragment.getElement(elementName);
        if (element == null) {
            throw new IllegalArgumentException("unknown element: " + elementName);
        }

        // An explicit variation of "master" is the base value, not a named variation (there is no variation
        // literally named "master"); route it through the base-element path so it never fails as "unknown".
        boolean useVariation = StringUtils.isNotBlank(variationName) && !MASTER.equalsIgnoreCase(variationName);
        ContentVariation variation = null;
        FragmentData targetData;
        if (useVariation) {
            variation = element.getVariation(variationName);
            if (variation == null) {
                throw new IllegalArgumentException("unknown variation: " + variationName);
            }
            targetData = variation.getValue();
        } else {
            targetData = element.getValue();
        }

        // Richtext elements take HTML via setContent; every other element goes through the shared, arity-aware
        // value writer (multi-value -> String[], single-value -> String), which is driven by the ELEMENT's own data
        // type rather than the JSON value shape -- so a JSON array or a scalar can both target a multi-value element.
        boolean richtext = RICHTEXT_CONTENT_TYPE.equals(targetData.getContentType());
        String expectedContent = null;
        String[] expectedValues = null;
        if (richtext) {
            if (valueNode.isArray()) {
                throw new IllegalArgumentException("richtext element expects an HTML string, not an array: "
                    + elementName);
            }
            expectedContent = valueNode.isTextual() ? valueNode.asText() : valueNode.toString();
            try {
                if (useVariation) {
                    variation.setContent(expectedContent, RICHTEXT_CONTENT_TYPE);
                } else {
                    element.setContent(expectedContent, RICHTEXT_CONTENT_TYPE);
                }
            } catch (ContentFragmentException e) {
                throw new IllegalArgumentException("element does not accept a richtext value: " + elementName, e);
            }
        } else {
            expectedValues = toValues(valueNode);
            CommerceWriteSupport.applyElementValue(element, variation, useVariation, expectedValues);
        }

        resolver.commit();

        Resource resource = fragment.adaptTo(Resource.class);
        String resolvedPath = resource != null ? resource.getPath() : fragmentPath;

        boolean updated = verifyWritten(fragment, elementName, useVariation ? variationName : null, richtext,
            expectedContent, expectedValues);

        ObjectNode out = mapper.createObjectNode();
        out.put("fragmentPath", resolvedPath);
        out.put("elementName", elementName);
        out.put("variation", useVariation ? variationName : MASTER);
        out.put("updated", updated);
        return out;
    }

    /**
     * Re-reads the just-written element (fresh from the fragment, after {@code commit()}) and confirms the value
     * actually round-tripped, so {@code updated} reflects a real per-field readback rather than merely "no exception
     * was thrown" (AGENTS.md 4). Richtext is read back via {@link ContentElement#getContent()}; every other value via
     * the shared arity-aware {@link CommerceWriteSupport#elementValueRoundTrips}.
     */
    private boolean verifyWritten(ContentFragment fragment, String elementName, String variationName,
        boolean richtext, String expectedContent, String[] expectedValues) {
        ContentElement element = fragment.getElement(elementName);
        if (element == null) {
            return false;
        }
        boolean useVariation = variationName != null;
        ContentVariation variation = useVariation ? element.getVariation(variationName) : null;
        if (useVariation && variation == null) {
            return false;
        }
        if (richtext) {
            String content = useVariation ? variation.getContent() : element.getContent();
            return expectedContent != null && expectedContent.equals(content);
        }
        return CommerceWriteSupport.elementValueRoundTrips(element, variation, useVariation, expectedValues);
    }

    /**
     * Normalizes the JSON {@code value} into the string value(s) to write: a JSON array yields one entry per item
     * (a JSON {@code null} item becomes a {@code null} entry, not the literal {@code "null"}); a JSON scalar yields a
     * single-element array. The element's arity (single vs multi) is decided later by
     * {@link CommerceWriteSupport#applyElementValue}, not here.
     */
    private String[] toValues(JsonNode valueNode) {
        if (valueNode.isArray()) {
            List<String> values = new ArrayList<String>();
            for (JsonNode item : valueNode) {
                values.add(item.isNull() ? null : item.asText());
            }
            return values.toArray(new String[0]);
        }
        return new String[] { valueNode.asText() };
    }

    /**
     * Resolves {@code fragmentPath} to a {@link ContentFragment}, failing closed via
     * {@link CommerceWriteSupport#resolveContentFragment}. Extracted as a protected seam so unit tests can supply a
     * canned {@link ContentFragment} (Mockito) without needing aem-mock's uncertain content-fragment write support.
     */
    protected ContentFragment resolveFragment(ResourceResolver resolver, String fragmentPath) {
        Resource resource = CommerceWriteSupport.resolveContentFragment(resolver, "fragmentPath", fragmentPath);
        return resource.adaptTo(ContentFragment.class);
    }
}
