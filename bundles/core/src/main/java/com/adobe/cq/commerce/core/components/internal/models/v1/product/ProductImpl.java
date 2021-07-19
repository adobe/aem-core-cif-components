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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.commerce.core.components.internal.datalayer.AssetDataImpl;
import com.adobe.cq.commerce.core.components.internal.datalayer.CategoryDataImpl;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.datalayer.ProductDataImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.internal.storefrontcontext.ProductStorefrontContextImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.GiftCardAmount;
import com.adobe.cq.commerce.core.components.models.product.GiftCardAttribute;
import com.adobe.cq.commerce.core.components.models.product.GiftCardOption;
import com.adobe.cq.commerce.core.components.models.product.GiftCardValue;
import com.adobe.cq.commerce.core.components.models.product.GroupItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.storefrontcontext.ProductStorefrontContext;
import com.adobe.cq.commerce.magento.graphql.BundleProduct;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableAttributeOption;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.CustomizableFieldValue;
import com.adobe.cq.commerce.magento.graphql.CustomizableOptionInterface;
import com.adobe.cq.commerce.magento.graphql.GiftCardAmounts;
import com.adobe.cq.commerce.magento.graphql.GiftCardProduct;
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.GroupedProductItem;
import com.adobe.cq.commerce.magento.graphql.MediaGalleryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductStockStatus;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.VirtualProduct;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.wcm.core.components.models.datalayer.AssetData;
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

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v1/product";
    protected static final String PLACEHOLDER_DATA = "product-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductImpl.class);
    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;
    private static final String SELECTION_PROPERTY = "selection";

    @Self
    private SlingHttpServletRequest request;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

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
    private Boolean isGiftCardProduct;

    protected AbstractProductRetriever productRetriever;

    private Locale locale;

    @PostConstruct
    protected void initModel() {
        // Get product selection from dialog
        String sku = properties.get(SELECTION_PROPERTY, String.class);

        if (magentoGraphqlClient != null) {
            // If no product is selected via dialog, extract it from the URL
            if (StringUtils.isEmpty(sku)) {
                sku = urlProvider.getProductIdentifier(request);
            }

            // Load product data for component
            if (StringUtils.isNotBlank(sku)) {
                productRetriever = new ProductRetriever(magentoGraphqlClient);
                productRetriever.setIdentifier(sku);
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

        locale = currentPage.getLanguage(false);

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
        return this.getPriceRange() != null ? this.getPriceRange().getFinalPrice() : null;
    }

    @Override
    public String getDataLayerCurrency() {
        return this.getPriceRange() != null ? this.getPriceRange().getCurrency() : null;
    }

    @Override
    public String getDataLayerDescription() {
        return this.getDescription();
    }

    @Override
    public CategoryData[] getDataLayerCategories() {
        List<CategoryInterface> productCategories = productRetriever.fetchProduct().getCategories();

        if (productCategories == null || productCategories.size() == 0) {
            return new CategoryData[0];
        }

        return productRetriever.fetchProduct().getCategories()
            .stream()
            .map(c -> new CategoryDataImpl(c.getUid().toString(), c.getName(), c.getImage()))
            .toArray(CategoryData[]::new);
    }

    @Override
    public AssetData[] getDataLayerAssets() {
        return getAssets().stream().map(AssetDataImpl::new).toArray(AssetData[]::new);
    }

    @Override
    public ProductStorefrontContext getStorefrontContext() {
        return new ProductStorefrontContextImpl(productRetriever.fetchProduct());
    }

    @Override
    public GiftCardAttribute getGiftCardAttributes() {
        if (!isGiftCardProduct) {
            return null;
        }

        GiftCardProduct giftCardProduct = (GiftCardProduct) productRetriever.fetchProduct();

        GiftCardAttributeImpl giftCardAttribute = new GiftCardAttributeImpl();
        giftCardAttribute.setAllowOpenAmount(giftCardProduct.getAllowOpenAmount());
        giftCardAttribute.setGiftCardType(giftCardProduct.getGiftcardType());
        giftCardAttribute.setOpenAmountMax(giftCardProduct.getOpenAmountMax());
        giftCardAttribute.setOpenAmountMin(giftCardProduct.getOpenAmountMin());
        giftCardAttribute.setGiftCardOptions(mapGiftCardOptions(giftCardProduct.getGiftCardOptions()));
        giftCardAttribute.setGiftCardAmount(mapGiftCardAmount(giftCardProduct.getGiftcardAmounts()));
        giftCardAttribute.setOpenAmountRange(getGiftCardPrice(giftCardProduct));

        return giftCardAttribute;
    }

    private Map mapGiftCardOptions(List<CustomizableOptionInterface> optionList) {
        Map<String, GiftCardOption> giftCardOptionMap = new HashMap<String, GiftCardOption>();

        optionList.stream().forEach(coi -> {
            GiftCardOptionImpl giftCardOption = new GiftCardOptionImpl();
            CustomizableFieldValue cfv = (CustomizableFieldValue) coi.get("value");
            giftCardOption.setTitle(coi.getTitle());
            giftCardOption.setValue(this.mapGiftCardValue(cfv));

            giftCardOptionMap.put(coi.getTitle(), giftCardOption);

        });
        return giftCardOptionMap;
    }

    private List<GiftCardAmount> mapGiftCardAmount(List<GiftCardAmounts> giftCardAmountList) {
        List<GiftCardAmount> amountList = new ArrayList<GiftCardAmount>();

        giftCardAmountList.stream().forEach(gca -> {
            GiftCardAmountImpl giftCardAmount = new GiftCardAmountImpl();
            giftCardAmount.setUid(gca.getUid().toString());
            giftCardAmount.setValue(gca.getValue());
            amountList.add(giftCardAmount);
        });
        return amountList;
    }

    private GiftCardValue mapGiftCardValue(CustomizableFieldValue value) {
        GiftCardValueImpl giftCardValue = new GiftCardValueImpl();
        giftCardValue.setUid(value.getUid().toString());

        return giftCardValue;
    }

    private Price getGiftCardPrice(GiftCardProduct giftCardProduct) {
        return new PriceImpl(giftCardProduct.getPriceRange(), locale, giftCardProduct.getAllowOpenAmount(), giftCardProduct
            .getOpenAmountMin(),
            giftCardProduct.getOpenAmountMax());
    }

    @Override
    public Boolean isGiftCardProduct() {
        if (isGiftCardProduct == null) {
            isGiftCardProduct = productRetriever != null && productRetriever.fetchProduct() instanceof GiftCardProduct;
        }
        return isGiftCardProduct;
    }
}
