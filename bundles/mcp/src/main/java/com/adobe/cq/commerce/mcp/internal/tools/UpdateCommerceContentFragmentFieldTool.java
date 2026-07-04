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
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.CommerceWriteSupport;
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

        boolean useVariation = StringUtils.isNotBlank(variationName);
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

        boolean richtext = RICHTEXT_CONTENT_TYPE.equals(targetData.getContentType());
        if (richtext) {
            String html = valueNode.isTextual() ? valueNode.asText() : valueNode.toString();
            if (useVariation) {
                variation.setContent(html, RICHTEXT_CONTENT_TYPE);
            } else {
                element.setContent(html, RICHTEXT_CONTENT_TYPE);
            }
        } else if (valueNode.isArray()) {
            String[] values = toStringArray(valueNode);
            if (!targetData.isTypeSupported(String[].class)) {
                throw new IllegalArgumentException("element does not support a multi-value (String[]) value: "
                    + elementName);
            }
            targetData.setValue(values);
            writeScalar(element, variation, useVariation, targetData);
        } else {
            String scalarValue = valueNode.asText();
            if (!targetData.isTypeSupported(String.class)) {
                throw new IllegalArgumentException("element does not support a string value: " + elementName);
            }
            targetData.setValue(scalarValue);
            writeScalar(element, variation, useVariation, targetData);
        }

        resolver.commit();

        Resource resource = fragment.adaptTo(Resource.class);
        String resolvedPath = resource != null ? resource.getPath() : fragmentPath;

        ObjectNode out = mapper.createObjectNode();
        out.put("fragmentPath", resolvedPath);
        out.put("elementName", elementName);
        out.put("variation", useVariation ? variationName : MASTER);
        out.put("updated", true);
        return out;
    }

    private void writeScalar(ContentElement element, ContentVariation variation, boolean useVariation,
        FragmentData data) throws ContentFragmentException {
        if (useVariation) {
            variation.setValue(data);
        } else {
            element.setValue(data);
        }
    }

    private String[] toStringArray(JsonNode arrayNode) {
        List<String> values = new ArrayList<String>();
        for (JsonNode item : arrayNode) {
            values.add(item.isNull() ? null : item.asText());
        }
        return values.toArray(new String[0]);
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
