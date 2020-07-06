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
package com.adobe.cq.commerce.core.components.internal.models.v1.productlist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;

class CategoryPlaceholderRetriever extends AbstractCategoryRetriever {
    CategoryPlaceholderRetriever(MagentoGraphqlClient client, String placeholderPath) throws IOException {
        super(client);

        String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(placeholderPath), StandardCharsets.UTF_8);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);

        category = rootQuery.getCategory();
    }

    @Override
    protected CategoryTreeQueryDefinition generateCategoryQuery() {
        return null;
    }
}
