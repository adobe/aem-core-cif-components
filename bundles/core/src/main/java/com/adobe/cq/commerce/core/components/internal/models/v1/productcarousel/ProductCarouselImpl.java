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
import java.util.Locale;
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
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
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

    @Inject
    private UrlProvider urlProvider;

    private Page productPage;
    private MagentoGraphqlClient magentoGraphqlClient;
    private List<String> baseProductSkus;
    private Locale locale;

    private AbstractProductsRetriever productsRetriever;

    @PostConstruct
    private void initModel() {
        if (!isConfigured()) {
            return;
        }

        List<String> productSkus = Arrays.asList(productSkuList);
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }

        locale = productPage.getLanguage(false);

        // Make sure we use the base product sku for each selected product (can be a variant)
        baseProductSkus = productSkus
            .stream()
            .map(s -> s.startsWith("/") ? StringUtils.substringAfterLast(s, "/") : s)
            .map(s -> SiteNavigation.toProductSkus(s).getLeft())
            .distinct()
            .collect(Collectors.toList());

        magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage);
        if (magentoGraphqlClient == null) {
            LOGGER.error("Cannot get a GraphqlClient using the resource at {}", resource.getPath());
        } else {
            productsRetriever = new ProductsRetriever(magentoGraphqlClient);
            productsRetriever.setIdentifiers(baseProductSkus);
        }
    }

    @Override
    public boolean isConfigured() {
        return productSkuList != null;
    }

    @Override
    public List<ProductListItem> getProducts() {
        if (productsRetriever == null) {
            return Collections.emptyList();
        }

        List<ProductInterface> products = productsRetriever.fetchProducts();
        Collections.sort(products, Comparator.comparing(item -> baseProductSkus.indexOf(item.getSku())));

        List<ProductListItem> carouselProductList = new ArrayList<>();
        if (!products.isEmpty()) {
            for (String combinedSku : productSkuList) {

                if (combinedSku.startsWith("/")) {
                    combinedSku = StringUtils.substringAfterLast(combinedSku, "/");
                }

                Pair<String, String> skus = SiteNavigation.toProductSkus(combinedSku);
                ProductInterface product = products.stream().filter(p -> p.getSku().equals(skus.getLeft())).findFirst().orElse(null);
                if (product == null) {
                    continue; // Can happen that a product is not found
                }

                String slug = product.getUrlKey();
                if (skus.getRight() != null && product instanceof ConfigurableProduct) {
                    SimpleProduct variant = findVariant((ConfigurableProduct) product, skus.getRight());
                    if (variant != null) {
                        product = variant;
                    }
                }

                try {
                    Price price = new PriceImpl(product.getPriceRange(), locale);
                    ProductImage thumbnail = product.getThumbnail();
                    carouselProductList.add(new ProductListItemImpl(
                        skus.getLeft(),
                        slug,
                        product.getName(),
                        price,
                        thumbnail == null ? null : thumbnail.getUrl(),
                        productPage,
                        skus.getRight(),
                        request,
                        urlProvider));
                } catch (Exception e) {
                    LOGGER.error("Failed to instantiate product " + combinedSku, e);
                }
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
