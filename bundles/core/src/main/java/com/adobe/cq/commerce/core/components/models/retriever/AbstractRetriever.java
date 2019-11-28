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

package com.adobe.cq.commerce.core.components.models.retriever;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

/**
 * Abstract implementation of retriever that fetches data using GraphQL.
 */
public abstract class AbstractRetriever {

    /**
     * Generated or fully customized query.
     */
    protected String query;

    /**
     * Instance of the Magento GraphQL client.
     */
    protected MagentoGraphqlClient client;

    public AbstractRetriever(MagentoGraphqlClient client) {
        if (client == null) {
            throw new java.lang.Error("No GraphQL client provided");
        }
        this.client = client;
    }

    /**
     * Replace the query with your own fully customized query.
     *
     * @param query GraphQL query
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Executes the query and parses the response.
     */
    abstract protected void populate();

    /**
     * Execute the GraphQL query with the GraphQL client.
     *
     * @return GraphqlResponse object
     */
    abstract protected GraphqlResponse<Query, Error> executeQuery();

}
