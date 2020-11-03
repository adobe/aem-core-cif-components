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

package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.datalayer.ProductDataImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.GroupItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.magento.graphql.BundleProduct;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableAttributeOption;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.GroupedProductItem;
import com.adobe.cq.commerce.magento.graphql.MediaGalleryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductStockStatus;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.VirtualProduct;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Product.class,
    resourceType = ProductImpl.RESOURCE_TYPE,
    cache = true)
public class ProductImpl extends DataLayerComponent implements Product {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v1/product";
    protected static final String PLACEHOLDER_DATA = "product-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductImpl.class);
    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Page currentPage;

    @Inject
    private UrlProvider urlProvider;

    @ScriptVariable
    private Style currentStyle;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable(name = "wcmmode")
    private SightlyWCMMode wcmMode;

    @Inject
    private XSSAPI xssApi;

    @Inject
    private Externalizer externalizer;

    private Boolean configurable;
    private Boolean isGroupedProduct;
    private Boolean isVirtualProduct;
    private Boolean isBundleProduct;
    private Boolean loadClientPrice;
    private String canonicalUrl;

    private AbstractProductRetriever productRetriever;

    private Locale locale;

    @PostConstruct
    private void initModel() {

        // Parse identifier in URL
        Pair<ProductIdentifierType, String> identifier = urlProvider.getProductIdentifier(request);

        locale = currentPage.getLanguage(false);

        // Get MagentoGraphqlClient from the resource.
        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage);
        if (magentoGraphqlClient != null) {
            if (identifier != null && StringUtils.isNotBlank(identifier.getRight())) {
                productRetriever = new ProductRetriever(magentoGraphqlClient);
                productRetriever.setIdentifier(identifier.getLeft(), identifier.getRight());
                loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE, currentStyle.get(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));
            } else if (!wcmMode.isDisabled()) {
                // In AEM Sites editor, load some dummy placeholder data for the component.
                try {
                    productRetriever = new ProductPlaceholderRetriever(magentoGraphqlClient, PLACEHOLDER_DATA);
                } catch (IOException e) {
                    LOGGER.warn("Cannot use placeholder data", e);
                }
                loadClientPrice = false;
            }
        }

        if (!wcmMode.isDisabled()) {
            canonicalUrl = externalizer.authorLink(resource.getResourceResolver(), request.getRequestURI());
        } else {
            canonicalUrl = externalizer.publishLink(resource.getResourceResolver(), request.getRequestURI());
        }
    }

    @Override
    public Boolean getFound() {
        return productRetriever != null && productRetriever.fetchProduct() != null;
    }

    @Override
    public String getName() {
        return productRetriever.fetchProduct().getName();
    }

    @Override
    public String getDescription() {
        return safeDescription(productRetriever.fetchProduct());
    }

    @Override
    public String getSku() {
        return productRetriever.fetchProduct().getSku();
    }

    @Override
    public String getCurrency() {
        return getPriceRange().getCurrency();
    }

    @Override
    public Double getPrice() {
        return getPriceRange().getFinalPrice();
    }

    @Override
    public Price getPriceRange() {
        return new PriceImpl(productRetriever.fetchProduct().getPriceRange(), locale);
    }

    @Override
    public Boolean getInStock() {
        return ProductStockStatus.IN_STOCK.equals(productRetriever.fetchProduct().getStockStatus());
    }

    @Override
    public Boolean isConfigurable() {
        if (configurable == null) {
            configurable = productRetriever != null && productRetriever.fetchProduct() instanceof ConfigurableProduct;
        }
        return configurable;
    }

    @Override
    public Boolean isGroupedProduct() {
        if (isGroupedProduct == null) {
            isGroupedProduct = productRetriever != null && productRetriever.fetchProduct() instanceof GroupedProduct;
        }
        return isGroupedProduct;
    }

    @Override
    public Boolean isVirtualProduct() {
        if (isVirtualProduct == null) {
            isVirtualProduct = productRetriever != null && productRetriever.fetchProduct() instanceof VirtualProduct;
        }
        return isVirtualProduct;
    }

    @Override
    public Boolean isBundleProduct() {
        if (isBundleProduct == null) {
            isBundleProduct = productRetriever != null && productRetriever.fetchProduct() instanceof BundleProduct;
        }
        return isBundleProduct;
    }

    @Override
    public String getVariantsJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(getVariants());
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not serialize product variants");
            return "[]";
        }
    }

    @Override
    public List<Variant> getVariants() {
        // Don't return any variants if the current product is not of type ConfigurableProduct.
        if (!isConfigurable()) {
            return Collections.emptyList();
        }
        ConfigurableProduct product = (ConfigurableProduct) productRetriever.fetchProduct();

        return product.getVariants().parallelStream().map(this::mapVariant).collect(Collectors.toList());
    }

    @Override
    public List<GroupItem> getGroupedProductItems() {
        // Don't return any items if the current product is not of type GroupedProduct.
        if (!isGroupedProduct()) {
            return Collections.emptyList();
        }
        GroupedProduct product = (GroupedProduct) productRetriever.fetchProduct();

        return product.getItems()
            .parallelStream()
            .sorted(Comparator.comparing(GroupedProductItem::getPosition))
            .map(this::mapGroupedProductItem)
            .collect(Collectors.toList());
    }

    @Override
    public List<Asset> getAssets() {
        return filterAndSortAssets(productRetriever.fetchProduct().getMediaGallery());
    }

    @Override
    public String getAssetsJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(getAssets());
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            return "";
        }
    }

    @Override
    public List<VariantAttribute> getVariantAttributes() {
        // Don't return any variant selection properties if the current product is not of type ConfigurableProduct.
        if (!isConfigurable()) {
            return Collections.emptyList();
        }

        ConfigurableProduct product = (ConfigurableProduct) productRetriever.fetchProduct();

        List<VariantAttribute> optionList = new ArrayList<>();
        for (ConfigurableProductOptions option : product.getConfigurableOptions()) {
            optionList.add(mapVariantAttribute(option));
        }

        return optionList;
    }

    @Override
    public Boolean loadClientPrice() {
        return loadClientPrice && !LaunchUtils.isLaunchBasedPath(currentPage.getPath());
    }

    @Override
    public String getFormattedPrice() {
        return getPriceRange().getFormattedFinalPrice();
    }

    @Override
    public AbstractProductRetriever getProductRetriever() {
        return productRetriever;
    }

    /* --- Mapping methods --- */
    private Variant mapVariant(ConfigurableVariant variant) {
        SimpleProduct product = variant.getProduct();

        VariantImpl productVariant = new VariantImpl();
        productVariant.setId(
            StringUtils.join("product", ID_SEPARATOR, StringUtils.substring(DigestUtils.sha256Hex(product.getSku()), 0, 10)));
        productVariant.setName(product.getName());
        productVariant.setDescription(safeDescription(product));
        productVariant.setSku(product.getSku());
        productVariant.setColor(product.getColor());
        productVariant.setPriceRange(new PriceImpl(product.getPriceRange(), locale));
        productVariant.setInStock(ProductStockStatus.IN_STOCK.equals(product.getStockStatus()));

        // Map variant attributes
        for (ConfigurableAttributeOption option : variant.getAttributes()) {
            productVariant.getVariantAttributes().put(option.getCode(), option.getValueIndex());
        }

        List<Asset> assets = filterAndSortAssets(product.getMediaGallery());
        productVariant.setAssets(assets);

        return productVariant;
    }

    private GroupItem mapGroupedProductItem(com.adobe.cq.commerce.magento.graphql.GroupedProductItem item) {
        ProductInterface product = item.getProduct();

        GroupItemImpl groupedProductItem = new GroupItemImpl();
        groupedProductItem.setName(product.getName());
        groupedProductItem.setSku(product.getSku());
        groupedProductItem.setPriceRange(new PriceImpl(product.getPriceRange(), locale));
        groupedProductItem.setDefaultQuantity(item.getQty());
        groupedProductItem.setVirtualProduct(product instanceof VirtualProduct);

        return groupedProductItem;
    }

    private List<Asset> filterAndSortAssets(List<MediaGalleryInterface> assets) {
        return assets == null ? Collections.emptyList()
            : assets.parallelStream()
                .filter(a -> (a.getDisabled() == null || !a.getDisabled()) && a instanceof ProductImage)
                .map(this::mapAsset)
                .sorted(Comparator.comparing(a -> a.getPosition() == null ? Integer.MAX_VALUE : a.getPosition()))
                .collect(Collectors.toList());
    }

    private Asset mapAsset(MediaGalleryInterface entry) {
        AssetImpl asset = new AssetImpl();
        asset.setLabel(entry.getLabel());
        asset.setPosition(entry.getPosition());
        asset.setType((entry instanceof ProductImage) ? "image" : "video");
        asset.setPath(entry.getUrl());

        return asset;
    }

    private VariantValue mapVariantValue(ConfigurableProductOptionsValues value) {
        VariantValueImpl variantValue = new VariantValueImpl();
        variantValue.setId(value.getValueIndex());
        variantValue.setLabel(value.getLabel());

        return variantValue;
    }

    private VariantAttribute mapVariantAttribute(ConfigurableProductOptions option) {
        // Get list of values
        List<VariantValue> values = option.getValues().parallelStream().map(this::mapVariantValue).collect(Collectors.toList());

        // Create attribute map
        VariantAttributeImpl attribute = new VariantAttributeImpl();
        attribute.setLabel(option.getLabel());
        attribute.setId(option.getAttributeCode());
        attribute.setValues(values);

        return attribute;
    }

    private String safeDescription(ProductInterface product) {
        ComplexTextValue description = product.getDescription();
        if (description == null) {
            return null;
        }

        // Filter HTML
        return xssApi.filterHTML(description.getHtml());
    }

    @Override
    public String getMetaDescription() {
        return productRetriever.fetchProduct().getMetaDescription();
    }

    @Override
    public String getMetaKeywords() {
        return productRetriever.fetchProduct().getMetaKeyword();
    }

    @Override
    public String getMetaTitle() {
        return StringUtils.defaultString(productRetriever.fetchProduct().getMetaTitle(), getName());
    }

    @Override
    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    // DataLayer methods

    @Override
    public ComponentData getComponentData() {
        return new ProductDataImpl(this, resource);
    }

    @Override
    protected String generateId() {
        return StringUtils.join("product", ID_SEPARATOR, StringUtils.substring(DigestUtils.sha256Hex(getSku()), 0, 10));
    }

    @Override
    public String getDataLayerTitle() {
        return this.getName();
    }

    @Override
    public String getDataLayerSKU() {
        return this.getSku();
    }

    @Override
    public Double getDataLayerPrice() {
        return this.getPrice();
    }

    @Override
    public String getDataLayerCurrency() {
        return this.getCurrency();
    }

    @Override
    public String getDataLayerDescription() {
        return this.getDescription();
    }
}
