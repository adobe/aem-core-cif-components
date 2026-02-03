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

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;

abstract class UrlToCategoryRetriever extends AbstractCategoryRetriever {

    private UrlToCategoryRetriever(MagentoGraphqlClient client) {
        super(client);
    }

    @Override
    protected CategoryTreeQueryDefinition generateCategoryQuery() {
        return q -> q.uid();
    }

    @Override
    public String generateQuery(String identifier) {
        CategoryTreeQueryDefinition queryArgs = generateCategoryQuery();
        return Operations.query(query -> {
            CategoryFilterInput filter = generateCategoryFilterInput();
            QueryQuery.CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);
            query.categoryList(searchArgs, queryArgs);
        }).toString();
    }

    protected abstract CategoryFilterInput generateCategoryFilterInput();

    static class ByUrlKey extends UrlToCategoryRetriever {
        ByUrlKey(MagentoGraphqlClient client) {
            super(client);
        }

        @Override
        protected CategoryFilterInput generateCategoryFilterInput() {
            return new CategoryFilterInput().setUrlKey(new FilterEqualTypeInput().setEq(identifier));
        }
    }

    static class ByUrlPath extends UrlToCategoryRetriever {
        ByUrlPath(MagentoGraphqlClient client) {
            super(client);
        }

        @Override
        protected CategoryFilterInput generateCategoryFilterInput() {
            return new CategoryFilterInput().setUrlPath(new FilterEqualTypeInput().setEq(identifier));
        }
    }
}
