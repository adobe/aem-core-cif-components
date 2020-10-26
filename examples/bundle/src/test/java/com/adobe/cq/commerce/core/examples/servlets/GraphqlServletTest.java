/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.examples.servlets;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryList;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.navigation.Navigation;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.BundleProduct;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPriceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphqlServletTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
                context.registerService(ImplementationPicker.class, new ResourceTypeImplementationPicker());

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);

                context.registerInjectActivateService(new SearchFilterServiceImpl());
                context.registerInjectActivateService(new SearchResultsServiceImpl());
                context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                    (Function<Resource, ComponentsConfiguration>) input -> MOCK_CONFIGURATION_OBJECT);

                context.registerService(Externalizer.class, Mockito.mock(Externalizer.class));
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PAGE = "/content/page";
    private static final String PRODUCT_PAGE = "/content/page/catalogpage/product-page";
    private static final String CATEGORY_PAGE = "/content/page/catalogpage/category-page";

    private static final String PRODUCT_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/product";
    private static final String PRODUCT_LIST_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/productlist";
    private static final String PRODUCT_CAROUSEL_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/productcarousel";
    private static final String PRODUCT_TEASER_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/productteaser";
    private static final String RELATED_PRODUCTS_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/relatedproducts";
    private static final String UPSELL_PRODUCTS_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/upsellproducts";
    private static final String CROSS_SELL_PRODUCTS_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/crosssellproducts";
    private static final String SEARCH_RESULTS_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/searchresults";
    private static final String CATEGORY_CAROUSEL_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/categorycarousel";
    private static final String FEATURED_CATEGORY_LIST_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/featuredcategorylist";
    private static final String NAVIGATION_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/navigation";
    private static final String PRODUCTPAGE_BREADCRUMB_RESOURCE = PRODUCT_PAGE + "/jcr:content/breadcrumb";
    private static final String CATEGORYPAGE_BREADCRUMB_RESOURCE = CATEGORY_PAGE + "/jcr:content/breadcrumb";

    private static final String CIF_DAM_ROOT = "/content/dam/core-components-examples/library/cif-sample-assets/";

    private GraphqlServlet graphqlServlet;
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @Before
    public void setUp() throws ServletException {
        graphqlServlet = new GraphqlServlet();
        graphqlServlet.init();
        request = new MockSlingHttpServletRequest(null);
        response = new MockSlingHttpServletResponse();
    }

    @Test
    public void testGetRequestWithVariables() throws ServletException, IOException {
        String query = "query rootCategory($catId: Int!) {category(id: $catId){id,name,url_path}}";

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("variables", Collections.singletonMap("catId", 2));
        params.put("operationName", "rootCategory");
        request.setParameterMap(params);

        graphqlServlet.doGet(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        CategoryTree category = graphqlResponse.getData().getCategory();

        Assert.assertEquals(2, category.getId().intValue());
    }

    @Test
    public void testPostRequestWithVariables() throws ServletException, IOException {
        String query = "query rootCategory($catId: Int!) {category(id: $catId){id,name,url_path}}";

        GraphqlRequest graphqlRequest = new GraphqlRequest(query);
        graphqlRequest.setVariables(Collections.singletonMap("catId", 2));
        graphqlRequest.setOperationName("rootCategory");
        String body = QueryDeserializer.getGson().toJson(graphqlRequest);
        request.setContent(body.getBytes());

        graphqlServlet.doPost(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        CategoryTree category = graphqlResponse.getData().getCategory();

        Assert.assertEquals(2, category.getId().intValue());
    }

    private Resource prepareModel(String resourcePath) throws ServletException {
        return prepareModel(resourcePath, PAGE);
    }

    private Resource prepareModel(String resourcePath, String currentPage) throws ServletException {
        Page page = Mockito.spy(context.currentPage(currentPage));
        context.currentPage(page);
        context.currentResource(resourcePath);
        Resource resource = Mockito.spy(context.currentResource());

        GraphqlClient graphqlClient = new MockGraphqlClient();
        Mockito.when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        Resource pageContent = Mockito.spy(page.getContentResource());
        when(page.getContentResource()).thenReturn(pageContent);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(resource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, resource.getValueMap());

        XSSAPI xssApi = mock(XSSAPI.class);
        when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
        slingBindings.put("xssApi", xssApi);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.isA(Boolean.class))).then(i -> i.getArgumentAt(1, Boolean.class));
        when(style.get(Mockito.anyString(), Mockito.isA(Integer.class))).then(i -> i.getArgumentAt(1, Integer.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);

        return resource;
    }

    @Test
    public void testProductModel() throws ServletException {
        prepareModel(PRODUCT_RESOURCE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("beaumont-summit-kit");

        Product productModel = context.request().adaptTo(Product.class);
        Assert.assertEquals("MH01", productModel.getSku());
        Assert.assertEquals(15, productModel.getVariants().size());

        // We make sure that all assets in the sample JSON response point to the DAM
        for (Asset asset : productModel.getAssets()) {
            Assert.assertTrue(asset.getPath().startsWith(CIF_DAM_ROOT));
        }
        for (Variant variant : productModel.getVariants()) {
            for (Asset asset : variant.getAssets()) {
                Assert.assertTrue(asset.getPath().startsWith(CIF_DAM_ROOT));
            }
        }

        // These are used in the Venia ITs
        Assert.assertEquals("Meta description for Chaz Kangeroo Hoodie", productModel.getMetaDescription());
        Assert.assertEquals("Meta keywords for Chaz Kangeroo Hoodie", productModel.getMetaKeywords());
        Assert.assertEquals("Meta title for Chaz Kangeroo Hoodie", productModel.getMetaTitle());
    }

    @Test
    public void testGroupedProductModel() throws ServletException {
        prepareModel(PRODUCT_RESOURCE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("set-of-sprite-yoga-straps");

        Product productModel = context.request().adaptTo(Product.class);
        Assert.assertEquals("24-WG085_Group", productModel.getSku());
        Assert.assertTrue(productModel.isGroupedProduct());
        Assert.assertEquals(3, productModel.getGroupedProductItems().size());

        // We make sure that all assets in the sample JSON response point to the DAM
        for (Asset asset : productModel.getAssets()) {
            Assert.assertTrue(asset.getPath().startsWith(CIF_DAM_ROOT));
        }
    }

    @Test
    public void testProductListModel() throws ServletException {
        prepareModel(PRODUCT_LIST_RESOURCE);

        // The category data is coming from magento-graphql-category.json
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("1");
        ProductList productListModel = context.request().adaptTo(ProductList.class);
        Assert.assertEquals("Outdoor Collection", productListModel.getTitle());

        // The products are coming from magento-graphql-category-products.json
        Assert.assertEquals(6, productListModel.getProducts().size());

        // We make sure that all assets in the sample JSON response point to the DAM
        for (ProductListItem product : productListModel.getProducts()) {
            Assert.assertTrue(product.getImageURL().startsWith(CIF_DAM_ROOT));
        }

        // These are used in the Venia ITs
        Assert.assertEquals("Meta description for Outdoor Collection", productListModel.getMetaDescription());
        Assert.assertEquals("Meta keywords for Outdoor Collection", productListModel.getMetaKeywords());
        Assert.assertEquals("Meta title for Outdoor Collection", productListModel.getMetaTitle());
    }

    @Test
    public void testProductCarouselModel() throws ServletException {
        Resource resource = prepareModel(PRODUCT_CAROUSEL_RESOURCE);

        String[] productSkuList = (String[]) resource.getValueMap().get("product"); // The HTL script uses an alias here
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put("productSkuList", productSkuList);

        ProductCarousel productCarouselModel = context.request().adaptTo(ProductCarousel.class);
        Assert.assertEquals(4, productCarouselModel.getProducts().size());
        Assert.assertEquals("24-MB02", productCarouselModel.getProducts().get(0).getSKU());

        // We make sure that all assets in the sample JSON response point to the DAM
        for (ProductListItem product : productCarouselModel.getProducts()) {
            Assert.assertTrue(product.getImageURL().startsWith(CIF_DAM_ROOT));
        }
    }

    @Test
    public void testProductTeaserModel() throws ServletException {
        prepareModel(PRODUCT_TEASER_RESOURCE);
        ProductTeaser productTeaserModel = context.request().adaptTo(ProductTeaser.class);
        Assert.assertEquals("Summit Watch", productTeaserModel.getName());

        // We make sure that all assets in the sample JSON response point to the DAM
        Assert.assertTrue(productTeaserModel.getImage().startsWith(CIF_DAM_ROOT));
    }

    @Test
    public void testRelatedProductsModel() throws ServletException {
        prepareModel(RELATED_PRODUCTS_RESOURCE);
        ProductCarousel relatedProductsModel = context.request().adaptTo(ProductCarousel.class);
        Assert.assertEquals(3, relatedProductsModel.getProducts().size());
        Assert.assertEquals("24-MB01", relatedProductsModel.getProducts().get(0).getSKU());
    }

    @Test
    public void testUpsellProductsModel() throws ServletException {
        prepareModel(UPSELL_PRODUCTS_RESOURCE);
        ProductCarousel relatedProductsModel = context.request().adaptTo(ProductCarousel.class);

        // We test the SKUs to make sure we return the right response for UPSELL_PRODUCTS
        List<ProductListItem> products = relatedProductsModel.getProducts();
        Assert.assertEquals(2, products.size());
        Assert.assertEquals("24-MG03", products.get(0).getSKU());
        Assert.assertEquals("24-WG01", products.get(1).getSKU());

        // We make sure that all assets in the sample JSON response point to the DAM
        for (ProductListItem product : products) {
            Assert.assertTrue(product.getImageURL().startsWith(CIF_DAM_ROOT));
        }
    }

    @Test
    public void testCrosssellProductsModel() throws ServletException {
        prepareModel(CROSS_SELL_PRODUCTS_RESOURCE);
        ProductCarousel relatedProductsModel = context.request().adaptTo(ProductCarousel.class);
        Assert.assertEquals(3, relatedProductsModel.getProducts().size());
        Assert.assertEquals("24-MB01", relatedProductsModel.getProducts().get(0).getSKU());
    }

    @Test
    public void testSearchResultsModel() throws ServletException {
        prepareModel(SEARCH_RESULTS_RESOURCE);
        context.request().setParameterMap(Collections.singletonMap("search_query", "beaumont"));
        SearchResults searchResultsModel = context.request().adaptTo(SearchResults.class);

        Collection<ProductListItem> products = searchResultsModel.getProducts();
        Assert.assertEquals(6, products.size());
        // We make sure that all assets in the sample JSON response point to the DAM
        for (ProductListItem product : products) {
            Assert.assertTrue(product.getImageURL().startsWith(CIF_DAM_ROOT));
        }
    }

    @Test
    public void testCategoryCarouselModel() throws ServletException {
        prepareModel(CATEGORY_CAROUSEL_RESOURCE);
        FeaturedCategoryList featureCategoryListModel = context.request().adaptTo(FeaturedCategoryList.class);
        List<CategoryTree> categories = featureCategoryListModel.getCategories();
        Assert.assertEquals(4, categories.size());

        // Test that the servlet returns the expected categories in the correct order
        Assert.assertEquals(15, categories.get(0).getId().intValue());
        Assert.assertEquals(24, categories.get(1).getId().intValue());
        Assert.assertEquals(28, categories.get(2).getId().intValue());
        Assert.assertEquals(32, categories.get(3).getId().intValue());
    }

    @Test
    public void testFeaturedCategoryListModel() throws ServletException {
        prepareModel(FEATURED_CATEGORY_LIST_RESOURCE);
        FeaturedCategoryList featureCategoryListModel = context.request().adaptTo(FeaturedCategoryList.class);
        List<CategoryTree> categories = featureCategoryListModel.getCategories();
        Assert.assertEquals(2, categories.size());

        // Test that the Servlet didn't return 2 times the catalog category tree
        Assert.assertEquals(15, categories.get(0).getId().intValue());
        Assert.assertEquals(24, categories.get(1).getId().intValue());
    }

    @Test
    public void testNavigationModel() throws ServletException {
        prepareModel(NAVIGATION_RESOURCE);

        // Mock OSGi services for the WCM Navigation component
        context.registerService(LanguageManager.class, Mockito.mock(LanguageManager.class));
        context.registerService(LiveRelationshipManager.class, Mockito.mock(LiveRelationshipManager.class));

        Navigation navigationModel = context.request().adaptTo(Navigation.class);
        Assert.assertEquals(7, navigationModel.getItems().size()); // Our test catalog has 7 top-level categories
    }

    @Test
    public void testProductPageBreadcrumbModel() throws ServletException {
        prepareModel(PRODUCTPAGE_BREADCRUMB_RESOURCE, PRODUCT_PAGE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("beaumont-summit-kit");

        Breadcrumb breadcrumbModel = context.request().adaptTo(Breadcrumb.class);
        Assert.assertEquals(4, breadcrumbModel.getItems().size()); // The base page, 2 categories and the product
    }

    @Test
    public void testCategoryPageBreadcrumbModel() throws ServletException {
        prepareModel(CATEGORYPAGE_BREADCRUMB_RESOURCE, CATEGORY_PAGE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("1");

        Breadcrumb breadcrumbModel = context.request().adaptTo(Breadcrumb.class);
        Assert.assertEquals(3, breadcrumbModel.getItems().size()); // The base page and 2 categories
    }

    @Test
    public void testPriceLoadingQueryForProductPage() throws ServletException, IOException {
        testPriceLoadingQueryFor("MH01");
    }

    @Test
    public void testPriceLoadingQueryForGroupedProductPage() throws ServletException, IOException {
        testPriceLoadingQueryFor("24-WG085_Group");
    }

    @Test
    public void testPriceLoadingQueryForCollectionPage() throws ServletException, IOException {
        testPriceLoadingQueryFor("24-MB02", "24-MG03", "24-WG01", "MH01", "MH03", "WJ04");
    }

    private void testPriceLoadingQueryFor(String... skus) throws ServletException, IOException {

        // This is the price query sent client-side by the components, it is defined in
        // the file CommerceGraphqlApi.js in the components "common" clientlib

        ProductInterfaceQueryDefinition priceQuery = q -> q
            .sku()
            .priceRange(r -> r
                .minimumPrice(generatePriceQuery()))
            .onConfigurableProduct(cp -> cp
                .priceRange(r -> r
                    .maximumPrice(generatePriceQuery())))
            .onGroupedProduct(g -> g
                .items(i -> i
                    .product(p -> p
                        .sku()
                        .priceRange(r -> r
                            .minimumPrice(generatePriceQuery())))));

        FilterEqualTypeInput in = new FilterEqualTypeInput().setIn(Arrays.asList(skus));
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setSku(in);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);
        String query = Operations.query(q -> q.products(searchArgs, p -> p.items(priceQuery))).toString();

        request.setParameterMap(Collections.singletonMap("query", query));
        graphqlServlet.doGet(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        Products products = graphqlResponse.getData().getProducts();

        Assert.assertEquals(skus.length, products.getItems().size());
        for (int i = 0, l = skus.length; i < l; i++) {
            Assert.assertEquals(skus[i], products.getItems().get(i).getSku());
        }
    }

    private ProductPriceQueryDefinition generatePriceQuery() {
        return q -> q
            .regularPrice(r -> r
                .value()
                .currency())
            .finalPrice(f -> f
                .value()
                .currency())
            .discount(d -> d
                .amountOff()
                .percentOff());
    }

    @Test
    public void testBundleProductItemsQuery() throws ServletException, IOException {

        // This is a simplified version of the query sent by the React BundleProductOptions component

        ProductInterfaceQueryDefinition priceQuery = q -> q
            .sku()
            .onBundleProduct(b -> b
                .dynamicSku()
                .dynamicPrice()
                .dynamicWeight()
                .priceView()
                .shipBundleItems()
                .items(i -> i
                    .optionId()
                    .options(o -> o
                        .id()
                        .product(p -> p
                            .priceRange(r -> r
                                .maximumPrice(generatePriceQuery()))))));

        FilterEqualTypeInput in = new FilterEqualTypeInput().setEq("24-WG080");
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setSku(in);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);
        String query = Operations.query(q -> q.products(searchArgs, p -> p.items(priceQuery))).toString();

        request.setParameterMap(Collections.singletonMap("query", query));
        graphqlServlet.doGet(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        Products products = graphqlResponse.getData().getProducts();

        BundleProduct bundleProduct = (BundleProduct) products.getItems().get(0);
        Assert.assertEquals("24-WG080", bundleProduct.getSku());
        Assert.assertEquals(4, bundleProduct.getItems().size());
    }
}
