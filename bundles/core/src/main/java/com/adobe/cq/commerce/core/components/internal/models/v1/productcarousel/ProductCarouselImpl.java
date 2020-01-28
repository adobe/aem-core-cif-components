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

package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductCarousel.class, resourceType = ProductCarouselImpl.RESOURCE_TYPE)
public class ProductCarouselImpl implements ProductCarousel {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productcarousel/v1/productcarousel";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCarouselImpl.class);

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @Inject
    @Optional
    private String[] productSkuList;

    @Inject
    private Page currentPage;

    private Page productPage;
    private MagentoGraphqlClient magentoGraphqlClient;
    private List<String> baseProductSkus;

    private AbstractProductsRetriever productsRetriever;

    @PostConstruct
    private void initModel() {
        if (!isConfigured()) {
            return;
        }

        List<String> productSkus = Arrays.asList(productSkuList);
        magentoGraphqlClient = MagentoGraphqlClient.create(resource);
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
        if (magentoGraphqlClient == null) {
            LOGGER.error("Cannot get a GraphqlClient using the resource at {}", resource.getPath());
        }

        // Make sure we use the base product sku for each selected product (can be a variant)
        baseProductSkus = productSkus
            .stream()
            .map(s -> s.startsWith("/") ? StringUtils.substringAfterLast(s, "/") : s)
            .map(s -> SiteNavigation.toProductSkus(s).getLeft())
            .collect(Collectors.toList());

        productsRetriever = new ProductsRetriever(magentoGraphqlClient);
        productsRetriever.setIdentifiers(baseProductSkus);
    }

    @Override
    public boolean isConfigured() {
        return productSkuList != null;
    }

    @Override
    public List<ProductListItem> getProducts() {
        List<ProductInterface> productList = productsRetriever.fetchProducts();
        Collections.sort(productList, Comparator.comparing(item -> baseProductSkus.indexOf(item.getSku())));

        List<ProductListItem> carouselProductList = new ArrayList<>();
        if (!productList.isEmpty()) {
            for (ProductInterface product : productList) {
                // Find the baseProductSku that was used to fetch that product
                int idx = baseProductSkus.indexOf(product.getSku());

                // We know the list of skus is in the same order as the list of products
                // but we searched the index because a product might not have been found (see unit test)
                Pair<String, String> skus = SiteNavigation.toProductSkus(productSkuList[idx]);

                String slug = product.getUrlKey();
                if (skus.getRight() != null && product instanceof ConfigurableProduct) {
                    SimpleProduct variant = findVariant((ConfigurableProduct) product, skus.getRight());
                    if (variant != null) {
                        product = variant;
                    }
                }

                carouselProductList.add(new ProductListItemImpl(
                    skus.getLeft(),
                    slug,
                    product.getName(),
                    product.getPrice().getRegularPrice().getAmount().getValue(),
                    product.getPrice().getRegularPrice().getAmount().getCurrency().toString(),
                    product.getThumbnail().getUrl(),
                    productPage,
                    skus.getRight(),
                    request));
            }
        }
        return carouselProductList;
    }

    @Override
    public AbstractProductsRetriever getProductsRetriever() {
        return productsRetriever;
    }

    protected SimpleProduct findVariant(ConfigurableProduct configurableProduct, String variantSku) {
        List<ConfigurableVariant> variants = configurableProduct.getVariants();
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream().map(v -> v.getProduct()).filter(sp -> variantSku.equals(sp.getSku())).findFirst().orElse(null);
    }
}
