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
import java.util.*;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.sitemap.SitemapLinkExternalizer;
import com.adobe.cq.commerce.core.components.internal.services.sitemap.SitemapLinkExternalizerProvider;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.GroupItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.MediaGalleryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductStockStatus;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.VirtualProduct;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);
    private static final String PAGE = "/content/pageA";
    private static final String PRODUCT = "/content/pageA/jcr:content/root/responsivegrid/product";
    private static final String PRODUCT_WITH_ID = "/content/pageA/jcr:content/root/responsivegrid/productwithid";

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                (Function<Resource, ComponentsConfiguration>) input -> !input.getPath().contains("pageB") ? MOCK_CONFIGURATION_OBJECT
                    : ComponentsConfiguration.EMPTY);

            ConfigurationBuilder mockConfigBuilder = mock(ConfigurationBuilder.class);
            Utils.addDataLayerConfig(mockConfigBuilder, true);
            Utils.addStorefrontContextConfig(mockConfigBuilder, true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
        })
        .build();

    protected Resource productResource;
    protected Resource pageResource;
    protected GraphqlClient graphqlClient;

    protected ProductInterface product;
    protected Product productModel;
    protected CloseableHttpClient httpClient;
    protected Style style;

    @Before
    public void setUp() throws Exception {
        Page page = spy(context.currentPage(PAGE));
        pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);

        httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        context.currentResource(PRODUCT);
        productResource = spy(context.resourceResolver().getResource(PRODUCT));

        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-product-result.json");
        product = rootQuery.getProducts().getItems().get(0);

        graphqlClient = spy(new GraphqlClientImpl());
        context.registerInjectActivateService(graphqlClient, "httpMethod", "POST");

        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, 200, "{products(filter:{sku");

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");
        context.request().setServletPath(PAGE + ".html/beaumont-summit-kit.html");

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productResource.getValueMap());

        style = mock(Style.class);
        doAnswer(i -> i.getArguments()[1]).when(style).get(eq("loadClientPrice"), anyBoolean());
        doAnswer(i -> i.getArguments()[1]).when(style).get(eq("enableAddToWishList"), anyBoolean());
        doAnswer(i -> i.getArguments()[1]).when(style).get(eq("visibleSections"), any(String[].class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);
    }

    protected void adaptToProduct() {
        productModel = context.request().adaptTo(ProductImpl.class);
    }

    @Test
    public void testGetIdentifierFromSelector() {
        adaptToProduct();

        assertEquals("MJ01", productModel.getSku());
    }

    @Test
    public void testGetIdentifierFromProperty() {
        // Use different page
        context.request().setServletPath("/content/product-of-the-week/jcr:content/root/responsivegrid/product.beaumont-summit-kit.html");
        productResource = context.resourceResolver().getResource("/content/product-of-the-week/jcr:content/root/responsivegrid/product");
        context.currentResource(productResource);

        // Update product properties in sling bindings
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productResource);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productResource.getValueMap());

        adaptToProduct();

        String sku = (String) Whitebox.getInternalState(productModel.getProductRetriever(), "identifier");
        assertEquals("MJ02", sku);
    }

    @Test
    public void testProduct() {
        testProductImpl(false);
    }

    public void testProductImpl(boolean hasStagedData) {
        adaptToProduct();
        testProduct(product, true);

        if (hasStagedData) {
            assertTrue("The product has staged data", productModel.isStaged());
        } else {
            assertFalse("The product doesn't have staged data", productModel.isStaged());
        }

        // We don't return these fields for the EDIT placeholder data
        assertEquals(product.getMetaDescription(), productModel.getMetaDescription());
        assertEquals(product.getMetaKeyword(), productModel.getMetaKeywords());
        assertEquals(product.getMetaTitle(), productModel.getMetaTitle());
        assertEquals("https://author" + PAGE + ".html/beaumont-summit-kit.html", productModel.getCanonicalUrl());

        assertFalse(productModel.getAddToWishListEnabled());
    }

    private void testProduct(ProductInterface product, boolean loadClientPrice) {
        assertTrue("The product is found", productModel.getFound());
        assertEquals(product.getSku(), productModel.getSku());
        assertEquals(product.getName(), productModel.getName());
        assertEquals(product.getDescription().getHtml(), productModel.getDescription());
        assertEquals(loadClientPrice, productModel.loadClientPrice());
        assertEquals(ProductStockStatus.IN_STOCK.equals(product.getStockStatus()), productModel.getInStock().booleanValue());

        assertEquals(product.getMediaGallery().size(), productModel.getAssets().size());
        for (int j = 0; j < product.getMediaGallery().size(); j++) {
            MediaGalleryInterface mge = product.getMediaGallery().get(j);
            Asset asset = productModel.getAssets().get(j);
            assertEquals(mge.getLabel(), asset.getLabel());
            assertEquals(mge.getPosition(), asset.getPosition());
            assertEquals("image", asset.getType());
            assertEquals(mge.getUrl(), asset.getPath());
        }

        assertTrue(productModel.getGroupedProductItems().isEmpty());
    }

    @Test
    public void testVariants() {
        adaptToProduct();
        List<Variant> variants = productModel.getVariants();
        assertNotNull(variants);

        ConfigurableProduct cp = (ConfigurableProduct) product;
        assertEquals(cp.getVariants().size(), variants.size());

        for (int i = 0; i < variants.size(); i++) {
            Variant variant = variants.get(i);
            SimpleProduct sp = cp.getVariants().get(i).getProduct();

            assertEquals(sp.getSku(), variant.getSku());
            assertEquals(sp.getName(), variant.getName());
            assertEquals(sp.getDescription().getHtml(), variant.getDescription());
            assertEquals(ProductStockStatus.IN_STOCK.equals(sp.getStockStatus()), variant.getInStock().booleanValue());
            assertEquals(sp.getColor(), variant.getColor());

            assertEquals(sp.getMediaGallery().size(), variant.getAssets().size());
            for (int j = 0; j < sp.getMediaGallery().size(); j++) {
                MediaGalleryInterface mge = sp.getMediaGallery().get(j);
                Asset asset = variant.getAssets().get(j);
                assertEquals(mge.getLabel(), asset.getLabel());
                assertEquals(mge.getPosition(), asset.getPosition());
                assertEquals("image", asset.getType());
                assertEquals(mge.getUrl(), asset.getPath());
            }
        }
    }

    @Test
    public void testGetVariantAttributes() {
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
                assertEquals(optionValue.getValueIndex(), value.getId());
                assertEquals(optionValue.getLabel(), value.getLabel());
                assertEquals(optionValue.getLabel().trim().replaceAll("\\s+", "-").toLowerCase(), value.getCssClassModifier());
                assertNull(value.getSwatchType());
            }
        }
    }

    @Test
    public void testSwatchDataInVariantAttributes() throws IOException {
        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-configurableproduct-result.json");
        product = rootQuery.getProducts().getItems().get(0);

        Utils.setupHttpResponse("graphql/magento-graphql-configurableproduct-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-configurableproduct-result.json", httpClient, 200, "{products(filter:{sku");

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
                assertEquals(optionValue.getValueIndex(), value.getId());
                assertEquals(optionValue.getLabel(), value.getLabel());
                assertEquals(optionValue.getDefaultLabel().trim().replaceAll("\\s+", "-").toLowerCase(), value.getCssClassModifier());
                assertTrue("SwatchData type mismatch", optionValue.getSwatchData().getGraphQlTypeName().toUpperCase().startsWith(
                    value.getSwatchType().toString()));
            }
        }
    }

    @Test
    public void testSimpleProduct() {
        adaptToProduct();
        Whitebox.setInternalState(productModel.getProductRetriever(), "product", Optional.of(new SimpleProduct()));
        assertTrue(productModel.getVariants().isEmpty());
        assertTrue(productModel.getVariantAttributes().isEmpty());
    }

    @Test
    public void testSafeDescriptionWithNull() {
        adaptToProduct();
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        when(product.getDescription()).thenReturn(null);
        Whitebox.setInternalState(productModel.getProductRetriever(), "product", Optional.of(product));
        assertNull(productModel.getDescription());
    }

    @Test
    public void testSafeDescriptionHtmlNull() {
        adaptToProduct();
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(null);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(productModel.getProductRetriever(), "product", Optional.of(product));

        assertNull(productModel.getDescription());
    }

    @Test
    public void testSafeDescription() {
        adaptToProduct();
        String sampleString = "<strong>abc</strong>";
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(sampleString);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(productModel.getProductRetriever(), "product", Optional.of(product));

        assertEquals(sampleString, productModel.getDescription());
    }

    @Test
    public void testSafeDescriptionConfigurableProduct() {
        adaptToProduct();
        String sampleString = "<strong>def</strong>";
        ConfigurableProduct product = mock(ConfigurableProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(sampleString);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(productModel.getProductRetriever(), "product", Optional.of(product));

        assertEquals(sampleString, productModel.getDescription());
    }

    @Test
    public void testEditModePlaceholderData() throws IOException {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix(null);
        adaptToProduct();

        String json = Utils.getResource(ProductImpl.PLACEHOLDER_DATA);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        product = rootQuery.getProducts().getItems().get(0);

        testProduct(product, false);
    }

    @Test
    public void testPriceRange() {
        adaptToProduct();
        Price price = productModel.getPriceRange();

        assertTrue(price.isRange());
        assertFalse(price.isDiscounted());
        assertEquals(58.0, price.getRegularPrice(), 0.001);
        assertEquals(58.0, price.getFinalPrice(), 0.001);
        assertEquals(62.0, price.getRegularPriceMax(), 0.001);
        assertEquals(62.0, price.getFinalPriceMax(), 0.001);
    }

    @Test
    public void testDiscountedPrice() {
        adaptToProduct();
        Price price = productModel.getVariants().get(0).getPriceRange();

        assertFalse(price.isRange());
        assertTrue(price.isDiscounted());
        assertEquals(58.0, price.getRegularPrice(), 0.001);
        assertEquals(46.0, price.getFinalPrice(), 0.001);
        assertEquals(12.0, price.getDiscountAmount(), 0.001);
        assertEquals(20.69, price.getDiscountPercent(), 0.001);
    }

    @Test
    public void testGroupedProduct() throws IOException {
        testGroupedProductImpl(false);
    }

    public void testGroupedProductImpl(boolean hasStagedData) throws IOException {
        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-groupedproduct-result.json");
        product = rootQuery.getProducts().getItems().get(0);

        Utils.setupHttpResponse("graphql/magento-graphql-groupedproduct-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-groupedproduct-result.json", httpClient, 200, "{products(filter:{sku");
        adaptToProduct();

        List<GroupItem> items = productModel.getGroupedProductItems();
        assertTrue(productModel.isGroupedProduct());
        assertEquals(4, items.size());

        if (hasStagedData) {
            assertTrue("The product has staged data", productModel.isStaged());
        } else {
            assertFalse("The product doesn't have staged data", productModel.isStaged());
        }

        GroupedProduct gp = (GroupedProduct) product;
        for (int i = 0; i < items.size(); i++) {
            GroupItem item = items.get(i);
            ProductInterface pi = gp.getItems().get(i).getProduct();

            assertEquals(pi.getSku(), item.getSku());
            assertEquals(pi.getName(), item.getName());
            assertEquals(pi.getPriceRange().getMinimumPrice().getFinalPrice().getValue(), item.getPriceRange().getFinalPrice(), 0);
            assertEquals(gp.getItems().get(i).getQty(), item.getDefaultQuantity(), 0);

            assertEquals(pi instanceof VirtualProduct, item.isVirtualProduct());
        }
    }

    @Test
    public void testVirtualProduct() throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-virtualproduct-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-virtualproduct-result.json", httpClient, 200, "{products(filter:{sku");
        adaptToProduct();

        assertNotNull("Product model is not null", productModel);
        assertTrue(productModel.isVirtualProduct());
        assertFalse("The product doesn't have staged data", productModel.isStaged());
    }

    @Test
    public void testBundleProduct() throws IOException {
        testBundleProductImpl(false);
    }

    public void testBundleProductImpl(boolean hasStagedData) throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-bundleproduct-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-bundleproduct-result.json", httpClient, 200, "{products(filter:{sku");
        adaptToProduct();

        assertNotNull("Product model is not null", productModel);
        assertTrue(productModel.isBundleProduct());

        if (hasStagedData) {
            assertTrue("The product has staged data", productModel.isStaged());
        } else {
            assertFalse("The product doesn't have staged data", productModel.isStaged());
        }
    }

    @Test
    public void testGiftCardProduct() throws IOException {
        testGiftCardProductImpl();
    }

    public void testGiftCardProductImpl() throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-gift-card-product-result.json", httpClient, 200,
            "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-gift-card-product-result.json", httpClient, 200,
            "{products(filter:{sku");
        adaptToProduct();

        assertNotNull("Product model is not null", productModel);
        assertTrue(productModel.isGiftCardProduct());
    }

    @Test
    public void testProductNoGraphqlClient() {
        when(productResource.adaptTo(ComponentsConfiguration.class)).thenReturn(ComponentsConfiguration.EMPTY);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(ComponentsConfiguration.EMPTY);
        adaptToProduct();

        assertFalse("Product is not found", productModel.getFound());
        assertFalse("Product is not configurable", productModel.isConfigurable());
        assertFalse("Product is not a grouped product", productModel.isGroupedProduct());
        assertFalse("Product is not virtual", productModel.isVirtualProduct());
        assertNull("The product retriever is not created", productModel.getProductRetriever());
    }

    @Test
    public void testProductNotFound() throws IOException {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(true);
        slingBindings.put("wcmmode", wcmMode);

        Utils.setupHttpResponse("graphql/magento-graphql-product-not-found-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-product-not-found-result.json", httpClient, 200, "{products(filter:{sku");
        adaptToProduct();
        assertFalse("Product is not found", productModel.getFound());

        // verify that graphql was called only once
        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testClientLoadingIsDisabledOnLaunchPage() {
        adaptToProduct();
        assertTrue(productModel.loadClientPrice());
        Page launch = context.pageManager().getPage("/content/launches/2020/09/14/mylaunch" + PAGE);
        Whitebox.setInternalState(productModel, "currentPage", launch);
        assertFalse(productModel.loadClientPrice());
    }

    @Test
    public void testMissingSuffixOnPublish() {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(true);
        slingBindings.put("wcmmode", wcmMode);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix(null);
        context.request().setServletPath(PAGE + ".html"); // used by context.request().getRequestURI();
        adaptToProduct();

        // Check that we get an empty list of products and the GraphQL client is never called
        assertFalse(productModel.getFound());
        verify(graphqlClient, never()).execute(any(), any(), any());
        verify(graphqlClient, never()).execute(any(), any(), any(), any());

        // Test canonical url on publish
        assertEquals("https://publish" + PAGE + ".html", productModel.getCanonicalUrl());
    }

    @Test
    public void testJsonRender() throws IOException {
        adaptToProduct();
        ObjectMapper mapper = new ObjectMapper();
        String expected = Utils.getResource("results/result-datalayer-product-component.json");
        String jsonResult = productModel.getData().getJson();
        assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }

    @Test
    public void testStorefrontContextRender() throws IOException {
        adaptToProduct();
        ObjectMapper mapper = new ObjectMapper();
        String expected = Utils.getResource("storefront-context/result-storefront-context-product-component.json");
        String jsonResult = productModel.getStorefrontContext().getJson();
        assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }

    @Test
    public void testManualHtmlId() {
        context.currentResource(PRODUCT_WITH_ID);
        context.request().setServletPath(PRODUCT_WITH_ID + ".beaumont-summit-kit.html");
        productResource = context.resourceResolver().getResource(PRODUCT_WITH_ID);
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productResource);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productResource.getValueMap());

        adaptToProduct();

        assertEquals("custom-id", productModel.getId());
    }

    @Test
    public void testCanonicalUrlFromSitemapLinkExternalizer() {
        UrlProvider urlProvider = context.getService(UrlProvider.class);
        SitemapLinkExternalizer externalizer = mock(SitemapLinkExternalizer.class);
        SitemapLinkExternalizerProvider externalizerProvider = mock(SitemapLinkExternalizerProvider.class);
        when(externalizerProvider.getExternalizer(any())).thenReturn(externalizer);
        when(externalizer.toExternalProductUrl(any(), any(), any())).then(inv -> {
            // assert the parameters
            ProductUrlFormat.Params parameters = inv.getArgumentAt(2, ProductUrlFormat.Params.class);
            assertNotNull(parameters);
            assertEquals("beaumont-summit-kit", parameters.getUrlKey());
            assertEquals("MJ01", parameters.getSku());
            Page page = inv.getArgumentAt(1, Page.class);
            assertNotNull(page);
            assertEquals("/content/pageA", page.getPath());
            // invoke the callback directly
            return urlProvider.toProductUrl(inv.getArgumentAt(0, SlingHttpServletRequest.class), page, parameters);
        });
        context.registerService(SitemapLinkExternalizerProvider.class, externalizerProvider);

        adaptToProduct();

        assertEquals("/content/product-page.html/beaumont-summit-kit.html", productModel.getCanonicalUrl());
    }

    @Test
    public void testAddToWishListDisabled() {
        when(style.get(eq("enableAddToWishList"), anyBoolean())).thenReturn(Boolean.TRUE);
        adaptToProduct();
        assertTrue(productModel.getAddToWishListEnabled());
    }

    @Test
    public void testVisibleSectionsDefault() {
        adaptToProduct();
        assertThat(productModel.getVisibleSections()).containsOnly(Product.ACTIONS_SECTION, Product.DESCRIPTION_SECTION,
            Product.DETAILS_SECTION, Product.IMAGE_SECTION, Product.PRICE_SECTION, Product.QUANTITY_SECTION, Product.OPTIONS_SECTION,
            Product.SKU_SECTION, Product.TITLE_SECTION);
    }
}
