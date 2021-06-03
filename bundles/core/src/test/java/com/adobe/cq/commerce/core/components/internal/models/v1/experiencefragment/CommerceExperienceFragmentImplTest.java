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

import java.io.IOException;

import javax.jcr.Session;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.CategoryIdentifierType;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private void setup(String pagePath, String resourcePath) throws IOException {
        setup(pagePath, resourcePath, false);
    }

    private void setup(String pagePath, String resourcePath, boolean uidSupport) throws IOException {
        setupUrlProvider(ProductIdentifierType.URL_KEY);

        Page page = Mockito.spy(context.currentPage(pagePath));
        Resource xfResource = context.resourceResolver().getResource(pagePath + resourcePath);
        context.currentResource(xfResource);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.setResource(xfResource);

        HttpClient httpClient = mock(HttpClient.class);
        GraphqlClient graphqlClient = Mockito.spy(new GraphqlClientImpl());

        GraphqlClientConfiguration graphqlClientConfiguration = mock(GraphqlClientConfiguration.class);
        when(graphqlClientConfiguration.httpMethod()).thenReturn(HttpMethod.POST);

        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "configuration", graphqlClientConfiguration);

        Utils.setupHttpResponse("graphql/magento-graphql-xf1-category-uid.json", httpClient, HttpStatus.SC_OK,
            "1\"}}){uid}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf1-category-id.json", httpClient, HttpStatus.SC_OK,
            "1\"}}){id}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf1-product.json", httpClient, HttpStatus.SC_OK,
            "1\"}}){items{__typename,sku}}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf2-category-uid.json", httpClient, HttpStatus.SC_OK,
            "2\"}}){uid}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf2-category-id.json", httpClient, HttpStatus.SC_OK,
            "2\"}}){id}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf2-product.json", httpClient, HttpStatus.SC_OK,
            "2\"}}){items{__typename,sku}}}");

        ValueMap mockConfig = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
            "my-store", "enableUIDSupport", String.valueOf(uidSupport)));

        Resource pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(new ComponentsConfiguration(mockConfig));

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

    }

    private void setupUrlProvider(ProductIdentifierType productIdentifierType) {
        setupUrlProvider(productIdentifierType, CategoryIdentifierType.ID);
    }

    private void setupUrlProvider(ProductIdentifierType productIdentifierType, CategoryIdentifierType categoryIdentifierType) {
        UrlProviderImpl urlProvider = new UrlProviderImpl();
        MockUrlProviderConfiguration config = new MockUrlProviderConfiguration();
        config.setProductIdentifierType(productIdentifierType);
        config.setCategoryIdentifierType(categoryIdentifierType);
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
    public void testFragmentOnProductPageWithoutLocationProperty() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF1);

        setupUrlProvider(ProductIdentifierType.SKU);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf1");

        verifyFragment(SITE_XF_ROOT, "sku-xf1", null, null, "xf-1-uid",
            "/content/experience-fragments/mysite/page/xf-1-uid/master/jcr:content");
    }

    @Test
    public void testFragmentOnProductPageWithLocationPropertyAndSkuInRequest() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF2);
        setupUrlProvider(ProductIdentifierType.SKU);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf2");

        verifyFragment(SITE_XF_ROOT, "sku-xf2", null, "location-xf2", "xf-2-uid",
            "/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content");
    }

    @Test
    public void testFragmentOnProductPageWithInvalidLanguageManager() throws IOException {
        Mockito.reset(languageManager);

        setup(PRODUCT_PAGE, RESOURCE_XF1);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("url-key-xf1");

        verifyFragment(XF_ROOT, "sku-xf1", null, null, "xf-1-uid", "/content/experience-fragments/mysite/page/xf-1-uid/master/jcr:content");
    }

    @Test
    public void testFragmentOnProductPageWithoutMatchingSkus() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF2);
        setupUrlProvider(ProductIdentifierType.SKU);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf3");

        verifyFragmentResourceIsNull(XF_ROOT, "sku-xf3", null, "location-xf2");
    }

    @Test
    public void testFragmentOnProductPageWhenProductNotFound() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF1);

        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Assert.assertNotNull(cxf);
        Assert.assertNull(cxf.getExperienceFragmentResource());
    }

    @Test
    public void testFragmentOnNonProductOrCategoryPage() throws IOException {
        setup(ANOTHER_PAGE, RESOURCE_XF1);
        setupUrlProvider(ProductIdentifierType.SKU);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf1");

        verifyFragmentResourceIsNull(XF_ROOT, "sku-xf1", null, null);
    }

    @Test
    public void testFragmentOnCategoryPageWithoutLocationProperty() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF1);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("1");

        verifyFragment(SITE_XF_ROOT, null, "1", null, "xf-1-id", "/content/experience-fragments/mysite/page/xf-1-id/master/jcr:content");
    }

    @Test
    public void testFragmentOnCategoryPageWithLocationPropertyAndIdInRequest() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("2");

        verifyFragment(SITE_XF_ROOT, null, "2", "location-xf2", "xf-2-id",
            "/content/experience-fragments/mysite/page/xf-2-id/master/jcr:content");
    }

    @Test
    public void testFragmentOnCategoryPageWithLocationPropertyAndUIDInRequest() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);
        setupUrlProvider(ProductIdentifierType.SKU, CategoryIdentifierType.UID);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("uid2");

        verifyFragment(SITE_XF_ROOT, null, "2", "location-xf2", "xf-2-id",
            "/content/experience-fragments/mysite/page/xf-2-id/master/jcr:content");
    }

    @Test
    public void testFragmentOnCategoryPageWithoutMatchingIds() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("3");

        verifyFragmentResourceIsNull(XF_ROOT, null, "3", "location-xf2");
    }

    @Test
    public void testFragmentOnCategoryPageWithInvalidId() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        verifyFragmentResourceIsNull(XF_ROOT, null, null, null);
    }

    @Test
    public void testUIDSupportWithIDSelector() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF1, true);
        setupUrlProvider(ProductIdentifierType.SKU, CategoryIdentifierType.ID);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("1");

        verifyFragment(SITE_XF_ROOT, null, "uid1", null, "xf-1-uid",
            "/content/experience-fragments/mysite/page/xf-1-uid/master/jcr:content");
    }

    @Test
    public void testUIDSupportWithURLPathSelector() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2, true);
        setupUrlProvider(ProductIdentifierType.SKU, CategoryIdentifierType.URL_PATH);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("url_path2");

        verifyFragment(SITE_XF_ROOT, null, "uid2", "location-xf2", "xf-2-uid",
            "/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content");
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
