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
import java.util.List;
import java.util.Optional;

import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.MockExternalizer;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.GroupItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
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
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);

                context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                    (Function<Resource, ComponentsConfiguration>) input -> !input.getPath().contains("pageB") ? MOCK_CONFIGURATION_OBJECT
                        : ComponentsConfiguration.EMPTY);

                context.registerService(Externalizer.class, new MockExternalizer());

                ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
                Utils.addDataLayerConfig(mockConfigBuilder, true);
                Utils.addStorefrontContextConfig(mockConfigBuilder, true);
                context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PAGE = "/content/pageA";
    private static final String PRODUCT = "/content/pageA/jcr:content/root/responsivegrid/product";

    private Resource productResource;
    private Resource pageResource;
    private ProductInterface product;
    private GraphqlClient graphqlClient;

    protected Product productModel;
    protected HttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        Page page = Mockito.spy(context.currentPage(PAGE));
        pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);

        httpClient = mock(HttpClient.class);

        context.currentResource(PRODUCT);
        productResource = Mockito.spy(context.resourceResolver().getResource(PRODUCT));

        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-product-result.json");
        product = rootQuery.getProducts().getItems().get(0);

        GraphqlClientConfiguration graphqlClientConfiguration = mock(GraphqlClientConfiguration.class);
        when(graphqlClientConfiguration.httpMethod()).thenReturn(HttpMethod.POST);

        graphqlClient = Mockito.spy(new GraphqlClientImpl());
        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "configuration", graphqlClientConfiguration);

        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, 200, "{products(filter:{sku");

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");
        context.request().setServletPath(PAGE + ".html/beaumont-summit-kit.html"); // used by context.request().getRequestURI();

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productResource.getValueMap());

        XSSAPI xssApi = mock(XSSAPI.class);
        when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
        slingBindings.put("xssApi", xssApi);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyBoolean())).then(i -> i.getArgumentAt(1, Boolean.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);

        // context.request().adaptTo(ProductImpl.class); is moved to each test because it uses an internal cache
        // and we want to override the "slug" in testEditModePlaceholderData()

    }

    protected void adaptToProduct() {
        productModel = context.request().adaptTo(ProductImpl.class);
    }

    @Test
    public void testGetIdentifierFromSelector() {
        adaptToProduct();

        String identifier = (String) Whitebox.getInternalState(productModel.getProductRetriever(), "identifier");
        Assert.assertEquals("MJ01", identifier);
    }

    @Test
    public void testGetIdentifierFromProperty() {
        // Use different page
        context.currentResource("/content/product-of-the-week");
        context.request().setServletPath("/content/product-of-the-week/jcr:content/root/responsivegrid/product.beaumont-summit-kit.html");
        productResource = context.resourceResolver().getResource("/content/product-of-the-week/jcr:content/root/responsivegrid/product");

        // Update product properties in sling bindings
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productResource);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productResource.getValueMap());

        adaptToProduct();

        String sku = (String) Whitebox.getInternalState(productModel.getProductRetriever(), "identifier");
        Assert.assertEquals("MJ01", sku);
    }

    @Test
    public void testProduct() {
        testProductImpl(false);
    }

    public void testProductImpl(boolean hasStagedData) {
        adaptToProduct();
        testProduct(product, true);

        if (hasStagedData) {
            Assert.assertTrue("The product has staged data", productModel.isStaged());
        } else {
            Assert.assertFalse("The product doesn't have staged data", productModel.isStaged());
        }

        // We don't return these fields for the EDIT placeholder data
        Assert.assertEquals(product.getMetaDescription(), productModel.getMetaDescription());
        Assert.assertEquals(product.getMetaKeyword(), productModel.getMetaKeywords());
        Assert.assertEquals(product.getMetaTitle(), productModel.getMetaTitle());
        Assert.assertEquals("https://author" + PAGE + ".html/beaumont-summit-kit.html", productModel.getCanonicalUrl());
    }

    private void testProduct(ProductInterface product, boolean loadClientPrice) {
        Assert.assertTrue("The product is found", productModel.getFound());
        Assert.assertEquals(product.getSku(), productModel.getSku());
        Assert.assertEquals(product.getName(), productModel.getName());
        Assert.assertEquals(product.getDescription().getHtml(), productModel.getDescription());
        Assert.assertEquals(loadClientPrice, productModel.loadClientPrice());
        Assert.assertEquals(ProductStockStatus.IN_STOCK.equals(product.getStockStatus()), productModel.getInStock().booleanValue());

        Assert.assertEquals(product.getMediaGallery().size(), productModel.getAssets().size());
        for (int j = 0; j < product.getMediaGallery().size(); j++) {
            MediaGalleryInterface mge = product.getMediaGallery().get(j);
            Asset asset = productModel.getAssets().get(j);
            Assert.assertEquals(mge.getLabel(), asset.getLabel());
            Assert.assertEquals(mge.getPosition(), asset.getPosition());
            Assert.assertEquals("image", asset.getType());
            Assert.assertEquals(mge.getUrl(), asset.getPath());
        }

        Assert.assertTrue(productModel.getGroupedProductItems().isEmpty());
    }

    @Test
    public void testVariants() {
        adaptToProduct();
        List<Variant> variants = productModel.getVariants();
        Assert.assertNotNull(variants);

        ConfigurableProduct cp = (ConfigurableProduct) product;
        Assert.assertEquals(cp.getVariants().size(), variants.size());

        for (int i = 0; i < variants.size(); i++) {
            Variant variant = variants.get(i);
            SimpleProduct sp = cp.getVariants().get(i).getProduct();

            Assert.assertEquals(sp.getSku(), variant.getSku());
            Assert.assertEquals(sp.getName(), variant.getName());
            Assert.assertEquals(sp.getDescription().getHtml(), variant.getDescription());
            Assert.assertEquals(ProductStockStatus.IN_STOCK.equals(sp.getStockStatus()), variant.getInStock().booleanValue());
            Assert.assertEquals(sp.getColor(), variant.getColor());

            Assert.assertEquals(sp.getMediaGallery().size(), variant.getAssets().size());
            for (int j = 0; j < sp.getMediaGallery().size(); j++) {
                MediaGalleryInterface mge = sp.getMediaGallery().get(j);
                Asset asset = variant.getAssets().get(j);
                Assert.assertEquals(mge.getLabel(), asset.getLabel());
                Assert.assertEquals(mge.getPosition(), asset.getPosition());
                Assert.assertEquals("image", asset.getType());
                Assert.assertEquals(mge.getUrl(), asset.getPath());
            }
        }
    }

    @Test
    public void testGetVariantAttributes() {
        adaptToProduct();
        List<VariantAttribute> attributes = productModel.getVariantAttributes();
        Assert.assertNotNull(attributes);

        ConfigurableProduct cp = (ConfigurableProduct) product;
        Assert.assertEquals(cp.getConfigurableOptions().size(), attributes.size());

        for (int i = 0; i < attributes.size(); i++) {
            VariantAttribute attribute = attributes.get(i);
            ConfigurableProductOptions option = cp.getConfigurableOptions().get(i);

            Assert.assertEquals(option.getAttributeCode(), attribute.getId());
            Assert.assertEquals(option.getLabel(), attribute.getLabel());

            for (int j = 0; j < attribute.getValues().size(); j++) {
                VariantValue value = attribute.getValues().get(j);
                ConfigurableProductOptionsValues optionValue = option.getValues().get(j);
                Assert.assertEquals(optionValue.getValueIndex(), value.getId());
                Assert.assertEquals(optionValue.getLabel(), value.getLabel());
            }
        }
    }

    @Test
    public void testSimpleProduct() {
        adaptToProduct();
        Whitebox.setInternalState(productModel.getProductRetriever(), "product", Optional.of(new SimpleProduct()));
        Assert.assertTrue(productModel.getVariants().isEmpty());
        Assert.assertTrue(productModel.getVariantAttributes().isEmpty());
    }

    @Test
    public void testSafeDescriptionWithNull() {
        adaptToProduct();
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        when(product.getDescription()).thenReturn(null);
        Whitebox.setInternalState(productModel.getProductRetriever(), "product", Optional.of(product));
        Assert.assertNull(productModel.getDescription());
    }

    @Test
    public void testSafeDescriptionHtmlNull() {
        adaptToProduct();
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(null);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(productModel.getProductRetriever(), "product", Optional.of(product));

        Assert.assertNull(productModel.getDescription());
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

        Assert.assertEquals(sampleString, productModel.getDescription());
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

        Assert.assertEquals(sampleString, productModel.getDescription());
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

        Assert.assertTrue(price.isRange());
        Assert.assertFalse(price.isDiscounted());
        Assert.assertEquals(58.0, price.getRegularPrice(), 0.001);
        Assert.assertEquals(58.0, price.getFinalPrice(), 0.001);
        Assert.assertEquals(62.0, price.getRegularPriceMax(), 0.001);
        Assert.assertEquals(62.0, price.getFinalPriceMax(), 0.001);
    }

    @Test
    public void testDiscountedPrice() {
        adaptToProduct();
        Price price = productModel.getVariants().get(0).getPriceRange();

        Assert.assertFalse(price.isRange());
        Assert.assertTrue(price.isDiscounted());
        Assert.assertEquals(58.0, price.getRegularPrice(), 0.001);
        Assert.assertEquals(46.0, price.getFinalPrice(), 0.001);
        Assert.assertEquals(12.0, price.getDiscountAmount(), 0.001);
        Assert.assertEquals(20.69, price.getDiscountPercent(), 0.001);
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
        Assert.assertTrue(productModel.isGroupedProduct());
        Assert.assertEquals(4, items.size());

        if (hasStagedData) {
            Assert.assertTrue("The product has staged data", productModel.isStaged());
        } else {
            Assert.assertFalse("The product doesn't have staged data", productModel.isStaged());
        }

        GroupedProduct gp = (GroupedProduct) product;
        for (int i = 0; i < items.size(); i++) {
            GroupItem item = items.get(i);
            ProductInterface pi = gp.getItems().get(i).getProduct();

            Assert.assertEquals(pi.getSku(), item.getSku());
            Assert.assertEquals(pi.getName(), item.getName());
            Assert.assertEquals(pi.getPriceRange().getMinimumPrice().getFinalPrice().getValue(), item.getPriceRange().getFinalPrice(), 0);
            Assert.assertEquals(gp.getItems().get(i).getQty(), item.getDefaultQuantity(), 0);

            Assert.assertEquals(pi instanceof VirtualProduct, item.isVirtualProduct());
        }
    }

    @Test
    public void testVirtualProduct() throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-virtualproduct-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-virtualproduct-result.json", httpClient, 200, "{products(filter:{sku");
        adaptToProduct();

        Assert.assertNotNull("Product model is not null", productModel);
        Assert.assertTrue(productModel.isVirtualProduct());
        Assert.assertFalse("The product doesn't have staged data", productModel.isStaged());
    }

    @Test
    public void testBundleProduct() throws IOException {
        testBundleProductImpl(false);
    }

    public void testBundleProductImpl(boolean hasStagedData) throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-bundleproduct-result.json", httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-bundleproduct-result.json", httpClient, 200, "{products(filter:{sku");
        adaptToProduct();

        Assert.assertNotNull("Product model is not null", productModel);
        Assert.assertTrue(productModel.isBundleProduct());

        if (hasStagedData) {
            Assert.assertTrue("The product has staged data", productModel.isStaged());
        } else {
            Assert.assertFalse("The product doesn't have staged data", productModel.isStaged());
        }
    }

    @Test
    public void testProductNoGraphqlClient() {
        when(productResource.adaptTo(ComponentsConfiguration.class)).thenReturn(ComponentsConfiguration.EMPTY);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(ComponentsConfiguration.EMPTY);
        adaptToProduct();

        Assert.assertFalse("Product is not found", productModel.getFound());
        Assert.assertFalse("Product is not configurable", productModel.isConfigurable());
        Assert.assertFalse("Product is not a grouped product", productModel.isGroupedProduct());
        Assert.assertFalse("Product is not virtual", productModel.isVirtualProduct());
        Assert.assertNull("The product retriever is not created", productModel.getProductRetriever());
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
        Assert.assertFalse("Product is not found", productModel.getFound());

        // verify that graphql was called only once
        verify(graphqlClient, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    public void testClientLoadingIsDisabledOnLaunchPage() {
        adaptToProduct();
        Assert.assertTrue(productModel.loadClientPrice());
        Page launch = context.pageManager().getPage("/content/launches/2020/09/14/mylaunch" + PAGE);
        Whitebox.setInternalState(productModel, "currentPage", launch);
        Assert.assertFalse(productModel.loadClientPrice());
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
        Assert.assertFalse(productModel.getFound());
        Mockito.verify(graphqlClient, never()).execute(any(), any(), any());
        Mockito.verify(graphqlClient, never()).execute(any(), any(), any(), any());

        // Test canonical url on publish
        Assert.assertEquals("https://publish" + PAGE + ".html", productModel.getCanonicalUrl());
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
        productModel = context.request().adaptTo(ProductImpl.class);
        ObjectMapper mapper = new ObjectMapper();
        String expected = Utils.getResource("storefront-context/result-storefront-context-product-component.json");
        String jsonResult = productModel.getStorefrontContext().getJson();
        assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }
}
