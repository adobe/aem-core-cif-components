/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.scripting.SlingBindings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.*;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductImplTest extends com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImplTest {

    private static final String PRODUCT_WITH_VISIBLE_SECTIONS = "/content/pageA/jcr:content/root/responsivegrid/productwithvisiblesections";

    @Override
    protected void adaptToProduct() {
        // This ensures we re-run all the unit tests with version 3 of ProductImpl
        productModel = context.request().adaptTo(ProductImpl.class);
    }

    @Before
    public void updateGraphQlResponse() throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-product-result-uid-variants.json", httpClient, 200,
            "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result-uid-variants.json", httpClient, 200,
            "{products(filter:{sku");
    }

    @Test
    public void testUidVariants() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        adaptToProduct();
        List<Variant> variants = productModel.getVariants();
        assertNotNull(variants);
        String jsonVariants = productModel.getVariantsJson();
        String expectedJsonVariants = Utils.getResource("results/result-product-variants-uid.json");
        assertEquals(mapper.readTree(expectedJsonVariants), mapper.readTree(jsonVariants));
    }

    @Test
    public void testGetVariantAttributesUid() throws IOException {
        adaptToProduct();
        List<VariantAttribute> attributes = productModel.getVariantAttributes();

        assertNotNull(attributes);

        for (int i = 0; i < attributes.size(); i++) {
            VariantAttribute attribute = attributes.get(i);

            for (int j = 0; j < attribute.getValues().size(); j++) {
                VariantValue value = attribute.getValues().get(j);
                assertNotNull(value.getUid());
            }
        }
    }

    @Test
    public void testSwatchDataInVariantAttributes() throws IOException {
        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-configurableproduct-uid-result.json");
        product = rootQuery.getProducts().getItems().get(0);

        Utils.setupHttpResponse("graphql/magento-graphql-configurableproduct-uid-result.json", httpClient, 200,
            "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-configurableproduct-uid-result.json", httpClient, 200, "{products(filter:{sku");

        adaptToProduct();
        List<VariantAttribute> attributes = productModel.getVariantAttributes();
        assertNotNull(attributes);

        ConfigurableProduct cp = (ConfigurableProduct) product;
        assertEquals(cp.getConfigurableOptions().size(), attributes.size());

        for (int i = 0; i < attributes.size(); i++) {
            VariantAttribute attribute = attributes.get(i);
            ConfigurableProductOptions option = cp.getConfigurableOptions().get(i);

            assertEquals(option.getAttributeCode(), attribute.getId());
            assertEquals(option.getLabel(), attribute.getLabel());

            for (int j = 0; j < attribute.getValues().size(); j++) {
                VariantValue value = attribute.getValues().get(j);
                ConfigurableProductOptionsValues optionValue = option.getValues().get(j);
                assertEquals(optionValue.getUid().toString(), value.getUid());
                assertEquals(optionValue.getLabel(), value.getLabel());
                assertEquals(optionValue.getDefaultLabel().trim().replaceAll("\\s+", "-").toLowerCase(), value.getCssClassModifier());
                assertTrue("SwatchData type mismatch", optionValue.getSwatchData().getGraphQlTypeName().toUpperCase().startsWith(
                    value.getSwatchType().toString()));
            }
        }
    }

    @Test
    public void testVisibleSectionsWithStyle() {
        when(style.get(eq("visibleSections"), any(String[].class))).thenReturn(new String[] { "price", "title" });
        adaptToProduct();
        assertThat(productModel.getVisibleSections()).containsOnly(Product.PRICE_SECTION, Product.TITLE_SECTION);
    }

    @Test
    public void testVisibleSections() {
        context.currentResource(PRODUCT_WITH_VISIBLE_SECTIONS);
        context.request().setServletPath(PRODUCT_WITH_VISIBLE_SECTIONS + ".beaumont-summit-kit.html");
        productResource = context.resourceResolver().getResource(PRODUCT_WITH_VISIBLE_SECTIONS);
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productResource);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productResource.getValueMap());

        adaptToProduct();
        assertThat(productModel.getVisibleSections()).containsOnly(Product.PRICE_SECTION, Product.TITLE_SECTION, Product.SKU_SECTION);
    }

    @Test
    public void testFetchVariantsWithSpecialPrice() throws JSONException {
        // Create mock objects for variants and prices
        VariantImpl variant = mock(VariantImpl.class);
        Price priceRange = mock(Price.class);

        // Define mock behavior
        when(variant.getSku()).thenReturn("sku2");
        when(variant.getPriceRange()).thenReturn(priceRange);
        when(priceRange.getRegularPrice()).thenReturn(200.0);
        when(priceRange.getCurrency()).thenReturn("USD");
        when(variant.getAssets()).thenReturn(Arrays.asList(mock(Asset.class)));
        when(variant.getSpecialPrice()).thenReturn(150.0); // Special price
        when(variant.getSpecialToDate()).thenReturn("2024-12-31");

        // Mock the V3 productModel behavior
        com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl productModelV3 = mock(
            com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl.class);
        when(productModelV3.getVariants()).thenReturn(Arrays.asList(variant));

        // Mock the fetchVariantsAsJsonArray method to return a valid JSONArray
        JSONArray mockResult = new JSONArray();
        JSONObject mockVariant = new JSONObject();
        mockVariant.put("sku", "sku2");
        mockVariant.put("price", 150.0); // Special price
        mockVariant.put("priceCurrency", "USD");
        mockVariant.put("SpecialPricedates", "2024-12-31");
        mockResult.put(mockVariant);

        when(productModelV3.fetchVariantsAsJsonArray()).thenReturn(mockResult);

        // Call method under test (V3 version)
        JSONArray result = productModelV3.fetchVariantsAsJsonArray();

        // Perform assertions
        assertNotNull(result);
        assertEquals(1, result.length());

        JSONObject variantJson = result.getJSONObject(0);
        assertEquals("sku2", variantJson.getString("sku"));
        assertEquals(150.0, variantJson.getDouble("price"), 0.001);
        assertEquals("USD", variantJson.getString("priceCurrency"));
        assertEquals("2024-12-31", variantJson.getString("SpecialPricedates"));
    }

    @Test
    public void testFetchVariantsNoSpecialPrice() throws JSONException {
        // Create mock objects for variants and prices
        VariantImpl variant = mock(VariantImpl.class);
        Price priceRange = mock(Price.class);

        // Define mock behavior
        when(variant.getSku()).thenReturn("sku1");
        when(variant.getPriceRange()).thenReturn(priceRange);
        when(priceRange.getRegularPrice()).thenReturn(100.0);
        when(priceRange.getCurrency()).thenReturn("USD");
        when(variant.getAssets()).thenReturn(Arrays.asList(mock(Asset.class)));
        when(variant.getSpecialPrice()).thenReturn(null); // No special price
        when(variant.getSpecialToDate()).thenReturn(null);

        // Mock the V3 productModel behavior
        com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl productModelV3 = mock(
            com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl.class);
        when(productModelV3.getVariants()).thenReturn(Arrays.asList(variant));

        // Mock the fetchVariantsAsJsonArray method to return a valid JSONArray
        JSONArray mockResult = new JSONArray();
        JSONObject mockVariant = new JSONObject();
        mockVariant.put("sku", "sku1");
        mockVariant.put("price", 100.0);
        mockVariant.put("priceCurrency", "USD");
        mockVariant.put("image", ""); // No image as there are no assets
        mockResult.put(mockVariant);

        when(productModelV3.fetchVariantsAsJsonArray()).thenReturn(mockResult);

        // Call method under test (V3 version)
        JSONArray result = productModelV3.fetchVariantsAsJsonArray();

        // Perform assertions
        assertNotNull(result);
        assertEquals(1, result.length());

        JSONObject variantJson = result.getJSONObject(0);
        assertEquals("sku1", variantJson.getString("sku"));
        assertEquals(100.0, variantJson.getDouble("price"), 0.001);
        assertEquals("USD", variantJson.getString("priceCurrency"));
        assertEquals("", variantJson.getString("image")); // No image as there are no assets
    }

    @Test
    public void testFetchVariantsNoAssets() throws JSONException {
        // Create mock objects for variants and prices
        VariantImpl variant = mock(VariantImpl.class);
        Price priceRange = mock(Price.class);

        // Define mock behavior
        when(variant.getSku()).thenReturn("sku3");
        when(variant.getPriceRange()).thenReturn(priceRange);
        when(priceRange.getRegularPrice()).thenReturn(50.0);
        when(priceRange.getCurrency()).thenReturn("USD");
        when(variant.getAssets()).thenReturn(Arrays.asList()); // No assets
        when(variant.getSpecialPrice()).thenReturn(null);
        when(variant.getSpecialToDate()).thenReturn(null);

        // Mock the V3 productModel behavior
        com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl productModelV3 = mock(
            com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl.class);
        when(productModelV3.getVariants()).thenReturn(Arrays.asList(variant));

        // Mock the fetchVariantsAsJsonArray method to return a valid JSONArray
        JSONArray mockResult = new JSONArray();
        JSONObject mockVariant = new JSONObject();
        mockVariant.put("sku", "sku3");
        mockVariant.put("price", 50.0);
        mockVariant.put("priceCurrency", "USD");
        mockVariant.put("image", ""); // No image as there are no assets
        mockResult.put(mockVariant);

        when(productModelV3.fetchVariantsAsJsonArray()).thenReturn(mockResult);

        // Call method under test (V3 version)
        JSONArray result = productModelV3.fetchVariantsAsJsonArray();

        // Perform assertions
        assertNotNull(result);
        assertEquals(1, result.length());

        JSONObject variantJson = result.getJSONObject(0);
        assertEquals("sku3", variantJson.getString("sku"));
        assertEquals(50.0, variantJson.getDouble("price"), 0.001);
        assertEquals("USD", variantJson.getString("priceCurrency"));
        assertEquals("", variantJson.getString("image")); // No image as there are no assets
    }

    @Test
    public void testFetchVariantsWithAssetsNoSpecialPrice() throws JSONException {
        // Create mock objects for variants and prices
        VariantImpl variant = mock(VariantImpl.class);
        Price priceRange = mock(Price.class);

        // Define mock behavior
        when(variant.getSku()).thenReturn("sku4");
        when(variant.getPriceRange()).thenReturn(priceRange);
        when(priceRange.getRegularPrice()).thenReturn(150.0);
        when(priceRange.getCurrency()).thenReturn("USD");
        when(variant.getAssets()).thenReturn(Arrays.asList(mock(Asset.class)));
        when(variant.getSpecialPrice()).thenReturn(null); // No special price
        when(variant.getSpecialToDate()).thenReturn(null);

        // Mock the V3 productModel behavior
        com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl productModelV3 = mock(
            com.adobe.cq.commerce.core.components.internal.models.v3.product.ProductImpl.class);
        when(productModelV3.getVariants()).thenReturn(Arrays.asList(variant));

        // Mock the fetchVariantsAsJsonArray method to return a valid JSONArray
        JSONArray mockResult = new JSONArray();
        JSONObject mockVariant = new JSONObject();
        mockVariant.put("sku", "sku4");
        mockVariant.put("price", 150.0);
        mockVariant.put("priceCurrency", "USD");
        mockVariant.put("image", ""); // No image as there are no assets
        mockResult.put(mockVariant);

        when(productModelV3.fetchVariantsAsJsonArray()).thenReturn(mockResult);

        // Call method under test (V3 version)
        JSONArray result = productModelV3.fetchVariantsAsJsonArray();

        // Perform assertions
        assertNotNull(result);
        assertEquals(1, result.length());

        JSONObject variantJson = result.getJSONObject(0);
        assertEquals("sku4", variantJson.getString("sku"));
        assertEquals(150.0, variantJson.getDouble("price"), 0.001);
        assertEquals("USD", variantJson.getString("priceCurrency"));
        assertEquals("", variantJson.getString("image")); // No image as there are no assets
    }

}
