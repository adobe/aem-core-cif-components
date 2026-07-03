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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.wcm.api.Page;

/**
 * Shared fail-closed validation for the page-routing diagnostics tools' {@code path}/{@code siteRoot} arguments:
 * resolve a raw path string to a {@code cq:Page} under {@code /content}, or throw {@link IllegalArgumentException}.
 * <p>
 * This extracts only the MECHANICAL part that {@code list_catalog_pages}, {@code explain_catalog_page_routing},
 * {@code detect_catalog_page_conflicts}, {@code explain_page_resolution}, {@code list_specific_pages},
 * {@code detect_specific_page_conflicts}, {@code validate_selector_filter_format}, and
 * {@code check_specific_page_capability} all duplicated identically: "under {@code /content}", "resource exists",
 * "adapts to {@link Page}". It intentionally does <b>not</b> decide whether the argument is required, nor what to
 * default to when absent -- callers differ on that (the {@code path}-arg tools require the argument; the
 * {@code siteRoot}-arg tools treat a blank argument as "use the endpoint nav root" and resolve that default
 * <em>before</em> calling this method).
 */
final class PathArgs {

    private PathArgs() {
        // static utility
    }

    /**
     * Validates {@code path} and resolves it to a {@link Page}: blank, not under {@code /content}, not found, and
     * not adaptable to {@link Page} each throw {@link IllegalArgumentException} with a message of the form
     * {@code "<argName> <problem>: <path>"} (or {@code "<argName> is required"} when blank).
     *
     * @param resolver the resolver to look up {@code path} with
     * @param argName the argument name to use in error messages (e.g. {@code "path"} or {@code "siteRoot"})
     * @param path the raw path string to resolve
     * @return the resolved page
     */
    static Page resolvePage(ResourceResolver resolver, String argName, String path) {
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException(argName + " is required");
        }
        if (!path.startsWith("/content/") && !"/content".equals(path)) {
            throw new IllegalArgumentException(argName + " must be under /content: " + path);
        }

        Resource resource = resolver.getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException(argName + " not found: " + path);
        }

        Page page = resource.adaptTo(Page.class);
        if (page == null) {
            throw new IllegalArgumentException(argName + " does not resolve to a page: " + path);
        }
        return page;
    }
}
