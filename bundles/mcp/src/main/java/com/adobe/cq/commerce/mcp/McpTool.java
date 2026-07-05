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
package com.adobe.cq.commerce.mcp;

import org.osgi.annotation.versioning.ConsumerType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ConsumerType
public interface McpTool {
    String name();

    String description();

    ObjectNode inputSchema();

    default boolean writesContent() {
        return false;
    }

    /**
     * Whether this tool is served <strong>only</strong> by the authoring endpoint ({@code …mcp-authoring}) and never
     * by the anonymous shopper endpoint ({@code …mcp}). Content-mutating tools ({@link #writesContent()}) are
     * authoring-only by definition (the default); a <em>read-only</em> tool that is nonetheless authoring-oriented
     * (page-routing / specific-page diagnostics, template / content-fragment / associated-content inspection,
     * authoring picker or binding-validation helpers) overrides this to {@code true} so it is not exposed to
     * anonymous storefront callers.
     * <p>
     * This is an <strong>explicit per-tool declaration</strong>; it is intentionally NOT inferred from the tool's
     * Java package (the {@code internal.tools.authoring} package is only code grouping).
     */
    default boolean authoringOnly() {
        return writesContent();
    }

    /**
     * Whether this tool is part of the guest commerce journey (cart / checkout / order placement or lookup) &mdash;
     * it mutates or reads the remote commerce backend on behalf of a shopper, not AEM content. These tools are
     * anonymous-shopper-only: served on the shopper endpoint (subject to {@link #authoringOnly()} as usual, which
     * is {@code false} for all of them since they never write JCR content) and explicitly excluded from the
     * authoring endpoint, which is for content authoring, not placing or inspecting real commerce orders on a
     * shopper's behalf.
     */
    default boolean commerceJourney() {
        return false;
    }

    JsonNode call(McpCallContext ctx, JsonNode args) throws Exception;
}
