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
package com.adobe.cq.commerce.core.search.internal.converters;

import java.util.function.Function;

import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

/**
 * Converts a {@link ProductInterface} object into a {@link ProductListItem}.
 */
public class ProductToProductListItemConverter implements Function<ProductInterface, ProductListItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductToProductListItemConverter.class);

    private final String parentId;
    private final Page productPage;
    private final UrlProvider urlProvider;
    private final SlingHttpServletRequest request;

    public ProductToProductListItemConverter(final Page productPage, final SlingHttpServletRequest request, final UrlProvider urlProvider,
                                             String parentId) {
        this.parentId = parentId;
        this.productPage = productPage;
        this.request = request;
        this.urlProvider = urlProvider;
    }

    @Override
    public ProductListItem apply(final ProductInterface product) {
        try {
            return new ProductListItemImpl.Builder(parentId, productPage, request, urlProvider)
                .product(product)
                .build();
        } catch (Exception e) {
            LOGGER.error("Failed to instantiate product " + product.getSku(), e);
            return null;
        }
    }
}
