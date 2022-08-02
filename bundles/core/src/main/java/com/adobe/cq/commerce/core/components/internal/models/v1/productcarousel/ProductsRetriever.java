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
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQuery;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

class ProductsRetriever extends AbstractProductsRetriever {
    private String categoryUid;
    private Integer productCount;
    private CategoryInterface category;

    ProductsRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    void setCategoryUid(String categoryUid) {
        this.categoryUid = categoryUid;
    }

    void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }

    /**
     * Lambda that allows to replace or extend the category filters.
     */
    private Function<CategoryFilterInput, CategoryFilterInput> categoryFilterHook;

    /**
     * Extends or replaces the category filter with a custom instance defined by a lambda hook.
     *
     * Example 1 (Extend):
     *
     * <pre>
     * {@code
     * productsRetriever.extendCategoryFilterWith(f -> f
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * Example 2 (Replace):
     *
     * <pre>
     * {@code
     * productsRetriever.extendCategoryFilterWith(f -> new CategoryFilterInput()
     *     .setCategoryUid(new FilterEqualTypeInput()
     *         .setEq("custom-uid"))
     *     .setCustomFilter("my-attribute", new FilterEqualTypeInput()
     *         .setEq("my-value")));
     * }
     * </pre>
     *
     * @param categoryFilterHook Lambda that extends or replaces the category filter.
     */
    public void extendCategoryFilterWith(Function<CategoryFilterInput, CategoryFilterInput> categoryFilterHook) {
        if (this.categoryFilterHook == null) {
            this.categoryFilterHook = categoryFilterHook;
        } else {
            this.categoryFilterHook = this.categoryFilterHook.andThen(categoryFilterHook);
        }
    }

    CategoryInterface fetchCategory() {
        fetchProducts();

        return category;
    }

    @Override
    protected String generateQuery(List<String> identifiers) {
        if (isCategoryQuery()) {
            CategoryFilterInput categoryFilter = new CategoryFilterInput();
            FilterEqualTypeInput uidFilter = new FilterEqualTypeInput().setEq(categoryUid);
            categoryFilter.setCategoryUid(uidFilter);

            // Apply category filter hook
            if (this.categoryFilterHook != null) {
                categoryFilter = this.categoryFilterHook.apply(categoryFilter);
            }

            CategoryFilterInput finalCategoryFilter = categoryFilter;
            QueryQuery.CategoryListArgumentsDefinition categoryArgs = s -> s.filters(finalCategoryFilter);

            CategoryTreeQuery.ProductsArgumentsDefinition productArgs = s -> s.currentPage(1).pageSize(productCount);
            return Operations.query(query -> query.categoryList(categoryArgs, generateCategoryListQuery(productArgs))).toString();
        } else {
            return super.generateQuery(identifiers);
        }
    }

    @Override
    protected void populate() {
        if (!isCategoryQuery()) {
            super.populate();
            return;
        }

        GraphqlResponse<Query, Error> response = executeQuery();
        if (CollectionUtils.isEmpty(response.getErrors())) {
            Query rootQuery = response.getData();
            List<CategoryTree> items = rootQuery.getCategoryList();
            if (CollectionUtils.isNotEmpty(items)) {
                category = items.get(0);
                products = category.getProducts().getItems();
            }
        }

        if (products == null) {
            products = Collections.emptyList();
        }
    }

    private boolean isCategoryQuery() {
        return StringUtils.isNotBlank(categoryUid);
    }

    @Override
    protected ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> {
            q.sku()
                .urlKey()
                .urlPath()
                .urlRewrites(uq -> uq.url())
                .name()
                .thumbnail(t -> t.label()
                    .url())
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery()))
                .onConfigurableProduct(cp -> {
                    if (!isCategoryQuery()) {
                        cp.variants(v -> v.product(generateSimpleProductQuery()));
                    }
                    cp.priceRange(r -> r.maximumPrice(generatePriceQuery()));
                })
                .onBundleProduct(bp -> bp
                    .priceRange(r -> r
                        .maximumPrice(generatePriceQuery())));

            // Apply product query hook
            if (productQueryHook != null) {
                productQueryHook.accept(q);
            }
        };
    }

    private CategoryTreeQueryDefinition generateCategoryListQuery(CategoryTreeQuery.ProductsArgumentsDefinition productArgs) {
        return c -> c.uid().urlKey().urlPath()
            .products(productArgs, q -> q.items(generateProductQuery()));
    }

    private SimpleProductQueryDefinition generateSimpleProductQuery() {
        return q -> {
            q.sku()
                .name()
                .thumbnail(t -> t
                    .label()
                    .url())
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery()));

            // Apply product variant query hook
            if (variantQueryHook != null) {
                variantQueryHook.accept(q);
            }
        };
    }
}
