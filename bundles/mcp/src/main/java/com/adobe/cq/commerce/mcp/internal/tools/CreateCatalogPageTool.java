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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.PageCreationSupport;
import com.adobe.cq.commerce.mcp.internal.PageTemplateSupport;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool ({@code create_catalog_page}, T-38) that creates a CIF catalog (PLP) page from a
 * {@code kind=catalog} editable template, then binds its root category by <em>delegating</em> to the already-shipped
 * {@link ConfigureCatalogPageTool} -- so create and configure never diverge. See catalog &sect;9.
 * <p>
 * Flow: validate the parent (must be an existing resource under {@code /content}, via
 * {@link PageCreationSupport#validatePageParent}) and resolve/validate a {@code catalog} template (via
 * {@link PageTemplateSupport#resolveTemplate}, which fails closed if the template's {@code initial} content does not
 * carry the expected empty-grid catalog signal) &rarr; create the page through the {@link #createPage} seam (which
 * calls {@code PageManager.create}; AEM copies the template's {@code initial} content automatically) &rarr; delegate
 * the {@code magentoRootCategoryId}/{@code …IdType}/{@code showMainCategories} binding to
 * {@code ConfigureCatalogPageTool} on the new page's path &rarr; return {@code {pagePath, template, rootCategoryId,
 * idType, dryRun}}.
 * <p>
 * <b>{@code dryRun}</b> (default {@code false}, an approved Tier-3 guardrail): still validates the parent and
 * resolves the template, computes the would-be page path ({@code parent + "/" + name}), and returns it with
 * {@code dryRun:true} -- but creates and commits <strong>nothing</strong> (neither the create seam nor the
 * delegated binding runs).
 * <p>
 * Writes run under the caller's {@link ResourceResolver} (never a service/admin resolver) so JCR ACLs are enforced;
 * there is no auto-publish (the tool ends at the delegated binding's {@code commit()}).
 */
@Component(service = McpTool.class)
public class CreateCatalogPageTool implements McpTool {

    private static final String KIND_CATALOG = "catalog";
    private static final String DEFAULT_ID_TYPE = "uid";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "create_catalog_page";
    }

    @Override
    public String description() {
        return "Create a CIF catalog (PLP) page from a catalog editable template and bind its root category. "
            + "Requires parent (existing, under /content), title, and rootCategoryId; optional name (else derived "
            + "uniquely from title), idType (uid|urlPath, default uid), showMainCategories (default false), and "
            + "template (else a catalog template is auto-discovered under /conf). Supports dryRun to preview the "
            + "would-be page path + template without creating anything. The root-category binding delegates to "
            + "configure_catalog_page so create and configure never diverge.";
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
        properties.putObject("name").put("type", "string");
        properties.putObject("title").put("type", "string");
        properties.putObject("rootCategoryId").put("type", "string");
        ObjectNode idType = properties.putObject("idType").put("type", "string");
        idType.putArray("enum").add("uid").add("urlPath");
        properties.putObject("showMainCategories").put("type", "boolean");
        properties.putObject("template").put("type", "string");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("parent").add("title").add("rootCategoryId");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String parentPath = args.path("parent").asText(null);
        String title = args.path("title").asText(null);
        String rootCategoryId = args.path("rootCategoryId").asText(null);
        String explicitName = args.path("name").asText(null);
        String explicitTemplate = args.path("template").asText(null);
        String idType = args.has("idType") && !args.path("idType").isNull()
            ? args.path("idType").asText()
            : DEFAULT_ID_TYPE;
        boolean showMainCategories = args.path("showMainCategories").asBoolean(false);
        boolean dryRun = args.path("dryRun").asBoolean(false);

        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("title is required");
        }
        if (StringUtils.isBlank(rootCategoryId)) {
            throw new IllegalArgumentException("rootCategoryId is required");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource parent = PageCreationSupport.validatePageParent(resolver, "parent", parentPath);
        Resource template = PageTemplateSupport.resolveTemplate(resolver, KIND_CATALOG, explicitTemplate);
        String templatePath = template.getPath();

        String name = StringUtils.isNotBlank(explicitName)
            ? explicitName
            : ResourceUtil.createUniqueChildName(parent, title);

        ObjectNode out = mapper.createObjectNode();
        out.put("template", templatePath);
        out.put("rootCategoryId", rootCategoryId);
        out.put("idType", idType);

        if (dryRun) {
            // Preview-only: compute the would-be page path, create/commit nothing (neither the seam nor the
            // delegated binding runs).
            out.put("pagePath", parent.getPath() + "/" + name);
            out.put("dryRun", true);
            return out;
        }

        String pagePath = createPage(resolver, parent.getPath(), name, templatePath, title);

        // Delegate the root-category binding to the shipped configure_catalog_page tool (its own jcr:content resolve
        // + resourceType gate + write + commit + readback), so create and configure never diverge.
        ObjectNode bindArgs = mapper.createObjectNode();
        bindArgs.put("path", pagePath);
        bindArgs.put("categoryUid", rootCategoryId);
        bindArgs.put("idType", idType);
        bindArgs.put("showMainCategories", showMainCategories);
        new ConfigureCatalogPageTool().call(ctx, bindArgs);

        out.put("pagePath", pagePath);
        out.put("dryRun", false);
        return out;
    }

    /**
     * Create seam over {@code PageManager.create} -- overridden in unit tests to observe the create args and return
     * a canned core-typed page (the pinned aem-mock can't resolve Venia's proxy super-typing, so the delegated
     * {@code configure_catalog_page}'s {@code isResourceType} gate needs a directly-core-typed {@code jcr:content}
     * in-mock; the real Venia proxy path is proven live -- see {@link PageTemplateSupport}'s aem-mock caveat).
     *
     * @param resolver the caller's {@link ResourceResolver}
     * @param parentPath the validated parent path the page is created under
     * @param name the (already unique) child name of the new page
     * @param templatePath the resolved {@code catalog} template path
     * @param title the new page's {@code jcr:title}
     * @return the path of the newly created page
     * @throws IllegalArgumentException if the underlying {@code PageManager.create} fails (checked
     *             {@code WCMException} translated to fail closed)
     */
    protected String createPage(ResourceResolver resolver, String parentPath, String name, String templatePath,
        String title) throws PersistenceException {
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            throw new IllegalArgumentException("cannot create page: no PageManager for the caller's resolver");
        }
        try {
            Page page = pageManager.create(parentPath, name, templatePath, title);
            return page.getPath();
        } catch (WCMException e) {
            throw new IllegalArgumentException("failed to create page under " + parentPath + ": " + e.getMessage(), e);
        }
    }
}
