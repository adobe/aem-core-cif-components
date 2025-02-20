/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v3.product;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantAttributeImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantValueImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.*;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.magento.graphql.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Product.class,
    resourceType = ProductImpl.RESOURCE_TYPE)
public class ProductImpl extends com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImpl
    implements Product {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductImpl.class);

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v3/product";

    protected static final String PN_VISIBLE_SECTIONS = "visibleSections";

    private String cachedJsonLd;

    public static final String PN_ENABLE_JSONLD_SCRIPT = "enableJsonLd";

    protected static final Map<String, String> SECTIONS_MAP = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put("title", TITLE_SECTION);
            put("price", PRICE_SECTION);
            put("sku", SKU_SECTION);
            put("images", IMAGE_SECTION);
            put("options", OPTIONS_SECTION);
            put("quantity", QUANTITY_SECTION);
            put("actions", ACTIONS_SECTION);
            put("description", DESCRIPTION_SECTION);
            put("details", DETAILS_SECTION);
        }
    });

    private static final String[] VISIBLE_SECTIONS_DEFAULT = SECTIONS_MAP.keySet().toArray(new String[0]);

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String[] visibleSections;

    private Set<String> visibleSectionsSet;

    private boolean enableJsonLd;

    @PostConstruct
    protected void initModel() {
        super.initModel();
        Resource contentResource = currentPage.getContentResource();
        ComponentsConfiguration configProperties = contentResource.adaptTo(ComponentsConfiguration.class);

        if (productRetriever != null) {
            productRetriever.extendProductQueryWith(p -> p.onConfigurableProduct(cp -> cp
                .configurableOptions(o -> o
                    .values(v -> v.uid()))
                .variants(v -> v
                    .attributes(a -> a.uid()))));
        }

        if (visibleSections == null || visibleSections.length == 0) {
            visibleSections = currentStyle.get(PN_VISIBLE_SECTIONS, VISIBLE_SECTIONS_DEFAULT);
        }
        visibleSectionsSet = Collections.unmodifiableSet(Arrays.stream(visibleSections).map(SECTIONS_MAP::get).collect(Collectors.toSet()));
        enableJsonLd = configProperties != null ? configProperties.get(PN_ENABLE_JSONLD_SCRIPT, Boolean.FALSE) : Boolean.FALSE;

    }

    @Override
    protected VariantValue mapVariantValue(ConfigurableProductOptionsValues value) {
        VariantValueImpl variantValue = new VariantValueImpl();
        variantValue.setId(value.getValueIndex());
        variantValue.setUid(value.getUid().toString());
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

    @Override
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

    @Override
    protected Variant mapVariant(ConfigurableVariant variant) {
        Variant mappedVariant = super.mapVariant(variant);
        SimpleProduct product = variant.getProduct();

        // Map variant attributes
        for (ConfigurableAttributeOption option : variant.getAttributes()) {
            mappedVariant.getVariantAttributesUid().put(option.getCode(), option.getUid().toString());
        }
        if (product.getSpecialPrice() != null) {
            ((VariantImpl) mappedVariant).setSpecialPrice(product.getSpecialPrice());
        }

        if (product.getSpecialToDate() != null) {
            ((VariantImpl) mappedVariant).setSpecialToDate(product.getSpecialToDate());
        }

        return mappedVariant;
    }

    @Override
    public Set<String> getVisibleSections() {
        return visibleSectionsSet;
    }

    private ArrayNode fetchVariantsAsJsonArray() throws JsonProcessingException {
        List<Variant> variants = getVariants();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonArray = mapper.createArrayNode();

        if (variants == null || variants.isEmpty()) {
            return jsonArray;
        }

        for (Variant variant : variants) {
            ObjectNode variantMap = mapper.createObjectNode();
            ObjectNode variantMapWithNoSpecialPrice = mapper.createObjectNode();
            ArrayNode assets = mapper.createArrayNode();

            for (Asset asset : variant.getAssets()) {
                ObjectNode jsonAsset = mapper.createObjectNode();
                jsonAsset.put("path", StringEscapeUtils.escapeHtml4(asset.getPath()));
                assets.add(jsonAsset);
            }

            Price priceRange = variant.getPriceRange();
            variantMap.put("@type", "Offer");
            variantMap.put("sku", StringEscapeUtils.escapeHtml4(variant.getSku()));
            variantMap.put("url", StringEscapeUtils.escapeHtml4(getCanonicalUrl()));
            variantMap.put("image", assets.size() > 0 ? assets.get(0).get("path").asText() : "");
            variantMap.put("priceCurrency", priceRange != null ? priceRange.getCurrency() : "");

            if (variant instanceof VariantImpl) {
                VariantImpl variantImpl = (VariantImpl) variant;
                if (variantImpl.getSpecialPrice() == null && variantImpl.getSpecialToDate() == null) {
                    variantMapWithNoSpecialPrice.setAll(variantMap);
                    variantMapWithNoSpecialPrice.put("price", priceRange != null ? priceRange.getRegularPrice() : 0);
                    jsonArray.add(variantMapWithNoSpecialPrice);
                } else {
                    variantMap.put("availability", variant.getInStock() ? "InStock" : "OutOfStock");

                    ObjectNode priceSpecification = mapper.createObjectNode();
                    priceSpecification.put("@type", "UnitPriceSpecification");
                    priceSpecification.put("priceType", "https://schema.org/ListPrice");
                    if (priceRange != null) {
                        priceSpecification.put("price", priceRange.getRegularPrice());
                        priceSpecification.put("priceCurrency", priceRange.getCurrency());
                    }
                    variantMap.set("priceSpecification", priceSpecification);

                    variantMap.put("price", variantImpl.getSpecialPrice());
                    variantMap.put("SpecialPricedates", StringEscapeUtils.escapeHtml4(variantImpl.getSpecialToDate()));

                    jsonArray.add(variantMap);
                }
            }
        }

        return jsonArray;
    }

    @Override
    public String getJsonLd() {
        if (!enableJsonLd) {
            return null;
        }

        if (cachedJsonLd != null) {
            return cachedJsonLd;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode productJson = createBasicProductJson(mapper);

            addOffersToJson(productJson, mapper);

            cachedJsonLd = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(productJson);
            return cachedJsonLd;

        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize product JSON-LD", e);
            return null;
        }
    }

    private ObjectNode createBasicProductJson(ObjectMapper mapper) {
        ObjectNode productJson = mapper.createObjectNode();

        // Set basic product attributes
        productJson.put("@context", "http://schema.org");
        productJson.put("@type", "Product");
        productJson.put("sku", StringEscapeUtils.escapeHtml4(Optional.ofNullable(getSku()).orElse("")));
        productJson.put("name", StringEscapeUtils.escapeHtml4(Optional.ofNullable(getName()).orElse("")));
        productJson.put("image", StringEscapeUtils.escapeHtml4(getAssets().stream().findFirst().map(Asset::getPath).orElse("")));
        productJson.put("description", StringEscapeUtils.escapeHtml4(Optional.ofNullable(getDescription()).orElse("")));
        productJson.put("@id", StringEscapeUtils.escapeHtml4(Optional.ofNullable(getId()).orElse("")));

        return productJson;
    }

    private void addOffersToJson(ObjectNode productJson, ObjectMapper mapper) throws JsonProcessingException {
        ArrayNode offersArray = mapper.createArrayNode();
        ArrayNode offers = fetchVariantsAsJsonArray();

        if (offers != null) {
            for (int i = 0; i < offers.size(); i++) {
                offersArray.add(offers.get(i));
            }
        }

        productJson.set("offers", offersArray);
    }
}
