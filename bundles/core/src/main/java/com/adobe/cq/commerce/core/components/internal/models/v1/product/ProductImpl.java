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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableAttributeOption;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.MediaGalleryEntry;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductStockStatus;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Product.class,
    resourceType = ProductImpl.RESOURCE_TYPE)
public class ProductImpl implements Product {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v1/product";
    protected static final String PLACEHOLDER_DATA = "/product-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductImpl.class);
    private static final String PRODUCT_IMAGE_FOLDER = "catalog/product";

    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @ScriptVariable
    private Style currentStyle;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable(name = "wcmmode")
    private SightlyWCMMode wcmMode;

    @Inject
    private XSSAPI xssApi;

    private NumberFormat priceFormatter;
    private Boolean configurable;
    private Boolean loadClientPrice;

    private AbstractProductRetriever productRetriever;

    @PostConstruct
    private void initModel() {
        // Parse slug from URL
        String slug = parseProductSlug();

        // Get MagentoGraphqlClient from the resource.
        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource);

        if (StringUtils.isNotBlank(slug)) {
            productRetriever = new ProductRetriever(magentoGraphqlClient);
            productRetriever.setIdentifier(slug);
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

    @Override
    public Boolean getFound() {
        return productRetriever.fetchProduct() != null;
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
        return productRetriever.fetchProduct().getPrice().getRegularPrice().getAmount().getCurrency().toString();
    }

    @Override
    public Double getPrice() {
        return productRetriever.fetchProduct().getPrice().getRegularPrice().getAmount().getValue();
    }

    @Override
    public Boolean getInStock() {
        return ProductStockStatus.IN_STOCK.equals(productRetriever.fetchProduct().getStockStatus());
    }

    @Override
    public Boolean isConfigurable() {
        if (configurable == null) {
            configurable = productRetriever.fetchProduct() instanceof ConfigurableProduct;
        }

        return configurable;
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
        // Don't return any variants if the current product
        // is not of type ConfigurableProduct.
        if (!isConfigurable()) {
            return Collections.emptyList();
        }
        ConfigurableProduct product = (ConfigurableProduct) productRetriever.fetchProduct();

        return product.getVariants().parallelStream().map(this::mapVariant).collect(Collectors.toList());
    }

    @Override
    public List<Asset> getAssets() {
        return filterAndSortAssets(productRetriever.fetchProduct().getMediaGalleryEntries());
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
        // Don't return any variant selection properties if the current
        // product is not of type ConfigurableProduct.
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
        return loadClientPrice;
    }

    @Override
    public String getFormattedPrice() {
        return getPriceFormatter().format(getPrice());
    }

    @Override
    public AbstractProductRetriever getProductRetriever() {
        return productRetriever;
    }

    /* --- Mapping methods --- */

    private Variant mapVariant(ConfigurableVariant variant) {
        SimpleProduct product = variant.getProduct();

        VariantImpl productVariant = new VariantImpl();
        productVariant.setName(product.getName());
        productVariant.setDescription(safeDescription(product));
        productVariant.setSku(product.getSku());
        productVariant.setColor(product.getColor());
        productVariant.setCurrency(product.getPrice().getRegularPrice().getAmount().getCurrency().toString());
        productVariant.setPrice(product.getPrice().getRegularPrice().getAmount().getValue());
        productVariant.setFormattedPrice(getPriceFormatter().format(productVariant.getPrice()));
        productVariant.setInStock(ProductStockStatus.IN_STOCK.equals(product.getStockStatus()));

        // Map variant attributes
        for (ConfigurableAttributeOption option : variant.getAttributes()) {
            productVariant.getVariantAttributes().put(option.getCode(), option.getValueIndex());
        }

        List<Asset> assets = filterAndSortAssets(product.getMediaGalleryEntries());
        productVariant.setAssets(assets);

        return productVariant;
    }

    private List<Asset> filterAndSortAssets(List<MediaGalleryEntry> assets) {
        return assets.parallelStream()
            .filter(e -> !e.getDisabled() && e.getMediaType().equals("image"))
            .map(this::mapAsset)
            .sorted(Comparator.comparing(Asset::getPosition))
            .collect(Collectors.toList());
    }

    private Asset mapAsset(MediaGalleryEntry entry) {
        AssetImpl asset = new AssetImpl();
        asset.setLabel(entry.getLabel());
        asset.setPosition(entry.getPosition());
        asset.setType(entry.getMediaType());

        // TODO WORKAROUND
        // Magento media gallery only provides that file path but not a full image url yet, we need the mediaBaseUrl
        // from the storeConfig to construct the full image url
        asset.setPath(productRetriever.fetchMediaBaseUrl() + PRODUCT_IMAGE_FOLDER + entry.getFile());

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

    /* --- Utility methods --- */

    /**
     * Returns the selector of the current request which is expected to be the
     * product slug.
     *
     * @return product slug
     */
    private String parseProductSlug() {
        return request.getRequestPathInfo().getSelectorString();
    }

    private NumberFormat getPriceFormatter() {
        if (priceFormatter == null) {
            // Initialize NumberFormatter with locale from current page.
            // Alternatively, the locale can potentially be retrieved via
            // the storeConfig query introduced with Magento 2.3.1
            Locale locale = currentPage.getLanguage(false);
            priceFormatter = Utils.buildPriceFormatter(locale, productRetriever.fetchProduct() != null ? getCurrency() : null);
        }
        return priceFormatter;
    }

    private String safeDescription(ProductInterface product) {
        ComplexTextValue description = product.getDescription();
        if (description == null) {
            return null;
        }

        // Filter HTML
        return xssApi.filterHTML(description.getHtml());
    }

}
