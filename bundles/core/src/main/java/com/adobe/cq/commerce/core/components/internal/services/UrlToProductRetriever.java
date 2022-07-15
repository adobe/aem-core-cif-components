/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

class UrlToProductRetriever extends AbstractProductRetriever {

    UrlToProductRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    @Override
    protected String generateQuery(String identifier) {
        ProductsQueryDefinition queryArgs = q -> q.items(generateProductQuery());
        return Operations.query(query -> {
            ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setUrlKey(new FilterEqualTypeInput().setEq(identifier));
            QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);
            query.products(searchArgs, queryArgs);
        }).toString();
    }

    @Override
    protected ProductInterfaceQueryDefinition generateProductQuery() {
        return ProductInterfaceQuery::sku;
    }

    @Override
    protected void populate() {
        // Get product list from response
        GraphqlResponse<Query, Error> response = executeQuery();

        if (CollectionUtils.isEmpty(response.getErrors())) {
            Query rootQuery = response.getData();
            List<ProductInterface> products = rootQuery.getProducts().getItems();

            // Return first product in list unless the identifier type is 'url_key',
            // then return the product whose 'url_key' matches the identifier
            if (products.size() > 0) {
                if (products.size() > 1) {
                    for (ProductInterface productInterface : products) {
                        if (identifier.equals(productInterface.getUrlKey())) {
                            product = Optional.of(productInterface);
                            return;
                        }
                    }
                } else {
                    product = Optional.of(products.get(0));
                    return;
                }
            }
        }

        product = Optional.empty();
    }
}
