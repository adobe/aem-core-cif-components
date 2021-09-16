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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
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
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.mockito.Mockito.when;

public class ProductTeaserImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
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

    private Resource teaserResource;
    private Resource pageResource;
    private ProductTeaserImpl productTeaser;
    private ProductInterface product;
    private boolean deepLink;

    public void setUp(String resourcePath, boolean deepLink) throws Exception {
        this.deepLink = deepLink;
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(resourcePath);
        teaserResource = Mockito.spy(context.resourceResolver().getResource(resourcePath));

        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-productteaser-result.json");
        product = rootQuery.getProducts().getItems().get(0);

        GraphqlClient graphqlClient = new GraphqlClientImpl();
        context.registerInjectActivateService(graphqlClient);
        Utils.addHttpResponseFrom(graphqlClient, "graphql/magento-graphql-productteaser-result.json");
        when(teaserResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(teaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, teaserResource.getValueMap());

        if (deepLink) {
            // Configure the component to create deep links to specific pages
            context.runMode("author");
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
        Assert.assertEquals("addToCart", productTeaser.getCallToAction());
        Assert.assertEquals("MJ01-XS-Orange", productTeaser.getSku());
    }

    public void verifyProduct(String resourcePath) throws Exception {
        setUp(resourcePath, true);

        Assert.assertEquals(product.getName(), productTeaser.getName());

        // There is a dedicated specific subpage for that product
        Assert.assertTrue(productTeaser.getUrl().startsWith(PRODUCT_SPECIFIC_PAGE));
        Assert.assertEquals(toProductUrl(product), productTeaser.getUrl());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        Money amount = product.getPriceRange().getMinimumPrice().getFinalPrice();
        priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
        Assert.assertEquals(priceFormatter.format(amount.getValue()), productTeaser.getPriceRange().getFormattedFinalPrice());

        Assert.assertEquals(product.getImage().getUrl(), productTeaser.getImage());
        Assert.assertEquals(product.getImage().getLabel(), productTeaser.getImageAlt());
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

        Assert.assertEquals(variant.getName(), productTeaser.getName());
        Assert.assertEquals(toProductUrl(product, variantSku), productTeaser.getUrl());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        Money amount = variant.getPriceRange().getMinimumPrice().getFinalPrice();
        priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
        Assert.assertEquals(priceFormatter.format(amount.getValue()), productTeaser.getPriceRange().getFormattedFinalPrice());

        Assert.assertEquals(variant.getImage().getUrl(), productTeaser.getImage());
        Assert.assertEquals(variant.getImage().getLabel(), productTeaser.getImageAlt());
    }

    @Test
    public void verifyProductTeaserNoGraphqlCLient() {
        Page page = context.currentPage(PAGE);
        context.currentResource(PRODUCTTEASER_NOCLIENT);
        Resource teaserResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTTEASER_NOCLIENT));
        when(teaserResource.adaptTo(GraphqlClient.class)).thenReturn(null);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(teaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, teaserResource.getValueMap());

        ProductTeaserImpl productTeaserNoClient = context.request().adaptTo(ProductTeaserImpl.class);

        Assert.assertNull(productTeaserNoClient.getProductRetriever());
        Assert.assertNull(productTeaserNoClient.getUrl());
        Assert.assertNull(productTeaserNoClient.getImage());
        Assert.assertNull(productTeaserNoClient.getImageAlt());
    }

    @Test
    public void testVirtualProduct() throws IOException {
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(PRODUCTTEASER_VIRTUAL);
        Resource teaserResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTTEASER_VIRTUAL));
        when(teaserResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        GraphqlClient graphqlClient = new GraphqlClientImpl();
        context.registerInjectActivateService(graphqlClient);
        Utils.addHttpResponseFrom(graphqlClient, "graphql/magento-graphql-virtualproduct-result.json");
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(teaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, teaserResource.getValueMap());

        productTeaser = context.request().adaptTo(ProductTeaserImpl.class);
        Assert.assertTrue(productTeaser.isVirtualProduct());
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
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
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
