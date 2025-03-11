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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.xss.XSSAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.*;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    private void setupXssApi(ProductImpl product) throws Exception {
        XSSAPI xssAPI = mock(XSSAPI.class);
        when(xssAPI.encodeForHTML(anyString())).thenAnswer(invocation -> invocation.getArguments()[0]); // Return input as is

        // Use reflection to find and inject xssApi field
        Field xssApiField = null;
        Class<?> clazz = product.getClass();
        while (clazz != null) {
            try {
                xssApiField = clazz.getDeclaredField("xssApi");
                xssApiField.setAccessible(true);
                xssApiField.set(product, xssAPI);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass(); // Traverse up in hierarchy
            }
        }

        if (xssApiField == null) {
            throw new NoSuchFieldException("xssApi field not found in class hierarchy");
        }
    }

    @Test
    public void testFetchVariantsAsJsonArray() throws Exception {
        ProductImpl product = spy(new ProductImpl());
        List<Variant> variants = new ArrayList<>();

        VariantImpl variant1 = createMockVariant("SKU789", true, "http://example.com/image1.jpg", 120.0, "USD", 100.0, "2023-12-31");
        variants.add(variant1);

        VariantImpl variant2 = createMockVariant("SKU790", false, "http://example.com/image2.jpg", 150.0, "USD", 130.0, "2023-12-31");
        variants.add(variant2);

        doReturn(variants).when(product).getVariants();
        doReturn("http://example.com/product").when(product).getCanonicalUrl();

        setupXssApi(product);

        // Invoke the method using reflection

        Method fetchVariantsAsJsonArray = ProductImpl.class.getDeclaredMethod("fetchVariantsAsJsonArray");
        fetchVariantsAsJsonArray.setAccessible(true);
        ArrayNode result = (ArrayNode) fetchVariantsAsJsonArray.invoke(product);

        assertNotNull(result);
        assertEquals(2, result.size());

        ObjectNode variantJson1 = (ObjectNode) result.get(0);
        assertEquals("Offer", variantJson1.get("@type").asText());
        assertEquals("SKU789", variantJson1.get("sku").asText().trim()); // Trim to avoid whitespace mismatches
        assertEquals("http://example.com/product", variantJson1.get("url").asText());
        assertEquals("http://example.com/image1.jpg", variantJson1.get("image").asText());
        assertEquals("USD", variantJson1.get("priceCurrency").asText());
        assertEquals(100.0, variantJson1.get("price").asDouble(), 0.001);
        assertEquals("InStock", variantJson1.get("availability").asText());
        assertEquals("2023-12-31", variantJson1.get("SpecialPricedates").asText());

        ObjectNode priceSpecification1 = (ObjectNode) variantJson1.get("priceSpecification");
        assertEquals("UnitPriceSpecification", priceSpecification1.get("@type").asText());
        assertEquals("https://schema.org/ListPrice", priceSpecification1.get("priceType").asText());
        assertEquals(120.0, priceSpecification1.get("price").asDouble(), 0.001);
        assertEquals("USD", priceSpecification1.get("priceCurrency").asText());

        ObjectNode variantJson2 = (ObjectNode) result.get(1);
        assertEquals("Offer", variantJson2.get("@type").asText());
        assertEquals("SKU790", variantJson2.get("sku").asText().trim());
        assertEquals("http://example.com/product", variantJson2.get("url").asText());
        assertEquals("http://example.com/image2.jpg", variantJson2.get("image").asText());
        assertEquals("USD", variantJson2.get("priceCurrency").asText());
        assertEquals(130.0, variantJson2.get("price").asDouble(), 0.001);
        assertEquals("OutOfStock", variantJson2.get("availability").asText());
        assertEquals("2023-12-31", variantJson2.get("SpecialPricedates").asText());

        ObjectNode priceSpecification2 = (ObjectNode) variantJson2.get("priceSpecification");
        assertEquals("UnitPriceSpecification", priceSpecification2.get("@type").asText());
        assertEquals("https://schema.org/ListPrice", priceSpecification2.get("priceType").asText());
        assertEquals(150.0, priceSpecification2.get("price").asDouble(), 0.001);
        assertEquals("USD", priceSpecification2.get("priceCurrency").asText());
    }

    private VariantImpl createMockVariant(String sku, boolean inStock, String imagePath, double regularPrice, String currency,
        Double specialPrice, String specialToDate) {
        VariantImpl variant = mock(VariantImpl.class);
        when(variant.getSku()).thenReturn(sku);
        when(variant.getInStock()).thenReturn(inStock);

        List<Asset> assetsList = new ArrayList<>();
        Asset asset = mock(Asset.class);
        when(asset.getPath()).thenReturn(imagePath);
        assetsList.add(asset);
        when(variant.getAssets()).thenReturn(assetsList);

        Price priceRange = mock(Price.class);
        when(priceRange.getRegularPrice()).thenReturn(regularPrice);
        when(priceRange.getCurrency()).thenReturn(currency);
        when(variant.getPriceRange()).thenReturn(priceRange);
        when(variant.getSpecialPrice()).thenReturn(specialPrice);
        when(variant.getSpecialToDate()).thenReturn(specialToDate);

        return variant;
    }

    @Test
    public void testCreateBasicProductJson() throws Exception {
        ProductImpl product = spy(new ProductImpl());
        ObjectMapper mapper = new ObjectMapper();
        doReturn("test-sku").when(product).getSku();
        doReturn("Test name").when(product).getName();
        doReturn("Test Description").when(product).getDescription();
        doReturn("test-id").when(product).getId();
        Asset asset = mock(Asset.class);
        when(asset.getPath()).thenReturn("http://example.com/image.jpg");
        doReturn(Collections.singletonList(asset)).when(product).getAssets();

        setupXssApi(product);

        // Invoke private method using reflection
        Method createBasicProductJson = ProductImpl.class.getDeclaredMethod("createBasicProductJson", ObjectMapper.class);
        createBasicProductJson.setAccessible(true);
        ObjectNode productJson = (ObjectNode) createBasicProductJson.invoke(product, mapper);

        assertNotNull(productJson);
        assertEquals("http://schema.org", productJson.get("@context").asText());
        assertEquals("Product", productJson.get("@type").asText());
        assertEquals("test-sku", productJson.get("sku").asText());
        assertEquals("Test name", productJson.get("name").asText());
        assertEquals("Test Description", productJson.get("description").asText());
        assertEquals("test-id", productJson.get("@id").asText());
        assertEquals("http://example.com/image.jpg", productJson.get("image").asText());
    }

    private ProductImpl createSpyProductWithVariants(List<Variant> variants) {
        ProductImpl product = spy(new ProductImpl());
        doReturn(variants).when(product).getVariants();
        return product;
    }

    private void assertJsonArray(JSONArray result, int expectedLength) throws JSONException {
        assertNotNull(result);
        assertEquals(expectedLength, result.length());
    }

    @Test
    public void testFetchVariantsAsJsonArrayWithEmptyVariantsList() throws Exception {
        ProductImpl product = createSpyProductWithVariants(Collections.emptyList());

        Method fetchVariantsAsJsonArray = ProductImpl.class.getDeclaredMethod("fetchVariantsAsJsonArray");
        fetchVariantsAsJsonArray.setAccessible(true);
        ArrayNode result = (ArrayNode) fetchVariantsAsJsonArray.invoke(product);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetAndSetSpecialPriceAndToDate() {
        VariantImpl variant = new VariantImpl();

        String specialToDate = "2023-12-31";
        variant.setSpecialToDate(specialToDate);
        assertEquals(specialToDate, variant.getSpecialToDate());

        Double specialPrice = 99.99;
        variant.setSpecialPrice(specialPrice);
        assertEquals(specialPrice, variant.getSpecialPrice(), 0.001);
    }

    @Test
    public void testFetchVariantsAsJsonArrayWithNullOrEmptyVariants() throws Exception {
        ProductImpl product = createSpyProductWithVariants(null);

        Method fetchVariantsAsJsonArray = ProductImpl.class.getDeclaredMethod("fetchVariantsAsJsonArray");
        fetchVariantsAsJsonArray.setAccessible(true);
        ArrayNode result = (ArrayNode) fetchVariantsAsJsonArray.invoke(product);

        assertNotNull(result);
        assertEquals(0, result.size());

        product = createSpyProductWithVariants(Collections.emptyList());
        result = (ArrayNode) fetchVariantsAsJsonArray.invoke(product);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testFetchVariantsAsJsonArrayWithSpecialPriceAndDate() throws Exception {
        List<Variant> variants = new ArrayList<>();
        VariantImpl variant = createMockVariant("SKU789", true, "http://example.com/image1.jpg", 120.0, "USD", null, null);
        variants.add(variant);

        ProductImpl product = createSpyProductWithVariants(variants);
        doReturn("http://example.com/product").when(product).getCanonicalUrl();

        setupXssApi(product);

        Method fetchVariantsAsJsonArray = ProductImpl.class.getDeclaredMethod("fetchVariantsAsJsonArray");
        fetchVariantsAsJsonArray.setAccessible(true);
        ArrayNode result = (ArrayNode) fetchVariantsAsJsonArray.invoke(product);

        assertEquals(1, result.size());

        ObjectNode variantJson = (ObjectNode) result.get(0);
        assertEquals("Offer", variantJson.get("@type").asText());
        assertEquals("SKU789", variantJson.get("sku").asText());
        assertEquals("http://example.com/product", variantJson.get("url").asText());
        assertEquals("http://example.com/image1.jpg", variantJson.get("image").asText());
        assertEquals("USD", variantJson.get("priceCurrency").asText());
        assertEquals(120.0, variantJson.get("price").asDouble(), 0.001);
    }

    @Test
    public void testAddOffersToJson() throws Exception {
        ProductImpl product = spy(new ProductImpl());
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode productJson = mapper.createObjectNode();

        // Use reflection to access the private fetchVariantsAsJsonArray method
        Method fetchVariantsAsJsonArray = ProductImpl.class.getDeclaredMethod("fetchVariantsAsJsonArray");
        fetchVariantsAsJsonArray.setAccessible(true);
        ArrayNode variantsArray = (ArrayNode) fetchVariantsAsJsonArray.invoke(product);

        // Use reflection to access the private addOffersToJson method
        Method addOffersToJson = ProductImpl.class.getDeclaredMethod("addOffersToJson", ObjectNode.class, ObjectMapper.class);
        addOffersToJson.setAccessible(true);
        addOffersToJson.invoke(product, productJson, mapper);

        assertTrue(productJson.has("offers"));
        assertTrue(productJson.get("offers").isArray());
        assertEquals(0, productJson.get("offers").size());
    }

    @Test
    public void testGetJsonLd() throws Exception {
        // Initialize and configure the ProductImpl object
        ProductImpl product = spy(new ProductImpl());
        SlingBindings bindings = new SlingBindings();

        // Create a resource and adapt it to a Page object
        Resource resource = context.create().resource("/content/page");
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(resource);

        bindings.put("currentPage", page);
        context.request().setAttribute(SlingBindings.class.getName(), bindings);

        // Mock the necessary methods
        doReturn("test-sku").when(product).getSku();
        doReturn("Test Product").when(product).getName();
        doReturn("Test Description").when(product).getDescription();
        doReturn("test-id").when(product).getId();
        doReturn(Collections.emptyList()).when(product).getAssets();

        setupXssApi(product);

        Whitebox.setInternalState(product, "enableJsonLd", true);

        // Call the method
        String jsonLd = product.getJsonLd();

        // Verify the result
        assertNotNull(jsonLd);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode productJson = (ObjectNode) mapper.readTree(jsonLd);
        assertEquals("http://schema.org", productJson.get("@context").asText());
        assertEquals("Product", productJson.get("@type").asText());
        assertEquals("test-sku", productJson.get("sku").asText());
        assertEquals("Test Product", productJson.get("name").asText());
        assertEquals("Test Description", productJson.get("description").asText());
        assertEquals("test-id", productJson.get("@id").asText());
        assertTrue(productJson.has("offers"));
    }

    @Test
    public void testGetJsonLdDisabled() throws Exception {
        // Initialize and configure the ProductImpl object
        ProductImpl product = spy(new ProductImpl());
        SlingBindings bindings = new SlingBindings();

        // Create a resource and adapt it to a Page object
        Resource resource = context.create().resource("/content/page");
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(resource);

        bindings.put("currentPage", page);
        context.request().setAttribute(SlingBindings.class.getName(), bindings);

        // Mock the necessary methods
        doReturn("test-sku").when(product).getSku();
        doReturn("Test Product").when(product).getName();
        doReturn("Test Description").when(product).getDescription();
        doReturn("test-id").when(product).getId();
        doReturn(Collections.emptyList()).when(product).getAssets();

        // Call the private method using reflection
        boolean isEnableJsonLd = (boolean) Whitebox.getInternalState(product, "enableJsonLd");
        assertFalse(isEnableJsonLd);

        // Call the method
        String jsonLd = product.getJsonLd();

        // Verify the result
        assertNull(jsonLd);
    }

}
