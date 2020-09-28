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

package com.adobe.cq.commerce.core.components.internal.models.v1.productteaser;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.VirtualProduct;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductTeaser.class, resourceType = ProductTeaserImpl.RESOURCE_TYPE)
public class ProductTeaserImpl implements ProductTeaser {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productteaser/v1/productteaser";
    private static final String SELECTION_PROPERTY = "selection";

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @Inject
    private UrlProvider urlProvider;

    @ScriptVariable
    private ValueMap properties;

    private Page productPage;
    private Pair<String, String> combinedSku;
    private AbstractProductRetriever productRetriever;

    private Locale locale;
    private Boolean isVirtualProduct;

    @PostConstruct
    protected void initModel() {
        locale = currentPage.getLanguage(false);

        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
        String selection = properties.get(SELECTION_PROPERTY, String.class);
        if (selection != null && !selection.isEmpty()) {
            if (selection.startsWith("/")) {
                selection = StringUtils.substringAfterLast(selection, "/");
            }
            combinedSku = SiteNavigation.toProductSkus(selection);

            // Get MagentoGraphqlClient from the resource.
            MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage);

            // Fetch product data
            if (magentoGraphqlClient != null) {
                productRetriever = new ProductRetriever(magentoGraphqlClient);
                productRetriever.setIdentifier(ProductIdentifierType.SKU, combinedSku.getLeft());
            }
        }
    }

    private ProductInterface getProduct() {
        if (productRetriever == null) {
            return null;
        }

        ProductInterface baseProduct = productRetriever.fetchProduct();
        if (combinedSku.getRight() != null && baseProduct instanceof ConfigurableProduct) {
            ConfigurableProduct configurableProduct = (ConfigurableProduct) baseProduct;
            SimpleProduct variant = findVariant(configurableProduct, combinedSku.getRight());
            if (variant != null) {
                return variant;
            }
        }
        return baseProduct;
    }

    @Override
    public String getName() {
        return getProduct().getName();
    }

    @Override
    public String getSku() {
        String sku = getProduct().getSku();
        return sku != null ? sku : combinedSku.getLeft();
    }

    @Override
    public String getCallToAction() {
        return properties.get("cta", null);
    }

    @Override
    public Price getPriceRange() {
        return new PriceImpl(getProduct().getPriceRange(), locale);
    }

    @Override
    public String getFormattedPrice() {
        return getPriceRange().getFormattedFinalPrice();
    }

    @Override
    public String getUrl() {
        if (getProduct() != null) {
            Map<String, String> params = new ParamsBuilder()
                .sku(combinedSku.getLeft())
                .variantSku(combinedSku.getRight())
                .urlKey(productRetriever.fetchProduct().getUrlKey()) // Get slug from base product
                .variantUrlKey(getProduct().getUrlKey())
                .map();

            return urlProvider.toProductUrl(request, productPage, params);
        }
        return null;
    }

    @Override
    public AbstractProductRetriever getProductRetriever() {
        return productRetriever;
    }

    @Override
    public String getImage() {
        if (getProduct() != null) {
            return getProduct().getImage().getUrl();
        }
        return null;
    }

    @Override
    public Boolean isVirtualProduct() {
        if (isVirtualProduct == null) {
            isVirtualProduct = getProduct() instanceof VirtualProduct;
        }
        return isVirtualProduct;
    }

    private SimpleProduct findVariant(ConfigurableProduct configurableProduct, String variantSku) {
        List<ConfigurableVariant> variants = configurableProduct.getVariants();
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream().map(v -> v.getProduct()).filter(sp -> variantSku.equals(sp.getSku())).findFirst().orElse(null);
    }

}
