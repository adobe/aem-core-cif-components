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

import java.util.Arrays;
import java.util.List;

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool ({@code scaffold_catalog_section}, T-42) that creates a whole catalog <em>section</em> in one call:
 * a catalog page (the section root, bound to the store's root category) plus a couple of minimal, clearly-named
 * example child pages an author fills in later. It <em>composes</em> the shipped page-creation tools rather than
 * re-coding page creation -- the section root delegates to {@link CreateCatalogPageTool} (which itself delegates the
 * root-category binding to {@code configure_catalog_page}), and the example children delegate to the shared
 * {@link PageCreationSupport#createPage}. See catalog &sect;9.
 * <p>
 * Flow:
 * <ol>
 * <li>Validate {@code parent} (existing, under {@code /content}, via
 * {@link PageCreationSupport#validatePageParent}), {@code name}, {@code rootCategoryId}, and {@code idType}
 * (&isin; {@code {uid, urlPath}}) UP FRONT (before the {@code dryRun} branch), so a dry run previews exactly what a
 * real run would do.</li>
 * <li>Create the section-root <b>catalog page</b> (bound to {@code rootCategoryId}) by delegating to
 * {@code create_catalog_page}; its returned {@code pagePath} is the section root ({@code sectionPath}).</li>
 * <li>Create minimal <b>example child pages</b> UNBOUND under the section root -- {@code example-product} (from a
 * {@code product} template) and {@code example-category} (from a {@code category} template). These are starting-point
 * pages, NOT bound to a specific SKU/category, so they are created via the plain page-create helper, not the
 * binding-requiring {@code create_specific_pdp}/{@code create_specific_plp}. <b>Graceful skip:</b> if no template of a
 * child's kind can be resolved, that child is skipped and recorded in {@code skipped} -- the whole section is NOT
 * failed.</li>
 * <li>Commit the staged children with the caller's {@code resolver} (the catalog root was already committed by its
 * delegate).</li>
 * </ol>
 * <p>
 * <b>Atomicity boundary (best-effort):</b> the catalog section root is committed by its own delegate before the
 * children are staged, so it is NOT part of the children's transaction. If a child create throws, the staged children
 * are {@code revert()}-ed and the error is rethrown, but the already-committed catalog root remains -- an author is
 * left with a valid, bound catalog page (just without the example children), which is a safe partial result rather
 * than an orphan. This is documented here because it is the one place the tool is not fully atomic.
 * <p>
 * <b>{@code dryRun}</b> (default {@code false}, an approved Tier-3 guardrail): delegates to
 * {@code create_catalog_page} with {@code dryRun:true} (the would-be catalog path), computes the would-be child paths
 * under it (resolving templates only to check availability -- absent templates are recorded in {@code skipped}), and
 * returns the full would-be tree, creating and committing <strong>nothing</strong>.
 * <p>
 * Result: {@code {sectionPath, catalogPage, rootCategoryId, children:[{path, pageType}], skipped:[{pageType, reason}],
 * dryRun}}.
 * <p>
 * Writes run under the caller's {@link ResourceResolver} (never a service/admin resolver) so JCR ACLs are enforced;
 * there is no auto-publish.
 */
@Component(service = McpTool.class)
public class ScaffoldCatalogSectionTool implements McpTool {

    private static final String KIND_PRODUCT = "product";
    private static final String KIND_CATEGORY = "category";
    private static final String DEFAULT_ID_TYPE = "uid";
    private static final List<String> VALID_ID_TYPES = Arrays.asList("uid", "urlPath");

    private static final String EXAMPLE_PRODUCT_NAME = "example-product";
    private static final String EXAMPLE_CATEGORY_NAME = "example-category";
    private static final String EXAMPLE_PRODUCT_TITLE = "Example Product";
    private static final String EXAMPLE_CATEGORY_TITLE = "Example Category";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "scaffold_catalog_section";
    }

    @Override
    public String description() {
        return "Scaffold a whole catalog section in one call: a catalog page (the section root, bound to the store "
            + "root category) plus minimal example child pages (example-product, example-category) an author fills in "
            + "later. Requires parent (existing, under /content), name (the section root node name), and "
            + "rootCategoryId; optional title (default = name), idType (uid|urlPath, default uid), template (a catalog "
            + "template for the root, else auto-discovered), and dryRun. Composes create_catalog_page (root) + the "
            + "shared page-create helper (children). If a product/category template is absent, that child is skipped "
            + "(recorded in skipped), not failed. Supports dryRun to preview the whole would-be tree without creating "
            + "anything.";
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
        properties.putObject("template").put("type", "string");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("parent").add("name").add("rootCategoryId");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String parentPath = args.path("parent").asText(null);
        String name = args.path("name").asText(null);
        String rootCategoryId = args.path("rootCategoryId").asText(null);
        String explicitTemplate = args.path("template").asText(null);
        String idType = args.has("idType") && !args.path("idType").isNull()
            ? args.path("idType").asText()
            : DEFAULT_ID_TYPE;
        String title = StringUtils.isNotBlank(args.path("title").asText(null)) ? args.path("title").asText() : name;
        boolean dryRun = args.path("dryRun").asBoolean(false);

        // Validate everything UP FRONT (before the dryRun branch) so a dry run previews exactly what a real run does.
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name is required");
        }
        if (StringUtils.isBlank(rootCategoryId)) {
            throw new IllegalArgumentException("rootCategoryId is required");
        }
        if (!VALID_ID_TYPES.contains(idType)) {
            throw new IllegalArgumentException("idType must be one of " + VALID_ID_TYPES + ": " + idType);
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        String confPath = SiteAppsSupport.confPathFor(ctx.getLandingPage());
        Resource parent = PageCreationSupport.validatePageParent(resolver, "parent", parentPath);

        // Section root = the catalog page. Delegate to create_catalog_page (which binds rootCategoryId itself); in
        // dryRun it too creates nothing and returns the would-be catalog path.
        ObjectNode catalogArgs = mapper.createObjectNode();
        catalogArgs.put("parent", parent.getPath());
        catalogArgs.put("name", name);
        catalogArgs.put("title", title);
        catalogArgs.put("rootCategoryId", rootCategoryId);
        catalogArgs.put("idType", idType);
        if (StringUtils.isNotBlank(explicitTemplate)) {
            catalogArgs.put("template", explicitTemplate);
        }
        catalogArgs.put("dryRun", dryRun);
        JsonNode catalogResult = createCatalogRoot(ctx, catalogArgs);
        String sectionPath = catalogResult.path("pagePath").asText();

        ObjectNode out = mapper.createObjectNode();
        out.put("sectionPath", sectionPath);
        out.put("catalogPage", sectionPath);
        out.put("rootCategoryId", rootCategoryId);
        ArrayNode children = out.putArray("children");
        ArrayNode skipped = out.putArray("skipped");
        out.put("dryRun", dryRun);

        if (dryRun) {
            // Section root does not exist, so its would-be children are simply the base names (no siblings to
            // disambiguate). Resolve templates only to record availability; create/commit nothing.
            previewChild(resolver, confPath, sectionPath, KIND_PRODUCT, EXAMPLE_PRODUCT_NAME, children, skipped);
            previewChild(resolver, confPath, sectionPath, KIND_CATEGORY, EXAMPLE_CATEGORY_NAME, children, skipped);
            return out;
        }

        // The catalog root was committed by its own delegate; the children are staged and committed together below.
        Resource sectionRoot = resolver.getResource(sectionPath);
        if (sectionRoot == null) {
            throw new IllegalStateException("catalog section root not found after create: " + sectionPath);
        }
        try {
            createExampleChild(resolver, confPath, sectionRoot, KIND_PRODUCT, EXAMPLE_PRODUCT_NAME,
                EXAMPLE_PRODUCT_TITLE, children, skipped);
            createExampleChild(resolver, confPath, sectionRoot, KIND_CATEGORY, EXAMPLE_CATEGORY_NAME,
                EXAMPLE_CATEGORY_TITLE, children, skipped);
            resolver.commit();
        } catch (RuntimeException e) {
            // Best-effort atomicity for the children: discard the staged children. The already-committed catalog root
            // remains (documented boundary) -- the author keeps a valid, bound catalog page.
            resolver.revert();
            throw e;
        }

        return out;
    }

    /**
     * Creates one example child page under the section root, or records a graceful skip if no template of {@code kind}
     * can be resolved. Never binds the child (these are starting-point pages).
     */
    private void createExampleChild(ResourceResolver resolver, String confPath, Resource sectionRoot, String kind,
        String baseName, String title, ArrayNode children, ArrayNode skipped) throws PersistenceException {
        Resource template;
        try {
            template = PageTemplateSupport.resolveTemplate(resolver, kind, null, confPath);
        } catch (IllegalArgumentException e) {
            recordSkip(skipped, kind, e.getMessage());
            return;
        }
        String childName = ResourceUtil.createUniqueChildName(sectionRoot, baseName);
        String childPath = createChildPage(resolver, sectionRoot.getPath(), childName, template.getPath(), title);
        recordChild(children, childPath, kind);
    }

    /**
     * Computes one would-be child path for the dryRun preview (records a skip when the template is absent). The
     * section root does not exist yet in a dry run, so the child name is simply the base name.
     */
    private void previewChild(ResourceResolver resolver, String confPath, String sectionPath, String kind,
        String baseName, ArrayNode children, ArrayNode skipped) {
        try {
            // Auto-discover a template of this kind; an explicit template is a catalog template (for the root) and
            // must not be forced onto a product/category child.
            PageTemplateSupport.resolveTemplate(resolver, kind, null, confPath);
        } catch (IllegalArgumentException e) {
            recordSkip(skipped, kind, e.getMessage());
            return;
        }
        recordChild(children, sectionPath + "/" + baseName, kind);
    }

    private void recordChild(ArrayNode children, String path, String pageType) {
        ObjectNode child = children.addObject();
        child.put("path", path);
        child.put("pageType", pageType);
    }

    private void recordSkip(ArrayNode skipped, String pageType, String reason) {
        ObjectNode skip = skipped.addObject();
        skip.put("pageType", pageType);
        skip.put("reason", reason);
    }

    /**
     * Section-root seam: creates the catalog section root by delegating to the shipped {@code create_catalog_page}
     * tool (which validates/resolves the catalog template, creates the page, and delegates the root-category binding
     * to {@code configure_catalog_page}). Extracted as a {@code protected} seam so unit tests can materialise a canned
     * core-typed catalog page without depending on the delegate's in-mock proxy resolution (see
     * {@link PageTemplateSupport}'s aem-mock caveat).
     *
     * @param ctx the caller's call context (the delegate runs under the same resolver / ACLs)
     * @param args the delegate's arguments ({@code parent}/{@code name}/{@code title}/{@code rootCategoryId}/
     *            {@code idType}/optional {@code template}/{@code dryRun})
     * @return the delegate's result node (carrying {@code pagePath})
     */
    protected JsonNode createCatalogRoot(McpCallContext ctx, ObjectNode args) throws Exception {
        return new CreateCatalogPageTool().call(ctx, args);
    }

    /**
     * Child-create seam over {@link PageCreationSupport#createPage} -- overridden in unit tests to materialise a canned
     * child page. The shared body stages the page ({@code autoSave=false}); the caller commits the staged children
     * together.
     *
     * @param resolver the caller's {@link ResourceResolver}
     * @param parentPath the section root path the child is created under
     * @param name the (already unique) child name of the new page
     * @param templatePath the resolved child template path
     * @param title the new page's {@code jcr:title}
     * @return the path of the newly created (staged, uncommitted) child page
     */
    protected String createChildPage(ResourceResolver resolver, String parentPath, String name, String templatePath,
        String title) throws PersistenceException {
        return PageCreationSupport.createPage(resolver, parentPath, name, templatePath, title);
    }
}
