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

package com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts.RelatedProductsRetriever.ProductIdType;
import com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts.RelatedProductsRetriever.RelationType;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductCarousel.class, resourceType = RelatedProductsImpl.RESOURCE_TYPE)
public class RelatedProductsImpl implements ProductCarousel {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/relatedproducts/v1/relatedproducts";
    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedProductsImpl.class);

    protected static final String PN_PRODUCT = "product";
    protected static final String PN_RELATION_TYPE = "relationType";

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @ScriptVariable
    private ValueMap properties;

    private Page productPage;
    private MagentoGraphqlClient magentoGraphqlClient;
    private AbstractProductsRetriever productsRetriever;

    @PostConstruct
    private void initModel() {
        if (!isConfigured()) {
            return;
        }

        magentoGraphqlClient = MagentoGraphqlClient.create(resource);
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
        if (magentoGraphqlClient == null) {
            LOGGER.error("Cannot get a GraphqlClient using the resource at {}", resource.getPath());
        }

        configureProductsRetriever();
    }

    @Override
    public boolean isConfigured() {
        return properties.get(PN_PRODUCT, String.class) != null || request.getRequestPathInfo().getSelectorString() != null;
    }

    private void configureProductsRetriever() {
        String relationType = properties.get(PN_RELATION_TYPE, String.class);
        String product = properties.get(PN_PRODUCT, String.class);
        String skuOrSlug;
        ProductIdType productIdType;

        if (product != null) {
            skuOrSlug = product; // The picker is configured to return the SKU
            productIdType = ProductIdType.SKU;
        } else {
            skuOrSlug = request.getRequestPathInfo().getSelectorString(); // The slug is coming from the URL selector
            productIdType = ProductIdType.SLUG;
        }

        RelationType rel = relationType != null ? RelationType.valueOf(relationType) : RelationType.RELATED_PRODUCTS;
        productsRetriever = new RelatedProductsRetriever(magentoGraphqlClient, rel, productIdType);
        productsRetriever.setIdentifiers(Collections.singletonList(skuOrSlug));
    }

    @Override
    public List<ProductListItem> getProducts() {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        List<ProductInterface> products = productsRetriever.fetchProducts();
        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductListItem> carouselProductList = new ArrayList<>();
        for (ProductInterface product : products) {
            carouselProductList.add(new ProductListItemImpl(
                product.getSku(),
                product.getUrlKey(),
                product.getName(),
                product.getPrice().getRegularPrice().getAmount().getValue(),
                product.getPrice().getRegularPrice().getAmount().getCurrency().toString(),
                product.getThumbnail().getUrl(),
                productPage,
                null,
                request));
        }
        return carouselProductList;
    }

    @Override
    public AbstractProductsRetriever getProductsRetriever() {
        return productsRetriever;
    }

}
