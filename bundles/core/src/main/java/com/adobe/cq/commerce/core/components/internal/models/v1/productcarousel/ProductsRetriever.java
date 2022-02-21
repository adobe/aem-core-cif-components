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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;

class ProductsRetriever extends AbstractProductsRetriever {
    private String categoryUid;
    private Integer productCount;

    ProductsRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    void setCategoryUid(String categoryUid) {
        this.categoryUid = categoryUid;
    }

    void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }

    @Override
    protected String generateQuery(List<String> identifiers) {
        if (!isCategoryQuery()) {
            return super.generateQuery(identifiers);
        } else {
            FilterEqualTypeInput uidFilter = new FilterEqualTypeInput().setEq(categoryUid);
            ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setCategoryUid(uidFilter);
            QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter).currentPage(1).pageSize(productCount);

            ProductsQueryDefinition queryArgs = q -> q.items(generateProductQuery());
            return Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
        }
    }

    @Override
    protected ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> {
            q.sku()
                .name()
                .thumbnail(t -> t.label()
                    .url())
                .urlKey()
                .urlPath()
                .urlRewrites(uq -> uq.url())
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery()))
                .onConfigurableProduct(cp -> {
                    if (!isCategoryQuery()) {
                        cp.variants(v -> v.product(generateSimpleProductQuery()));
                    }
                    cp.priceRange(r -> r.maximumPrice(generatePriceQuery()));
                });

            if (isCategoryQuery()) {
                q.categories(cq -> cq.uid().urlKey().urlPath());
            }

            // Apply product query hook
            if (productQueryHook != null) {
                productQueryHook.accept(q);
            }
        };
    }

    private boolean isCategoryQuery() {
        return StringUtils.isNotBlank(categoryUid);
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
