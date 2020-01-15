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
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;

public class ProductTeaserImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            // Load page structure
            context.load()
                .json(contentPath, "/content");
        }, ResourceResolverType.JCR_MOCK);
    }

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PRODUCT_SPECIFIC_PAGE = PRODUCT_PAGE + "/product-specific-page";
    private static final String PAGE = "/content/pageA";

    private static final String PRODUCTTEASER_SIMPLE = "/content/pageA/jcr:content/root/responsivegrid/productteaser-simple";
    private static final String PRODUCTTEASER_VARIANT = "/content/pageA/jcr:content/root/responsivegrid/productteaser-variant";
    private static final String PRODUCTTEASER_PATH = "/content/pageA/jcr:content/root/responsivegrid/productteaser-path";

    private Resource teaserResource;

    private ProductTeaserImpl productTeaser;

    private ProductInterface product;

    public void setUp(String resourcePath) throws Exception {
        Page page = context.currentPage(PAGE);
        context.currentResource(resourcePath);
        teaserResource = Mockito.spy(context.resourceResolver().getResource(resourcePath));

        String json = getResource("/graphql/magento-graphql-productteaser-result.json");
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        product = rootQuery.getProducts().getItems().get(0);

        GraphqlResponse<Object, Object> response = new GraphqlResponse<>();
        response.setData(rootQuery);
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(teaserResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        Mockito.when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(teaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, teaserResource.getValueMap());

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
        setUp(PRODUCTTEASER_VARIANT);
        Assert.assertEquals("addToCart", productTeaser.getCallToAction());
        Assert.assertEquals("MJ01-XS-Orange", productTeaser.getSku());
    }

    public void verifyProduct(String resourcePath) throws Exception {
        setUp(resourcePath);

        Assert.assertEquals(product.getName(), productTeaser.getName());
        Page productPage = context.pageManager().getPage(PRODUCT_PAGE);

        // There is a dedicated specific subpage for that product
        Assert.assertTrue(productTeaser.getUrl().startsWith(PRODUCT_SPECIFIC_PAGE));
        Assert.assertEquals(SiteNavigation.toProductUrl(productPage, product.getUrlKey()), productTeaser.getUrl());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        Money amount = product.getPrice().getRegularPrice().getAmount();
        priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
        Assert.assertEquals(priceFormatter.format(amount.getValue()), productTeaser.getFormattedPrice());

        Assert.assertEquals(product.getImage().getUrl(), productTeaser.getImage());
    }

    @Test
    public void verifyProductVariant() throws Exception {
        setUp(PRODUCTTEASER_VARIANT);

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
        Assert.assertEquals(SiteNavigation.toProductUrl(productPage, product.getUrlKey(), variantSku), productTeaser.getUrl());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        Money amount = variant.getPrice().getRegularPrice().getAmount();
        priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
        Assert.assertEquals(priceFormatter.format(amount.getValue()), productTeaser.getFormattedPrice());

        Assert.assertEquals(variant.getImage().getUrl(), productTeaser.getImage());
    }

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }
}
