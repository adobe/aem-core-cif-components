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

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.components.internal.models.v1.productteaser.ProductTeaserImpl.CALL_TO_ACTION_TEXT_ADD_TO_CART;
import static com.adobe.cq.commerce.core.components.internal.models.v1.productteaser.ProductTeaserImpl.CALL_TO_ACTION_TYPE_ADD_TO_CART;
import static com.adobe.cq.commerce.core.components.internal.models.v1.productteaser.ProductTeaserImpl.CALL_TO_ACTION_TYPE_DETAILS;
import static com.adobe.cq.commerce.core.components.internal.models.v1.productteaser.ProductTeaserImpl.PN_STYLE_ADD_TO_WISHLIST_ENABLED;
import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProductTeaserImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            ConfigurationBuilder mockConfigBuilder = mock(ConfigurationBuilder.class);
            Utils.addDataLayerConfig(mockConfigBuilder, true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
            context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory());
        })
        .build();

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PRODUCT_SPECIFIC_PAGE = PRODUCT_PAGE + "/product-specific-page";
    private static final String PAGE = "/content/pageA";

    private static final String PRODUCTTEASER_SIMPLE = "/content/pageA/jcr:content/root/responsivegrid/productteaser-simple";
    private static final String PRODUCTTEASER_NO_SKU = "/content/pageA/jcr:content/root/responsivegrid/productteaser-no-sku";
    private static final String PRODUCTTEASER_VIRTUAL = "/content/pageA/jcr:content/root/responsivegrid/productteaser-virtual";
    private static final String PRODUCTTEASER_VARIANT = "/content/pageA/jcr:content/root/responsivegrid/productteaser-variant";
    private static final String PRODUCTTEASER_PATH = "/content/pageA/jcr:content/root/responsivegrid/productteaser-path";
    private static final String PRODUCTTEASER_NOCLIENT = "/content/pageA/jcr:content/root/responsivegrid/productteaser-noclient";
    private static final String PRODUCTTEASER_FULL = "/content/pageA/jcr:content/root/responsivegrid/productteaser-full";
    private static final String PRODUCTTEASER_CTA_TEST = "/content/pageA/jcr:content/root/responsivegrid/productteaser-cta-test";
    private static final String PRODUCTTEASER_CTA_TEXT_TEST = "/content/pageA/jcr:content/root/responsivegrid/productteaser-cta-text-test";
    private static final String PRODUCTTEASER_LINK_TARGET_UNCHECKED = "/content/pageA/jcr:content/root/responsivegrid/productteaser-link-target-unchecked";
    private static final String PRODUCTTEASER_LINK_TARGET_CHECKED = "/content/pageA/jcr:content/root/responsivegrid/productteaser-link-target-checked";

    private Style style;
    private Resource teaserResource;
    private Resource pageResource;
    private ProductTeaserImpl productTeaser;
    private ProductInterface product;
    private boolean deepLink;

    @Before
    public void setup() {
        style = mock(Style.class);
        when(style.get(any(), anyBoolean())).then(i -> i.getArgumentAt(1, Boolean.class));
    }

    public void setUp(String resourcePath, boolean deepLink) throws Exception {
        setUp(resourcePath, "graphql/magento-graphql-productteaser-result.json", deepLink);
    }

    public void setUp(String resourcePath, String graphqlResultPath, boolean deepLink) throws Exception {
        this.deepLink = deepLink;
        Page page = spy(context.currentPage(PAGE));
        context.currentResource(resourcePath);
        teaserResource = spy(context.resourceResolver().getResource(resourcePath));

        Query rootQuery = Utils.getQueryFromResource(graphqlResultPath);
        product = rootQuery.getProducts().getItems().get(0);

        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Utils.registerGraphqlClient(context, graphqlClient, null);
        ;
        Utils.addHttpResponseFrom(graphqlClient, graphqlResultPath);
        when(teaserResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(teaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_STYLE, style);

        if (deepLink) {
            // TODO: CIF-2469
            // With a newer version of OSGI mock we could re-inject the reference into the existing UrlProviderImpl
            // context.registerInjectActivateService(new SpecificPageStrategy(), "generateSpecificPageUrls", true);
            SpecificPageStrategy specificPageStrategy = context.getService(SpecificPageStrategy.class);
            Whitebox.setInternalState(specificPageStrategy, "generateSpecificPageUrls", true);
        }

        productTeaser = context.request().adaptTo(ProductTeaserImpl.class);
    }

    @Test
    public void verifyProductBySku() throws Exception {
        verifyProduct(PRODUCTTEASER_SIMPLE);
    }

    @Test
    public void verifyProductByPath() throws Exception {
        verifyProduct(PRODUCTTEASER_PATH);
    }

    @Test
    public void verifyCtaDetails() throws Exception {
        setUp(PRODUCTTEASER_VARIANT, false);
        assertEquals(CALL_TO_ACTION_TYPE_ADD_TO_CART, productTeaser.getCallToAction());
        assertEquals("MJ01-XS-Orange", productTeaser.getSku());
    }

    public void verifyProduct(String resourcePath) throws Exception {
        setUp(resourcePath, true);

        assertEquals(product.getName(), productTeaser.getName());
        assertEquals("MJ01", productTeaser.getCombinedSku().getBaseSku());
        assertNull(productTeaser.getCombinedSku().getVariantSku());

        // There is a dedicated specific subpage for that product
        assertTrue(productTeaser.getUrl().startsWith(PRODUCT_SPECIFIC_PAGE));
        assertNull(productTeaser.getLinkTarget());
        assertEquals(toProductUrl(product), productTeaser.getUrl());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        Money amount = product.getPriceRange().getMinimumPrice().getFinalPrice();
        priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
        assertEquals(priceFormatter.format(amount.getValue()), productTeaser.getPriceRange().getFormattedFinalPrice());

        assertEquals(product.getImage().getUrl(), productTeaser.getImage());
        assertEquals(product.getImage().getLabel(), productTeaser.getImageAlt());
    }

    @Test
    public void verifyProductVariant() throws Exception {
        setUp(PRODUCTTEASER_VARIANT, false);

        // Find the selected variant
        ConfigurableProduct cp = (ConfigurableProduct) product;
        String selection = teaserResource.getValueMap().get("selection", String.class);
        String variantSku = SiteNavigation.toProductSkus(selection).getRight();
        SimpleProduct variant = cp.getVariants()
            .stream()
            .map(v -> v.getProduct())
            .filter(sp -> variantSku.equals(sp.getSku()))
            .findFirst()
            .orElse(null);

        assertEquals(variant.getName(), productTeaser.getName());
        assertEquals(toProductUrl(product, variantSku), productTeaser.getUrl());
        assertEquals("MJ01", productTeaser.getCombinedSku().getBaseSku());
        assertEquals(variantSku, productTeaser.getCombinedSku().getVariantSku());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        Money amount = variant.getPriceRange().getMinimumPrice().getFinalPrice();
        priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
        assertEquals(priceFormatter.format(amount.getValue()), productTeaser.getPriceRange().getFormattedFinalPrice());

        assertEquals(variant.getImage().getUrl(), productTeaser.getImage());
        assertEquals(variant.getImage().getLabel(), productTeaser.getImageAlt());
    }

    @Test
    public void verifyProductTeaserNoGraphqlCLient() {
        Page page = context.currentPage(PAGE);
        context.currentResource(PRODUCTTEASER_NOCLIENT);
        Resource teaserResource = spy(context.resourceResolver().getResource(PRODUCTTEASER_NOCLIENT));
        when(teaserResource.adaptTo(GraphqlClient.class)).thenReturn(null);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(teaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, teaserResource.getValueMap());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_STYLE, style);

        ProductTeaserImpl productTeaserNoClient = context.request().adaptTo(ProductTeaserImpl.class);

        assertNull(productTeaserNoClient.getProductRetriever());
        assertNull(productTeaserNoClient.getUrl());
        assertNull(productTeaserNoClient.getImage());
        assertNull(productTeaserNoClient.getImageAlt());
    }

    @Test
    public void testVirtualProduct() throws IOException {
        Page page = spy(context.currentPage(PAGE));
        context.currentResource(PRODUCTTEASER_VIRTUAL);
        Resource teaserResource = spy(context.resourceResolver().getResource(PRODUCTTEASER_VIRTUAL));
        when(teaserResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Utils.registerGraphqlClient(context, graphqlClient, null);
        Utils.addHttpResponseFrom(graphqlClient, "graphql/magento-graphql-virtualproduct-result.json");
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(teaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, teaserResource.getValueMap());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_STYLE, style);

        productTeaser = context.request().adaptTo(ProductTeaserImpl.class);
        assertTrue(productTeaser.isVirtualProduct());
    }

    @Test
    public void testCtaForConfigurable() throws Exception {
        setUp(PRODUCTTEASER_CTA_TEST, false);
        assertEquals(CALL_TO_ACTION_TYPE_DETAILS, productTeaser.getCallToAction());
        assertEquals(CALL_TO_ACTION_TEXT_ADD_TO_CART, productTeaser.getCallToActionText());
    }

    @Test
    public void testCtaTextForConfigurable() throws Exception {
        setUp(PRODUCTTEASER_CTA_TEXT_TEST, false);
        assertEquals(CALL_TO_ACTION_TYPE_DETAILS, productTeaser.getCallToAction());
        assertEquals("custom", productTeaser.getCallToActionText());
    }

    @Test
    public void testCtaForVirtual() throws Exception {
        setUp(PRODUCTTEASER_CTA_TEST, "graphql/magento-graphql-virtualproduct-result.json", false);
        assertEquals(CALL_TO_ACTION_TYPE_ADD_TO_CART, productTeaser.getCallToAction());
        assertEquals(null, productTeaser.getCallToActionText());
    }

    @Test
    public void testCtaTextForVirtual() throws Exception {
        setUp(PRODUCTTEASER_CTA_TEXT_TEST, "graphql/magento-graphql-virtualproduct-result.json", false);
        assertEquals(CALL_TO_ACTION_TYPE_ADD_TO_CART, productTeaser.getCallToAction());
        assertEquals("custom", productTeaser.getCallToActionText());
    }

    @Test
    public void testCtaForGroup() throws Exception {
        setUp(PRODUCTTEASER_CTA_TEST, "graphql/magento-graphql-groupedproduct-result.json", false);
        assertEquals(CALL_TO_ACTION_TYPE_DETAILS, productTeaser.getCallToAction());
        assertEquals(CALL_TO_ACTION_TEXT_ADD_TO_CART, productTeaser.getCallToActionText());
    }

    @Test
    public void testCtaForBundle() throws Exception {
        setUp(PRODUCTTEASER_CTA_TEST, "graphql/magento-graphql-bundleproduct-result.json", false);
        assertEquals(CALL_TO_ACTION_TYPE_DETAILS, productTeaser.getCallToAction());
        assertEquals(CALL_TO_ACTION_TEXT_ADD_TO_CART, productTeaser.getCallToActionText());
    }

    @Test
    public void testJsonExportNoSku() throws Exception {
        setUp(PRODUCTTEASER_NO_SKU, false);
        Utils.testJSONExport(productTeaser, "/exporter/productteaser-nosku.json");
    }

    @Test
    public void testJsonExportSimple() throws Exception {
        setUp(PRODUCTTEASER_SIMPLE, false);
        Utils.testJSONExport(productTeaser, "/exporter/productteaser.json");
    }

    @Test
    public void testJsonExportFull() throws Exception {
        setUp(PRODUCTTEASER_FULL, false);
        Utils.testJSONExport(productTeaser, "/exporter/productteaser-full.json");
    }

    @Test
    public void testDataLayer() throws Exception {
        setUp(PRODUCTTEASER_SIMPLE, true);
        ObjectMapper mapper = new ObjectMapper();
        String expected = Utils.getResource("results/result-datalayer-productteaser-component.json");
        String jsonResult = productTeaser.getData().getJson();
        assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }

    @Test
    public void testAddToWishListDefault() throws Exception {
        setUp(PRODUCTTEASER_SIMPLE, false);
        assertFalse(productTeaser.getAddToWishListEnabled());
    }

    @Test
    public void testAddToWishListDisabled() throws Exception {
        when(style.get(eq(PN_STYLE_ADD_TO_WISHLIST_ENABLED), anyBoolean())).thenReturn(Boolean.FALSE);
        setUp(PRODUCTTEASER_SIMPLE, false);
        assertFalse(productTeaser.getAddToWishListEnabled());
    }

    @Test
    public void testAddToWishListEnabled() throws Exception {
        when(style.get(eq(PN_STYLE_ADD_TO_WISHLIST_ENABLED), anyBoolean())).thenReturn(Boolean.TRUE);
        setUp(PRODUCTTEASER_SIMPLE, false);
        assertTrue(productTeaser.getAddToWishListEnabled());
    }

    @Test
    public void testLinkTargetUnchecked() throws Exception {
        setUp(PRODUCTTEASER_LINK_TARGET_UNCHECKED, false);
        assertNull(productTeaser.getLinkTarget());
    }

    @Test
    public void testLinkTargetChecked() throws Exception {
        setUp(PRODUCTTEASER_LINK_TARGET_CHECKED, false);
        assertEquals("_blank", productTeaser.getLinkTarget());
    }

    private String toProductUrl(ProductInterface product) {
        Page productPage = deepLink
            ? context.pageManager().getPage(PRODUCT_SPECIFIC_PAGE)
            : context.pageManager().getPage(PRODUCT_PAGE);
        return productPage.getPath() + ".html/" + product.getUrlKey() + ".html";
    }

    private String toProductUrl(ProductInterface product, String variantPart) {
        return toProductUrl(product) + '#' + variantPart;
    }
}
