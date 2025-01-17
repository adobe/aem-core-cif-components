/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.experiencefragment;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CommerceExperienceFragmentImplTest {

    private static final String PAGE = "/content/mysite/page";
    private static final String PRODUCT_PAGE = PAGE + "/product-page";
    private static final String CATEGORY_PAGE = PAGE + "/category-page";
    private static final String ANOTHER_PAGE = PAGE + "/another-page";
    private static final String RESOURCE_XF1 = "/jcr:content/root/xf-component-1";
    private static final String RESOURCE_XF2 = "/jcr:content/root/xf-component-2";

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content-experiencefragment.json")
        .<AemContext>afterSetUp(context -> {
            context.registerService(LiveRelationshipManager.class, mock(LiveRelationshipManager.class));
            CommerceExperienceFragmentsRetriever cxfRetriever = context.registerService(CommerceExperienceFragmentsRetriever.class, mock(
                CommerceExperienceFragmentsRetriever.class));

            Resource xf1uid = context.resourceResolver().getResource(
                "/content/experience-fragments/mysite/page/xf-1-uid/master/jcr:content");
            Resource xf2uid = context.resourceResolver()
                .getResource("/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content");
            Mockito.when(cxfRetriever.getExperienceFragmentsForProduct(eq("sku-xf1"),
                isNull(String.class), any())).thenReturn(Collections.singletonList(xf1uid));
            Mockito.when(cxfRetriever.getExperienceFragmentsForProduct(eq("sku-xf2"),
                eq("location-xf2"), any())).thenReturn(Collections.singletonList(xf2uid));
            Mockito.when(cxfRetriever.getExperienceFragmentsForProduct(eq("sku-xf3"),
                any(), any())).thenReturn(Collections.emptyList());
            Mockito.when(cxfRetriever.getExperienceFragmentsForProduct(isNull(String.class),
                any(), any())).thenReturn(Collections.emptyList());
            Mockito.when(cxfRetriever.getExperienceFragmentsForCategory(eq("uid1"),
                isNull(String.class), any())).thenReturn(Collections.singletonList(xf1uid));
            Mockito.when(cxfRetriever.getExperienceFragmentsForCategory(eq("uid2"),
                eq("location-xf2"), any())).thenReturn(Collections.singletonList(xf2uid));
            Mockito.when(cxfRetriever.getExperienceFragmentsForCategory(eq("uid3"), any(), any())).thenReturn(Collections
                .emptyList());
            Mockito.when(cxfRetriever.getExperienceFragmentsForCategory(isNull(String.class),
                any(), any())).thenReturn(Collections.emptyList());
        })
        .build();

    private void setup(String pagePath, String resourcePath) throws IOException {
        Page page = spy(context.currentPage(pagePath));
        Resource xfResource = context.resourceResolver().getResource(pagePath + resourcePath);
        context.currentResource(xfResource);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.setResource(xfResource);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));
        GraphqlClient graphqlClient = Mockito.spy(new GraphqlClientImpl());
        Utils.activateGraphqlClient(context, graphqlClient, null);

        Utils.setupHttpResponse("graphql/magento-graphql-xf1-category.json", httpClient, HttpStatus.SC_OK, "1\"}}){uid}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf1-product.json", httpClient, HttpStatus.SC_OK,
            "1\"}}){items{__typename,sku,url_key}}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf2-category.json", httpClient, HttpStatus.SC_OK, "2\"}}){uid}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf2-product.json", httpClient, HttpStatus.SC_OK,
            "2\"}}){items{__typename,sku,url_key}}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf3-category.json", httpClient, HttpStatus.SC_OK, "3\"}}){uid}}");
        Utils.setupHttpResponse("graphql/magento-graphql-xf3-product.json", httpClient, HttpStatus.SC_OK,
            "3\"}}){items{__typename,sku,url_key}}}");

        ValueMap mockConfig = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
            "my-store", "enableUIDSupport", "true"));

        Resource pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(new ComponentsConfiguration(mockConfig));

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

    }

    @Test
    public void testFragmentOnProductPageWithoutLocationProperty() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF1);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/url-key-xf1.html");

        verifyFragment("sku-xf1", null, null, "xf-1-uid",
            "/content/experience-fragments/mysite/page/xf-1-uid/master/jcr:content");
    }

    @Test
    public void testFragmentOnProductPageWithLocationProperty() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/url-key-xf2.html");

        verifyFragment("sku-xf2", null, "location-xf2", "xf-2-uid",
            "/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content");
    }

    @Test
    public void testFragmentOnProductPageWithoutMatchingSkus() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/sku-xf3.html");

        verifyFragmentResourceIsNull("sku-xf3", null, "location-xf2");
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

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSelectorString("sku-xf1");

        verifyFragmentResourceIsNull("sku-xf1", null, null);
    }

    @Test
    public void testFragmentOnCategoryPageWithoutLocationProperty() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF1);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/uid1.html");

        verifyFragment(null, "uid1", null, "xf-1-uid",
            "/content/experience-fragments/mysite/page/xf-1-uid/master/jcr:content");
    }

    @Test
    public void testFragmentOnCategoryPageWithLocationProperty() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/uid2.html");

        verifyFragment(null, "uid2", "location-xf2", "xf-2-uid",
            "/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content");
    }

    @Test
    public void testFragmentOnCategoryPageWithoutMatchingUids() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/uid3.html");

        verifyFragmentResourceIsNull(null, "uid3", "location-xf2");
    }

    @Test
    public void testFragmentOnCategoryPageWithInvalidUid() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        verifyFragmentResourceIsNull(null, null, null);
    }

    @Test
    public void testUIDSupportWithURLPathSelector() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/url_path2.html");

        verifyFragment(null, "uid2", "location-xf2", "xf-2-uid",
            "/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content");
    }

    private void verifyFragment(String productSku, String categoryId, String fragmentLocation, String expectedXFName,
        String expectedXFPath) {
        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Assert.assertNotNull(cxf);
        Assert.assertEquals(expectedXFName, cxf.getName());
        Assert.assertEquals(CommerceExperienceFragmentImpl.RESOURCE_TYPE, cxf.getExportedType());
        Assert.assertEquals(expectedXFPath, cxf.getExperienceFragmentResource().getPath());

    }

    private void verifyFragmentResourceIsNull(String productSku, String categoryId, String fragmentLocation) {
        CommerceExperienceFragmentImpl cxf = context.request().adaptTo(CommerceExperienceFragmentImpl.class);
        Assert.assertNotNull(cxf);
        Assert.assertNull(cxf.getExperienceFragmentResource());
    }
}
