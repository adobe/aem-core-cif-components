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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v3/product";

    protected static final String PN_VISIBLE_SECTIONS = "visibleSections";

    protected String cachedJsonLD;

    public static final String PN_ENABLE_JSONLD_SCRIPT = "enableJson";

    private static final Logger LOGGER = LoggerFactory.getLogger(
        com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl.class);

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

    boolean enableJson;

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
        enableJson = configProperties != null ? configProperties.get(PN_ENABLE_JSONLD_SCRIPT, Boolean.FALSE) : Boolean.FALSE;

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

    public JSONArray fetchVariantsAsJsonArray() throws JSONException {
        List<Variant> variants = getVariants();
        JSONArray jsonArray = new JSONArray();

        if (variants == null || variants.isEmpty()) {
            return jsonArray;
        }

        for (Variant variant : variants) {
            LinkedHashMap<String, Object> variantMap = new LinkedHashMap<>();
            LinkedHashMap<String, Object> variantMapWithNoSpecialPrice = new LinkedHashMap<>();
            JSONArray assets = new JSONArray();

            for (Asset asset : variant.getAssets()) {
                JSONObject jsonAsset = new JSONObject();
                jsonAsset.put("path", asset.getPath());
                assets.put(jsonAsset);
            }

            Price priceRange = variant.getPriceRange();
            variantMap.put("@type", "Offer");
            variantMap.put("sku", variant.getSku());
            variantMap.put("url", getCanonicalUrl());
            variantMap.put("image", assets.length() > 0 ? assets.getJSONObject(0).getString("path") : "");
            variantMap.put("priceCurrency", priceRange != null ? priceRange.getCurrency() : "");

            if (variant instanceof VariantImpl) {
                VariantImpl variantImpl = (VariantImpl) variant;
                if (variantImpl.getSpecialPrice() == null && variantImpl.getSpecialToDate() == null) {
                    variantMapWithNoSpecialPrice.putAll(variantMap);
                    variantMapWithNoSpecialPrice.put("price", priceRange != null ? priceRange.getRegularPrice() : 0);
                    jsonArray.put(new JSONObject(variantMapWithNoSpecialPrice));
                } else {
                    variantMap.put("availability", variant.getInStock() ? "InStock" : "OutOfStock");

                    JSONObject priceSpecification = new JSONObject();
                    priceSpecification.put("@type", "UnitPriceSpecification");
                    priceSpecification.put("priceType", "https://schema.org/ListPrice");
                    if (priceRange != null) {
                        priceSpecification.put("price", priceRange.getRegularPrice());
                        priceSpecification.put("priceCurrency", priceRange.getCurrency());
                    }
                    variantMap.put("priceSpecification", priceSpecification);

                    variantMap.put("price", variantImpl.getSpecialPrice());
                    variantMap.put("SpecialPricedates", variantImpl.getSpecialToDate());

                    jsonArray.put(new JSONObject(variantMap));
                }
            }
        }

        return jsonArray;
    }

    public boolean isEnableJson() {
        return enableJson;
    }

    @Override
    public String generateProductJsonLDString() {
        if (!isEnableJson()) {
            return null;
        }

        if (cachedJsonLD != null) {
            return cachedJsonLD;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode productJson = createBasicProductJson(mapper);

            addOffersToJson(productJson, mapper);

            cachedJsonLD = mapper.writeValueAsString(productJson);
            return cachedJsonLD;

        } catch (JsonProcessingException | JSONException e) {
            LOGGER.warn("Failed to serialize product JSON-LD", e);
            return null;
        }
    }

    protected ObjectNode createBasicProductJson(ObjectMapper mapper) {
        ObjectNode productJson = mapper.createObjectNode();

        // Set basic product attributes
        productJson.put("@context", "http://schema.org");
        productJson.put("@type", "Product");
        productJson.put("sku", Optional.ofNullable(getSku()).orElse(""));
        productJson.put("name", Optional.ofNullable(getName()).orElse(""));
        productJson.put("image", getAssets().stream().findFirst().map(Asset::getPath).orElse(""));
        productJson.put("description", Optional.ofNullable(getDescription()).orElse(""));
        productJson.put("@id", Optional.ofNullable(getId()).orElse(""));

        return productJson;
    }

    protected void addOffersToJson(ObjectNode productJson, ObjectMapper mapper) throws JSONException, JsonProcessingException {
        ArrayNode offersArray = mapper.createArrayNode();
        JSONArray offers = fetchVariantsAsJsonArray();

        if (offers != null) {
            for (int i = 0; i < offers.length(); i++) {
                offersArray.add(mapper.readTree(offers.get(i).toString()));
            }
        }

        productJson.set("offers", offersArray);
    }

}
