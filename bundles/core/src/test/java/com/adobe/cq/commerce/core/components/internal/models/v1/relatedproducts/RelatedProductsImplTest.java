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

package com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts.RelatedProductsRetriever.RelationType;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RelatedProductsImplTest {

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

    private static final Map<RelationType, String> RESOURCES_PATHS = new HashMap<>();
    static {
        RESOURCES_PATHS.put(RelationType.RELATED_PRODUCTS, "/content/pageA/jcr:content/root/responsivegrid/relatedproducts");
        RESOURCES_PATHS.put(RelationType.UPSELL_PRODUCTS, "/content/pageA/jcr:content/root/responsivegrid/upsellproducts");
        RESOURCES_PATHS.put(RelationType.CROSS_SELL_PRODUCTS, "/content/pageA/jcr:content/root/responsivegrid/crosssellproducts");
        RESOURCES_PATHS.put(null, "/content/pageA/jcr:content/root/responsivegrid/relatedproducts-without-relation-type");
    }

    private static final Map<RelationType, Function<ProductInterface, List<ProductInterface>>> PRODUCTS_GETTER = new HashMap<>();
    static {
        PRODUCTS_GETTER.put(RelationType.RELATED_PRODUCTS, ProductInterface::getRelatedProducts);
        PRODUCTS_GETTER.put(RelationType.UPSELL_PRODUCTS, ProductInterface::getUpsellProducts);
        PRODUCTS_GETTER.put(RelationType.CROSS_SELL_PRODUCTS, ProductInterface::getCrosssellProducts);
        PRODUCTS_GETTER.put(null, ProductInterface::getRelatedProducts);
    }

    private Resource relatedProductsResource;
    private RelatedProductsImpl relatedProducts;
    private List<ProductInterface> products;

    private void setUp(RelationType relationType, String jsonResponsePath, boolean addSlugInSelector) throws Exception {
        Page page = context.currentPage(PAGE);
        String resourcePath = RESOURCES_PATHS.get(relationType);
        context.currentResource(resourcePath);
        relatedProductsResource = Mockito.spy(context.resourceResolver().getResource(resourcePath));

        if (addSlugInSelector) {
            MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
            requestPathInfo.setSelectorString("endurance-watch");
        }

        String json = getResource(jsonResponsePath);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        ProductInterface product = rootQuery.getProducts().getItems().get(0);
        products = PRODUCTS_GETTER.get(relationType).apply(product);

        GraphqlResponse<Object, Object> response = new GraphqlResponse<>();
        response.setData(rootQuery);
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(relatedProductsResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        Mockito.when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(relatedProductsResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put("properties", relatedProductsResource.getValueMap());

        relatedProducts = context.request().adaptTo(RelatedProductsImpl.class);
    }

    @Test
    public void testRelatedProducts() throws Exception {
        setUp(RelationType.RELATED_PRODUCTS, "/graphql/magento-graphql-relatedproducts-result.json", false);
        assertProducts();
    }

    @Test
    public void testRelatedProductsWithoutRelationType() throws Exception {
        setUp(null, "/graphql/magento-graphql-relatedproducts-result.json", false);
        assertProducts();
    }

    @Test
    public void testUpsellProducts() throws Exception {
        setUp(RelationType.UPSELL_PRODUCTS, "/graphql/magento-graphql-upsellproducts-result.json", true);
        assertProducts();
    }

    @Test
    public void testCrossSellProducts() throws Exception {
        setUp(RelationType.CROSS_SELL_PRODUCTS, "/graphql/magento-graphql-crosssellproducts-result.json", true);
        assertProducts();
    }

    @Test
    public void testIsNotConfigured() throws Exception {
        setUp(RelationType.CROSS_SELL_PRODUCTS, "/graphql/magento-graphql-crosssellproducts-result.json", false);
        Assert.assertTrue(relatedProducts.getProducts().isEmpty());
    }

    @Test
    public void testQueryExtensions() throws Exception {
        setUp(RelationType.RELATED_PRODUCTS, "/graphql/magento-graphql-relatedproducts-result.json", false);
        AbstractProductsRetriever retriever = relatedProducts.getProductsRetriever();
        retriever.extendProductQueryWith(p -> p.description(d -> d.html()));
        retriever.extendVariantQueryWith(v -> v.sku());

        MagentoGraphqlClient client = (MagentoGraphqlClient) Whitebox.getInternalState(retriever, "client");
        MagentoGraphqlClient clientSpy = Mockito.spy(client);
        Whitebox.setInternalState(retriever, "client", clientSpy);

        assertProducts();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(clientSpy, times(1)).execute(captor.capture());

        String expectedQuery = "{products(filter:{sku:{eq:\"24-MG01\"}}){items{__typename,related_products{__typename,sku,name,thumbnail{label,url},url_key,price{regularPrice{amount{currency,value}}},... on ConfigurableProduct{variants{product{sku}}},description{html}}}}}";
        Assert.assertEquals(expectedQuery, captor.getValue());
    }

    private void assertProducts() {
        List<ProductListItem> items = relatedProducts.getProducts();
        Assert.assertFalse(items.isEmpty());
        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);

        for (int i = 0; i < items.size(); i++) {
            ProductListItem item = items.get(i);
            ProductInterface product = products.get(i);

            Assert.assertEquals(product.getName(), item.getTitle());
            Assert.assertEquals(product.getSku(), item.getSKU());
            Assert.assertEquals(product.getUrlKey(), item.getSlug());

            Assert.assertEquals(SiteNavigation.toProductUrl(PAGE, product.getUrlKey()), item.getURL());

            Money amount = product.getPrice().getRegularPrice().getAmount();
            Assert.assertEquals(amount.getValue(), item.getPrice(), 0);
            Assert.assertEquals(amount.getCurrency().toString(), item.getCurrency());
            priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
            Assert.assertEquals(priceFormatter.format(amount.getValue()), item.getFormattedPrice());

            Assert.assertEquals(product.getThumbnail().getUrl(), item.getImageURL());
        }
    }

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }
}
