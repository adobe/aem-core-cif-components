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

package com.adobe.cq.commerce.core.components.internal.models.v1.retriever;

import java.util.List;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.ProductRetriever;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public class ProductRetrieverImpl implements ProductRetriever {

    private String query;

    private MagentoGraphqlClient client;

    private ProductInterface product;

    private String mediaBaseUrl;

    public ProductRetrieverImpl(MagentoGraphqlClient client) {
        if (client == null)
            throw new java.lang.Error("No GraphQL client provided");
        this.client = client;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public GraphqlResponse<Query, Error> getData() {
        return client.execute(query);
    }

    @Override
    public ProductInterface getProduct() {
        if (product == null)
            populate();
        return product;
    }

    @Override
    public String getMediaBaseUrl() {
        if (mediaBaseUrl == null)
            populate();
        return mediaBaseUrl;
    }

    private void populate() {
        // Get product list from response
        GraphqlResponse<Query, Error> response = getData();
        Query rootQuery = response.getData();
        List<ProductInterface> products = rootQuery.getProducts().getItems();

        // TODO WORKAROUND
        // we need a temporary detour and use storeconfig to get the base media url since the product media gallery only returns the images
        // file names but no full URLs
        mediaBaseUrl = rootQuery.getStoreConfig().getSecureBaseMediaUrl();

        // Return first product in list
        if (products.size() > 0) {
            product = products.get(0);
        }
    }

}
