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

import com.adobe.cq.commerce.core.components.models.retriever.ProductPlaceholderRetriever;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public class ProductPlaceholderRetrieverImpl implements ProductPlaceholderRetriever {

    private ProductInterface product;
    private String mediaBaseUrl;

    @Override
    public void setProduct(ProductInterface product) {
        this.product = product;
    }

    @Override
    public void setMediaBaseUrl(String mediaBaseUrl) {
        this.mediaBaseUrl = mediaBaseUrl;
    }

    @Override
    public void setQuery(String query) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GraphqlResponse<Query, Error> getData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProductInterface getProduct() {
        return product;
    }

    @Override
    public String getMediaBaseUrl() {
        return mediaBaseUrl;
    }
}
