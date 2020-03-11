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

package com.adobe.cq.commerce.core.components.internal.models.v1.productlist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;

//todo-kevin: GroupedProduct and ProductImage may need to be looked at to see if I need to include them from converter
import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.StoreConfig;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductListImplTest {

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
    private static final String PRODUCTLIST = "/content/pageA/jcr:content/root/responsivegrid/productlist";

    private Resource productListResource;
    private ProductListImpl productListModel;
    private CategoryInterface category;
    private StoreConfig storeConfig;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(PAGE);
        context.currentResource(PRODUCTLIST);
        SearchResultsService searchResultsService = Mockito.mock(SearchResultsServiceImpl.class);
        SearchResultsSet searchResultSet = Mockito.mock(SearchResultsSetImpl.class);

        Mockito.when(searchResultSet.getProductListItems()).thenReturn(new ArrayList<>());
        Mockito.when(searchResultsService.performSearch(any(), any(), any(), any())).thenReturn(searchResultSet);
        context.registerService(searchResultsService);

        productListResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTLIST));

        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-category-result.json");
        category = rootQuery.getCategory();
        storeConfig = rootQuery.getStoreConfig();

        GraphqlResponse<Object, Object> response = new GraphqlResponse<>();
        response.setData(rootQuery);
        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);

        Mockito.when(productListResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        Mockito.when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("6");

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productListResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productListResource.getValueMap());

        XSSAPI xssApi = mock(XSSAPI.class);
        when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
        slingBindings.put("xssApi", xssApi);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);

        // context.request().adaptTo(ProductListImpl.class); is moved to each test because it uses an internal cache
        // and we want to override the "slug" in testEditModePlaceholderData()
    }

    @Test
    public void getTitle() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertEquals(category.getName(), productListModel.getTitle());
    }

    @Test
    public void getImage() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertEquals(category.getImage(), productListModel.getImage());
    }

    @Test
    public void getImageWhenMissingInResponse() {
        productListModel = context.request().adaptTo(ProductListImpl.class);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getImage()).thenReturn("");
        Whitebox.setInternalState(productListModel.getCategoryRetriever(), "category", category);

        String image = productListModel.getImage();
        Assert.assertEquals("", image);
    }

    @Test
    public void getProducts() {
        productListModel = context.request().adaptTo(ProductListImpl.class);

        Collection<ProductListItem> products = productListModel.getProducts();
        Assert.assertNotNull(products);

        // We introduce one "faulty" product data in the response, it should be skipped
        Assert.assertEquals(6, products.size());

        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        List<ProductListItem> results = products.stream().collect(Collectors.toList());
        for (int i = 0; i < results.size(); i++) {
            // get raw GraphQL object
            ProductInterface productInterface = category.getProducts().getItems().get(i);
            // get mapped product list item
            ProductListItem item = results.get(i);

            Assert.assertEquals(productInterface.getName(), item.getTitle());
            Assert.assertEquals(productInterface.getSku(), item.getSKU());
            Assert.assertEquals(productInterface.getUrlKey(), item.getSlug());
            Assert.assertEquals(String.format(PRODUCT_PAGE + ".%s.html", productInterface.getUrlKey()), item.getURL());

            // Make sure deprecated methods still work
            Assert.assertEquals(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getValue(), item.getPrice(), 0);
            Assert.assertEquals(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getCurrency().toString(), item
                .getCurrency());
            priceFormatter.setCurrency(Currency.getInstance(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getCurrency()
                .toString()));
            Assert.assertEquals(priceFormatter.format(productInterface.getPriceRange().getMinimumPrice().getFinalPrice().getValue()), item
                .getFormattedPrice());

            ProductImage smallImage = productInterface.getSmallImage();
            if (smallImage == null) {
                // if small image is missing for a product in GraphQL response then image URL is null for the related item
                Assert.assertNull(item.getImageURL());
            } else {
                Assert.assertEquals(smallImage.getUrl(), item.getImageURL());
            }

            Assert.assertEquals(productInterface instanceof GroupedProduct, item.getPriceRange().isStartPrice());
        }
    }

    @Test
    public void testParseCategoryId() {

        productListModel = context.request().adaptTo(ProductListImpl.class);
        final String TEST_CATEGORY_ID = "13";
        final Optional<String> TEST_CATEGORY_OPTIONAL = Optional.of(TEST_CATEGORY_ID);

        // null values should result in a null category id
        Optional<String> parsedCategoryId = productListModel.parseCategoryId(null, null);
        Assert.assertEquals("null values result in empty category id", parsedCategoryId, Optional.empty());

        parsedCategoryId = productListModel.parseCategoryId(null, "13");
        Assert.assertNotNull("fallback query string parameter works with valid category_id parameter", parsedCategoryId);
        Assert.assertEquals("fallback query string parameter works with valid category_id parameter", TEST_CATEGORY_OPTIONAL,
            parsedCategoryId);

        parsedCategoryId = productListModel.parseCategoryId("13", null);
        Assert.assertEquals("main path parsing returns correct cateogry", TEST_CATEGORY_OPTIONAL, parsedCategoryId);

    }

    @Test
    public void testCreateFilterMap() {

        productListModel = context.request().adaptTo(ProductListImpl.class);

        Map<String, String[]> queryParameters;
        Map<String, String> filterMap;

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] {});
        filterMap = productListModel.createFilterMap(queryParameters);
        Assert.assertEquals("filters out query parameters without values", 0, filterMap.size());

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] { "123" });
        filterMap = productListModel.createFilterMap(queryParameters);
        Assert.assertEquals("retails valid query filters", 1, filterMap.size());
    }

    @Test
    public void testCalculateCurrentPageCursor() {
        productListModel = context.request().adaptTo(ProductListImpl.class);
        Assert.assertEquals("negative page indexes are not allowed", 1, productListModel.calculateCurrentPageCursor("-1").intValue());
        Assert.assertEquals("null value is dealt with", 1, productListModel.calculateCurrentPageCursor(null).intValue());
    }

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }

    @Test
    public void testProductListNoGraphqlClient() throws IOException {
        Mockito.when(productListResource.adaptTo(GraphqlClient.class)).thenReturn(null);
        productListModel = context.request().adaptTo(ProductListImpl.class);

        Assert.assertTrue(productListModel.getTitle().isEmpty());
        Assert.assertTrue(productListModel.getImage().isEmpty());
        Assert.assertTrue(productListModel.getProducts().isEmpty());
    }
}
