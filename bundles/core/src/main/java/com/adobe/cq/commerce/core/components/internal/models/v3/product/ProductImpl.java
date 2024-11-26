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
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private ObjectMapper objectMapper;

    private static final String PN_ENABLE_JSONLD_SCRIPT = "enableJson";

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

    @Inject
    private ResourceResolver resourceResolver;

    private boolean enableJson;

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
            // Assuming the special price is a Double, you can directly set it on the VariantImpl
            ((VariantImpl) mappedVariant).setSpecialPrice(product.getSpecialPrice());
        }

        if (product.getSpecialToDate() != null) {
            // Assuming specialToDate is a String, you can directly set it on the VariantImpl
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

        for (Variant variant : variants) {

            LinkedHashMap<String, Object> variantMap = new LinkedHashMap<>();
            LinkedHashMap<String, Object> variantMapWithNoSpecialPrice = new LinkedHashMap<>();
            JSONArray assets = new JSONArray();

            // Process assets
            for (Asset asset : variant.getAssets()) {
                JSONObject jsonAsset = new JSONObject();
                jsonAsset.put("path", asset.getPath());
                assets.put(jsonAsset);
            }

            Price priceRange = variant.getPriceRange();
            // Set common fields for both cases
            variantMap.put("@type", "Offer");
            variantMap.put("sku", variant.getSku());
            variantMap.put("url", getCanonicalUrl());
            variantMap.put("image", assets.length() > 0 ? assets.getJSONObject(0).getString("path") : "");
            variantMap.put("priceCurrency", priceRange != null ? priceRange.getCurrency() : "");

            // Case when there is no special price
            if (variant.getSpecialPrice() == null && variant.getSpecialToDate() == null) {
                // For variants with no special price
                variantMapWithNoSpecialPrice.putAll(variantMap);  // Copy common fields
                variantMapWithNoSpecialPrice.put("price", priceRange.getRegularPrice());  // Set regular price
                jsonArray.put(new JSONObject(variantMapWithNoSpecialPrice));  // Add to the array
            } else {
                // Case when there's a special price
                variantMap.put("availability", variant.getInStock() ? "InStock" : "OutOfStock");

                // Price specification
                JSONObject priceSpecification = new JSONObject();
                priceSpecification.put("@type", "UnitPriceSpecification");
                priceSpecification.put("priceType", "https://schema.org/ListPrice");
                if (priceRange != null) {
                    priceSpecification.put("price", priceRange.getRegularPrice());
                    priceSpecification.put("priceCurrency", priceRange.getCurrency());
                }
                variantMap.put("priceSpecification", priceSpecification);

                // Set special price and date
                variantMap.put("price", variant.getSpecialPrice());
                variantMap.put("SpecialPricedate", variant.getSpecialToDate());

                // Add the variant with special price to the array
                jsonArray.put(new JSONObject(variantMap));
            }
        }

        return jsonArray;
    }

    @Override
    public String generateProductJsonLDString() throws JsonProcessingException, JSONException {
        if (!enableJson) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode productJson = mapper.createObjectNode();

        productJson.put("@context", "http://schema.org");
        productJson.put("@type", "Product");
        productJson.put("sku", getSku());
        productJson.put("name", getName());
        productJson.put("image", getAssets().get(0).getPath());  // Assuming the first asset is the image
        productJson.put("description", getDescription());
        productJson.put("@id", getId());

        ArrayNode offersArray = mapper.createArrayNode();
        JSONArray offers = fetchVariantsAsJsonArray();
        for (int i = 0; i < offers.length(); i++) {
            offersArray.add(mapper.readTree(offers.get(i).toString()));
        }
        productJson.set("offers", offersArray);

        return mapper.writeValueAsString(productJson);
    }
}
