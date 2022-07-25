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
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentQuery;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.XfParams;
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
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommerceExperienceFragmentsRetrieverTest {
    private static final String PAGE = "/content/mysite/page";
    private static final String PRODUCT_PAGE = PAGE + "/product-page";
    private static final String CATEGORY_PAGE = PAGE + "/category-page";
    private static final String RESOURCE_XF1 = "/jcr:content/root/xf-component-1";
    private static final String RESOURCE_XF2 = "/jcr:content/root/xf-component-2";
    private static final String XF_ROOT = "/content/experience-fragments/";
    private static final String SITE_XF_ROOT = XF_ROOT + "mysite/page";

    private Page page;
    private List<Page> experienceFragments;
    private AssociatedContentService associatedContentService;

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content-experiencefragment.json")
        .<AemContext>afterSetUp(context -> {
            context.registerService(LiveRelationshipManager.class, mock(LiveRelationshipManager.class));
            LanguageManager languageManager = context.registerService(LanguageManager.class,
                mock(LanguageManager.class));
            Page rootPage = context.pageManager().getPage(PAGE);
            when(languageManager.getLanguageRoot(any())).thenReturn(rootPage);

            CommerceExperienceFragmentsRetriever cxfRetriver = new CommerceExperienceFragmentsRetrieverImpl();
            Whitebox.setInternalState(cxfRetriver, "languageManager", context.getService(LanguageManager.class));
            associatedContentService = mock(AssociatedContentService.class);
            Whitebox.setInternalState(cxfRetriver, "associatedContentService", associatedContentService);
            context.registerService(CommerceExperienceFragmentsRetriever.class, cxfRetriver);

            AssociatedContentQuery<Page> query = mock(AssociatedContentQuery.class);
            when(query.withLimit(anyLong())).thenReturn(query);
            when(associatedContentService.listProductExperienceFragments(any(), any())).thenReturn(query);
            when(associatedContentService.listCategoryExperienceFragments(any(), any())).thenReturn(query);
            experienceFragments = new ArrayList<>();
            when(query.execute()).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return experienceFragments.iterator();
                }
            });
        }).build();

    private void setup(String pagePath, String resourcePath, String xfPath) {
        page = spy(context.currentPage(PRODUCT_PAGE));
        Resource xfResource = context.resourceResolver().getResource(PRODUCT_PAGE + RESOURCE_XF2);
        context.currentResource(xfResource);
        experienceFragments.clear();
        if (StringUtils.isNotBlank(xfPath)) {
            experienceFragments.add(context.pageManager().getContainingPage(xfPath));
        }
    }

    @Test
    public void testFragmentOnProductPageWithoutLocationProperty() {
        String xfPath = "/content/experience-fragments/mysite/page/xf-1-uid/master/jcr:content";

        setup(PRODUCT_PAGE, RESOURCE_XF1, xfPath);
        List<Resource> xfs = getProductFragments(SITE_XF_ROOT, "sku-xf1", null);

        assertNotNull(xfs);
        assertFalse("Fragments list empty", xfs.isEmpty());

        assertEquals(xfPath, xfs.get(0).getPath());

        verify(associatedContentService).listProductExperienceFragments(same(context.resourceResolver()),
            argThat(new CustomTypeSafeMatcher<XfParams>("") {
                @Override
                protected boolean matchesSafely(XfParams xfParams) {
                    return xfParams.identifiers().contains("sku-xf1") && StringUtils.isBlank(xfParams.location());
                }
            }));
    }

    @Test
    public void testFragmentOnProductPageWithLocationProperty() {
        String xfPath = "/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content";
        setup(PRODUCT_PAGE, RESOURCE_XF2, xfPath);
        List<Resource> xfs = getProductFragments(SITE_XF_ROOT, "sku-xf2", "location-xf2");

        assertNotNull(xfs);
        assertFalse("Fragments list empty", xfs.isEmpty());

        assertEquals(xfPath, xfs.get(0).getPath());

        verify(associatedContentService).listProductExperienceFragments(same(context.resourceResolver()),
            argThat(new CustomTypeSafeMatcher<XfParams>("") {
                @Override
                protected boolean matchesSafely(XfParams xfParams) {
                    return xfParams.identifiers().contains("sku-xf2") && "location-xf2".equals(xfParams.location());
                }
            }));
    }

    @Test
    public void testFragmentOnProductPageWithoutMatchingSkus() {
        setup(PRODUCT_PAGE, RESOURCE_XF2, null);
        List<Resource> xfs = getProductFragments(SITE_XF_ROOT, "sku-xf3", "location-xf2");

        assertNotNull(xfs);
        assertTrue(xfs.isEmpty());
    }

    @Test
    public void testFragmentOnProductPageWithNullSku() {
        setup(CATEGORY_PAGE, RESOURCE_XF2, null);
        List<Resource> xfs = getProductFragments(XF_ROOT, null, "location-xf2");

        assertNotNull(xfs);
        assertTrue(xfs.isEmpty());
    }

    @Test
    public void testFragmentOnCategoryPageWithLocationProperty() {
        String xfPath = "/content/experience-fragments/mysite/page/xf-2-uid/master/jcr:content";
        setup(CATEGORY_PAGE, RESOURCE_XF2, xfPath);
        List<Resource> xfs = getCategoryFragments(SITE_XF_ROOT, "uid2", "location-xf2");

        assertNotNull(xfs);
        assertFalse("Fragments list empty", xfs.isEmpty());

        assertEquals(xfPath, xfs.get(0).getPath());

        verify(associatedContentService).listCategoryExperienceFragments(same(context.resourceResolver()),
            argThat(new CustomTypeSafeMatcher<XfParams>("") {
                @Override
                protected boolean matchesSafely(XfParams xfParams) {
                    return xfParams.identifiers().contains("uid2") && "location-xf2".equals(xfParams.location());
                }
            }));
    }

    @Test
    public void testFragmentOnCategoryPageWithoutMatchingUids() throws IOException {
        setup(CATEGORY_PAGE, RESOURCE_XF2, "");
        List<Resource> xfs = getCategoryFragments(XF_ROOT, "uid3", "location-xf2");

        assertNotNull(xfs);
        assertTrue(xfs.isEmpty());
    }

    @Test
    public void testFragmentOnCategoryPageWithNullUid() {
        setup(CATEGORY_PAGE, RESOURCE_XF2, "");
        List<Resource> xfs = getCategoryFragments(XF_ROOT, null, "location-xf2");

        assertNotNull(xfs);
        assertTrue(xfs.isEmpty());
    }

    private List<Resource> getProductFragments(String xfRootPath, String productSku, String fragmentLocation) {
        mockJcrQueryResult(xfRootPath, productSku, null, fragmentLocation);
        CommerceExperienceFragmentsRetriever cxfRetriever = context
            .getService(CommerceExperienceFragmentsRetriever.class);
        return cxfRetriever.getExperienceFragmentsForProduct(productSku, fragmentLocation, 1, page);
    }

    private List<Resource> getCategoryFragments(String xfRootPath, String categoryUid, String fragmentLocation) {
        mockJcrQueryResult(xfRootPath, null, categoryUid, fragmentLocation);
        CommerceExperienceFragmentsRetriever cxfRetriever = context
            .getService(CommerceExperienceFragmentsRetriever.class);
        return cxfRetriever.getExperienceFragmentsForCategory(categoryUid, fragmentLocation, 1, page);
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
