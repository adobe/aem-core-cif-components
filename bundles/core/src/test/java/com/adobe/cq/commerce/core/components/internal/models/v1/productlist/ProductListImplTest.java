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
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
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

import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.StoreConfig;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
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

    private static final String PAGE = "/content/pageA";
    private static final String PRODUCTLIST = "/content/pageA/jcr:content/root/responsivegrid/productlist";

    private Resource productListResource;
    private ProductListImpl productListModel;
    private CategoryInterface category;
    private StoreConfig storeConfig;

    SlingHttpServletRequest incomingRequest;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(PAGE);
        context.currentResource(PRODUCTLIST);
        productListResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTLIST));

        String json = getResource("/graphql/magento-graphql-category-result.json");
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
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

        productListModel = context.request().adaptTo(ProductListImpl.class);
    }

    @Test
    public void getTitle() {
        Assert.assertEquals(category.getName(), productListModel.getTitle());
    }

    @Test
    public void getImage() {
        String baseMediaPath = storeConfig.getSecureBaseMediaUrl() + "catalog/category/";
        Assert.assertEquals(baseMediaPath + category.getImage(), productListModel.getImage());
    }

    @Test
    public void getImageWhenMissingInResponse() {
        ProductList list = new ProductListImpl();
        CategoryTree category = mock(CategoryTree.class);
        when(category.getImage()).thenReturn("");
        Whitebox.setInternalState(list, "category", category);

        String image = list.getImage();
        Assert.assertEquals("", image);
    }

    @Test
    public void getProducts() {
        Collection<ProductListItem> products = productListModel.getProducts();
        Assert.assertNotNull(products);
        Assert.assertEquals(true, category.getProducts().getItems().size() <= products.size());

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
            Assert.assertEquals(String.format(PAGE + ".%s.html", productInterface.getUrlKey()), item.getURL());

            Money amount = productInterface.getPrice().getRegularPrice().getAmount();
            Assert.assertEquals(amount.getValue(), item.getPrice(), 0);
            Assert.assertEquals(amount.getCurrency().toString(), item.getCurrency());

            priceFormatter.setCurrency(Currency.getInstance(amount.getCurrency().toString()));
            Assert.assertEquals(priceFormatter.format(amount.getValue()), item.getFormattedPrice());

            Assert.assertEquals(productInterface.getSmallImage().getUrl(), item.getImageURL());
        }
    }

    @Test
    public void testPagination() {
        // Cannot be added to JCR JSON content because of long/int conversion
        int pageSize = (int) Whitebox.getInternalState(productListModel, "navPageSize");

        Assert.assertTrue(pageSize >= productListModel.getProducts().size());
        Assert.assertTrue(pageSize <= category.getProductCount());

        incomingRequest = mock(SlingHttpServletRequest.class);

        when(incomingRequest.getParameter("page")).thenReturn("1");
        Whitebox.setInternalState(productListModel, "request", incomingRequest);
        productListModel.setNavPageCursor();
        productListModel.setupPagination();
        Assert.assertEquals(3, productListModel.getPageList().length);

        Assert.assertEquals(1, productListModel.getCurrentNavPage());
        Assert.assertEquals(1, productListModel.getPreviousNavPage());
        Assert.assertEquals(2, productListModel.getNextNavPage());
        Assert.assertTrue(productListModel.getProducts().size() <= pageSize);

        when(incomingRequest.getParameter("page")).thenReturn("2");
        Whitebox.setInternalState(productListModel, "request", incomingRequest);
        productListModel.setNavPageCursor();
        productListModel.setupPagination();

        Assert.assertEquals(2, productListModel.getCurrentNavPage());
        Assert.assertEquals(1, productListModel.getPreviousNavPage());
        Assert.assertEquals(3, productListModel.getNextNavPage());
        Assert.assertTrue(productListModel.getProducts().size() <= pageSize);

        when(incomingRequest.getParameter("page")).thenReturn("3");
        Whitebox.setInternalState(productListModel, "request", incomingRequest);
        productListModel.setNavPageCursor();
        productListModel.setupPagination();

        Assert.assertEquals(3, productListModel.getCurrentNavPage());
        Assert.assertEquals(2, productListModel.getPreviousNavPage());
        Assert.assertEquals(3, productListModel.getNextNavPage());
        Assert.assertTrue(productListModel.getProducts().size() <= pageSize);

        // Test when page size matches number of products
        Whitebox.setInternalState(productListModel, "navPageSize", 13);
        when(incomingRequest.getParameter("page")).thenReturn("1");
        Whitebox.setInternalState(productListModel, "request", incomingRequest);
        productListModel.setNavPageCursor();
        productListModel.setupPagination();
        Assert.assertEquals(1, productListModel.getCurrentNavPage());
        Assert.assertEquals(1, productListModel.getPreviousNavPage());
        Assert.assertEquals(1, productListModel.getNextNavPage());

        // Test with invalid page size
        when(incomingRequest.getParameter("page")).thenReturn("0");
        Whitebox.setInternalState(productListModel, "request", incomingRequest);
        productListModel.setNavPageCursor();
        productListModel.setupPagination();
        Assert.assertEquals(1, productListModel.getCurrentNavPage());
    }

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }
}
