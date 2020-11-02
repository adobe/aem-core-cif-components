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

package com.adobe.cq.commerce.core.components.internal.models.v1.productcollection;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.client.MockExternalizer;
import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductCollectionImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);

                context.registerInjectActivateService(new SearchFilterServiceImpl());
                context.registerInjectActivateService(new SearchResultsServiceImpl());

                context.registerService(Externalizer.class, new MockExternalizer());
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PAGE = "/content/pageA";
    private static final String PRODUCT_COLLECTION = "/content/pageA/jcr:content/root/responsivegrid/productcollection";
    private static final String PRODUCT_COLLECTION2 = "/content/pageA/jcr:content/root/responsivegrid/productcollection2";

    private ProductCollectionImpl productCollectionModel;

    @Before
    public void setUp() {

        context.currentResource(PRODUCT_COLLECTION);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("6");

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        Resource productCollectionResource = context.resourceResolver().getResource(PRODUCT_COLLECTION);
        SlingBindings slingBindings = getSlingBindings(PRODUCT_COLLECTION);

        XSSAPI xssApi = mock(XSSAPI.class);
        when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
        slingBindings.put("xssApi", xssApi);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);
    }

    private SlingBindings getSlingBindings(String resourcePath) {
        Page page = context.currentPage(PAGE);
        Resource productCollectionResource = context.resourceResolver().getResource(resourcePath);
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productCollectionResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productCollectionResource.getValueMap());
        return slingBindings;
    }

    @Test
    public void testCreateFilterMap() {
        productCollectionModel = context.request().adaptTo(ProductCollectionImpl.class);

        Map<String, String[]> queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] {});
        Map<String, String> filterMap = productCollectionModel.createFilterMap(queryParameters);
        Assert.assertEquals("filters out query parameters without values", 0, filterMap.size());

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] { "123" });
        filterMap = productCollectionModel.createFilterMap(queryParameters);
        Assert.assertEquals("retails valid query filters", 1, filterMap.size());
    }

    @Test
    public void testCalculateCurrentPageCursor() {
        productCollectionModel = context.request().adaptTo(ProductCollectionImpl.class);
        Assert.assertEquals("negative page indexes are not allowed", 1, productCollectionModel.calculateCurrentPageCursor("-1").intValue());
        Assert.assertEquals("null value is dealt with", 1, productCollectionModel.calculateCurrentPageCursor(null).intValue());
        Assert.assertEquals("non numeric value is dealt with", 1, productCollectionModel.calculateCurrentPageCursor("a").intValue());
        Assert.assertEquals("extra large value is dealt with", 1,
            productCollectionModel.calculateCurrentPageCursor("99999999999999").intValue());
    }

    @Test
    public void testDefaultProperties() {
        productCollectionModel = context.request().adaptTo(ProductCollectionImpl.class);
        Assert.assertEquals(ProductCollectionImpl.LOAD_CLIENT_PRICE_DEFAULT, productCollectionModel.loadClientPrice());
        Assert.assertEquals(SearchOptionsImpl.PAGE_SIZE_DEFAULT.intValue(), productCollectionModel.navPageSize);
        Assert.assertEquals(ProductCollectionImpl.PAGINATION_TYPE_DEFAULT, productCollectionModel.getPaginationType());
    }

    @Test
    public void testProperties() {
        getSlingBindings(PRODUCT_COLLECTION2);
        productCollectionModel = context.request().adaptTo(ProductCollectionImpl.class);
        Assert.assertFalse(productCollectionModel.loadClientPrice());
        Assert.assertEquals(8, productCollectionModel.navPageSize);
        Assert.assertEquals("loadmorebutton", productCollectionModel.getPaginationType());
    }

    @Test
    public void testStubMethods() {
        productCollectionModel = context.request().adaptTo(ProductCollectionImpl.class);
        Assert.assertNotNull(productCollectionModel.getSearchResultsSet());
        Assert.assertNotNull(productCollectionModel.getSearchResultsSet().getProductListItems());
        Assert.assertTrue(productCollectionModel.getSearchResultsSet().getProductListItems().isEmpty());
        Assert.assertNotNull(productCollectionModel.getProducts());
        Assert.assertTrue(productCollectionModel.getProducts().isEmpty());

    }
}
