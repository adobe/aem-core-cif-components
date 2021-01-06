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
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
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
    private static final String ANOTHER_PAGE = "/content/mysite/another-page";
    private static final String RESOURCE_XF1 = "/content/mysite/page/jcr:content/root/xf-component-1";
    private static final String RESOURCE_XF2 = "/content/mysite/page/jcr:content/root/xf-component-2";
    private static final String XF_ROOT = "/content/experience-fragments/mysite/page";

    private static final String QUERY_1 = "SELECT * FROM [cq:PageContent] as node" +
        " WHERE ISDESCENDANTNODE('/content/experience-fragments/mysite/page')" +
        " AND node.[cq:products] = 'sku-xf1' AND node.[fragmentLocation] IS NULL";

    private static final String QUERY_2 = "SELECT * FROM [cq:PageContent] as node" +
        " WHERE ISDESCENDANTNODE('/content/experience-fragments/mysite/page')" +
        " AND node.[cq:products] = 'sku-xf2' AND node.[fragmentLocation] = 'location-xf2'";

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                LanguageManager languageManager = Mockito.mock(LanguageManager.class);
                Page rootPage = context.pageManager().getPage(PAGE);
                Mockito.when(languageManager.getLanguageRoot(Mockito.any())).thenReturn(rootPage);
                context.registerService(LanguageManager.class, languageManager);

                LiveRelationshipManager liveRelationshipManager = Mockito.mock(LiveRelationshipManager.class);
                context.registerService(LiveRelationshipManager.class, liveRelationshipManager);
            },
            ResourceResolverType.JCR_MOCK);
    }

    private void setup(String pagePath, String resourcePath, ProductIdentifierType productIdentifierType) {
        Page page = Mockito.spy(context.currentPage(pagePath));
        Resource xfResource = context.resourceResolver().getResource(resourcePath);
        context.currentResource(xfResource);

        UrlProviderImpl urlProvider = new UrlProviderImpl();
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierType(productIdentifierType);
        urlProvider.activate(config);
        context.registerService(UrlProvider.class, urlProvider);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.setResource(xfResource);
    }

    @Test
    public void testFragmentOnProductPageWithoutLocationProperty() {
        setup(PAGE, RESOURCE_XF1, ProductIdentifierType.URL_KEY);

        Product product = Mockito.mock(Product.class);
        Mockito.when(product.getFound()).thenReturn(true);
        Mockito.when(product.getSku()).thenReturn("sku-xf1");
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        Resource pageResource = context.resourceResolver().getResource(XF_ROOT);
        Session session = context.resourceResolver().adaptTo(Session.class);
        XFMockQueryResultHandler queryHandler = new XFMockQueryResultHandler(pageResource, "sku-xf1", null);
        MockJcr.addQueryResultHandler(session, queryHandler);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Resource xf = cxf.getExperienceFragmentResource();
        Assert.assertEquals("/content/experience-fragments/mysite/page/xf-1/master/jcr:content", xf.getPath());
        Assert.assertEquals("xf-1", cxf.getName());
        Assert.assertEquals(QUERY_1, queryHandler.getQuery().getStatement());
    }

    @Test
    public void testFragmentWithSkuInRequestAndLocationProperty() {
        setup(PAGE, RESOURCE_XF2, ProductIdentifierType.SKU);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf2");

        Resource pageResource = context.resourceResolver().getResource(XF_ROOT);
        Session session = context.resourceResolver().adaptTo(Session.class);
        XFMockQueryResultHandler queryHandler = new XFMockQueryResultHandler(pageResource, "sku-xf2", "location-xf2");
        MockJcr.addQueryResultHandler(session, queryHandler);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Resource xf = cxf.getExperienceFragmentResource();
        Assert.assertEquals("/content/experience-fragments/mysite/page/xf-2/master/jcr:content", xf.getPath());
        Assert.assertEquals("xf-2", cxf.getName());
        Assert.assertEquals(QUERY_2, queryHandler.getQuery().getStatement());
    }

    @Test
    public void testFragmentWithoutMatchingSkus() {
        setup(PAGE, RESOURCE_XF2, ProductIdentifierType.SKU);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf3");

        Resource pageResource = context.resourceResolver().getResource(XF_ROOT);
        Session session = context.resourceResolver().adaptTo(Session.class);
        XFMockQueryResultHandler queryHandler = new XFMockQueryResultHandler(pageResource, "sku-xf3", "location-xf2");
        MockJcr.addQueryResultHandler(session, queryHandler);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Resource xf = cxf.getExperienceFragmentResource();
        Assert.assertNull(xf);
    }

    @Test
    public void testProductNotFound() {
        setup(PAGE, RESOURCE_XF1, ProductIdentifierType.URL_KEY);

        Product product = Mockito.mock(Product.class);
        Mockito.when(product.getFound()).thenReturn(false);
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Resource xf = cxf.getExperienceFragmentResource();
        Assert.assertNull(xf);
    }

    @Test
    public void testIsNotProductPage() {
        setup(ANOTHER_PAGE, RESOURCE_XF1, ProductIdentifierType.URL_KEY);

        Product product = Mockito.mock(Product.class);
        Mockito.when(product.getFound()).thenReturn(true);
        Mockito.when(product.getSku()).thenReturn("sku-xf1");
        context.registerAdapter(MockSlingHttpServletRequest.class, Product.class, product);

        Resource pageResource = context.resourceResolver().getResource(XF_ROOT);
        Session session = context.resourceResolver().adaptTo(Session.class);
        XFMockQueryResultHandler queryHandler = new XFMockQueryResultHandler(pageResource, "sku-xf1", null);
        MockJcr.addQueryResultHandler(session, queryHandler);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Resource xf = cxf.getExperienceFragmentResource();
        Assert.assertNull(xf);
    }
}
