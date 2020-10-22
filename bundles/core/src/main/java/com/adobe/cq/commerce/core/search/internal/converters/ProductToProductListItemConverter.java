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

package com.adobe.cq.commerce.core.search.internal.converters;

import java.util.Locale;
import java.util.function.Function;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

/**
 * Converts a {@link ProductInterface} object into a {@link ProductListItem}.
 */
public class ProductToProductListItemConverter implements Function<ProductInterface, ProductListItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductToProductListItemConverter.class);

    private final Resource parentResource;
    private final Page productPage;
    private final Locale locale;
    private final UrlProvider urlProvider;

    private final SlingHttpServletRequest request;

    public ProductToProductListItemConverter(final Page productPage, final SlingHttpServletRequest request, final UrlProvider urlProvider,
                                             Resource parentResource) {
        this.parentResource = parentResource;
        this.productPage = productPage;
        this.locale = productPage.getLanguage(false);
        this.request = request;
        this.urlProvider = urlProvider;
    }

    @Override
    public ProductListItem apply(final ProductInterface product) {
        try {
            boolean isStartPrice = product instanceof GroupedProduct;
            Price price = new PriceImpl(product.getPriceRange(), locale, isStartPrice);
            final ProductImage smallImage = product.getSmallImage();

            String resourceType = parentResource.getResourceType();
            String prefix = StringUtils.substringAfterLast(resourceType, "/");
            String path = parentResource.getPath();
            String parentId = StringUtils.join(prefix, DataLayerComponent.ID_SEPARATOR, StringUtils.substring(DigestUtils.sha256Hex(path),
                0, 10));

            ProductListItem productListItem = new ProductListItemImpl(product.getSku(),
                product.getUrlKey(),
                product.getName(),
                price,
                smallImage == null ? null : smallImage.getUrl(),
                productPage,
                null, // search results aren't targeting specific variant
                request,
                urlProvider,
                parentId);

            return productListItem;
        } catch (Exception e) {
            LOGGER.error("Failed to instantiate product " + product.getSku(), e);
            return null;
        }
    }
}
