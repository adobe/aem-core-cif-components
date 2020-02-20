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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;

public class ProductCarouselImplTest {

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

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PAGE = "/content/pageA";
    private static final String PRODUCTCAROUSEL = "/content/pageA/jcr:content/root/responsivegrid/productcarousel";

    private Resource carouselResource;
    private ProductCarouselImpl productCarousel;
    private List<ProductInterface> products;
    private String[] productSkuArray;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(PAGE);
        context.currentResource(PRODUCTCAROUSEL);
        carouselResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTCAROUSEL));

        String json = getResource("/graphql/magento-graphql-productcarousel-result.json");
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        products = rootQuery.getProducts().getItems();

        GraphqlResponse<Object, Object> response = new GraphqlResponse<>();
        response.setData(rootQuery);
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(carouselResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        Mockito.when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(carouselResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

        productSkuArray = (String[]) carouselResource.getValueMap().get("product"); // The HTL script uses an alias here
        slingBindings.put("productSkuList", productSkuArray);

        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);
    }

    @Test
    public void getProducts() {

        List<ProductListItem> items = productCarousel.getProducts();
        Assert.assertFalse(items.isEmpty());

        List<String> productSkuList = Arrays.asList(productSkuArray);
        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);

        for (int i = 0; i < items.size(); i++) {
            ProductListItem item = items.get(i);
            ProductInterface product = products.get(i);

            // Find the combinedSku that was used to fetch that product
            String combinedSku = productSkuList.stream().filter(s -> s.contains(item.getSKU())).findFirst().orElse(null);
            Pair<String, String> skus = SiteNavigation.toProductSkus(combinedSku);
            ProductInterface productOrVariant = toProductOrVariant(product, skus);

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

            Assert.assertEquals(productOrVariant.getThumbnail().getUrl(), item.getImageURL());
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

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }
}
