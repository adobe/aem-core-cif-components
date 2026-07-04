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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Shared validation scaffold for MCP Tier-3 <em>page-creation</em> write tools ({@code writesContent() == true}) --
 * the strict parent-path validation those tools apply before {@code PageManager.create} (an approved Tier-3
 * guardrail given the larger blast radius of creating pages).
 * <p>
 * The actual page create is intentionally <strong>not</strong> here: each creation tool puts
 * {@code PageManager.create} behind its own {@code protected createPage(...)} seam (real impl:
 * {@code resolver.adaptTo(PageManager.class).create(parentPath, name, templatePath, title)}, catching the checked
 * {@code com.day.cq.wcm.api.WCMException} and re-throwing {@link IllegalArgumentException}) so the create can be
 * observed/stubbed in unit tests without a Venia-proxy-resolving instance -- see the aem-mock caveat on
 * {@code PageTemplateSupport}. This class only factors out the parent validation shared across those tools.
 * <p>
 * This is a plain stateless helper, not an {@code McpTool} -- it is not an OSGi component and not discovered by
 * {@code ToolRegistry}.
 */
public final class PageCreationSupport {

    private PageCreationSupport() {}

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
