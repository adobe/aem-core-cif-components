/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.core.components.internal.models.v1.productteaser;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.when;

public class ProductTeaserImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
            "my-store"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            // Load page structure
            context.load().json(contentPath, "/content");

            UrlProviderImpl urlProvider = new UrlProviderImpl();
            urlProvider.activate(new MockUrlProviderConfiguration());
            context.registerService(UrlProvider.class, urlProvider);
            ConfigurationBuilder mockConfigBuilder = Utils.getDataLayerConfig(true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
        }, ResourceResolverType.JCR_MOCK);
    }

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PRODUCT_SPECIFIC_PAGE = PRODUCT_PAGE + "/product-specific-page";
    private static final String PAGE = "/content/pageA";

    private static final String PRODUCTTEASER_SIMPLE = "/content/pageA/jcr:content/root/responsivegrid/productteaser-simple";
    private static final String PRODUCTTEASER_VIRTUAL = "/content/pageA/jcr:content/root/responsivegrid/productteaser-virtual";
    private static final String PRODUCTTEASER_VARIANT = "/content/pageA/jcr:content/root/responsivegrid/productteaser-variant";
    private static final String PRODUCTTEASER_PATH = "/content/pageA/jcr:content/root/responsivegrid/productteaser-path";
    private static final String PRODUCTTEASER_NOCLIENT = "/content/pageA/jcr:content/root/responsivegrid/productteaser-noclient";

    private Resource teaserResource;
    private Resource pageResource;
    private ProductTeaserImpl productTeaser;
    private ProductInterface product;

    public void setUp(String resourcePath, boolean deepLink) throws Exception {
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(resourcePath);
        teaserResource = Mockito.spy(context.resourceResolver().getResource(resourcePath));

        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-productteaser-result.json");
        product = rootQuery.getProducts().getItems().get(0);

        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-productteaser-result.json");
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
            context.request().setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
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
        Page productPage = context.pageManager().getPage(PRODUCT_PAGE);

        // There is a dedicated specific subpage for that product
        Assert.assertTrue(productTeaser.getUrl().startsWith(PRODUCT_SPECIFIC_PAGE));
        SiteNavigation siteNavigation = new SiteNavigation(context.request());
        Assert.assertEquals(siteNavigation.toPageUrl(productPage, product.getUrlKey()), productTeaser.getUrl());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        Money amount = product.getPriceRange().getMinimumPrice().getFinalPrice();
        priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
        Assert.assertEquals(priceFormatter.format(amount.getValue()), productTeaser.getFormattedPrice());

        Assert.assertEquals(product.getImage().getUrl(), productTeaser.getImage());
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
        Page productPage = context.pageManager().getPage(PRODUCT_PAGE);
        SiteNavigation siteNavigation = new SiteNavigation(context.request());
        Assert.assertEquals(siteNavigation.toProductUrl(productPage, product.getUrlKey(), variantSku), productTeaser.getUrl());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        Money amount = variant.getPriceRange().getMinimumPrice().getFinalPrice();
        priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
        Assert.assertEquals(priceFormatter.format(amount.getValue()), productTeaser.getFormattedPrice());

        Assert.assertEquals(variant.getImage().getUrl(), productTeaser.getImage());
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
    }

    @Test
    public void testVirtualProduct() throws IOException {
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(PRODUCTTEASER_VIRTUAL);
        Resource teaserResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTTEASER_VIRTUAL));
        when(teaserResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-virtualproduct-result.json");
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
    public void testJsonRender() throws Exception {
        setUp(PRODUCTTEASER_SIMPLE, true);
        ObjectMapper mapper = new ObjectMapper();
        String expected = Utils.getResource("results/result-datalayer-productteaser-component.json");
        String jsonResult = productTeaser.getData().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }
}
