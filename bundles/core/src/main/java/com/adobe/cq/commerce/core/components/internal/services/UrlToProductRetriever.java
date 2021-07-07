/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.services;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;

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
        return q -> q.sku().urlKey();
    }
}