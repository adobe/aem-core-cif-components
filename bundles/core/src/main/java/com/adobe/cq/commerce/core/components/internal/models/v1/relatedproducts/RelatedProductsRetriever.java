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
package com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts;

import java.util.List;
import java.util.Optional;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
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

    private RelationType relationtype;
    private Optional<ProductInterface> fetchedProduct;

    RelatedProductsRetriever(MagentoGraphqlClient client, RelationType relationType) {
        super(client);
        this.relationtype = relationType;
    }

    @Override
    protected String generateQuery(List<String> identifiers) {
        String firstIdentifier = identifiers != null && !identifiers.isEmpty() ? identifiers.get(0) : null;
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput();
        FilterEqualTypeInput skuFilter = new FilterEqualTypeInput().setEq(firstIdentifier);
        filter.setSku(skuFilter);

        // Apply product attribute filter hook
        if (this.productAttributeFilterHook != null) {
            filter = this.productAttributeFilterHook.apply(filter);
        }

        ProductAttributeFilterInput finalFilter = filter;
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(finalFilter);

        ProductInterfaceQueryDefinition def;
        if (RelationType.UPSELL_PRODUCTS.equals(relationtype)) {
            def = p -> p.sku().upsellProducts(generateProductQuery());
        } else if (RelationType.CROSS_SELL_PRODUCTS.equals(relationtype)) {
            def = p -> p.sku().crosssellProducts(generateProductQuery());
        } else {
            def = p -> p.sku().relatedProducts(generateProductQuery());
        }

        ProductsQueryDefinition queryArgs = q -> q.items(def);
        return Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
    }

    public String fetchSku() {
        if (fetchedProduct == null) {
            populate();
        }
        return fetchedProduct.map(ProductInterface::getSku).orElse(null);
    }

    @Override
    protected void populate() {
        super.populate();
        if (products == null || products.isEmpty()) {
            fetchedProduct = Optional.empty();
            return;
        }

        ProductInterface product = products.get(0);
        fetchedProduct = Optional.of(product);

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
                .urlKey()
                .urlPath()
                .urlRewrites(uq -> uq.url())
                .name()
                .thumbnail(t -> t.label().url())
                .priceRange(r -> r
                    .minimumPrice(generatePriceQuery()))
                .onConfigurableProduct(cp -> cp
                    .priceRange(r -> r
                        .maximumPrice(generatePriceQuery())))
                .onBundleProduct(bp -> bp
                    .priceRange(r -> r
                        .maximumPrice(generatePriceQuery())));

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
}
