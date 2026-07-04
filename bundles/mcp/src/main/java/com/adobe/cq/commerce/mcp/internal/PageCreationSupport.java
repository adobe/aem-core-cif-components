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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;

/**
 * Shared validation + page-create scaffold for MCP Tier-3 <em>page-creation</em> write tools
 * ({@code writesContent() == true}) -- the strict parent-path validation those tools apply before
 * {@code PageManager.create} (an approved Tier-3 guardrail given the larger blast radius of creating pages), plus the
 * one {@link #createPage} body they all share.
 * <p>
 * Each creation tool still keeps its own {@code protected createPage(...)} seam (which delegates here) so the create
 * can be observed/stubbed in unit tests without a Venia-proxy-resolving instance -- see the aem-mock caveat on
 * {@code PageTemplateSupport}. The shared body is factored here so it lives in one place rather than being duplicated
 * across every creation tool.
 * <p>
 * This is a plain stateless helper, not an {@code McpTool} -- it is not an OSGi component and not discovered by
 * {@code ToolRegistry}.
 */
public final class PageCreationSupport {

    private PageCreationSupport() {}

    /**
     * Creates a page via {@code PageManager.create} with {@code autoSave=false} (staged, <strong>not</strong>
     * committed): the caller commits it together with any follow-on write (e.g. a delegated binding) as one unit, and
     * can {@code revert()} it if that follow-on fails. This is the single shared body every Tier-3 page-creation tool's
     * {@code createPage} seam delegates to.
     * <p>
     * Fails closed: an {@link IllegalArgumentException} is thrown if the caller's resolver has no {@code PageManager}
     * or the underlying create fails (the checked {@code com.day.cq.wcm.api.WCMException} is translated).
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param parentPath the (already validated) parent path the page is created under
     * @param name the (already unique) child name of the new page
     * @param templatePath the resolved template path (AEM copies its {@code initial} content into the new page)
     * @param title the new page's {@code jcr:title}
     * @return the path of the newly created (staged, uncommitted) page
     * @throws PersistenceException never actually thrown by this body (declared to match the seam contract of tools
     *             that derive names via {@code ResourceUtil} and share the checked signature)
     * @throws IllegalArgumentException if the resolver has no {@code PageManager}, or the underlying
     *             {@code PageManager.create} fails (checked {@code WCMException} translated to fail closed)
     */
    public static String createPage(ResourceResolver resolver, String parentPath, String name, String templatePath,
        String title) throws PersistenceException {
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            throw new IllegalArgumentException("cannot create page: no PageManager for the caller's resolver");
        }
        try {
            Page page = pageManager.create(parentPath, name, templatePath, title, false);
            return page.getPath();
        } catch (WCMException e) {
            throw new IllegalArgumentException("failed to create page under " + parentPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Validates and resolves the parent a new page will be created under: fail closed
     * ({@link IllegalArgumentException}) if {@code parentPath} is blank, is not under {@code /content/}, or does not
     * resolve to an existing resource under the caller's {@code resolver}.
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param argName the tool argument name {@code parentPath} was read from, used in error messages
     * @param parentPath the parent path to validate; must be non-blank and under {@code /content/}
     * @return the resolved existing parent {@link Resource} (a page or a folder)
     * @throws IllegalArgumentException if {@code parentPath} is blank, not under {@code /content/}, or does not
     *             resolve to an existing resource
     */
    public static Resource validatePageParent(ResourceResolver resolver, String argName, String parentPath) {
        if (StringUtils.isBlank(parentPath) || !parentPath.startsWith("/content/")) {
            throw new IllegalArgumentException(argName + " (under /content) is required");
        }
        Resource parent = resolver.getResource(parentPath);
        if (parent == null) {
            throw new IllegalArgumentException("parent not found: " + parentPath);
        }
        return parent;
    }
}
