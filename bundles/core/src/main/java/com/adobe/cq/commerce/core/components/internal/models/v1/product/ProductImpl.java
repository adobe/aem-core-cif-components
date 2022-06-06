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
package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.commerce.core.components.internal.datalayer.AssetDataImpl;
import com.adobe.cq.commerce.core.components.internal.datalayer.CategoryDataImpl;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.datalayer.ProductDataImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.internal.services.sitemap.SitemapLinkExternalizerProvider;
import com.adobe.cq.commerce.core.components.internal.storefrontcontext.ProductStorefrontContextImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.GroupItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.storefrontcontext.ProductStorefrontContext;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.BundleProduct;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableAttributeOption;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
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
import com.adobe.cq.wcm.core.components.models.Component;
import com.adobe.cq.wcm.core.components.models.datalayer.AssetData;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.adobe.cq.wcm.core.components.util.ComponentUtils.ID_SEPARATOR;

@Model(adaptables = SlingHttpServletRequest.class, adapters = Product.class, resourceType = ProductImpl.RESOURCE_TYPE)
public class ProductImpl extends DataLayerComponent implements Product {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v1/product";
    protected static final String PLACEHOLDER_DATA = "product-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductImpl.class);
    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;
    private static final String SELECTION_PROPERTY = "selection";
    /**
     * Name of the boolean policy property indicating if the product component
     * should show an add to wish list button or not.
     */
    private static final String PN_STYLE_ENABLE_ADD_TO_WISHLIST = "enableAddToWishList";
    /**
     * Name of a boolean configuration properties used by the CIF Configuration to
     * store if the endpoint has wish lists enabled.
     */
    private static final String PN_CONFIG_ENABLE_WISH_LISTS = "enableWishLists";
    /**
     * Name of a boolean configuration properties used by the CIF Configuration to
     * store if the endpoint has wish lists enabled.
     */
    protected static final Map<Section, String> SECTIONS_MAP = new EnumMap<Section, String>(Section.class) {
        {
            put(Section.TITLE, "showTitle");
            put(Section.PRICE, "showPrice");
            put(Section.SKU, "showSku");
            put(Section.IMAGE, "showImage");
            put(Section.OPTIONS, "showOptions");
            put(Section.QUANTITY, "showQuantity");
            put(Section.ACTIONS, "showActions");
            put(Section.DESCRIPTION, "showDescription");
            put(Section.DETAILS, "showDetails");

        }
    };

    @Self
    private SlingHttpServletRequest request;
    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Page currentPage;
    @OSGiService
    private UrlProvider urlProvider;
    @ScriptVariable(name = WCMBindingsConstants.NAME_CURRENT_STYLE, injectionStrategy = InjectionStrategy.OPTIONAL)
    protected ValueMap currentStyle;
    @ScriptVariable(name = "wcmmode", injectionStrategy = InjectionStrategy.OPTIONAL)
    private SightlyWCMMode wcmMode;
    @SlingObject
    private SlingScriptHelper sling;
    @OSGiService
    private XSSAPI xssApi;
    @OSGiService
    private PageManagerFactory pageManagerFactory;
    @OSGiService
    private Externalizer externalizer;

    private Boolean configurable;
    private Boolean isGroupedProduct;
    private Boolean isVirtualProduct;
    private Boolean isBundleProduct;
    private Boolean isGiftCardProduct;
    private Boolean loadClientPrice;
    private boolean usePlaceholderData = false;
    private boolean isAuthor = true;
    private String canonicalUrl;
    private boolean enableAddToWishList;

    protected AbstractProductRetriever productRetriever;

    private Locale locale;

    @PostConstruct
    protected void initModel() {
        // When the Model is created by the CatalogPageNotFoundFilter, script variables
        // will not yet be available. In this case we have to
        // initialise some fields manually, which is necessary as the Model is
        // cache=true and will not be recreated during rendering.
        if (currentPage == null) {
            currentPage = pageManagerFactory.getPageManager(request.getResourceResolver())
                .getContainingPage(request.getResource());
        }
        if (currentStyle == null) {
            currentStyle = Utils.getStyleProperties(request, resource);
        }

        ComponentsConfiguration configProperties = currentPage.getContentResource()
            .adaptTo(ComponentsConfiguration.class);
        // Get product selection from dialog
        ValueMap properties = request.getResource().getValueMap();
        String sku = properties.get(SELECTION_PROPERTY, String.class);
        isAuthor = wcmMode != null && !wcmMode.isDisabled();

        if (magentoGraphqlClient != null) {
            // If no product is selected via dialog, extract it from the URL
            if (StringUtils.isEmpty(sku)) {
                sku = urlProvider.getProductIdentifier(request);
            }

            // Load product data for component
            if (StringUtils.isNotBlank(sku)) {
                productRetriever = new ProductRetriever(magentoGraphqlClient);
                productRetriever.setIdentifier(sku);
                loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE,
                    currentStyle.get(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));
            } else if (isAuthor) {
                // In AEM Sites editor, load some dummy placeholder data for the component.
                try {
                    productRetriever = new ProductPlaceholderRetriever(magentoGraphqlClient, PLACEHOLDER_DATA);
                } catch (IOException e) {
                    LOGGER.warn("Cannot use placeholder data", e);
                }
                usePlaceholderData = true;
                loadClientPrice = false;
            }
        }

        locale = currentPage.getLanguage(false);
        enableAddToWishList = (configProperties != null
            ? configProperties.get(PN_CONFIG_ENABLE_WISH_LISTS, Boolean.TRUE)
            : Boolean.TRUE)
            && currentStyle.get(PN_STYLE_ENABLE_ADD_TO_WISHLIST, Product.super.getAddToWishListEnabled());
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
    public Boolean isGiftCardProduct() {
        if (isGiftCardProduct == null) {
            isGiftCardProduct = productRetriever != null && productRetriever.fetchProduct() instanceof GiftCardProduct;
        }
        return isGiftCardProduct;
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
        // Don't return any variants if the current product is not of type
        // ConfigurableProduct.
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
        // Don't return any variant selection properties if the current product is not
        // of type ConfigurableProduct.
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
    protected Variant mapVariant(ConfigurableVariant variant) {
        SimpleProduct product = variant.getProduct();

        VariantImpl productVariant = new VariantImpl();
        productVariant.setId(
            StringUtils.join("product", ID_SEPARATOR,
                StringUtils.substring(DigestUtils.sha256Hex(product.getSku()), 0, 10)));
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
                .sorted(Comparator
                    .comparing(a -> a.getPosition() == null ? Integer.MAX_VALUE : a.getPosition()))
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

    protected VariantValue mapVariantValue(ConfigurableProductOptionsValues value) {
        VariantValueImpl variantValue = new VariantValueImpl();
        variantValue.setId(value.getValueIndex());
        variantValue.setLabel(value.getLabel());
        String cssModifierSource = value.getDefaultLabel() != null ? value.getDefaultLabel() : value.getLabel();
        variantValue.setCssClassModifier(cssModifierSource.trim().replaceAll("\\s+", "-").toLowerCase());
        VariantValue.SwatchType swatchType = null;

        if (value.getSwatchData() != null) {
            switch (value.getSwatchData().getGraphQlTypeName()) {
                case "ImageSwatchData":
                    swatchType = VariantValue.SwatchType.IMAGE;
                    break;
                case "TextSwatchData":
                    swatchType = VariantValue.SwatchType.TEXT;
                    break;
                case "ColorSwatchData":
                    swatchType = VariantValue.SwatchType.COLOR;
                    break;
                default:
                    break;
            }
        }

        variantValue.setSwatchType(swatchType);

        return variantValue;
    }

    protected VariantAttribute mapVariantAttribute(ConfigurableProductOptions option) {
        // Get list of values
        List<VariantValue> values = option.getValues().parallelStream().map(this::mapVariantValue)
            .collect(Collectors.toList());

        // Create attribute map
        VariantAttributeImpl attribute = new VariantAttributeImpl();
        attribute.setLabel(option.getLabel());
        attribute.setId(option.getAttributeCode());
        attribute.setValues(values);

        return attribute;
    }

    protected String safeDescription(ProductInterface product) {
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
        if (usePlaceholderData) {
            // placeholder data has no canonical url
            return null;
        }
        if (canonicalUrl == null) {
            Page productPage = SiteNavigation.getProductPage(currentPage);
            ProductInterface product = productRetriever != null ? productRetriever.fetchProduct() : null;
            SitemapLinkExternalizerProvider sitemapLinkExternalizerProvider = sling
                .getService(SitemapLinkExternalizerProvider.class);

            if (productPage != null && product != null && sitemapLinkExternalizerProvider != null) {
                canonicalUrl = sitemapLinkExternalizerProvider.getExternalizer(request.getResourceResolver())
                    .toExternalProductUrl(request, productPage, new ProductUrlFormat.Params(product));
            } else {
                // fallback to the previous/legacy logic
                if (isAuthor) {
                    canonicalUrl = externalizer.authorLink(resource.getResourceResolver(), request.getRequestURI());
                } else {
                    canonicalUrl = externalizer.publishLink(resource.getResourceResolver(), request.getRequestURI());
                }
            }
        }
        return canonicalUrl;
    }

    @Override
    public Map<Locale, String> getAlternateLanguageLinks() {
        // we don't support alternate language links on products yet
        return Collections.emptyMap();
    }

    // DataLayer methods

    @Override
    public ComponentData getComponentData() {
        return new ProductDataImpl(this, resource);
    }

    @Override
    protected String generateId() {
        String id = super.generateId();
        ValueMap properties = request.getResource().getValueMap();
        if (StringUtils.isNotBlank(properties.get(Component.PN_ID, String.class))) {
            // if available use the id provided by the user
            return id;
        } else {
            // otherwise include the product SKU in the id
            String prefix = StringUtils.substringBefore(id, ID_SEPARATOR);
            String suffix = StringUtils.substringAfterLast(id, ID_SEPARATOR) + getSku();
            return ComponentUtils.generateId(prefix, suffix);
        }
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
        return new ProductStorefrontContextImpl(productRetriever.fetchProduct(), resource);
    }

    @Override
    public boolean getAddToWishListEnabled() {
        return enableAddToWishList;
    }

    @Override
    public Set<String> getVisibleSections() {
        return SECTIONS_MAP.keySet().stream().filter(k -> currentStyle.get(SECTIONS_MAP.get(k), true))
            .map(Enum::toString).collect(
                Collectors.toSet());
    }
}
