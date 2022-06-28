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
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.TitleTypeProvider;
import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { ProductCarousel.class, ComponentExporter.class },
    resourceType = ProductCarouselImpl.RESOURCE_TYPE)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ProductCarouselImpl extends ProductCarouselBase implements ProductCarousel {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productcarousel/v1/productcarousel";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCarouselImpl.class);
    private static final String PRODUCT_SELECTION = "product";
    private static final String CATEGORY_SELECTION = "category";
    private static final String PRODUCT_COUNT_PROPERTY = "productCount";
    private static final String CATEGORY_PROPERTY = "category";
    private static final String PRODUCT_PROPERTY = "product";
    private static final String SELECTION_TYPE_PROPERTY = "selectionType";
    static final int DEFAULT_PRODUCT_COUNT = 10;
    static final int MIN_PRODUCT_COUNT = 1;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @ValueMapValue(
        name = SELECTION_TYPE_PROPERTY,
        injectionStrategy = InjectionStrategy.OPTIONAL)
    private String selectionType;

    @ValueMapValue(
        name = PRODUCT_PROPERTY,
        injectionStrategy = InjectionStrategy.OPTIONAL)
    private String[] productSkuList;

    @ValueMapValue(
        name = CATEGORY_PROPERTY,
        injectionStrategy = InjectionStrategy.OPTIONAL)
    private String categoryUid;

    @OSGiService
    private UrlProvider urlProvider;

    private Integer productCount;
    private List<String> baseProductSkus = Collections.emptyList();

    private ProductsRetriever productsRetriever;

    @PostConstruct
    private void initModel() {
        if (StringUtils.isBlank(selectionType)) {
            selectionType = PRODUCT_SELECTION;
        }

        productCount = resource.getValueMap().get(PRODUCT_COUNT_PROPERTY, currentStyle.get(PRODUCT_COUNT_PROPERTY, DEFAULT_PRODUCT_COUNT));
        productCount = Math.max(MIN_PRODUCT_COUNT, productCount);

        if (magentoGraphqlClient == null) {
            LOGGER.warn("Cannot get a GraphqlClient using the resource at {}", resource.getPath());
            return;
        }

        if (!isConfigured()) {
            return;
        }

        productsRetriever = new ProductsRetriever(magentoGraphqlClient);
        if (PRODUCT_SELECTION.equals(selectionType)) {
            // Make sure we use the base product sku for each selected product (can be a variant)
            List<String> productSkus = Arrays.asList(productSkuList);
            baseProductSkus = productSkus
                .stream()
                .map(s -> s.startsWith("/") ? StringUtils.substringAfterLast(s, "/") : s)
                .map(s -> CombinedSku.parse(s).getBaseSku())
                .distinct()
                .collect(Collectors.toList());
            productsRetriever.setIdentifiers(baseProductSkus);
        } else {
            productsRetriever.setCategoryUid(categoryUid);
            productsRetriever.setProductCount(productCount);
        }
    }

    @Override
    public boolean isConfigured() {
        return selectionType.equals(PRODUCT_SELECTION) && productSkuList != null ||
            selectionType.equals(CATEGORY_SELECTION) && categoryUid != null;
    }

    @Override
    @JsonIgnore
    @Nonnull
    public List<ProductListItem> getProducts() {
        if (productsRetriever != null) {
            if (PRODUCT_SELECTION.equals(selectionType)) {
                return processManualProducts(productsRetriever.fetchProducts());
            } else if (CATEGORY_SELECTION.equals(selectionType)) {
                return processCategoryProducts(productsRetriever.fetchProducts(), productsRetriever.fetchCategory());
            }
        }

        return Collections.emptyList();
    }

    @Override
    @JsonIgnore
    public AbstractProductsRetriever getProductsRetriever() {
        return productsRetriever;
    }

    @Override
    public String getTitleType() {
        return TitleTypeProvider.getTitleType(currentStyle, resource.getValueMap());
    }

    @JsonProperty("productIdentifiers")
    public List<CommerceIdentifier> getProductCommerceIdentifiers() {
        return baseProductSkus.stream().map(ListItemIdentifier::new).collect(Collectors.toList());
    }

    @Override
    @Deprecated
    @JsonIgnore
    @Nonnull
    public List<ProductListItem> getProductIdentifiers() {
        return baseProductSkus.stream()
            .map(ListItemIdentifier::new)
            .map(id -> new ProductListItemImpl(id, getId(), currentPage))
            .collect(Collectors.toList());
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }

    protected SimpleProduct findVariant(ConfigurableProduct configurableProduct, String variantSku) {
        List<ConfigurableVariant> variants = configurableProduct.getVariants();
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream().map(v -> v.getProduct()).filter(sp -> variantSku.equals(sp.getSku())).findFirst().orElse(null);
    }

    private List<ProductListItem> processManualProducts(List<ProductInterface> products) {
        products.sort(Comparator.comparing(item -> baseProductSkus.indexOf(item.getSku())));
        List<ProductListItem> carouselProductList = new ArrayList<>();
        if (products.isEmpty()) {
            return carouselProductList;
        }

        for (String combinedSku : productSkuList) {
            if (combinedSku.startsWith("/")) {
                combinedSku = StringUtils.substringAfterLast(combinedSku, "/");
            }

            CombinedSku skus = CombinedSku.parse(combinedSku);
            ProductInterface product = products.stream().filter(p -> p.getSku().equals(skus.getBaseSku()))
                .findFirst().orElse(null);
            if (product == null) {
                continue; // Can happen that a product is not found
            }

            // retain urlKey, urlPath and urlRewrites from the base product
            String urlKey = product.getUrlKey();
            String urlPath = product.getUrlPath();
            List<UrlRewrite> urlRewrites = product.getUrlRewrites();
            if (skus.getVariantSku() != null && product instanceof ConfigurableProduct) {
                SimpleProduct variant = findVariant((ConfigurableProduct) product, skus.getVariantSku());
                if (variant != null) {
                    product = variant;
                }
            }

            try {
                ProductListItemImpl.Builder builder = new ProductListItemImpl.Builder(getId(), currentPage,
                    request, urlProvider)
                        .product(product)
                        .image(product.getThumbnail())
                        .sku(skus.getBaseSku())
                        .urlKey(urlKey)
                        .urlPath(urlPath)
                        .urlRewrites(urlRewrites)
                        .variantSku(skus.getVariantSku());
                carouselProductList.add(builder.build());
            } catch (Exception e) {
                LOGGER.warn("Failed to instantiate product " + combinedSku, e);
            }
        }
        return carouselProductList;
    }

    private List<ProductListItem> processCategoryProducts(List<ProductInterface> productInterfaces, CategoryInterface category) {
        return productInterfaces.stream().map(product -> {
            try {
                return new ProductListItemImpl.Builder(getId(), currentPage, request, urlProvider)
                    .product(product)
                    .image(product.getThumbnail())
                    .sku(product.getSku())
                    .urlKey(product.getUrlKey())
                    .urlPath(product.getUrlPath())
                    .urlRewrites(product.getUrlRewrites())
                    .categoryContext(category).build();
            } catch (Exception e) {
                LOGGER.warn("Failed to instantiate product " + product.getSku(), e);
                return null;
            }
        }).filter(Objects::nonNull).limit(productCount).collect(Collectors.toList());
    }
}
