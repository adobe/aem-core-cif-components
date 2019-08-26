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

package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.MediaGalleryEntry;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductStockStatus;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.StoreConfig;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PAGE = "/content/pageA";
    private static final String PRODUCT = "/content/pageA/jcr:content/root/responsivegrid/product";

    private Resource productResource;
    private ProductImpl productModel;
    private ProductInterface product;
    private StoreConfig storeConfig;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(PAGE);
        context.currentResource(PRODUCT);
        productResource = Mockito.spy(context.resourceResolver().getResource(PRODUCT));

        String json = getResource("/graphql/magento-graphql-product-result.json");
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        product = rootQuery.getProducts().getItems().get(0);
        storeConfig = rootQuery.getStoreConfig();

        GraphqlResponse<Object, Object> response = new GraphqlResponse<>();
        response.setData(rootQuery);
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(productResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        Mockito.when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("beaumont-summit-kit");

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

        productModel = context.request().adaptTo(ProductImpl.class);
    }

    @Test
    public void testProduct() {
        Assert.assertTrue(productModel.getFound());
        Assert.assertEquals(product.getSku(), productModel.getSku());
        Assert.assertEquals(product.getName(), productModel.getName());
        Assert.assertEquals(product.getDescription().getHtml(), productModel.getDescription());
        Assert.assertTrue(productModel.loadClientPrice());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        priceFormatter.setCurrency(Currency.getInstance(product.getPrice().getRegularPrice().getAmount().getCurrency().toString()));

        Money amount = product.getPrice().getRegularPrice().getAmount();
        Assert.assertEquals(priceFormatter.format(amount.getValue()), productModel.getFormattedPrice());

        Assert.assertEquals(ProductStockStatus.IN_STOCK.equals(product.getStockStatus()), productModel.getInStock().booleanValue());

        Assert.assertEquals(product.getMediaGalleryEntries().size(), productModel.getAssets().size());
        String baseMediaPath = storeConfig.getSecureBaseMediaUrl() + "catalog/product";
        for (int j = 0; j < product.getMediaGalleryEntries().size(); j++) {
            MediaGalleryEntry mge = product.getMediaGalleryEntries().get(j);
            Asset asset = productModel.getAssets().get(j);
            Assert.assertEquals(mge.getLabel(), asset.getLabel());
            Assert.assertEquals(mge.getPosition(), asset.getPosition());
            Assert.assertEquals(mge.getMediaType(), asset.getType());
            Assert.assertEquals(baseMediaPath + mge.getFile(), asset.getPath());
        }
    }

    @Test
    public void testVariants() {
        List<Variant> variants = productModel.getVariants();
        Assert.assertNotNull(variants);

        ConfigurableProduct cp = (ConfigurableProduct) product;
        Assert.assertEquals(cp.getVariants().size(), variants.size());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        priceFormatter.setCurrency(Currency.getInstance(product.getPrice().getRegularPrice().getAmount().getCurrency().toString()));

        for (int i = 0; i < variants.size(); i++) {
            Variant variant = variants.get(i);
            SimpleProduct sp = cp.getVariants().get(i).getProduct();

            Assert.assertEquals(sp.getSku(), variant.getSku());
            Assert.assertEquals(sp.getName(), variant.getName());
            Assert.assertEquals(sp.getDescription().getHtml(), variant.getDescription());

            Money amount = cp.getPrice().getRegularPrice().getAmount();
            Assert.assertEquals(priceFormatter.format(amount.getValue()), variant.getFormattedPrice());

            Assert.assertEquals(ProductStockStatus.IN_STOCK.equals(sp.getStockStatus()), variant.getInStock().booleanValue());
            Assert.assertEquals(sp.getColor(), variant.getColor());

            Assert.assertEquals(sp.getMediaGalleryEntries().size(), variant.getAssets().size());
            String baseMediaPath = storeConfig.getSecureBaseMediaUrl() + "catalog/product";
            for (int j = 0; j < sp.getMediaGalleryEntries().size(); j++) {
                MediaGalleryEntry mge = sp.getMediaGalleryEntries().get(j);
                Asset asset = variant.getAssets().get(j);
                Assert.assertEquals(mge.getLabel(), asset.getLabel());
                Assert.assertEquals(mge.getPosition(), asset.getPosition());
                Assert.assertEquals(mge.getMediaType(), asset.getType());
                Assert.assertEquals(baseMediaPath + mge.getFile(), asset.getPath());
            }
        }
    }

    @Test
    public void testGetVariantAttributes() {
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
        Whitebox.setInternalState(productModel, "product", new SimpleProduct());
        Assert.assertTrue(productModel.getVariants().isEmpty());
        Assert.assertTrue(productModel.getVariantAttributes().isEmpty());
    }

    @Test
    public void testSafeDescriptionWithNull() {
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        when(product.getDescription()).thenReturn(null);
        Whitebox.setInternalState(productModel, "product", product);
        Assert.assertNull(productModel.getDescription());
    }

    @Test
    public void testSafeDescriptionHtmlNull() {
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(null);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(productModel, "product", product);

        Assert.assertNull(productModel.getDescription());
    }

    @Test
    public void testSafeDescription() {
        String sampleString = "<strong>abc</strong>";
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(sampleString);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(productModel, "product", product);

        Assert.assertEquals(sampleString, productModel.getDescription());
    }

    @Test
    public void testSafeDescriptionConfigurableProduct() {
        String sampleString = "<strong>def</strong>";
        ConfigurableProduct product = mock(ConfigurableProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(sampleString);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(productModel, "product", product);

        Assert.assertEquals(sampleString, productModel.getDescription());
    }

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }
}
