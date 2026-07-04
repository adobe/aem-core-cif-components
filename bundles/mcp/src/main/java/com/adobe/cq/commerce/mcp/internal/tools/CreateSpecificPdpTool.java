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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP write tool ({@code create_specific_pdp}, T-39) that creates a CIF specific product (PDP) page from a
 * {@code kind=product} editable template, then binds it to a specific set of products by <em>delegating</em> to the
 * already-shipped {@link BindPageToProductsTool} -- so create and bind never diverge. See catalog &sect;9.
 * <p>
 * Flow: validate the parent (must be an existing resource under {@code /content}, via
 * {@link PageCreationSupport#validatePageParent}) and the (non-empty) {@code skusOrUrlKeys} up front, then
 * resolve/validate a {@code product} template (via {@link PageTemplateSupport#resolveTemplate}, which fails closed if
 * the template's {@code initial} content does not carry the expected product-component signal) &rarr; create the page
 * through the {@link #createPage} seam (which calls {@code PageManager.create}; AEM copies the template's
 * {@code initial} content automatically) &rarr; delegate the {@code selectorFilter} (plain SKUs/URL keys) binding to
 * {@code bind_page_to_products} on the new page's path &rarr; return {@code {pagePath, template, boundSkus, dryRun}}.
 * <p>
 * <b>{@code dryRun}</b> (default {@code false}, an approved Tier-3 guardrail): still validates the parent +
 * {@code skusOrUrlKeys} and resolves the template, computes the would-be page path ({@code parent + "/" + name}), and
 * returns it with {@code dryRun:true} -- but creates and commits <strong>nothing</strong> (neither the create seam
 * nor the delegated binding runs).
 * <p>
 * Writes run under the caller's {@link ResourceResolver} (never a service/admin resolver) so JCR ACLs are enforced;
 * there is no auto-publish (the tool ends at the delegated binding's {@code commit()}).
 */
@Component(service = McpTool.class)
public class CreateSpecificPdpTool implements McpTool {

    private static final String KIND_PRODUCT = "product";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "create_specific_pdp";
    }

    @Override
    public String description() {
        return "Create a CIF specific product (PDP) page from a product editable template and bind it to a specific "
            + "set of products. Requires parent (existing, under /content), title, and a non-empty skusOrUrlKeys "
            + "array (plain SKUs/URL keys/slugs, not pipe-encoded); optional name (else derived uniquely from title) "
            + "and template (else a product template is auto-discovered under /conf). Supports dryRun to preview the "
            + "would-be page path + template without creating anything. The product binding delegates to "
            + "bind_page_to_products so create and bind never diverge.";
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
        ObjectNode skusOrUrlKeys = properties.putObject("skusOrUrlKeys");
        skusOrUrlKeys.put("type", "array");
        skusOrUrlKeys.putObject("items").put("type", "string");
        properties.putObject("template").put("type", "string");
        properties.putObject("dryRun").put("type", "boolean");
        schema.putArray("required").add("parent").add("title").add("skusOrUrlKeys");
        return schema;
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) context;
        String parentPath = args.path("parent").asText(null);
        String title = args.path("title").asText(null);
        String explicitName = args.path("name").asText(null);
        String explicitTemplate = args.path("template").asText(null);
        boolean dryRun = args.path("dryRun").asBoolean(false);

        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("title is required");
        }
        // Validate skusOrUrlKeys up front (before the dryRun branch) so a dry run previews exactly what a real run
        // would do -- a real run with no products would be rejected by the delegate, so the dry run must be too.
        if (!args.has("skusOrUrlKeys") || !args.path("skusOrUrlKeys").isArray()) {
            throw new IllegalArgumentException("skusOrUrlKeys (non-empty array) is required");
        }
        List<String> skus = new ArrayList<>();
        for (JsonNode entry : (ArrayNode) args.path("skusOrUrlKeys")) {
            if (!entry.isNull() && !entry.asText().trim().isEmpty()) {
                skus.add(entry.asText());
            }
        }
        if (skus.isEmpty()) {
            throw new IllegalArgumentException("skusOrUrlKeys must contain at least one non-blank product SKU/URL key");
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource parent = PageCreationSupport.validatePageParent(resolver, "parent", parentPath);
        Resource template = PageTemplateSupport.resolveTemplate(resolver, KIND_PRODUCT, explicitTemplate);
        String templatePath = template.getPath();

        String name;
        try {
            name = StringUtils.isNotBlank(explicitName)
                ? explicitName
                : ResourceUtil.createUniqueChildName(parent, title);
        } catch (PersistenceException e) {
            throw new IllegalArgumentException("cannot derive a unique page name under " + parent.getPath(), e);
        }

        ObjectNode out = mapper.createObjectNode();
        out.put("template", templatePath);
        ArrayNode boundSkus = out.putArray("boundSkus");
        for (String sku : skus) {
            boundSkus.add(sku);
        }

        if (dryRun) {
            // Preview-only: compute the would-be page path, create/commit nothing (neither the seam nor the
            // delegated binding runs).
            out.put("pagePath", parent.getPath() + "/" + name);
            out.put("dryRun", true);
            return out;
        }

        // Stage the page (createPage does NOT commit); the delegated binding's commit() then flushes the page + the
        // binding together as one unit. If the binding fails before its commit (e.g. the created page is not a
        // structure page, or a write error), revert() discards the staged page so we never leave an orphaned,
        // unbound page.
        String pagePath = createPage(resolver, parent.getPath(), name, templatePath, title);

        // Delegate the product binding to the shipped bind_page_to_products tool (its own jcr:content resolve +
        // resourceType gate + write + commit + readback), so create and bind never diverge.
        ObjectNode bindArgs = mapper.createObjectNode();
        bindArgs.put("path", pagePath);
        ArrayNode bindSkus = bindArgs.putArray("skusOrUrlKeys");
        for (String sku : skus) {
            bindSkus.add(sku);
        }
        JsonNode bindResult;
        try {
            bindResult = bindProducts(ctx, bindArgs);
        } catch (Exception e) {
            resolver.revert();
            throw e;
        }
        // The delegate does a real readback and reports updated=false (without throwing) when the write did not
        // persist -- surface that as a failure rather than reporting a successful create over a broken binding.
        if (!bindResult.path("updated").asBoolean(false)) {
            throw new IllegalStateException("product page created but product binding did not persist: " + pagePath);
        }

        out.put("pagePath", pagePath);
        out.put("dryRun", false);
        return out;
    }

    /**
     * Binding seam: delegates the product binding to the shipped {@code bind_page_to_products} tool. Extracted as a
     * {@code protected} seam so unit tests can assert the create tool surfaces a binding failure (an
     * {@code updated:false} readback -&gt; {@link IllegalStateException}) and reverts the staged page on a thrown
     * delegate error, without depending on the delegate's own in-mock behavior.
     *
     * @param ctx the caller's call context (the delegate runs under the same resolver / ACLs)
     * @param bindArgs the delegate's arguments ({@code path}/{@code skusOrUrlKeys})
     * @return the delegate's result node (carrying its real-readback {@code updated} flag)
     */
    protected JsonNode bindProducts(McpCallContext ctx, ObjectNode bindArgs) throws Exception {
        return new BindPageToProductsTool().call(ctx, bindArgs);
    }

    /**
     * Create seam over {@code PageManager.create} -- overridden in unit tests to observe the create args and return
     * a canned core-typed structure page (the pinned aem-mock can't resolve Venia's proxy super-typing, so the
     * delegated {@code bind_page_to_products}'s {@code isResourceType} gate needs a directly-core-typed
     * {@code jcr:content} in-mock; the real Venia proxy path is proven live -- see {@link PageTemplateSupport}'s
     * aem-mock caveat).
     * <p>
     * Uses the {@code autoSave=false} overload so the page is <strong>staged, not committed</strong>: the caller
     * commits it together with the delegated binding (one atomic unit), and can {@code revert()} it if the binding
     * fails -- so a rejected binding never leaves an orphaned, unbound page.
     *
     * @param resolver the caller's {@link ResourceResolver}
     * @param parentPath the validated parent path the page is created under
     * @param name the (already unique) child name of the new page
     * @param templatePath the resolved {@code product} template path
     * @param title the new page's {@code jcr:title}
     * @return the path of the newly created (staged, uncommitted) page
     * @throws IllegalArgumentException if the underlying {@code PageManager.create} fails (checked
     *             {@code WCMException} translated to fail closed)
     */
    protected String createPage(ResourceResolver resolver, String parentPath, String name, String templatePath,
        String title) throws PersistenceException {
        return PageCreationSupport.createPage(resolver, parentPath, name, templatePath, title);
    }
}
