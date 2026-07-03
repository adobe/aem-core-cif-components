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
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQuery;

/**
 * {@link AbstractProductRetriever} for MCP variant lookups. The GraphQL query includes configurable options and
 * variant simple products when the parent is a {@code ConfigurableProduct}.
 */
public class McpProductVariantsRetriever extends AbstractProductRetriever {

    public McpProductVariantsRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    @Override
    protected ProductInterfaceQueryDefinition generateProductQuery() {
        return (ProductInterfaceQuery q) -> {
            q.sku()
                .name()
                .urlKey()
                .onConfigurableProduct(cp -> cp
                    .configurableOptions(o -> o
                        .label()
                        .attributeCode()
                        .values(v -> v
                            .valueIndex()
                            .label()))
                    .variants(v -> v
                        .attributes(a -> a
                            .code()
                            .valueIndex())
                        .product(this::generateVariantProductQuery)));

            if (productQueryHook != null) {
                productQueryHook.accept(q);
            }
        };
    }

    private void generateVariantProductQuery(SimpleProductQuery q) {
        q.sku()
            .name()
            .stockStatus()
            .image(i -> i.url().label())
            .priceRange(r -> r
                .minimumPrice(p -> p
                    .finalPrice(f -> f
                        .value()
                        .currency())));

        if (variantQueryHook != null) {
            variantQueryHook.accept(q);
        }
    }
}
