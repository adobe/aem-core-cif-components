/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.models.v1.experiencefragment;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.CategoryIdentifierType;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class CommerceExperienceFragmentImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content-experiencefragment.json");

    private static final String PAGE = "/content/mysite/page";
    private static final String PRODUCT_PAGE = PAGE + "/product-page";
    private static final String CATEGORY_PAGE = PAGE + "/category-page";
    private static final String ANOTHER_PAGE = PAGE + "/another-page";
    private static final String RESOURCE_XF1 = "/jcr:content/root/xf-component-1";
    private static final String RESOURCE_XF2 = "/jcr:content/root/xf-component-2";
    private static final String XF_ROOT = "/content/experience-fragments/";
    private static final String SITE_XF_ROOT = XF_ROOT + "mysite/page";

    private static final String PRODUCT_QUERY_TEMPLATE = "SELECT * FROM [cq:PageContent] as node WHERE ISDESCENDANTNODE('%s')" +
        " AND (node.[" + CommerceExperienceFragment.PN_CQ_PRODUCTS + "] = '%s'" +
        " OR node.[" + CommerceExperienceFragment.PN_CQ_PRODUCTS + "] LIKE '%s#%%')" +
        " AND node.[" + CommerceExperienceFragment.PN_FRAGMENT_LOCATION + "] %s";

    private static final String CATEGORY_QUERY_TEMPLATE = "SELECT * FROM [cq:PageContent] as node WHERE ISDESCENDANTNODE('%s')" +
        " AND node.[" + CommerceExperienceFragment.PN_CQ_CATEGORIES + "] = '%s'" +
        " AND node.[" + CommerceExperienceFragment.PN_FRAGMENT_LOCATION + "] %s";

    private LanguageManager languageManager;

    private AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                languageManager = Mockito.mock(LanguageManager.class);
                Page rootPage = context.pageManager().getPage(PAGE);
                Mockito.when(languageManager.getLanguageRoot(Mockito.any())).thenReturn(rootPage);
                context.registerService(LanguageManager.class, languageManager);

                LiveRelationshipManager liveRelationshipManager = Mockito.mock(LiveRelationshipManager.class);
                context.registerService(LiveRelationshipManager.class, liveRelationshipManager);
            },
            ResourceResolverType.JCR_MOCK);
    }

    private void setup(String pagePath, String resourcePath) {
        setupUrlProvider(ProductIdentifierType.URL_KEY);

        Page page = Mockito.spy(context.currentPage(pagePath));
        Resource xfResource = context.resourceResolver().getResource(pagePath + resourcePath);
        context.currentResource(xfResource);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.setResource(xfResource);
    }

    private void setupUrlProvider(ProductIdentifierType productIdentifierType) {
        UrlProviderImpl urlProvider = new UrlProviderImpl();
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierType(productIdentifierType);
        config.setCategoryIdentifierType(CategoryIdentifierType.ID);
        urlProvider.activate(config);
        context.registerService(UrlProvider.class, urlProvider);
    }

    private String buildQuery(String xfRoot, String productSku, String categoryId, String fragmentLocation) {
        String flCondition = fragmentLocation != null ? "= '" + fragmentLocation + "'" : "IS NULL";
        String query;
        if (productSku != null) {
            query = String.format(PRODUCT_QUERY_TEMPLATE, xfRoot, productSku, productSku, flCondition);
        } else {
            query = String.format(CATEGORY_QUERY_TEMPLATE, xfRoot, categoryId, flCondition);
        }
        return query;
    }

    @Test
    public void testFragmentOnProductPageWithoutLocationProperty() {
        setup(PRODUCT_PAGE, RESOURCE_XF1);

        Product product = Mockito.mock(Product.class);
        Mockito.when(product.getFound()).thenReturn(true);
        Mockito.when(product.getSku()).thenReturn("sku-xf1");
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        verifyFragment(SITE_XF_ROOT, "sku-xf1", null, null, "xf-1", "/content/experience-fragments/mysite/page/xf-1/master/jcr:content");
    }

    @Test
    public void testFragmentOnProductPageWithLocationPropertyAndSkuInRequest() {
        setup(PRODUCT_PAGE, RESOURCE_XF2);
        setupUrlProvider(ProductIdentifierType.SKU);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf2");

        verifyFragment(SITE_XF_ROOT, "sku-xf2", null, "location-xf2", "xf-2",
            "/content/experience-fragments/mysite/page/xf-2/master/jcr:content");
    }

    @Test
    public void testFragmentOnProductPageWithInvalidLanguageManager() {
        Mockito.reset(languageManager);

        setup(PRODUCT_PAGE, RESOURCE_XF1);

        Product product = Mockito.mock(Product.class);
        Mockito.when(product.getFound()).thenReturn(true);
        Mockito.when(product.getSku()).thenReturn("sku-xf1");
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        verifyFragment(XF_ROOT, "sku-xf1", null, null, "xf-1", "/content/experience-fragments/mysite/page/xf-1/master/jcr:content");
    }

    @Test
    public void testFragmentOnProductPageWithoutMatchingSkus() {
        setup(PRODUCT_PAGE, RESOURCE_XF2);
        setupUrlProvider(ProductIdentifierType.SKU);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf3");

        verifyFragmentResourceIsNull(XF_ROOT, "sku-xf3", null, "location-xf2");
    }

    @Test
    public void testFragmentOnProductPageWhenProductNotFound() {
        setup(PRODUCT_PAGE, RESOURCE_XF1);

        Product product = Mockito.mock(Product.class);
        Mockito.when(product.getFound()).thenReturn(false);
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Assert.assertNotNull(cxf);
        Assert.assertNull(cxf.getExperienceFragmentResource());
    }

    @Test
    public void testFragmentOnNonProductOrCategoryPage() {
        setup(ANOTHER_PAGE, RESOURCE_XF1);

        Product product = Mockito.mock(Product.class);
        Mockito.when(product.getFound()).thenReturn(true);
        Mockito.when(product.getSku()).thenReturn("sku-xf1");
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        verifyFragmentResourceIsNull(XF_ROOT, "sku-xf1", null, null);
    }

    @Test
    public void testFragmentOnCategoryPageWithoutLocationProperty() {
        setup(CATEGORY_PAGE, RESOURCE_XF1);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("catid-xf1");

        verifyFragment(SITE_XF_ROOT, null, "catid-xf1", null, "xf-1", "/content/experience-fragments/mysite/page/xf-1/master/jcr:content");
    }

    @Test
    public void testFragmentOnCategoryPageWithLocationPropertyAndIdInRequest() {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("catid-xf2");

        verifyFragment(SITE_XF_ROOT, null, "catid-xf2", "location-xf2", "xf-2",
            "/content/experience-fragments/mysite/page/xf-2/master/jcr:content");
    }

    @Test
    public void testFragmentOnCategoryPageWithoutMatchingIds() {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("catid-xf3");

        verifyFragmentResourceIsNull(XF_ROOT, null, "catid-xf3", "location-xf2");
    }

    @Test
    public void testFragmentOnCategoryPageWithInvalidId() {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        verifyFragmentResourceIsNull(XF_ROOT, null, null, null);
    }

    private void verifyFragment(String xfRootPath, String productSku, String categoryId, String fragmentLocation, String expectedXFName,
        String expectedXFPath) {
        XFMockQueryResultHandler queryHandler = mockJcrQueryResult(xfRootPath, productSku, categoryId, fragmentLocation);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Assert.assertNotNull(cxf);
        Assert.assertEquals(expectedXFName, cxf.getName());
        Assert.assertEquals(CommerceExperienceFragmentImpl.RESOURCE_TYPE, cxf.getExportedType());
        Assert.assertEquals(expectedXFPath, cxf.getExperienceFragmentResource().getPath());

        String expectedQuery = buildQuery(xfRootPath, productSku, categoryId, fragmentLocation);
        Assert.assertEquals(expectedQuery, queryHandler.getQuery().getStatement());
    }

    private void verifyFragmentResourceIsNull(String xfRootPath, String productSku, String categoryId, String fragmentLocation) {
        mockJcrQueryResult(xfRootPath, productSku, categoryId, fragmentLocation);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Assert.assertNotNull(cxf);
        Assert.assertNull(cxf.getExperienceFragmentResource());
    }

    private XFMockQueryResultHandler mockJcrQueryResult(String xfRootPath, String productSku, String categoryId, String fragmentLocation) {
        Resource pageResource = context.resourceResolver().getResource(xfRootPath);
        Session session = context.resourceResolver().adaptTo(Session.class);
        XFMockQueryResultHandler queryHandler = new XFMockQueryResultHandler(pageResource, productSku, categoryId, fragmentLocation);
        MockJcr.addQueryResultHandler(session, queryHandler);
        return queryHandler;
    }
}
