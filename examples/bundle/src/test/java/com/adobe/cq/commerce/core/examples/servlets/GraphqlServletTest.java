/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.examples.servlets;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
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

import com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy;
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
import com.adobe.cq.wcm.core.components.services.link.PathProcessor;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManagerFactory;
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
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
                context.registerService(PageManagerFactory.class, r -> context.pageManager());
                SpecificPageStrategy specificPageStrategy = new SpecificPageStrategy();
                context.registerInjectActivateService(specificPageStrategy);
                UrlProviderImpl urlProvider = new UrlProviderImpl();
                context.registerInjectActivateService(urlProvider);

                context.registerInjectActivateService(new SearchFilterServiceImpl());
                context.registerInjectActivateService(new SearchResultsServiceImpl());
                context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                    (Function<Resource, ComponentsConfiguration>) input -> MOCK_CONFIGURATION_OBJECT);

                context.registerService(Externalizer.class, Mockito.mock(Externalizer.class));

                XSSAPI xssApi = mock(XSSAPI.class);
                when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
                context.registerService(XSSAPI.class, xssApi);
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PAGE = "/content/page";
    private static final String PRODUCT_PAGE = "/content/page/catalogpage/product-page";
    private static final String CATEGORY_PAGE = "/content/page/catalogpage/category-page";

    private static final String PRODUCT_V1_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/product-v1";
    private static final String PRODUCT_V2_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/product-v2";
    private static final String PRODUCT_LIST_V1_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/productlist-v1";
    private static final String PRODUCT_LIST_V2_RESOURCE = PAGE + "/jcr:content/root/responsivegrid/productlist-v2";
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
    private SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);

    @Before
    public void setUp() throws ServletException {
        graphqlServlet = new GraphqlServlet();
        graphqlServlet.init();
        request = new MockSlingHttpServletRequest(null);
        response = new MockSlingHttpServletResponse();
        context.registerService(PathProcessor.class, new MockPathProcessor());
        context.registerService(PageManagerFactory.class, rr -> context.pageManager());
    }

    @Test
    public void testGetRequestWithVariables() throws ServletException, IOException {
        String query = "query rootCategory($id: String!) {categoryList(filters:{category_uid:{eq:$id}}){uid,name,url_path}}";

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("variables", Collections.singletonMap("id", "MTU%3D"));
        params.put("operationName", "rootCategory");
        request.setParameterMap(params);

        graphqlServlet.doGet(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        CategoryTree category = graphqlResponse.getData().getCategoryList().get(0);

        Assert.assertEquals("MTU=", category.getUid().toString());
    }

    @Test
    public void testPostRequestWithVariables() throws ServletException, IOException {
        String query = "query rootCategory($id: String!) {categoryList(filters:{category_uid:{eq:$id}}){uid,name,url_path}}";

        GraphqlRequest graphqlRequest = new GraphqlRequest(query);
        graphqlRequest.setVariables(Collections.singletonMap("id", "MTU="));
        graphqlRequest.setOperationName("rootCategory");
        String body = QueryDeserializer.getGson().toJson(graphqlRequest);
        request.setContent(body.getBytes());

        graphqlServlet.doPost(request, response);
        String output = response.getOutputAsString();

        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(output, type);
        CategoryTree category = graphqlResponse.getData().getCategoryList().get(0);

        Assert.assertEquals("MTU=", category.getUid().toString());
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

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.isA(Boolean.class))).then(i -> i.getArgumentAt(1, Boolean.class));
        when(style.get(Mockito.anyString(), Mockito.isA(Integer.class))).then(i -> i.getArgumentAt(1, Integer.class));
        slingBindings.put("currentStyle", style);

        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);

        return resource;
    }

    @Test
    public void testProductModelV1() throws ServletException {
        prepareModel(PRODUCT_V1_RESOURCE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");

        Product productModel = context.request().adaptTo(Product.class);
        Assert.assertTrue(productModel instanceof com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImpl);
        testProductModelImpl(productModel);
    }

    @Test
    public void testProductModelV2() throws ServletException {
        prepareModel(PRODUCT_V2_RESOURCE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");

        Product productModel = context.request().adaptTo(Product.class);
        Assert.assertTrue(productModel instanceof com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImpl);
        testProductModelImpl(productModel);
    }

    private void testProductModelImpl(Product productModel) throws ServletException {
        Assert.assertEquals("MH01", productModel.getSku());
        Assert.assertFalse(productModel.isStaged());
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
    public void testProductModelV2Staged() throws ServletException {
        prepareModel(PRODUCT_V2_RESOURCE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/chaz-crocodile-hoodie.html");

        Product productModel = context.request().adaptTo(Product.class);
        Assert.assertTrue(productModel instanceof com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImpl);

        Assert.assertEquals("MH02", productModel.getSku());
        Assert.assertTrue(productModel.isStaged());
    }

    @Test
    public void testGroupedProductModel() throws ServletException {
        prepareModel(PRODUCT_V1_RESOURCE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/set-of-sprite-yoga-straps.html");

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
    public void testBundleProductModel() throws ServletException {
        prepareModel(PRODUCT_V1_RESOURCE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/sprite-yoga-companion-kit.html");

        Product productModel = context.request().adaptTo(Product.class);
        Assert.assertEquals("24-WG080", productModel.getSku());
        Assert.assertTrue(productModel.isBundleProduct());

        // We make sure that all assets in the sample JSON response point to the DAM
        for (Asset asset : productModel.getAssets()) {
            Assert.assertTrue(asset.getPath().startsWith(CIF_DAM_ROOT));
        }
    }

    @Test
    public void testUnknownProductModel() throws ServletException {
        prepareModel(PRODUCT_V1_RESOURCE);
        when(wcmMode.isDisabled()).thenReturn(Boolean.TRUE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/unknown-product.html");

        Product productModel = context.request().adaptTo(Product.class);
        Assert.assertFalse(productModel.getFound());
    }

    // @Test - disabled
    public void testProductListModelV1() throws ServletException {
        prepareModel(PRODUCT_LIST_V1_RESOURCE);

        // The category data is coming from magento-graphql-category.json
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/outdoor.html");

        ProductList productListModel = context.request().adaptTo(ProductList.class);
        Assert.assertTrue(productListModel instanceof com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl);
        testProductListModelImpl(productListModel);
    }

    // @Test - disabled
    public void testProductListModelV2() throws ServletException {
        prepareModel(PRODUCT_LIST_V2_RESOURCE);

        // The category data is coming from magento-graphql-category.json
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/outdoor.html");

        ProductList productListModel = context.request().adaptTo(ProductList.class);
        Assert.assertTrue(productListModel instanceof com.adobe.cq.commerce.core.components.internal.models.v2.productlist.ProductListImpl);
        testProductListModelImpl(productListModel);
    }

    private void testProductListModelImpl(ProductList productListModel) {
        Assert.assertEquals("Outdoor Collection", productListModel.getTitle());
        Assert.assertFalse(productListModel.isStaged());

        // The products are coming from magento-graphql-category-products.json
        Assert.assertEquals(6, productListModel.getProducts().size());

        // We make sure that all assets in the sample JSON response point to the DAM
        for (ProductListItem product : productListModel.getProducts()) {
            Assert.assertTrue(product.getImageURL().startsWith(CIF_DAM_ROOT));
            Assert.assertFalse(product.isStaged());
        }

        // These are used in the Venia ITs
        Assert.assertEquals("Meta description for Outdoor Collection", productListModel.getMetaDescription());
        Assert.assertEquals("Meta keywords for Outdoor Collection", productListModel.getMetaKeywords());
        Assert.assertEquals("Meta title for Outdoor Collection", productListModel.getMetaTitle());
    }

    // @Test - disabled
    public void testProductListModelV2Staged() throws ServletException {
        prepareModel(PRODUCT_LIST_V2_RESOURCE);

        // The category data is coming from magento-graphql-category.json
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/outdoor-staged.html");

        ProductList productListModel = context.request().adaptTo(ProductList.class);
        Assert.assertTrue(productListModel instanceof com.adobe.cq.commerce.core.components.internal.models.v2.productlist.ProductListImpl);
        // anything else is already asserted in testProductListModelV2 with uid-1
        Assert.assertEquals("Outdoor Collection (staged)", productListModel.getTitle());
        List<ProductListItem> items = new ArrayList<>(productListModel.getProducts());
        Assert.assertTrue(items.get(1).isStaged());
        Assert.assertTrue(items.get(5).isStaged());
    }

    @Test
    public void testUnknownProductListModel() throws ServletException {
        prepareModel(PRODUCT_LIST_V1_RESOURCE);
        when(wcmMode.isDisabled()).thenReturn(Boolean.TRUE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/unknown-category.html");

        ProductList productListModel = context.request().adaptTo(ProductList.class);
        Assert.assertNull(productListModel.getCategoryRetriever());
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
        context.registerService(ImplementationPicker.class, new ResourceTypeImplementationPicker());
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
        Assert.assertEquals("MTU=", categories.get(0).getUid().toString());
        Assert.assertEquals("MjQ=", categories.get(1).getUid().toString());
        Assert.assertEquals("NA==", categories.get(2).getUid().toString());
        Assert.assertEquals("Ng==", categories.get(3).getUid().toString());
    }

    @Test
    public void testFeaturedCategoryListModel() throws ServletException {
        prepareModel(FEATURED_CATEGORY_LIST_RESOURCE);
        FeaturedCategoryList featureCategoryListModel = context.request().adaptTo(FeaturedCategoryList.class);
        List<CategoryTree> categories = featureCategoryListModel.getCategories();
        Assert.assertEquals(2, categories.size());

        // Test that the Servlet didn't return 2 times the catalog category tree
        Assert.assertEquals("MTU=", categories.get(0).getUid().toString());
        Assert.assertEquals("MjQ=", categories.get(1).getUid().toString());
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
        requestPathInfo.setSuffix("/beaumont-summit-kit.html");

        Breadcrumb breadcrumbModel = context.request().adaptTo(Breadcrumb.class);
        Assert.assertEquals(4, breadcrumbModel.getItems().size()); // The base page, 2 categories and the product
    }

    @Test
    public void testCategoryPageBreadcrumbModel() throws ServletException {
        prepareModel(CATEGORYPAGE_BREADCRUMB_RESOURCE, CATEGORY_PAGE);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/1.html");

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
                    .uid()
                    .options(o -> o
                        .uid()
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

    public static class MockPathProcessor implements PathProcessor {
        @Override
        public boolean accepts(String s, SlingHttpServletRequest slingHttpServletRequest) {
            return true;
        }

        @Override
        public String sanitize(String s, SlingHttpServletRequest slingHttpServletRequest) {
            return s;
        }

        @Override
        public String map(String s, SlingHttpServletRequest slingHttpServletRequest) {
            return s;
        }

        @Override
        public String externalize(String s, SlingHttpServletRequest slingHttpServletRequest) {
            return s;
        }
    }
}
