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
package com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.TitleTypeProvider;
import com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel.ProductCarouselBase;
import com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts.RelatedProductsRetriever.RelationType;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { ProductCarousel.class, ComponentExporter.class },
    resourceType = RelatedProductsImpl.RESOURCE_TYPE)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class RelatedProductsImpl extends ProductCarouselBase {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/relatedproducts/v1/relatedproducts";
    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedProductsImpl.class);

    protected static final String PN_PRODUCT = "product";
    protected static final String PN_RELATION_TYPE = "relationType";

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @OSGiService
    private UrlProvider urlProvider;

    @ScriptVariable
    private ValueMap properties;

    private RelatedProductsRetriever productsRetriever;
    private RelationType relationType;
    private String productSku;

    @PostConstruct
    private void initModel() {
        if (magentoGraphqlClient == null) {
            LOGGER.error("Cannot get a GraphqlClient using the resource at {}", resource.getPath());
        } else {
            configureProductsRetriever();
        }
    }

    @Override
    public boolean isConfigured() {
        return productSku != null || (productsRetriever != null && productsRetriever.fetchSku() != null);
    }

    private void configureProductsRetriever() {
        String relationTypeProperty = properties.get(PN_RELATION_TYPE, String.class);
        String product = properties.get(PN_PRODUCT, String.class);

        relationType = relationTypeProperty != null ? RelationType.valueOf(relationTypeProperty) : RelationType.RELATED_PRODUCTS;
        productsRetriever = new RelatedProductsRetriever(magentoGraphqlClient, relationType);

        if (StringUtils.isNotBlank(product)) {
            productSku = product; // The picker is configured to return the SKU
            productsRetriever.setIdentifiers(Collections.singletonList(productSku));
        } else {
            productsRetriever.extendProductFilterWith(urlProvider.getProductIdentifierFilterHook(request));
        }
    }

    @Override
    @JsonIgnore
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
            try {
                ProductListItemImpl.Builder builder = new ProductListItemImpl.Builder(getId(), currentPage, request, urlProvider)
                    .product(product)
                    .image(product.getThumbnail());
                carouselProductList.add(builder.build());
            } catch (Exception e) {
                LOGGER.error("Failed to instantiate product " + (product != null ? product.getSku() : null), e);
            }
        }
        return carouselProductList;
    }

    @Override
    @JsonIgnore
    public AbstractProductsRetriever getProductsRetriever() {
        return productsRetriever;
    }

    @Override
    public String getTitleType() {
        return TitleTypeProvider.getTitleType(currentStyle, properties);
    }

    @JsonProperty("productIdentifiers")
    public List<CommerceIdentifier> getProductCommerceIdentifiers() {
        return getProducts().stream()
            .map(ProductListItem::getSKU)
            .map(ListItemIdentifier::new)
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public List<ProductListItem> getProductIdentifiers() {
        return getProducts().stream()
            .map(ProductListItem::getSKU)
            .map(sku -> new ProductListItemImpl(sku, new ListItemIdentifier(sku), getId(), currentPage))
            .collect(Collectors.toList());
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    /**
     * Returns the {@link CommerceIdentifier} as part of the JSON interface of the component.
     *
     * @return
     */
    @JsonSerialize(as = CommerceIdentifier.class)
    public CommerceIdentifier getCommerceIdentifier() {
        if (productSku == null) {
            productSku = productsRetriever.fetchSku();
        }
        return new ListItemIdentifier(productSku);
    }
}
