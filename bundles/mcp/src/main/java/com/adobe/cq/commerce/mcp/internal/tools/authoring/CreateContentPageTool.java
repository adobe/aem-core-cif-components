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

import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool ({@code create_content_page}) that creates a generic content page from any of the site's editable
 * templates (as listed by {@code list_page_templates}) -- the generic counterpart of the commerce-specific
 * {@code create_specific_pdp}/{@code create_specific_plp}/{@code create_catalog_page} creation tools, for the
 * marketing/landing pages a commerce-content author also needs (no commerce-kind gate on the template).
 * <p>
 * Flow: validate {@code title} + the parent ({@link PageCreationSupport#validatePageParent}) &rarr; resolve the
 * <b>explicitly required</b> {@code template} (must be an existing {@code /conf/...} editable template whose
 * {@code jcr:content} exists, is not {@code status=disabled}, and whose {@code allowedPaths} patterns -- when
 * declared -- match the parent path; strict, fail closed) &rarr; create the page through the {@link #createPage}
 * seam ({@code PageManager.create} copies the template's {@code initial} content) &rarr; commit &rarr; verify the
 * page persisted. The result carries the template's {@code editableContainerPath}
 * ({@link SiteAppsSupport#findEditableContainerPath}) and the new page's absolute {@code containerPath} so a
 * follow-up {@code add_components} call knows exactly where new components must land to be editable in the Sites
 * editor.
 * <p>
 * <b>{@code dryRun}</b> (default {@code false}): still validates everything and resolves the template, computes the
 * would-be page path, and returns it with {@code dryRun:true} -- but creates and commits nothing.
 * <p>
 * Writes run under the caller's {@link ResourceResolver} (never a service/admin resolver) so JCR ACLs are enforced;
 * there is no auto-publish.
 */
@Component(service = McpTool.class)
public class CreateContentPageTool implements McpTool {

    private static final String JCR_CONTENT = "jcr:content";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "create_content_page";
    }

    @Override
    public String description() {
        return "Create a generic content page from one of the site's editable templates (see list_page_templates). "
            + "Requires parent (existing, under /content), title, and template (an existing /conf/... template "
            + "path; its allowedPaths -- when declared -- must match the parent, and it must not be disabled); "
            + "optional name (else derived uniquely from title). Supports dryRun to preview the would-be page path "
            + "without creating anything. The result's containerPath is where add_components should place new "
            + "components so they are editable in the Sites editor.";
    }

    @Override
    public boolean writesContent() {
        return true;
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("parent").put("type", "string");
        properties.putObject("title").put("type", "string");
        properties.putObject("template").put("type", "string");
        properties.putObject("name").put("type", "string");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("parent").add("title").add("template");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String parentPath = args.path("parent").asText(null);
        String title = args.path("title").asText(null);
        String templatePath = args.path("template").asText(null);
        String explicitName = args.path("name").asText(null);
        boolean dryRun = args.path("dryRun").asBoolean(false);

        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("title is required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource parent = PageCreationSupport.validatePageParent(resolver, "parent", parentPath);
        Resource template = resolveTemplate(resolver, templatePath, parent.getPath());

        String name;
        try {
            name = StringUtils.isNotBlank(explicitName)
                ? explicitName
                : ResourceUtil.createUniqueChildName(parent, title);
        } catch (PersistenceException e) {
            throw new IllegalArgumentException("cannot derive a unique page name under " + parent.getPath(), e);
        }

        String editableContainerPath = SiteAppsSupport.findEditableContainerPath(template);

        ObjectNode out = mapper.createObjectNode();
        out.put("template", template.getPath());
        if (editableContainerPath != null) {
            out.put("editableContainerPath", editableContainerPath);
        }

        if (dryRun) {
            // Preview-only: compute the would-be page path, create/commit nothing.
            String pagePath = parent.getPath() + "/" + name;
            out.put("pagePath", pagePath);
            if (editableContainerPath != null) {
                out.put("containerPath", pagePath + "/" + JCR_CONTENT + "/" + editableContainerPath);
            }
            out.put("dryRun", true);
            return out;
        }

        String pagePath = createPage(resolver, parent.getPath(), name, template.getPath(), title);
        resolver.commit();

        // Post-write verification: never report success for a create that did not persist.
        if (resolver.getResource(pagePath + "/" + JCR_CONTENT) == null) {
            throw new IllegalStateException("failed to verify created page: " + pagePath);
        }

        out.put("pagePath", pagePath);
        if (editableContainerPath != null) {
            out.put("containerPath", pagePath + "/" + JCR_CONTENT + "/" + editableContainerPath);
        }
        out.put("dryRun", false);
        return out;
    }

    /**
     * Resolves and validates the requested template: it must be an existing {@code /conf/...} resource with a
     * {@code jcr:content}, must not be {@code status=disabled}, and -- when it declares {@code allowedPaths} --
     * at least one pattern must match the parent page path (strict, fail closed).
     */
    private static Resource resolveTemplate(ResourceResolver resolver, String templatePath, String parentPath) {
        if (StringUtils.isBlank(templatePath)) {
            throw new IllegalArgumentException("template is required");
        }
        if (!templatePath.startsWith("/conf/")) {
            throw new IllegalArgumentException("template must be under /conf/: " + templatePath);
        }
        Resource template = resolver.getResource(templatePath);
        if (template == null) {
            throw new IllegalArgumentException("template not found: " + templatePath);
        }
        Resource content = template.getChild(JCR_CONTENT);
        if (content == null) {
            throw new IllegalArgumentException("not an editable template (no jcr:content): " + templatePath);
        }
        if ("disabled".equals(content.getValueMap().get("status", String.class))) {
            throw new IllegalArgumentException("template is disabled: " + templatePath);
        }
        String[] allowedPaths = content.getValueMap().get("allowedPaths", String[].class);
        if (allowedPaths != null && allowedPaths.length > 0 && !matchesAny(allowedPaths, parentPath)) {
            throw new IllegalArgumentException(
                "template " + templatePath + " does not allow pages under " + parentPath
                    + " (allowedPaths: " + String.join(", ", allowedPaths) + ")");
        }
        return template;
    }

    private static boolean matchesAny(String[] patterns, String path) {
        for (String pattern : patterns) {
            if (StringUtils.isBlank(pattern)) {
                continue;
            }
            try {
                if (path.matches(pattern)) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                // fail closed on an invalid pattern: treat as non-matching
            }
        }
        return false;
    }

    /**
     * Test seam: creates the page via {@link com.day.cq.wcm.api.PageManager#create} without auto-save, so the
     * caller controls the commit. Returns the created page's path.
     */
    protected String createPage(ResourceResolver resolver, String parentPath, String name, String templatePath,
        String title) throws Exception {
        return PageCreationSupport.createPage(resolver, parentPath, name, templatePath, title);
    }
}
