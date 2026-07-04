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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP read tool listing editable-template candidates under {@code /conf/*&#47;settings/wcm/templates/*} whose
 * pre-placed {@code initial} content matches a requested page-type ({@code product}/{@code category}/
 * {@code catalog}) -- see catalog &sect;9. This is a discovery aid for the Tier-3 page-creation tools (e.g.
 * {@code create_catalog_page}): given a template path, those tools seed a new page's {@code initial} content via
 * {@code PageManager.create(parentPath, name, templatePath, title)}.
 * <p>
 * The classification signal is shared with those creation tools: it delegates to
 * {@link PageTemplateSupport#classify(Resource)} (the pre-placed component under
 * {@code initial/jcr:content/root/container/container}, classified via {@link Resource#isResourceType(String)}
 * against the known CIF commerce resource types; empty grid &rArr; {@code catalog}; {@code jcr:title} fallback only
 * when the grid can't be read) so that a {@code suggest_template_for_page_type} discovery result and what
 * {@code create_*} would actually resolve never diverge. See {@link PageTemplateSupport} for the full signal
 * priority and the aem-mock {@code isResourceType} identity-only caveat.
 */
@Component(service = McpTool.class)
public class SuggestTemplateForPageTypeTool implements McpTool {

    private static final String TEMPLATES_QUERY_ROOT = "/conf";
    private static final String TEMPLATES_SUFFIX = "settings/wcm/templates";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "suggest_template_for_page_type";
    }

    @Override
    public String description() {
        return "List editable-template candidates under /conf/*/settings/wcm/templates/ whose pre-placed initial "
            + "content matches a page type: kind=product (product-detail template), kind=category (product-list/PLP "
            + "template), or kind=catalog (empty structural grid, filled in later). Signal is the pre-placed "
            + "commerce component's resourceType/resourceSuperType where available, falling back to the template's "
            + "jcr:title only when no component signal is present.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode kind = properties.putObject("kind").put("type", "string");
        kind.putArray("enum").add("product").add("category").add("catalog");
        schema.putArray("required").add("kind");
        return schema;
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String kind = args.path("kind").asText(null);
        if (!PageTemplateSupport.VALID_KINDS.contains(kind)) {
            throw new IllegalArgumentException("kind must be one of " + PageTemplateSupport.VALID_KINDS + ": " + kind);
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        ObjectNode out = mapper.createObjectNode();
        out.put("kind", kind);
        ArrayNode templatesNode = out.putArray("templates");

        Resource confRoot = resolver.getResource(TEMPLATES_QUERY_ROOT);
        if (confRoot == null) {
            return out;
        }

        for (Resource configRoot : confRoot.getChildren()) {
            Resource templatesFolder = configRoot.getChild(TEMPLATES_SUFFIX);
            if (templatesFolder == null) {
                continue;
            }
            for (Resource template : templatesFolder.getChildren()) {
                PageTemplateSupport.Classification classification = PageTemplateSupport.classify(template);
                if (classification != null && classification.getKind().equals(kind)) {
                    ObjectNode entry = templatesNode.addObject();
                    entry.put("path", template.getPath());
                    entry.put("title", classification.getTitle());
                    entry.put("signal", classification.getSignal());
                }
            }
        }

        return out;
    }
}
