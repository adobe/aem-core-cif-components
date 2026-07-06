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

import org.apache.commons.lang3.StringUtils;
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
 * MCP read tool ({@code list_page_templates}, authoring-only) listing the site's editable page templates under
 * {@code /conf/<site>/settings/wcm/templates/*} with everything an agent needs to pick a real template for
 * {@code create_content_page} instead of guessing: {@code path}, {@code title}, {@code description},
 * {@code status}, the template's {@code allowedPaths} patterns, its {@code commerceKind}
 * ({@code product}/{@code category}/{@code catalog} via {@link PageTemplateSupport#classify(Resource)}, absent for
 * generic marketing templates), and -- the key addition over {@code suggest_template_for_page_type} -- the
 * template's <b>{@code editableContainerPath}</b>: the {@code jcr:content}-relative path of the deepest
 * {@code editable=true} container in the template's {@code structure} (via
 * {@link SiteAppsSupport#findEditableContainerPath(Resource)}), i.e. where components added to a page created from
 * this template must land to be visible/editable in the Sites editor.
 * <p>
 * The {@code confPath} argument is optional: when absent, the site's {@code /conf/<site>} root is derived from the
 * endpoint's own nav-root page ({@link SiteAppsSupport#confPathFor}); if that carries no derivable template, every
 * {@code /conf/*&#47;settings/wcm/templates} is scanned (mirroring {@code suggest_template_for_page_type}).
 */
@Component(service = McpTool.class)
public class ListPageTemplatesTool implements McpTool {

    private static final String CONF_ROOT = "/conf";
    private static final String TEMPLATES_SUFFIX = "settings/wcm/templates";
    private static final String JCR_CONTENT = "jcr:content";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "list_page_templates";
    }

    @Override
    public String description() {
        return "List the site's editable page templates (path, title, description, status, allowedPaths, "
            + "commerceKind, and the template's editableContainerPath -- the jcr:content-relative container new "
            + "components must be added under to be editable in the Sites editor). Optional confPath (e.g. "
            + "/conf/venia); when absent it is derived from the endpoint's own nav-root page, falling back to "
            + "scanning all /conf/*/settings/wcm/templates. Use this before create_content_page / add_components "
            + "so pages are created from a real site template and components land in its editable container.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("confPath").put("type", "string");
        return schema;
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        String confPath = args.path("confPath").asText(null);

        ObjectNode out = mapper.createObjectNode();
        if (StringUtils.isNotBlank(confPath)) {
            if (!confPath.startsWith(CONF_ROOT + "/")) {
                throw new IllegalArgumentException("confPath must be under /conf: " + confPath);
            }
            Resource confRoot = resolver.getResource(confPath);
            if (confRoot == null) {
                throw new IllegalArgumentException("confPath not found: " + confPath);
            }
            out.put("confPath", confPath);
            ArrayNode templates = out.putArray("templates");
            appendTemplates(confRoot.getChild(TEMPLATES_SUFFIX), templates);
            return out;
        }

        String derived = SiteAppsSupport.confPathFor(ctx.getLandingPage());
        Resource derivedRoot = derived == null ? null : resolver.getResource(derived);
        if (derivedRoot != null) {
            out.put("confPath", derived);
            ArrayNode templates = out.putArray("templates");
            appendTemplates(derivedRoot.getChild(TEMPLATES_SUFFIX), templates);
            return out;
        }

        // No site signal on the landing page: scan every /conf/* config root instead.
        out.putNull("confPath");
        ArrayNode templates = out.putArray("templates");
        Resource confRoot = resolver.getResource(CONF_ROOT);
        if (confRoot != null) {
            for (Resource configRoot : confRoot.getChildren()) {
                appendTemplates(configRoot.getChild(TEMPLATES_SUFFIX), templates);
            }
        }
        return out;
    }

    private void appendTemplates(Resource templatesFolder, ArrayNode out) {
        if (templatesFolder == null) {
            return;
        }
        for (Resource template : templatesFolder.getChildren()) {
            Resource content = template.getChild(JCR_CONTENT);
            if (content == null) {
                continue; // not a template (e.g. rep:policy)
            }
            ObjectNode entry = out.addObject();
            entry.put("path", template.getPath());
            entry.put("title", content.getValueMap().get("jcr:title", String.class));
            String description = content.getValueMap().get("jcr:description", String.class);
            if (description != null) {
                entry.put("description", description);
            }
            String status = content.getValueMap().get("status", String.class);
            if (status != null) {
                entry.put("status", status);
            }
            String[] allowedPaths = content.getValueMap().get("allowedPaths", String[].class);
            if (allowedPaths != null && allowedPaths.length > 0) {
                ArrayNode allowed = entry.putArray("allowedPaths");
                for (String path : allowedPaths) {
                    allowed.add(path);
                }
            }
            PageTemplateSupport.Classification classification = PageTemplateSupport.classify(template);
            if (classification != null) {
                entry.put("commerceKind", classification.getKind());
            }
            String editableContainerPath = SiteAppsSupport.findEditableContainerPath(template);
            if (editableContainerPath != null) {
                entry.put("editableContainerPath", editableContainerPath);
            }
        }
    }
}
