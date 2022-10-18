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
package com.adobe.cq.commerce.core.components.internal.models.v1.productlist;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.adobe.cq.commerce.core.components.internal.services.sitemap.SitemapLinkExternalizer;
import com.adobe.cq.commerce.core.components.internal.services.sitemap.SitemapLinkExternalizerProvider;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragmentContainer;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.storefrontcontext.CategoryStorefrontContext;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;
import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductListImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
            "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(
        MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            context.registerInjectActivateService(new SearchFilterServiceImpl());
            context.registerInjectActivateService(new SearchResultsServiceImpl());
            ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
            Utils.addDataLayerConfig(mockConfigBuilder, true);
            Utils.addStorefrontContextConfig(mockConfigBuilder, true);
            context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
            context.registerService(LiveRelationshipManager.class, mock(LiveRelationshipManager.class));
            LanguageManager languageManager = context.registerService(LanguageManager.class,
                mock(LanguageManager.class));
            Page rootPage = context.pageManager().getPage(PAGE);
            Mockito.when(languageManager.getLanguageRoot(any())).thenReturn(rootPage);
            CommerceExperienceFragmentsRetriever cxfRetriever = context.registerService(
                CommerceExperienceFragmentsRetriever.class,
                mock(CommerceExperienceFragmentsRetriever.class));
            List<Resource> xfs = new ArrayList<>();
            xfs.add(context.resourceResolver().getResource("/content/experience-fragments/pageA/xf"));
            Mockito.when(cxfRetriever.getExperienceFragmentsForCategory(any(), eq("grid"), any())).thenReturn(xfs);
        })
        .build();

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PAGE = "/content/pageA";
    private static final String PRODUCTLIST = "/content/pageA/jcr:content/root/responsivegrid/productlist";
    private static final String PRODUCT_LIST_NO_SORTING = "/content/pageA/jcr:content/root/responsivegrid/productlist_no_sorting";
    private static final String PRODUCT_LIST_WITH_XF = "/content/pageA/jcr:content/root/responsivegrid/productlist_with_xf";
    private static final String PRODUCT_LIST_WITH_MULTIPLE_XF = "/content/pageA/jcr:content/root/responsivegrid/productlist_with_multiple_xf";

    private Resource productListResource;
    private Resource pageResource;
    protected ProductListImpl productListModel;
    private CategoryTree category;
    private Products products;
    private GraphqlClient graphqlClient;

    @Mock
    CloseableHttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        Page page = Mockito.spy(context.currentPage(PAGE));
        context.currentResource(PRODUCTLIST);
        productListResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTLIST));

        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        category = Utils.getQueryFromResource("graphql/magento-graphql-search-category-result-category.json")
            .getCategoryList().get(0);
        products = Utils.getQueryFromResource("graphql/magento-graphql-search-category-result-products.json")
            .getProducts();

        graphqlClient = Mockito.spy(new GraphqlClientImpl());
        context.registerInjectActivateService(graphqlClient, "httpMethod", "POST");

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK,
            "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK,
            "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-category.json", httpClient,
            HttpStatus.SC_OK,
            "{categoryList(filters:{url_path");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-category.json", httpClient,
            HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid:{eq:\"MTI==\"}");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-products.json", httpClient,
            HttpStatus.SC_OK, "pageSize:6");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-products.json", httpClient,
            HttpStatus.SC_OK, "pageSize:5");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-products-pagesize-1.json", httpClient,
            HttpStatus.SC_OK,
            "pageSize:1");

        // magento-graphql-category-uid

        when(productListResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
        context.registerAdapter(Resource.class, GraphqlClient.class,
            (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
                "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        // This is needed by the SearchResultsService used by the productlist component
        pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(productListResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        Function<Resource, ComponentsConfiguration> adapter = r -> r.getPath().equals(PAGE) ? MOCK_CONFIGURATION_OBJECT
            : ComponentsConfiguration.EMPTY;
        context.registerAdapter(Resource.class, ComponentsConfiguration.class, adapter);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/category-1.html");
        context.request().setServletPath(PAGE + ".html/category-1.html"); // used by context.request().getRequestURI();

        // This sets the page attribute injected in the models with @Inject or
        // @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productListResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productListResource.getValueMap());

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);
    }

    protected void adaptToProductList() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
    }

    @Test
    public void testTitleAndMetadata() {
        adaptToProductList();
        Assert.assertEquals(category.getName(), productListModel.getTitle());
        Assert.assertEquals(category.getMetaDescription(), productListModel.getMetaDescription());
        Assert.assertEquals(category.getMetaKeywords(), productListModel.getMetaKeywords());
        Assert.assertEquals(category.getMetaTitle(), productListModel.getMetaTitle());
        Assert.assertEquals("https://author" + PAGE + ".html/category-1.html", productListModel.getCanonicalUrl());
    }

    @Test
    public void testStagedData() {
        testStagedDataImpl(false);
    }

    protected void testStagedDataImpl(boolean hasStagedData) {
        adaptToProductList();
        Assert.assertEquals(hasStagedData, productListModel.isStaged());
        Collection<ProductListItem> products = productListModel.getProducts();

        // We cannot differentiate if the items are created from a productlist v1 and v2
        // and do not want to introduce a new mock JSON response for this, so this is
        // always "true"
        Assert.assertTrue(products.stream().allMatch(p -> p.isStaged().equals(true)));
    }

    @Test
    public void testUrlPathIdentifier() {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/running.html");
        context.request().setServletPath(PAGE + ".html/running.html"); // used by context.request().getRequestURI();

        adaptToProductList();

        Assert.assertEquals(category.getName(), productListModel.getTitle());
        Assert.assertEquals(category.getMetaDescription(), productListModel.getMetaDescription());
        Assert.assertEquals(category.getMetaKeywords(), productListModel.getMetaKeywords());
        Assert.assertEquals(category.getMetaTitle(), productListModel.getMetaTitle());
        Assert.assertEquals("https://author" + PAGE + ".html/running.html", productListModel.getCanonicalUrl());
    }

    @Test
    public void testPropertyIdentifier() {
        String customPage = "/content/custom-category-page";
        context.currentResource(customPage + "/jcr:content/root/responsivegrid/productlist");
        context.request().setServletPath(customPage + ".html");

        adaptToProductList();

        String uid = (String) Whitebox.getInternalState(productListModel.getCategoryRetriever(), "identifier");
        assertEquals("MTI==", uid);
        Assert.assertEquals(category.getName(), productListModel.getTitle());
        Assert.assertEquals(category.getMetaDescription(), productListModel.getMetaDescription());
        Assert.assertEquals(category.getMetaKeywords(), productListModel.getMetaKeywords());
        Assert.assertEquals(category.getMetaTitle(), productListModel.getMetaTitle());
        Assert.assertEquals("https://author" + customPage + ".html", productListModel.getCanonicalUrl());
    }

    @Test
    public void getImage() {
        adaptToProductList();
        Assert.assertEquals(category.getImage(), productListModel.getImage());
    }

    @Test
    public void getImageWhenMissingInResponse() {
        adaptToProductList();
        ProductListImpl spyProductListModel = Mockito.spy(productListModel);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getImage()).thenReturn("");
        Mockito.doReturn(category).when(spyProductListModel).getCategory();

        String image = spyProductListModel.getImage();
        Assert.assertEquals("", image);
    }

    @Test
    public void getProducts() {
        adaptToProductList();
        Collection<ProductListItem> products = productListModel.getProducts();
        Assert.assertNotNull(products);

        // We introduce one "faulty" product data in the response, it should be skipped
        Assert.assertEquals(4, products.size());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        List<ProductListItem> results = products.stream().collect(Collectors.toList());
        for (int i = 0; i < results.size(); i++) {
            // get raw GraphQL object
            ProductInterface productInterface = this.products.getItems().get(i);
            // get mapped product list item
            ProductListItem item = results.get(i);

            Assert.assertEquals(productInterface.getName(), item.getTitle());
            Assert.assertEquals(productInterface.getSku(), item.getSKU());
            Assert.assertEquals(productInterface.getUrlKey(), item.getSlug());
            Assert.assertEquals(String.format(PRODUCT_PAGE + ".html/%s.html", productInterface.getUrlKey()),
                item.getURL());

            Assert.assertEquals(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getValue(),
                item.getPriceRange().getFinalPrice(), 0);
            Assert.assertEquals(
                productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getCurrency().toString(),
                item.getPriceRange().getCurrency());
            priceFormatter.setCurrency(Currency
                .getInstance(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getCurrency()
                    .toString()));
            Assert.assertEquals(
                priceFormatter
                    .format(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getValue()),
                item.getPriceRange().getFormattedFinalPrice());

            ProductImage smallImage = productInterface.getSmallImage();
            if (smallImage == null) {
                // if small image is missing for a product in GraphQL response then image URL is
                // null for the related item
                Assert.assertNull(item.getImageURL());
            } else {
                Assert.assertEquals(smallImage.getUrl(), item.getImageURL());
            }

            Assert.assertEquals(productInterface instanceof GroupedProduct, item.getPriceRange().isStartPrice());
        }

        SearchResultsSet searchResultsSet = productListModel.getSearchResultsSet();
        List<SearchAggregation> searchAggregations = searchResultsSet.getSearchAggregations();
        Assert.assertEquals(8, searchAggregations.size());

        // check category aggregation
        Optional<SearchAggregation> categoryIdAggregation = searchAggregations.stream()
            .filter(a -> a.getIdentifier().equals(
                ProductListImpl.CATEGORY_AGGREGATION_ID))
            .findAny();
        Assert.assertTrue(categoryIdAggregation.isPresent());
        List<SearchAggregationOption> options = categoryIdAggregation.get().getOptions();
        Assert.assertEquals(2, options.size());

        SearchAggregationOption opt = options.get(0);
        Assert.assertEquals("3", opt.getFilterValue());
        Assert.assertEquals("Gear", opt.getDisplayLabel());
        Assert.assertEquals("/content/category-page.html/running/gear.html", opt.getPageUrl());

        opt = options.get(1);
        Assert.assertEquals("4", opt.getFilterValue());
        Assert.assertEquals("Bags", opt.getDisplayLabel());
        Assert.assertEquals("/content/category-page.html/running/bags.html", opt.getPageUrl());

        // We want to make sure all price ranges are properly processed
        SearchAggregation priceAggregation = searchAggregations.stream().filter(a -> a.getIdentifier().equals("price"))
            .findFirst().get();
        Assert.assertEquals(3, priceAggregation.getOptions().size());
        Assert.assertEquals(3, priceAggregation.getOptionCount());
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("30-40")));
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("40-*")));
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("14")));
    }

    @Test
    public void getProductsWithOneFragment() {
        context.currentResource(PRODUCT_LIST_WITH_XF);
        adaptToProductList();

        List<CommerceExperienceFragmentContainer> fragments = productListModel.getExperienceFragments();
        Assert.assertNotNull(fragments);
        Assert.assertEquals(1, fragments.size());

        CommerceExperienceFragmentContainer xfItem = fragments.get(0);
        Assert.assertNotNull(xfItem.getCssClassName());
        Assert.assertNotNull(xfItem.getRenderResource());
    }

    @Test
    public void getProductsWithMultipleFragments() {
        context.currentResource(PRODUCT_LIST_WITH_MULTIPLE_XF);
        adaptToProductList();

        List<CommerceExperienceFragmentContainer> fragments = productListModel.getExperienceFragments();
        Assert.assertNotNull(fragments);

        // There are 4 configured XFs. one has a location that does not match any XF
        // resource and another one is set for page 2
        Assert.assertEquals(2, fragments.size());

        CommerceExperienceFragmentContainer xfItem = fragments.get(0);
        Assert.assertNull(xfItem.getCssClassName());
        Assert.assertNotNull(xfItem.getRenderResource());

        xfItem = fragments.get(1);
        Assert.assertNotNull(xfItem.getCssClassName());
        Assert.assertNotNull(xfItem.getRenderResource());
    }

    @Test
    public void getProductsWithMultipleFragmentsPage2() {
        context.currentResource(PRODUCT_LIST_WITH_MULTIPLE_XF);
        context.request().getParameterMap().put("page", new String[] { "2" });
        adaptToProductList();

        List<CommerceExperienceFragmentContainer> fragments = productListModel.getExperienceFragments();
        Assert.assertNotNull(fragments);

        // There are 4 configured XFs. one has a location that does not match any XF
        // resource and another one is set for page 2
        Assert.assertEquals(1, fragments.size());

        CommerceExperienceFragmentContainer xfItem = fragments.get(0);
        Assert.assertNotNull(xfItem.getCssClassName());
        Assert.assertNotNull(xfItem.getRenderResource());
    }

    // custom marker interface for search aggregation options
    private interface MySearchAggregationOption {};

    @Test
    public void getProductsWithCustomAggregationOptions() {
        adaptToProductList();

        // inject custom search results service which returns custom search aggregation
        // objects
        SearchResultsService searchResultsService = (SearchResultsService) Whitebox.getInternalState(productListModel,
            "searchResultsService");
        ClassLoader classLoader = getClass().getClassLoader();
        Whitebox.setInternalState(productListModel, "searchResultsService", Proxy.newProxyInstance(classLoader,
            new Class[] { SearchResultsService.class }, (proxy, method, args) -> {
                if (method.getName().equals("performSearch") && method.getParameterCount() == 7) {
                    Pair<CategoryInterface, SearchResultsSet> pair = searchResultsService.performSearch(
                        (SearchOptions) args[0],
                        (Resource) args[1],
                        (Page) args[2],
                        (SlingHttpServletRequest) args[3],
                        (Consumer<ProductInterfaceQuery>) args[4],
                        (java.util.function.Function<ProductAttributeFilterInput, ProductAttributeFilterInput>) args[5],
                        (AbstractCategoryRetriever) args[6]);

                    Class[] optionInterfaces = { SearchAggregationOption.class, MySearchAggregationOption.class };
                    for (SearchAggregation aggregation : pair.getRight().getSearchAggregations()) {
                        List<SearchAggregationOption> options = aggregation.getOptions();
                        List<SearchAggregationOption> myOptions = options.stream().map(
                            o -> (SearchAggregationOption) Proxy.newProxyInstance(classLoader, optionInterfaces,
                                (oProxy, oMethod, oArgs) -> oMethod.invoke(o, oArgs)))
                            .collect(Collectors.toList());
                        options.clear();
                        options.addAll(myOptions);
                    }

                    return pair;
                } else {
                    return method.invoke(searchResultsService, args);
                }
            }));

        Collection<ProductListItem> products = productListModel.getProducts();
        Assert.assertNotNull(products);

        // We introduce one "faulty" product data in the response, it should be skipped
        Assert.assertEquals(4, products.size());

        SearchResultsSet searchResultsSet = productListModel.getSearchResultsSet();
        List<SearchAggregation> searchAggregations = searchResultsSet.getSearchAggregations();
        Assert.assertEquals(8, searchAggregations.size());

        // check category aggregation
        Optional<SearchAggregation> categoryIdAggregation = searchAggregations.stream()
            .filter(a -> a.getIdentifier().equals(
                ProductListImpl.CATEGORY_AGGREGATION_ID))
            .findAny();
        Assert.assertTrue(categoryIdAggregation.isPresent());
        List<SearchAggregationOption> options = categoryIdAggregation.get().getOptions();
        Assert.assertEquals(2, options.size());

        SearchAggregationOption opt = options.get(0);
        // for category aggregation custom options are replaced
        Assert.assertFalse(opt instanceof MySearchAggregationOption);
        Assert.assertEquals("3", opt.getFilterValue());
        Assert.assertEquals("Gear", opt.getDisplayLabel());
        Assert.assertEquals("/content/category-page.html/running/gear.html", opt.getPageUrl());

        opt = options.get(1);
        Assert.assertFalse(opt instanceof MySearchAggregationOption);
        Assert.assertEquals("4", opt.getFilterValue());
        Assert.assertEquals("Bags", opt.getDisplayLabel());
        Assert.assertEquals("/content/category-page.html/running/bags.html", opt.getPageUrl());

        // We want to make sure all price ranges are properly processed
        SearchAggregation priceAggregation = searchAggregations.stream().filter(a -> a.getIdentifier().equals("price"))
            .findFirst().get();
        Assert.assertEquals(3, priceAggregation.getOptions().size());
        Assert.assertEquals(3, priceAggregation.getOptionCount());
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("30-40")));
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("40-*")));
        Assert.assertTrue(priceAggregation.getOptions().stream().anyMatch(o -> o.getDisplayLabel().equals("14")));

        // for other aggregations custom options are preserved
        Assert.assertTrue(priceAggregation.getOptions().get(0) instanceof MySearchAggregationOption);
    }

    @Test
    public void testFilterQueriesReturnNull() throws IOException {
        // We want to make sure that components will not fail if the __type and/or
        // customAttributeMetadata fields are null
        // For example, 3rd-party integrations might not support this immediately

        Mockito.reset(httpClient);
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-empty-data.json", httpClient, HttpStatus.SC_OK,
            "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-products.json", httpClient,
            HttpStatus.SC_OK, "{products");
        Utils.setupHttpResponse("graphql/magento-graphql-category-uid.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-category.json", httpClient,
            HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid");

        adaptToProductList();
        Collection<ProductListItem> productList = productListModel.getProducts();
        Assert.assertEquals("Return the correct number of products", 4, productList.size());

        SearchResultsSet searchResultsSet = productListModel.getSearchResultsSet();
        Assert.assertEquals(0, searchResultsSet.getAvailableAggregations().size());
    }

    @Test
    public void testEditModePlaceholderData() throws IOException {
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix(null);
        adaptToProductList();

        String json = Utils.getResource(ProductListImpl.PLACEHOLDER_DATA);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        category = rootQuery.getCategory();

        Assert.assertEquals(category.getName(), productListModel.getTitle());
        Assert.assertEquals(category.getProducts().getItems().size(), productListModel.getProducts().size());
    }

    @Test
    public void testMissingSuffixOnPublish() throws IOException {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(true);
        slingBindings.put("wcmmode", wcmMode);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix(null);
        context.request().setServletPath(PAGE + ".html"); // used by context.request().getRequestURI();
        adaptToProductList();

        // Check that we get an empty list of products and the GraphQL client is never
        // called
        Assert.assertTrue(productListModel.getProducts().isEmpty());
        Mockito.verify(graphqlClient, never()).execute(any(), any(), any());
        Mockito.verify(graphqlClient, never()).execute(any(), any(), any(), any());

        // Test canonical url on publish
        Assert.assertEquals("https://publish" + PAGE + ".html", productListModel.getCanonicalUrl());
    }

    @Test
    public void testProductListNoGraphqlClient() throws IOException {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(context.resourceResolver().getResource("/content/pageB"));
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, context.pageManager().getPage("/content/pageB"));

        adaptToProductList();

        Assert.assertTrue(productListModel.getTitle().isEmpty());
        Assert.assertTrue(productListModel.getImage().isEmpty());
        Assert.assertTrue(productListModel.getProducts().isEmpty());
        Assert.assertTrue(productListModel.getMetaTitle().isEmpty());
        Assert.assertNull(productListModel.getMetaDescription());
        Assert.assertNull(productListModel.getMetaKeywords());
    }

    @Test
    public void testExtendProductQuery() {
        adaptToProductList();

        AbstractCategoryRetriever retriever = productListModel.getCategoryRetriever();
        retriever.extendProductQueryWith(p -> p.createdAt().stockStatus());
        retriever.extendProductQueryWith(p -> p.staged()); // use extend method twice to test the "merge" feature

        productListModel.getProducts();

        ArgumentCaptor<GraphqlRequest> captor = ArgumentCaptor.forClass(GraphqlRequest.class);
        verify(graphqlClient, atLeastOnce()).execute(captor.capture(), any(), any(), any());

        // Check the "products" query
        List<GraphqlRequest> requests = captor.getAllValues();
        String productsQuery = null;
        for (GraphqlRequest request : requests) {
            if (request.getQuery().startsWith("{products")) {
                productsQuery = request.getQuery();
                break;
            }
        }
        Assert.assertNotNull(productsQuery);
        Assert.assertTrue(productsQuery.contains("created_at,stock_status,staged"));
    }

    @Test
    public void testExtendProductFilterQuery() {
        adaptToProductList();

        productListModel.extendProductFilterWith(f -> f
            .setName(new FilterMatchTypeInput()
                .setMatch("winter")));
        // Add another filter which should be concatenated to the first
        productListModel.extendProductFilterWith(f -> f
            .setCustomFilter("myKey", new FilterEqualTypeInput()
                .setEq("myValue")));
        productListModel.getProducts();

        ArgumentCaptor<GraphqlRequest> captor = ArgumentCaptor.forClass(GraphqlRequest.class);
        verify(graphqlClient, atLeastOnce()).execute(captor.capture(), any(), any(), any());

        // Check the "products" query
        List<GraphqlRequest> requests = captor.getAllValues();
        String productsQuery = null;
        for (GraphqlRequest request : requests) {
            if (request.getQuery().startsWith("{products")) {
                productsQuery = request.getQuery();
                break;
            }
        }
        Assert.assertNotNull(productsQuery);
        Assert.assertTrue(productsQuery.contains("filter:{myKey:{eq:\"myValue\"},name:{match:\"winter\"}}"));
    }

    @Test
    public void testSorting() {
        adaptToProductList();
        SearchResultsSet resultSet = productListModel.getSearchResultsSet();
        Assert.assertNotNull(resultSet);
        Assert.assertTrue(resultSet.hasSorting());
        Sorter sorter = resultSet.getSorter();
        Assert.assertNotNull(sorter);

        SorterKey currentKey = sorter.getCurrentKey();
        Assert.assertNotNull(currentKey);
        Assert.assertEquals("price", currentKey.getName());
        Assert.assertEquals("Price", currentKey.getLabel());
        Assert.assertEquals(Sorter.Order.ASC, currentKey.getOrder());
        Assert.assertTrue(currentKey.isSelected());

        Map<String, String> currentOrderParameters = currentKey.getCurrentOrderParameters();
        Assert.assertNotNull(currentOrderParameters);
        Assert.assertEquals(resultSet.getAppliedQueryParameters().size() + 2, currentOrderParameters.size());
        resultSet.getAppliedQueryParameters()
            .forEach((key, value) -> Assert.assertEquals(value, currentOrderParameters.get(key)));
        Assert.assertEquals("price", currentOrderParameters.get(Sorter.PARAMETER_SORT_KEY));
        Assert.assertEquals("asc", currentOrderParameters.get(Sorter.PARAMETER_SORT_ORDER));

        Map<String, String> oppositeOrderParameters = currentKey.getOppositeOrderParameters();
        Assert.assertNotNull(oppositeOrderParameters);
        Assert.assertEquals(resultSet.getAppliedQueryParameters().size() + 2, oppositeOrderParameters.size());
        resultSet.getAppliedQueryParameters()
            .forEach((key, value) -> Assert.assertEquals(value, oppositeOrderParameters.get(key)));
        Assert.assertEquals("price", oppositeOrderParameters.get(Sorter.PARAMETER_SORT_KEY));
        Assert.assertEquals("desc", oppositeOrderParameters.get(Sorter.PARAMETER_SORT_ORDER));

        List<SorterKey> keys = sorter.getKeys();
        Assert.assertNotNull(keys);
        Assert.assertEquals(2, keys.size());
        SorterKey defaultKey = keys.get(0);
        Assert.assertEquals(currentKey.getName(), defaultKey.getName());

        SorterKey otherKey = keys.get(1);
        Assert.assertEquals("name", otherKey.getName());
        Assert.assertEquals("Product Name", otherKey.getLabel());
        Assert.assertEquals(Sorter.Order.ASC, otherKey.getOrder());
    }

    @Test
    public void testDefaultSorting() {
        context.currentResource(PRODUCT_LIST_NO_SORTING);
        adaptToProductList();
        SearchResultsSet resultSet = productListModel.getSearchResultsSet();
        Assert.assertNotNull(resultSet);
        Assert.assertTrue(resultSet.hasSorting());
        Sorter sorter = resultSet.getSorter();
        Assert.assertNotNull(sorter);

        SorterKey currentKey = sorter.getCurrentKey();
        Assert.assertNotNull(currentKey);
        Assert.assertEquals("price", currentKey.getName());
        Assert.assertEquals("Price", currentKey.getLabel());
        Assert.assertEquals(Sorter.Order.ASC, currentKey.getOrder());
        Assert.assertTrue(currentKey.isSelected());

        Map<String, String> currentOrderParameters = currentKey.getCurrentOrderParameters();
        Assert.assertNotNull(currentOrderParameters);
        Assert.assertEquals(resultSet.getAppliedQueryParameters().size() + 2, currentOrderParameters.size());
        resultSet.getAppliedQueryParameters()
            .forEach((key, value) -> Assert.assertEquals(value, currentOrderParameters.get(key)));
        Assert.assertEquals("price", currentOrderParameters.get(Sorter.PARAMETER_SORT_KEY));
        Assert.assertEquals("asc", currentOrderParameters.get(Sorter.PARAMETER_SORT_ORDER));

        Map<String, String> oppositeOrderParameters = currentKey.getOppositeOrderParameters();
        Assert.assertNotNull(oppositeOrderParameters);
        Assert.assertEquals(resultSet.getAppliedQueryParameters().size() + 2, oppositeOrderParameters.size());
        resultSet.getAppliedQueryParameters()
            .forEach((key, value) -> Assert.assertEquals(value, oppositeOrderParameters.get(key)));
        Assert.assertEquals("price", oppositeOrderParameters.get(Sorter.PARAMETER_SORT_KEY));
        Assert.assertEquals("desc", oppositeOrderParameters.get(Sorter.PARAMETER_SORT_ORDER));

        List<SorterKey> keys = sorter.getKeys();
        Assert.assertNotNull(keys);
        Assert.assertEquals(2, keys.size());
        SorterKey defaultKey = keys.get(0);
        Assert.assertEquals(currentKey.getName(), defaultKey.getName());

        SorterKey otherKey = keys.get(1);
        Assert.assertEquals("name", otherKey.getName());
        Assert.assertEquals("Product Name", otherKey.getLabel());
        Assert.assertEquals(Sorter.Order.ASC, otherKey.getOrder());
    }

    @Test
    public void testPagination() {
        context.request().getParameterMap().put("page", new String[] { "3" });

        adaptToProductList();

        productListModel.getProducts();

        ArgumentCaptor<GraphqlRequest> captor = ArgumentCaptor.forClass(GraphqlRequest.class);
        verify(graphqlClient, atLeastOnce()).execute(captor.capture(), any(), any(), any());

        // Check the "products" query
        List<GraphqlRequest> requests = captor.getAllValues();
        String productsQuery = null;
        for (GraphqlRequest request : requests) {
            if (request.getQuery().startsWith("{products")) {
                productsQuery = request.getQuery();
                break;
            }
        }
        Assert.assertTrue(productsQuery.contains("currentPage:3"));
    }

    @Test
    public void testClientLoadingIsDisabledOnLaunchPage() {
        adaptToProductList();
        Assert.assertTrue(productListModel.loadClientPrice());
        Page launch = context.pageManager().getPage("/content/launches/2020/09/14/mylaunch" + PAGE);
        Whitebox.setInternalState(productListModel, "currentPage", launch);
        Assert.assertFalse(productListModel.loadClientPrice());
    }

    @Test
    public void testJsonRender() throws IOException {
        adaptToProductList();
        ObjectMapper mapper = new ObjectMapper();

        String expected = Utils.getResource("results/result-datalayer-productlist-component.json");
        String jsonResult = productListModel.getData().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));

        String itemsJsonExpected = Utils.getResource("results/result-datalayer-productlistitem-component.json");
        String itemsJsonResult = productListModel.getProducts()
            .stream()
            .map(i -> i.getData().getJson())
            .collect(Collectors.joining(",", "[", "]"));
        Assert.assertEquals(mapper.readTree(itemsJsonExpected), mapper.readTree(itemsJsonResult));
    }

    @Test
    public void testStorefrontContextRender() throws IOException {
        adaptToProductList();

        ObjectMapper mapper = new ObjectMapper();

        String expected = Utils.getResource("storefront-context/result-storefront-context-productlist-component.json");
        CategoryStorefrontContext storefrontContext = productListModel.getStorefrontContext();

        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(storefrontContext.getJson()));
    }

    @Test
    public void testCanonicalUrlFromSitemapLinkExternalizer() {
        UrlProvider urlProvider = context.getService(UrlProvider.class);
        SitemapLinkExternalizer externalizer = mock(SitemapLinkExternalizer.class);
        SitemapLinkExternalizerProvider externalizerProvider = mock(SitemapLinkExternalizerProvider.class);
        when(externalizerProvider.getExternalizer(any())).thenReturn(externalizer);
        when(externalizer.toExternalCategoryUrl(any(), any(), any())).then(inv -> {
            // assert the parameters
            CategoryUrlFormat.Params parameters = inv.getArgumentAt(2, CategoryUrlFormat.Params.class);
            assertEquals("running-key", parameters.getUrlKey());
            assertEquals("running", parameters.getUrlPath());
            assertEquals("MTI==", parameters.getUid());
            Page page = inv.getArgumentAt(1, Page.class);
            assertNotNull(page);
            assertEquals("/content/pageA", page.getPath());
            // invoke the callback directly
            return urlProvider.toCategoryUrl(inv.getArgumentAt(0, SlingHttpServletRequest.class), page, parameters);
        });
        context.registerService(SitemapLinkExternalizerProvider.class, externalizerProvider);

        adaptToProductList();

        assertEquals("/content/category-page.html/running.html", productListModel.getCanonicalUrl());
    }
}
