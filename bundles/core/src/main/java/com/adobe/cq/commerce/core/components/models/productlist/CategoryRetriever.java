/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.models.productlist;

import java.util.function.Function;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQuery;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;

/**
 * This extension of the {@link AbstractCategoryRetriever} allows to extend the products query of the {@link ProductList} with a filter,
 * additionally to the query field extension.
 */
@ConsumerType
public class CategoryRetriever extends AbstractCategoryRetriever {

    private Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook;

    public CategoryRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    @Override
    protected CategoryTreeQueryDefinition generateCategoryQuery() {
        CategoryTreeQueryDefinition categoryTreeQueryDefinition = (CategoryTreeQuery q) -> {
            q.uid()
                .description()
                .name()
                .image()
                .productCount()
                .metaDescription()
                .metaKeywords()
                .metaTitle()
                .urlKey()
                .urlPath()
                .children(cq -> cq.id().uid().urlKey().urlPath());

            if (categoryQueryHook != null) {
                categoryQueryHook.accept(q);
            }
        };

        return categoryTreeQueryDefinition;
    }

    /**
     * Extends or replaces the product attribute filter with a custom instance defined by a lambda hook.
     * <p>
     * Example 1 (Extend):
     *
     * <pre>
     * {@code
     * categoryRetriever.extendProductFilterWith(f -> f
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     * <p>
     * Example 2 (Replace):
     *
     * <pre>
     * {@code
     * categoryRetriever.extendProductFilterWith(f -> new ProductAttributeFilterInput()
     *     .setSku(new FilterEqualTypeInput()
     *         .setEq("custom-sku"))
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * @param productAttributeFilterHook Lambda that extends or replaces the product attribute filter.
     */
    public void extendProductFilterWith(Function<ProductAttributeFilterInput, ProductAttributeFilterInput> productAttributeFilterHook) {
        if (this.productAttributeFilterHook == null) {
            this.productAttributeFilterHook = productAttributeFilterHook;
        } else {
            this.productAttributeFilterHook = this.productAttributeFilterHook.andThen(productAttributeFilterHook);
        }
    }

    /**
     * @return The extended product filter hook if it was set with {@link CategoryRetriever#extendProductFilterWith(Function)}.
     */
    public Function<ProductAttributeFilterInput, ProductAttributeFilterInput> getProductFilterHook() {
        return this.productAttributeFilterHook;
    }

}
