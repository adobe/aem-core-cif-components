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
package com.adobe.cq.commerce.core.components.internal.models.v1.productteaser;

import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.datalayer.ProductDataImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.CommerceIdentifierImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.VirtualProduct;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { ProductTeaser.class, ComponentExporter.class },
    resourceType = ProductTeaserImpl.RESOURCE_TYPE)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ProductTeaserImpl extends DataLayerComponent implements ProductTeaser {
    static final String CALL_TO_ACTION_TYPE_DETAILS = "details";
    static final String CALL_TO_ACTION_TYPE_ADD_TO_CART = "add-to-cart";
    static final String CALL_TO_ACTION_TEXT_ADD_TO_CART = "Add to Cart";

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productteaser/v1/productteaser";
    protected static final String PN_STYLE_ADD_TO_WISHLIST_ENABLED = "enableAddToWishList";
    protected static final String PN_STYLE_LOAD_PRICES_CLIENTSIDE = "loadClientPrice";
    private static final String PN_CONFIG_ENABLE_WISH_LISTS = "enableWishLists";

    private static final String SELECTION_PROPERTY = "selection";

    @Self
    private SlingHttpServletRequest request;
    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;
    @ScriptVariable
    private Page currentPage;
    @OSGiService
    private UrlProvider urlProvider;
    @Self
    @Via("resource")
    private ValueMap properties;
    @ValueMapValue(
        name = "cta",
        injectionStrategy = InjectionStrategy.OPTIONAL)
    private String cta;
    @ValueMapValue(
        name = "ctaText",
        injectionStrategy = InjectionStrategy.OPTIONAL)
    private String ctaText;
    @ValueMapValue(
        name = Link.PN_LINK_TARGET,
        injectionStrategy = InjectionStrategy.OPTIONAL)
    private String linkTarget;
    @ScriptVariable(name = WCMBindingsConstants.NAME_CURRENT_STYLE)
    private Style currentStyle;

    private CombinedSku combinedSku;
    private AbstractProductRetriever productRetriever;

    private Locale locale;
    private Boolean isVirtualProduct;
    private boolean ctaOverride;
    private boolean enableAddToWishList;
    private boolean loadPriceClientSide;

    @PostConstruct
    protected void initModel() {
        locale = currentPage.getLanguage(false);

        ComponentsConfiguration configProperties = currentPage.getContentResource().adaptTo(ComponentsConfiguration.class);

        String selection = properties.get(SELECTION_PROPERTY, String.class);
        if (selection != null && !selection.isEmpty()) {
            if (selection.startsWith("/")) {
                selection = StringUtils.substringAfterLast(selection, "/");
            }
            combinedSku = CombinedSku.parse(selection);

            // Fetch product data
            if (magentoGraphqlClient != null) {
                productRetriever = new ProductRetriever(magentoGraphqlClient);
                productRetriever.setIdentifier(combinedSku.getBaseSku());
                ctaOverride = CALL_TO_ACTION_TYPE_ADD_TO_CART.equals(cta) && !Utils.isShoppableProduct(getProduct());
            }
        }

        enableAddToWishList = (configProperties != null ? configProperties.get(PN_CONFIG_ENABLE_WISH_LISTS, Boolean.TRUE) : Boolean.TRUE)
            && currentStyle.get(PN_STYLE_ADD_TO_WISHLIST_ENABLED, ProductTeaser.super.getAddToWishListEnabled());
        loadPriceClientSide = currentStyle.get(PN_STYLE_LOAD_PRICES_CLIENTSIDE, Boolean.FALSE);
    }

    @JsonIgnore
    private ProductInterface getProduct() {
        if (productRetriever == null) {
            return null;
        }

        ProductInterface baseProduct = productRetriever.fetchProduct();
        if (combinedSku.getVariantSku() != null && baseProduct instanceof ConfigurableProduct) {
            ConfigurableProduct configurableProduct = (ConfigurableProduct) baseProduct;
            SimpleProduct variant = findVariant(configurableProduct, combinedSku.getVariantSku());
            if (variant != null) {
                return variant;
            }
        }
        return baseProduct;
    }

    @Override
    public CommerceIdentifier getCommerceIdentifier() {
        if (getSku() != null) {
            return new CommerceIdentifierImpl(getSku(), CommerceIdentifier.IdentifierType.SKU, CommerceIdentifier.EntityType.PRODUCT);
        }
        return null;
    }

    @Override
    public String getName() {
        if (getProduct() != null) {
            return getProduct().getName();
        }
        return null;
    }

    @Override
    @JsonIgnore
    public String getSku() {
        return combinedSku != null ? StringUtils.defaultIfEmpty(combinedSku.getVariantSku(), combinedSku.getBaseSku()) : null;
    }

    @Override
    @JsonIgnore
    public CombinedSku getCombinedSku() {
        return combinedSku;
    }

    @Override
    public String getCallToAction() {
        if (ctaOverride) {
            return CALL_TO_ACTION_TYPE_DETAILS;
        }

        return cta;
    }

    @Override
    public String getCallToActionText() {
        if (ctaOverride && StringUtils.isBlank(ctaText)) {
            return CALL_TO_ACTION_TEXT_ADD_TO_CART;
        }

        return ctaText;
    }

    @Override
    @JsonIgnore
    public Price getPriceRange() {
        if (getProduct() != null) {
            return new PriceImpl(getProduct().getPriceRange(), locale);
        }
        return null;
    }

    @Override
    @JsonIgnore
    public String getUrl() {
        if (getProduct() != null) {
            ProductUrlFormat.Params params = new ProductUrlFormat.Params();
            params.setSku(combinedSku.getBaseSku());
            params.setVariantSku(combinedSku.getVariantSku());
            // Get slug from base product
            params.setUrlKey(productRetriever.fetchProduct().getUrlKey());
            params.setUrlPath(productRetriever.fetchProduct().getUrlPath());
            params.setUrlRewrites(productRetriever.fetchProduct().getUrlRewrites());
            params.setVariantUrlKey(getProduct().getUrlKey());

            return urlProvider.toProductUrl(request, currentPage, params);
        }
        return null;
    }

    @Override
    @JsonIgnore
    public String getLinkTarget() {
        return Utils.normalizeLinkTarget(linkTarget);
    }

    @Override
    @JsonIgnore
    public AbstractProductRetriever getProductRetriever() {
        return productRetriever;
    }

    @Override
    @JsonIgnore
    public String getImage() {
        if (getProduct() != null) {
            return getProduct().getImage().getUrl();
        }
        return null;
    }

    @Override
    @JsonIgnore
    public String getImageAlt() {
        ProductInterface product = getProduct();
        if (product != null) {
            return StringUtils.defaultIfBlank(product.getImage().getLabel(), product.getName());
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

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }

    @Override
    @JsonIgnore
    public boolean getAddToWishListEnabled() {
        return enableAddToWishList;
    }

    // DataLayer methods

    @Override
    protected ComponentData getComponentData() {
        return new ProductDataImpl(this, resource);
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
        return getPriceRange() != null ? getPriceRange().getFinalPrice() : null;
    }

    @Override
    public String getDataLayerCurrency() {
        return getPriceRange() != null ? getPriceRange().getCurrency() : null;
    }

    @Override
    @JsonIgnore
    public boolean loadClientPrice() {
        return loadPriceClientSide;
    }
}
