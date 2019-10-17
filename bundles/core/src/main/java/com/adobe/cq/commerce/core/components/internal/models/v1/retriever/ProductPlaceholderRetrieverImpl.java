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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

import com.adobe.cq.commerce.core.components.models.retriever.ProductRetriever;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.shopify.graphql.support.AbstractQuery;

public class ProductPlaceholderRetrieverImpl implements ProductRetriever {

    private ProductInterface product;
    private String mediaBaseUrl;

    public ProductPlaceholderRetrieverImpl(String placeholderPath) throws IOException {
        String json = IOUtils.toString(getClass().getResourceAsStream(placeholderPath), StandardCharsets.UTF_8);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);

        product = rootQuery.getProducts().getItems().get(0);
        mediaBaseUrl = rootQuery.getStoreConfig().getSecureBaseMediaUrl();
    }

    @Override
    public void setQuery(String query) {
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

    @Override
    public void setSlug(String slug) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends AbstractQuery<?>> void setProductQueryHook(Consumer<U> hook) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends AbstractQuery<?>> void setVariantQueryHook(Consumer<U> hook) {
        throw new UnsupportedOperationException();
    }
}
