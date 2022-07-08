/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services.experiencefragments;

import java.io.IOException;
import java.util.List;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class CommerceExperienceFragmentsRetrieverTest {
    private static final String PAGE = "/content/mysite/page";
    private static final String PRODUCT_PAGE = PAGE + "/product-page";
    private static final String CATEGORY_PAGE = PAGE + "/category-page";
    private static final String RESOURCE_XF1 = "/jcr:content/root/xf-component-1";
    private static final String RESOURCE_XF2 = "/jcr:content/root/xf-component-2";
    private static final String XF_ROOT = "/content/experience-fragments/";
    private static final String SITE_XF_ROOT = XF_ROOT + "mysite/page";

    private Page page;

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content-experiencefragment.json")
        .<AemContext>afterSetUp(context -> {
            context.registerService(LiveRelationshipManager.class, mock(LiveRelationshipManager.class));
            LanguageManager languageManager = context.registerService(LanguageManager.class,
                mock(LanguageManager.class));
            Page rootPage = context.pageManager().getPage(PAGE);
            Mockito.when(languageManager.getLanguageRoot(any())).thenReturn(rootPage);

            CommerceExperienceFragmentsRetriever cxfRetriver = new CommerceExperienceFragmentsRetrieverImpl();
            Whitebox.setInternalState(cxfRetriver, "languageManager", context.getService(LanguageManager.class));
            context.registerService(CommerceExperienceFragmentsRetriever.class, cxfRetriver);
        })
        .build();

    private void setup(String pagePath, String resourcePath) {
        page = spy(context.currentPage(PRODUCT_PAGE));
        Resource xfResource = context.resourceResolver().getResource(PRODUCT_PAGE + RESOURCE_XF2);
        context.currentResource(xfResource);
    }

    @Test
    public void testFragmentOnProductPageWithoutLocationProperty() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF1);
        List<Resource> xfs = getProductFragments(SITE_XF_ROOT, "sku-xf1", null);

        assertNotNull(xfs);
        assertFalse("Fragments list empty", xfs.isEmpty());
        assertEquals("/content/experience-fragments/mysite/page/xf-1-uid/master/jcr:content", xfs.get(0).getPath());
    }

    @Test
    public void testFragmentOnProductPageWithLocationProperty() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF2);
        List<Resource> xfs = getProductFragments(SITE_XF_ROOT, "sku-xf2", "location-xf2");

        assertNotNull(xfs);
        assertFalse("Fragments list empty", xfs.isEmpty());
        assertEquals("/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content", xfs.get(0).getPath());
    }

    @Test
    public void testFragmentOnProductPageWithoutMatchingSkus() throws IOException {
        setup(PRODUCT_PAGE, RESOURCE_XF2);
        List<Resource> xfs = getProductFragments(SITE_XF_ROOT, "sku-xf3", "location-xf2");

        assertNotNull(xfs);
        assertTrue(xfs.isEmpty());
    }

    @Test
    public void testFragmentOnProductPageWithNullSku() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);
        List<Resource> xfs = getProductFragments(XF_ROOT, null, "location-xf2");

        assertNotNull(xfs);
        assertTrue(xfs.isEmpty());
    }

    @Test
    public void testFragmentOnCategoryPageWithLocationProperty() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);
        List<Resource> xfs = getCategoryFragments(SITE_XF_ROOT, "uid2", "location-xf2");

        assertNotNull(xfs);
        assertFalse("Fragments list empty", xfs.isEmpty());
        assertEquals("/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content", xfs.get(0).getPath());
    }

    @Test
    public void testFragmentOnCategoryPageWithoutMatchingUids() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);
        List<Resource> xfs = getCategoryFragments(XF_ROOT, "uid3", "location-xf2");

        assertNotNull(xfs);
        assertTrue(xfs.isEmpty());
    }

    @Test
    public void testFragmentOnCategoryPageWithNullUid() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2);
        List<Resource> xfs = getCategoryFragments(XF_ROOT, null, "location-xf2");

        assertNotNull(xfs);
        assertTrue(xfs.isEmpty());
    }

    private List<Resource> getProductFragments(String xfRootPath, String productSku, String fragmentLocation) {
        mockJcrQueryResult(xfRootPath, productSku, null, fragmentLocation);
        CommerceExperienceFragmentsRetriever cxfRetriever = context
            .getService(CommerceExperienceFragmentsRetriever.class);
        return cxfRetriever.getExperienceFragmentsForProduct(productSku, fragmentLocation, page);
    }

    private List<Resource> getCategoryFragments(String xfRootPath, String categoryUid, String fragmentLocation) {
        mockJcrQueryResult(xfRootPath, null, categoryUid, fragmentLocation);
        CommerceExperienceFragmentsRetriever cxfRetriever = context
            .getService(CommerceExperienceFragmentsRetriever.class);
        return cxfRetriever.getExperienceFragmentsForCategory(categoryUid, fragmentLocation, page);
    }

    private XFMockQueryResultHandler mockJcrQueryResult(String xfRootPath, String productSku, String categoryId,
        String fragmentLocation) {
        Resource pageResource = context.resourceResolver().getResource(xfRootPath);
        Session session = context.resourceResolver().adaptTo(Session.class);
        XFMockQueryResultHandler queryHandler = new XFMockQueryResultHandler(pageResource, productSku, categoryId,
            fragmentLocation);
        MockJcr.addQueryResultHandler(session, queryHandler);
        return queryHandler;
    }
}
