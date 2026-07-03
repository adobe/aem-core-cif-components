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

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;

/**
 * Minimal {@link AbstractCategoryRetriever} implementation used by the MCP category tools. The GraphQL query is
 * trimmed to the fields required by the tools' response DTOs and their PLP links: uid, name, url_key, url_path and
 * the same fields for the immediate children. Tools that need more (e.g. breadcrumbs) extend the query via
 * {@link #extendCategoryQueryWith(java.util.function.Consumer)}.
 */
public class McpCategoryRetriever extends AbstractCategoryRetriever {

    public McpCategoryRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    @Override
    protected CategoryTreeQueryDefinition generateCategoryQuery() {
        return q -> {
            q.uid()
                .name()
                .urlKey()
                .urlPath()
                .children(c -> c.uid().name().urlKey().urlPath());

            // Apply category query hook
            if (categoryQueryHook != null) {
                categoryQueryHook.accept(q);
            }
        };
    }
}
