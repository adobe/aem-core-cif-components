/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts;

import java.util.List;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPricesQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;

class RelatedProductsRetriever extends AbstractProductsRetriever {

    static enum RelationType {
        RELATED_PRODUCTS("Related products"),
        UPSELL_PRODUCTS("Upsell products"),
        CROSS_SELL_PRODUCTS("Cross-sell products");

        private final String text;

        private RelationType(String text) {
            this.text = text;
        }

        String getText() {
            return text;
        }
    }

    static enum ProductIdType {
        SKU, SLUG
    }

    private RelationType relationtype;
    private ProductIdType productIdType;

    RelatedProductsRetriever(MagentoGraphqlClient client, RelationType relationType, ProductIdType productIdType) {
        super(client);
        this.relationtype = relationType;
        this.productIdType = productIdType;
    }

    @Override
    protected String generateQuery(List<String> identifiers) {
        FilterEqualTypeInput input = new FilterEqualTypeInput().setEq(identifiers.get(0));
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput();
        if (ProductIdType.SKU.equals(productIdType)) {
            filter.setSku(input);
        } else {
            filter.setUrlKey(input);
        }
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        ProductInterfaceQueryDefinition def;
        if (RelationType.UPSELL_PRODUCTS.equals(relationtype)) {
            def = p -> p.upsellProducts(generateProductQuery());
        } else if (RelationType.CROSS_SELL_PRODUCTS.equals(relationtype)) {
            def = p -> p.crosssellProducts(generateProductQuery());
        } else {
            def = p -> p.relatedProducts(generateProductQuery());
        }

        ProductsQueryDefinition queryArgs = q -> q.items(def);
        return Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
    }

    @Override
    protected void populate() {
        super.populate();
        if (products == null || products.isEmpty()) {
            return;
        }

        ProductInterface product = products.get(0);

        if (RelationType.UPSELL_PRODUCTS.equals(relationtype)) {
            products = product.getUpsellProducts();
        } else if (RelationType.CROSS_SELL_PRODUCTS.equals(relationtype)) {
            products = product.getCrosssellProducts();
        } else {
            products = product.getRelatedProducts();
        }
    }

    @Override
    protected ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> {
            q.sku()
                .name()
                .thumbnail(t -> t.label().url())
                .urlKey()
                .price(generatePriceQuery());

            // By default, we don't fetch any variants data, except if this has been customised via the hook
            if (variantQueryHook != null) {
                ConfigurableProductQueryDefinition cpDef = cp -> cp.variants(cv -> cv.product(sp -> variantQueryHook.accept(sp)));
                q.onConfigurableProduct(cpDef);
            }

            // Apply product query hook
            if (productQueryHook != null) {
                productQueryHook.accept(q);
            }
        };
    }

    private ProductPricesQueryDefinition generatePriceQuery() {
        return q -> q.regularPrice(rp -> rp.amount(a -> a.currency().value()));
    }
}
