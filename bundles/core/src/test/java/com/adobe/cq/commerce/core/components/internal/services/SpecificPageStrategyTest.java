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
package com.adobe.cq.commerce.core.components.internal.services;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.SiteNavigation;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SpecificPageStrategyTest {

    @Rule
    public final AemContext context = newAemContext("/context/jcr-page-filter.json");
    public final SpecificPageStrategy subject = new SpecificPageStrategy();

    private Page navRoot;
    private Page productPage;
    private Page categoryPage;
    private Page anyPage;

    @Before
    public void setup() {
        context.registerInjectActivateService(subject);
        navRoot = context.pageManager().getPage("/content");
        productPage = context.pageManager().getPage("/content/product-page");
        categoryPage = context.pageManager().getPage("/content/category-page");
        anyPage = context.pageManager().getPage("/content/catalog-page");
    }

    private SiteNavigation.Entry newEntry(Page page) {
        return new SiteNavigationImpl.EntryImpl(page, null, navRoot);
    }

    @Test
    public void testDefaultGenerateSpecificPageUrls() {
        assertFalse(subject.isGenerateSpecificPageUrlsEnabled());
    }

    @Test
    public void testSpecificProductPageNotFound() {
        // given
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setSku("unknown");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(productPage), params);

        // then
        assertNull(specificPage);
    }

    @Test
    public void testSpecificProductPageByUrlKey() {
        // given
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setUrlKey("productId1");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(productPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(productPage.getPath() + "/sub-page", specificPage.getPath());
    }

    @Test
    public void testSpecificProductPageBySku() {
        // given
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setSku("productId2");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(productPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(productPage.getPath() + "/sub-page-2", specificPage.getPath());
    }

    @Test
    public void testSpecificProductPageByCategoryPathExact() {
        // given
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setSku("productId999");
        params.getCategoryUrlParams().setUrlPath("women");
        params.getCategoryUrlParams().setUrlKey("women");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(productPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(productPage.getPath() + "/sub-page-3", specificPage.getPath());
    }

    @Test
    public void testSpecificProductPageByCategoryUrlPathDescendant() {
        // given
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setSku("productId999");
        params.getCategoryUrlParams().setUrlPath("women/women-bottoms");
        params.getCategoryUrlParams().setUrlKey("women-bottoms");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(productPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(productPage.getPath() + "/sub-page-3", specificPage.getPath());
    }

    @Test
    public void testSpecificProductPageByCategoryUrlKey() {
        // given
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setSku("productId999");
        params.getCategoryUrlParams().setUrlKey("men-tops");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(productPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(productPage.getPath() + "/sub-page-3", specificPage.getPath());
    }

    @Test
    public void testNestedSpecificProductPage() {
        // given
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setSku("productId1.1");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(productPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(productPage.getPath() + "/sub-page/nested-page", specificPage.getPath());
    }

    @Test
    public void testSpecificCategoryPageNotFound() {
        // given
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUid("unknown");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(productPage), params);

        // then
        assertNull(specificPage);
    }

    @Test
    public void testSpecificCategoryPageByUid() {
        // given
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUid("category-uid-3");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(categoryPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(categoryPage.getPath() + "/sub-page-with-urlpath-array", specificPage.getPath());
    }

    @Test
    public void testNestedSpecificCategory() {
        // given
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUid("categoryId-uid-2.1");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(categoryPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(categoryPage.getPath() + "/sub-page/nested-page", specificPage.getPath());
    }

    @Test
    public void testSpecificCategoryPageByUrlKey() {
        // given
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUrlKey("women-bottoms");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(categoryPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(categoryPage.getPath() + "/sub-page-with-urlpath-array-v2", specificPage.getPath());
    }

    @Test
    public void testSpecificCategoryPageByUrlPathExact() {
        // given
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUrlPath("men/tops");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(categoryPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(categoryPage.getPath() + "/sub-page-with-urlpath", specificPage.getPath());
    }

    @Test
    public void testSpecificCategoryPageByUrlPathDescendant() {
        // given
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUrlPath("women/tops/sweaters");

        // when
        Page specificPage = subject.getSpecificPage(newEntry(categoryPage), params);

        // then
        assertNotNull(specificPage);
        assertEquals(categoryPage.getPath() + "/sub-page-with-urlpath-v2", specificPage.getPath());
    }
}
