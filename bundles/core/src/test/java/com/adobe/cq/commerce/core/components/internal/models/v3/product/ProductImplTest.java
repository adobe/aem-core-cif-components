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
import java.lang.reflect.Field;
import java.util.ArrayList;
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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

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
    public void testFetchVariantsAsJsonArrayWithNoSpecialPrice() throws JSONException {
        ProductImpl product = spy(new ProductImpl());
        List<Variant> variants = new ArrayList<>();
        VariantImpl variant = mock(VariantImpl.class);
        when(variant.getSku()).thenReturn("SKU789");
        when(variant.getInStock()).thenReturn(true);
        when(variant.getAssets()).thenReturn(new ArrayList<>());
        Price priceRange = mock(Price.class);
        when(priceRange.getRegularPrice()).thenReturn(120.0);
        when(priceRange.getCurrency()).thenReturn("USD");
        when(variant.getPriceRange()).thenReturn(priceRange);
        when(variant.getSpecialPrice()).thenReturn(null);
        when(variant.getSpecialToDate()).thenReturn(null);
        variants.add(variant);

        doReturn(variants).when(product).getVariants();
        doReturn("http://example.com/product").when(product).getCanonicalUrl();

        JSONArray result = product.fetchVariantsAsJsonArray();

        assertNotNull(result);
        assertEquals(1, result.length());
        JSONObject variantJson = result.getJSONObject(0);
        assertEquals("Offer", variantJson.getString("@type"));
        assertEquals("SKU789", variantJson.getString("sku"));
        assertEquals("http://example.com/product", variantJson.getString("url"));
        assertEquals("", variantJson.getString("image"));
        assertEquals("USD", variantJson.getString("priceCurrency"));
        assertEquals(120.0, variantJson.getDouble("price"), 0.001);
        assertFalse(variantJson.has("SpecialPricedates"));
        assertFalse(variantJson.has("priceSpecification"));
    }

    @Test
    public void testFetchVariantsAsJsonArrayWithSpecialPrice() throws JSONException {
        ProductImpl product = spy(new ProductImpl());
        List<Variant> variants = new ArrayList<>();
        VariantImpl variant = mock(VariantImpl.class);
        when(variant.getSku()).thenReturn("SKU789");
        when(variant.getInStock()).thenReturn(true);
        when(variant.getAssets()).thenReturn(new ArrayList<>());
        Price priceRange = mock(Price.class);
        when(priceRange.getRegularPrice()).thenReturn(120.0);
        when(priceRange.getCurrency()).thenReturn("USD");
        when(variant.getPriceRange()).thenReturn(priceRange);
        when(variant.getSpecialPrice()).thenReturn(100.0);
        when(variant.getSpecialToDate()).thenReturn("2023-12-31");
        variants.add(variant);

        doReturn(variants).when(product).getVariants();
        doReturn("http://example.com/product").when(product).getCanonicalUrl();

        JSONArray result = product.fetchVariantsAsJsonArray();

        assertNotNull(result);
        assertEquals(1, result.length());
        JSONObject variantJson = result.getJSONObject(0);
        assertEquals("Offer", variantJson.getString("@type"));
        assertEquals("SKU789", variantJson.getString("sku"));
        assertEquals("http://example.com/product", variantJson.getString("url"));
        assertEquals("", variantJson.getString("image"));
        assertEquals("USD", variantJson.getString("priceCurrency"));
        assertEquals(100.0, variantJson.getDouble("price"), 0.001);
        assertEquals("InStock", variantJson.getString("availability"));
        assertEquals("2023-12-31", variantJson.getString("SpecialPricedates"));

        JSONObject priceSpecification = variantJson.getJSONObject("priceSpecification");
        assertEquals("UnitPriceSpecification", priceSpecification.getString("@type"));
        assertEquals("https://schema.org/ListPrice", priceSpecification.getString("priceType"));
        assertEquals(120.0, priceSpecification.getDouble("price"), 0.001);
        assertEquals("USD", priceSpecification.getString("priceCurrency"));
    }

    @Test
    public void testFetchVariantsAsJsonArrayWithNoVariants() throws JSONException {
        ProductImpl product = spy(new ProductImpl());
        List<Variant> variants = new ArrayList<>();

        doReturn(variants).when(product).getVariants();
        doReturn("http://example.com/product").when(product).getCanonicalUrl();

        JSONArray result = product.fetchVariantsAsJsonArray();

        assertNotNull(result);
        assertEquals(0, result.length());
    }

    @Test
    public void testFetchVariantsAsJsonArrayWithMultipleVariants() throws JSONException {
        ProductImpl product = spy(new ProductImpl());
        List<Variant> variants = new ArrayList<>();
        VariantImpl variant1 = mock(VariantImpl.class);
        when(variant1.getSku()).thenReturn("SKU789");
        when(variant1.getInStock()).thenReturn(true);
        when(variant1.getAssets()).thenReturn(new ArrayList<>());
        Price priceRange1 = mock(Price.class);
        when(priceRange1.getRegularPrice()).thenReturn(120.0);
        when(priceRange1.getCurrency()).thenReturn("USD");
        when(variant1.getPriceRange()).thenReturn(priceRange1);
        when(variant1.getSpecialPrice()).thenReturn(100.0);
        when(variant1.getSpecialToDate()).thenReturn("2023-12-31");
        variants.add(variant1);

        VariantImpl variant2 = mock(VariantImpl.class);
        when(variant2.getSku()).thenReturn("SKU790");
        when(variant2.getInStock()).thenReturn(false);
        when(variant2.getAssets()).thenReturn(new ArrayList<>());
        Price priceRange2 = mock(Price.class);
        when(priceRange2.getRegularPrice()).thenReturn(150.0);
        when(priceRange2.getCurrency()).thenReturn("USD");
        when(variant2.getPriceRange()).thenReturn(priceRange2);
        when(variant2.getSpecialPrice()).thenReturn(130.0);
        when(variant2.getSpecialToDate()).thenReturn("2023-12-31");
        variants.add(variant2);

        doReturn(variants).when(product).getVariants();
        doReturn("http://example.com/product").when(product).getCanonicalUrl();

        JSONArray result = product.fetchVariantsAsJsonArray();

        assertNotNull(result);
        assertEquals(2, result.length());

        JSONObject variantJson1 = result.getJSONObject(0);
        assertEquals("Offer", variantJson1.getString("@type"));
        assertEquals("SKU789", variantJson1.getString("sku"));
        assertEquals("http://example.com/product", variantJson1.getString("url"));
        assertEquals("", variantJson1.getString("image"));
        assertEquals("USD", variantJson1.getString("priceCurrency"));
        assertEquals(100.0, variantJson1.getDouble("price"), 0.001);
        assertEquals("InStock", variantJson1.getString("availability"));
        assertEquals("2023-12-31", variantJson1.getString("SpecialPricedates"));

        JSONObject priceSpecification1 = variantJson1.getJSONObject("priceSpecification");
        assertEquals("UnitPriceSpecification", priceSpecification1.getString("@type"));
        assertEquals("https://schema.org/ListPrice", priceSpecification1.getString("priceType"));
        assertEquals(120.0, priceSpecification1.getDouble("price"), 0.001);
        assertEquals("USD", priceSpecification1.getString("priceCurrency"));

        JSONObject variantJson2 = result.getJSONObject(1);
        assertEquals("Offer", variantJson2.getString("@type"));
        assertEquals("SKU790", variantJson2.getString("sku"));
        assertEquals("http://example.com/product", variantJson2.getString("url"));
        assertEquals("", variantJson2.getString("image"));
        assertEquals("USD", variantJson2.getString("priceCurrency"));
        assertEquals(130.0, variantJson2.getDouble("price"), 0.001);
        assertEquals("OutOfStock", variantJson2.getString("availability"));
        assertEquals("2023-12-31", variantJson2.getString("SpecialPricedates"));

        JSONObject priceSpecification2 = variantJson2.getJSONObject("priceSpecification");
        assertEquals("UnitPriceSpecification", priceSpecification2.getString("@type"));
        assertEquals("https://schema.org/ListPrice", priceSpecification2.getString("priceType"));
        assertEquals(150.0, priceSpecification2.getDouble("price"), 0.001);
        assertEquals("USD", priceSpecification2.getString("priceCurrency"));
    }

    @Test
    public void testGenerateProductJsonLDStringWithCachedJsonLD() throws JSONException, NoSuchFieldException, IllegalAccessException {
        System.out.println("Starting testGenerateProductJsonLDStringWithCachedJsonLD");

        // Initialize the ProductImpl instance
        ProductImpl product = spy(new ProductImpl());
        System.out.println("ProductImpl instance created");

        // Use reflection to set the cachedJsonLD field
        Field cachedJsonLDField = ProductImpl.class.getDeclaredField("cachedJsonLD");
        cachedJsonLDField.setAccessible(true);
        cachedJsonLDField.set(product, "{\"@type\":\"Product\"}");
        System.out.println("cachedJsonLD field set");

        // Generate the JSON-LD string
        String jsonLD = product.generateProductJsonLDString();
        System.out.println("Generated JSON-LD string: " + jsonLD);

        // Assert the JSON-LD string
        assertNotNull(jsonLD);
        assertEquals("{\"@type\":\"Product\"}", jsonLD);
        System.out.println("Assertions passed");
    }

    @Test
    public void testGenerateProductJsonLDStringWithEnableJsonFalse() throws JSONException, NoSuchFieldException, IllegalAccessException {
        System.out.println("Starting testGenerateProductJsonLDStringWithEnableJsonFalse");

        // Initialize the ProductImpl instance
        ProductImpl product = spy(new ProductImpl());
        System.out.println("ProductImpl instance created");

        // Use reflection to set the enableJson field
        Field enableJsonField = ProductImpl.class.getDeclaredField("enableJson");
        enableJsonField.setAccessible(true);
        enableJsonField.set(product, false);
        System.out.println("enableJson field set to false");

        // Generate the JSON-LD string
        String jsonLD = product.generateProductJsonLDString();
        System.out.println("Generated JSON-LD string: " + jsonLD);

        // Assert the JSON-LD string
        assertNull(jsonLD);
        System.out.println("Assertions passed");
    }
}
