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

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantAttributeImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantValueImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.*;
import com.adobe.cq.commerce.magento.graphql.ConfigurableAttributeOption;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Product.class,
    resourceType = ProductImpl.RESOURCE_TYPE)
public class ProductImpl extends com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImpl
    implements Product {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v3/product";

    protected static final String PN_VISIBLE_SECTIONS = "visibleSections";

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

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Page currentPage;

    @Inject
    private ResourceResolver resourceResolver;

    @PostConstruct
    protected void initModel() {
        super.initModel();

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

        // Map variant attributes
        for (ConfigurableAttributeOption option : variant.getAttributes()) {
            mappedVariant.getVariantAttributesUid().put(option.getCode(), option.getUid().toString());
        }

        return mappedVariant;
    }

    @Override
    public Set<String> getVisibleSections() {
        return visibleSectionsSet;
    }

    public JSONArray fetchVariantsAsJsonArray() throws JSONException {
        List<Variant> variants = getVariants(); // Fetch variants using existing method
        JSONArray jsonArray = new JSONArray();

        for (Variant variant : variants) {

            LinkedHashMap<String, Object> variantMap = new LinkedHashMap<>();

            variantMap.put("@type", "Offer");
            variantMap.put("sku", variant.getSku());
            variantMap.put("url", getCanonicalUrl());

            variantMap.put("availability", variant.getInStock() ? "InStock" : "OutOfStock");

            // Create assets array
            JSONArray assets = new JSONArray();
            for (Asset asset : variant.getAssets()) {
                JSONObject jsonAsset = new JSONObject();
                jsonAsset.put("label", asset.getLabel());
                jsonAsset.put("path", asset.getPath()); // Fetching image path from the Asset
                assets.put(jsonAsset);
            }

            // Use the first asset as the image
            variantMap.put("image", assets.length() > 0 ? assets.getJSONObject(0).getString("path") : "");

            // Get price range
            Price priceRange = variant.getPriceRange();
            JSONObject priceSpecification = new JSONObject();
            priceSpecification.put("@type", "UnitPriceSpecification");
            priceSpecification.put("priceType", "https://schema.org/ListPrice");
            if (priceRange != null) {
                priceSpecification.put("price", priceRange.getRegularPrice());
                priceSpecification.put("priceCurrency", priceRange.getCurrency());
            }
            variantMap.put("priceSpecification", priceSpecification);

            // Handle special price
            if (variant.getSpecialPrice() != null) {
                variantMap.put("price", variant.getSpecialPrice());
            } else {
                variantMap.put("price", " ");
            }

            if (variant.getSpecialToDate() != null) {
                variantMap.put("SpecialPricedate", variant.getSpecialToDate());
            } else {
                variantMap.put("SpecialPricedate", " ");
            }

            // Add date and currency

            variantMap.put("priceCurrency", priceRange != null ? priceRange.getCurrency() : "");

            // Create JSONObject from the LinkedHashMap to preserve the key order
            JSONObject jsonVariant = new JSONObject(variantMap);

            // Add the jsonVariant to the jsonArray
            jsonArray.put(jsonVariant);
        }

        return jsonArray;

    }

    // Method to transform products into the desired JSON structure
    public String transformProducts() throws JSONException, JSONException {
        JSONArray products = fetchVariantsAsJsonArray(); // Fetch the products dynamically
        JSONArray offers = new JSONArray();

        // Iterate through each product in the JSONArray
        for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);
            offers.put(product); // Add each product directly to offers
        }

        // Create the final result JSON object
        JSONObject result = new JSONObject();
        result.put("offers", offers);

        return offers.toString(2); // Pretty print with 2 spaces
    }

    // Method to generate offers JSON
    public String generateOffersJson() throws JSONException {

        String finaljson = StringEscapeUtils.unescapeHtml4(transformProducts());
        return finaljson; // Generate the JSON output
    }

    public String getConfigProperty() {
        Page page = currentPage; // Assuming this is your Page object

        // Traverse up the page hierarchy to look for the cq:conf property
        while (page != null) {
            // Get the resource for the current page
            Resource pageResource = page.adaptTo(Resource.class);

            if (pageResource != null) {
                // Append "/jcr:content" to the page path
                String jcrContentPath = page.getPath() + "/jcr:content";
                Resource jcrContentResource = pageResource.getChild("jcr:content");

                // Try to get the "cq:conf" property from the current page's jcr:content node
                if (jcrContentResource != null) {
                    String cqConfing = jcrContentResource.getValueMap().get("cq:conf", String.class);
                    if (cqConfing != null && !cqConfing.isEmpty()) {
                        return cqConfing; // Return the value if it's found
                    }
                }
            }

            // If "cq:conf" not found, move to the parent page
            page = page.getParent();
        }

        // Return null if the property is not found in any of the parent pages
        return null;
    }

    public Boolean getEnableJsonLDScript() {
        // Construct the new resource path by appending "/settings/cloudconfigs/commerce/jcr:content"
        String newConfigPath = getConfigProperty() + "/settings/cloudconfigs/commerce/jcr:content";

        // Retrieve the resource at the constructed path
        Resource configResource = resourceResolver.getResource(newConfigPath);

        // If the resource exists, get the property "enableJsonLDScript"
        if (configResource != null) {
            ValueMap valueMap = configResource.getValueMap();
            // Return the value of "enableJsonLDScript", if it exists
            return valueMap.get("enableJsonLDScript", Boolean.class);
        } else {
            // If the resource is not found, log the error and return null

            return false;
        }
    }

}
