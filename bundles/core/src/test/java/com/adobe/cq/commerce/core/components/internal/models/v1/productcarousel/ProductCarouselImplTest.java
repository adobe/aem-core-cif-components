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

package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductCarouselImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");
    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
            "my-store"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PAGE = "/content/pageA";
    private static final String PRODUCTCAROUSEL = "/content/pageA/jcr:content/root/responsivegrid/productcarousel";

    private Resource carouselResource;
    private ProductCarouselImpl productCarousel;
    private List<ProductInterface> products;
    private String[] productSkuArray;

    @Before
    public void setUp() throws Exception {
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(PRODUCTCAROUSEL);
        carouselResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTCAROUSEL));

        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-productcarousel-result.json");
        products = rootQuery.getProducts().getItems();

        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-productcarousel-result.json");
        Mockito.when(carouselResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);

        Resource pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(carouselResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_STYLE, style);

        productSkuArray = (String[]) carouselResource.getValueMap().get("product"); // The HTL script uses an alias here
        slingBindings.put("productSkuList", productSkuArray);

        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);
    }

    @Test
    public void getProducts() {

        Assert.assertEquals("h2", productCarousel.getTitleType());

        List<ProductListItem> items = productCarousel.getProducts();
        Assert.assertEquals(4, items.size()); // one product is not found and the JSON response contains a "faulty" product

        List<String> productSkuList = Arrays.asList(productSkuArray)
            .stream()
            .map(s -> s.startsWith("/") ? StringUtils.substringAfterLast(s, "/") : s)
            .collect(Collectors.toList());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);

        int idx = 0;
        for (String combinedSku : productSkuList) {
            Pair<String, String> skus = SiteNavigation.toProductSkus(combinedSku);
            ProductInterface product = products.stream().filter(p -> p.getSku().equals(skus.getLeft())).findFirst().orElse(null);
            if (product == null) {
                continue; // Can happen that a product is not found in the Magento JSON response
            }

            if (!items.stream().filter(i -> i.getSKU().equals(skus.getLeft())).findFirst().isPresent()) {
                continue; // A "faulty" product does not appear in the parsed product instances
            }

            ProductInterface productOrVariant = toProductOrVariant(product, skus);
            ProductListItem item = items.get(idx);

            Assert.assertEquals(productOrVariant.getName(), item.getTitle());
            Assert.assertEquals(product.getSku(), item.getSKU());
            Assert.assertEquals(product.getUrlKey(), item.getSlug());

            Page productPage = context.pageManager().getPage(PRODUCT_PAGE);
            SiteNavigation siteNavigation = new SiteNavigation(context.request());
            Assert.assertEquals(siteNavigation.toProductUrl(productPage, product.getUrlKey(), skus.getRight()), item.getURL());

            Money amount = productOrVariant.getPriceRange().getMinimumPrice().getFinalPrice();
            Assert.assertEquals(amount.getValue(), item.getPrice(), 0);
            Assert.assertEquals(amount.getCurrency().toString(), item.getCurrency());
            priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
            Assert.assertEquals(priceFormatter.format(amount.getValue()), item.getFormattedPrice());

            ProductImage thumbnail = productOrVariant.getThumbnail();
            if (thumbnail == null) {
                // if thumbnail is missing for a product in GraphQL response then thumbnail is null for the related item
                Assert.assertNull(item.getImageURL());
            } else {
                Assert.assertEquals(thumbnail.getUrl(), item.getImageURL());
            }
            idx++;
        }
    }

    protected ProductInterface toProductOrVariant(ProductInterface product, Pair<String, String> skus) {
        if (!(product instanceof ConfigurableProduct) || skus.getRight() == null) {
            return product;
        }

        List<ConfigurableVariant> variants = ((ConfigurableProduct) product).getVariants();
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        String variantSku = skus.getRight();
        return variants.stream().map(v -> v.getProduct()).filter(sp -> variantSku.equals(sp.getSku())).findFirst().orElse(null);
    }
}
