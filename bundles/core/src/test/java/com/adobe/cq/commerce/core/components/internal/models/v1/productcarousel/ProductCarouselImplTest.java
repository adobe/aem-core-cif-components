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
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductCarouselImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true", ProductCarouselBase.PN_CONFIG_ENABLE_WISH_LISTS, "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PAGE = "/content/pageA";
    private static final String PRODUCTCAROUSEL = "/content/pageA/jcr:content/root/responsivegrid/productcarousel";
    private static final String PRODUCTCAROUSEL_WITH_CATEGORY = "/content/pageA/jcr:content/root/responsivegrid/productcarousel_with_category";
    private static final String PRODUCTCAROUSEL_WITH_LINK_TARGET_UNCHECKED = "/content/pageA/jcr:content/root/responsivegrid/productcarousel_with_link_target_unchecked";
    private static final String PRODUCTCAROUSEL_WITH_LINK_TARGET_CHECKED = "/content/pageA/jcr:content/root/responsivegrid/productcarousel_with_link_target_checked";

    private Resource carouselResource;
    private ProductCarouselImpl productCarousel;
    private List<ProductInterface> products;
    private String[] productSkuArray;
    private GraphqlClient graphqlClient;

    @Before
    public void setUp() throws Exception {
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(PRODUCTCAROUSEL);
        carouselResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTCAROUSEL));

        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-productcarousel-result.json");
        products = rootQuery.getProducts().getItems();

        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory());
        graphqlClient = new GraphqlClientImpl();
        Utils.registerGraphqlClient(context, graphqlClient, null);

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
    }

    @Test
    public void getProducts() throws IOException {
        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);
        Utils.addHttpResponseFrom(graphqlClient, "graphql/magento-graphql-productcarousel-result.json");

        Assert.assertEquals("h2", productCarousel.getTitleType());
        Assert.assertFalse(productCarousel.isAddToCartEnabled());
        Assert.assertFalse(productCarousel.isAddToWishListEnabled());
        Assert.assertNull(productCarousel.getLinkTarget());

        List<ProductListItem> items = productCarousel.getProducts();
        Assert.assertEquals(4, items.size()); // one product is not found and the JSON response contains a "faulty" product

        List<String> productSkuList = Arrays.stream(productSkuArray)
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

            if (items.stream().noneMatch(i -> i.getSKU().equals(skus.getLeft()))) {
                continue; // A "faulty" product does not appear in the parsed product instances
            }

            ProductInterface productOrVariant = toProductOrVariant(product, skus);
            ProductListItem item = items.get(idx);

            Assert.assertEquals(productOrVariant.getName(), item.getTitle());
            Assert.assertEquals(product.getSku(), item.getSKU());
            Assert.assertEquals(product.getUrlKey(), item.getSlug());
            Assert.assertEquals(toProductUrl(product, skus.getRight()), item.getURL());

            if (idx == 2 || idx == 3) {
                Assert.assertEquals(ProductListItemImpl.CALL_TO_ACTION_DETAILS, item.getCallToAction());
            } else {
                Assert.assertEquals(ProductListItemImpl.CALL_TO_ACTION_ADD_TO_CART, item.getCallToAction());
            }

            Money amount = productOrVariant.getPriceRange().getMinimumPrice().getFinalPrice();
            Assert.assertEquals(amount.getValue(), item.getPriceRange().getFinalPrice(), 0);
            Assert.assertEquals(amount.getCurrency().toString(), item.getPriceRange().getCurrency());
            priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
            Assert.assertEquals(priceFormatter.format(amount.getValue()), item.getPriceRange().getFormattedFinalPrice());

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

    @Test
    public void getProductsForCategory() throws Exception {
        String productsJson = "graphql/magento-graphql-productcarousel-with-category-result.json";
        Utils.addHttpResponseFrom(graphqlClient, productsJson);
        products = Utils.getQueryFromResource(productsJson).getCategoryList().get(0).getProducts().getItems();

        context.currentResource(PRODUCTCAROUSEL_WITH_CATEGORY);

        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        Assert.assertEquals("h2", productCarousel.getTitleType());
        int productCount = 2;

        List<ProductListItem> items = productCarousel.getProducts();
        Assert.assertEquals(productCount, items.size()); // one product is not found and the JSON response contains a "faulty" product

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);

        for (int i = 0; i < productCount; i++) {
            ProductListItem item = items.get(i);
            ProductInterface product = products.get(i);

            Assert.assertEquals(product.getName(), item.getTitle());
            Assert.assertEquals(product.getSku(), item.getSKU());
            Assert.assertEquals(product.getUrlKey(), item.getSlug());
            Assert.assertEquals(toProductUrl(product, null), item.getURL());

            Money amount = product.getPriceRange().getMinimumPrice().getFinalPrice();
            Assert.assertEquals(amount.getValue(), item.getPriceRange().getFinalPrice(), 0);
            Assert.assertEquals(amount.getCurrency().toString(), item.getPriceRange().getCurrency());
            priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
            Assert.assertEquals(priceFormatter.format(amount.getValue()), item.getPriceRange().getFormattedFinalPrice());
            if (i == 0) {
                CategoryInterface category = (CategoryInterface) Whitebox.getInternalState(item, "categoryContext");
                Assert.assertEquals("MTI=", category.getUid().toString());
                Assert.assertEquals("watch", category.getUrlKey());
                Assert.assertEquals("watch", category.getUrlPath());
            }
            ProductImage thumbnail = product.getThumbnail();
            if (thumbnail == null) {
                // if thumbnail is missing for a product in GraphQL response then thumbnail is null for the related item
                Assert.assertNull(item.getImageURL());
            } else {
                Assert.assertEquals(thumbnail.getUrl(), item.getImageURL());
            }
        }
    }

    @Test
    public void getProductsForCategoryDefaultProductCount() throws Exception {
        Utils.addHttpResponseFrom(graphqlClient, "graphql/magento-graphql-productcarousel-with-category-result.json");
        context.currentResource(PRODUCTCAROUSEL_WITH_CATEGORY + "_no_product_count");
        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        Integer productCount = (Integer) Whitebox.getInternalState(productCarousel, "productCount");
        Assert.assertEquals(ProductCarouselImpl.DEFAULT_PRODUCT_COUNT, (int) productCount);
        // one product is not found and the JSON response contains a "faulty" product
        Assert.assertEquals(3, productCarousel.getProducts().size());
    }

    @Test
    public void getProductsForCategoryMinProductCount() throws Exception {
        Utils.addHttpResponseFrom(graphqlClient, "graphql/magento-graphql-productcarousel-with-category-result.json");
        context.currentResource(PRODUCTCAROUSEL_WITH_CATEGORY + "_small_product_count");
        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        Integer productCount = (Integer) Whitebox.getInternalState(productCarousel, "productCount");
        Assert.assertEquals(ProductCarouselImpl.MIN_PRODUCT_COUNT, (int) productCount);
        Assert.assertEquals(ProductCarouselImpl.MIN_PRODUCT_COUNT, productCarousel.getProducts().size());
    }

    @Test
    public void addToCartAndAddToWishList() {
        context.currentResource(PRODUCTCAROUSEL + "_with_add_to_buttons");

        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        Assert.assertTrue(productCarousel.isAddToCartEnabled());
        Assert.assertTrue(productCarousel.isAddToWishListEnabled());
    }

    @Test
    public void testGetProductIdentifiers() {
        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        List<ProductListItem> items = productCarousel.getProductIdentifiers();
        Set<String> expectedIdentifiers = ImmutableSet.of(
            "NOT-FOUND",
            "24-MG01",
            "MJ01",
            "faultyproduct",
            "WJ01");

        assertThat(items.stream().map(ProductListItem::getSKU).collect(Collectors.toList()))
            .hasSize(expectedIdentifiers.size())
            .containsOnlyElementsOf(expectedIdentifiers);
    }

    @Test
    public void getLinkTargetUnchecked() {
        context.currentResource(PRODUCTCAROUSEL_WITH_LINK_TARGET_UNCHECKED);

        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        Assert.assertNotNull(productCarousel);
        Assert.assertNull(productCarousel.getLinkTarget());
    }

    @Test
    public void getLinkTargetChecked() {
        context.currentResource(PRODUCTCAROUSEL_WITH_LINK_TARGET_CHECKED);

        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        Assert.assertNotNull(productCarousel);
        Assert.assertEquals("_blank", productCarousel.getLinkTarget());
    }

    @Test
    public void testJsonExport() {
        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        Utils.testJSONExport(productCarousel, "/exporter/productcarousel.json");
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

    private String toProductUrl(ProductInterface product, String variantPart) {
        Page productPage = context.pageManager().getPage(PRODUCT_PAGE);
        return productPage.getPath() + ".html/" + product.getUrlKey() + ".html" + (variantPart != null ? '#' + variantPart : "");
    }
}
