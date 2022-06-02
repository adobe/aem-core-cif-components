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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.sling.api.resource.Resource;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Version;

import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import com.adobe.granite.resourcestatus.ResourceStatus;
import com.day.cq.wcm.commons.status.EditorResourceStatus;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogPageResourceStatusProviderTest {

    private final CatalogPageResourceStatusProvider subject = new CatalogPageResourceStatusProvider();

    @Rule
    public final AemContext aemContext = newAemContext("/context/jcr-content-pagestatus.json");

    @Test
    public void testReturnsNoStatusForNonCatalogPages() {
        aemContext.registerInjectActivateService(subject);

        Resource productPage = aemContext.resourceResolver().getResource("/content/brand/content-page");
        List<ResourceStatus> statuses = subject.getStatuses(productPage);

        assertNotNull(statuses);
        assertEquals(0, statuses.size());
    }

    @Test
    public void testReturnsStatusForProductPage() {
        aemContext.registerInjectActivateService(subject);

        Resource productPage = aemContext.resourceResolver().getResource("/content/brand/product-page");
        List<ResourceStatus> statuses = subject.getStatuses(productPage);

        assertNotNull(statuses);
        assertEquals(1, statuses.size());

        ResourceStatus status = statuses.get(0);
        assertEquals("Product Page", status.getData().get("title"));
        assertEquals(EditorResourceStatus.Variant.WARNING.toString().toLowerCase(Locale.ROOT), status.getData().get("variant"));
        // no actions without addon version information available
        assertNull(status.getData().get("actionIds"));
    }

    @Test
    public void testReturnsStatusForSpecificProductPage() {
        aemContext.registerInjectActivateService(subject);

        Resource productPage = aemContext.resourceResolver().getResource("/content/brand/product-page/product-specific-page");
        List<ResourceStatus> statuses = subject.getStatuses(productPage);

        assertNotNull(statuses);
        assertEquals(1, statuses.size());

        ResourceStatus status = statuses.get(0);
        assertEquals("Specific Product Page", status.getData().get("title"));
    }

    @Test
    public void testReturnsStatusForCategoryPage() {
        aemContext.registerInjectActivateService(subject);

        Resource categoryPage = aemContext.resourceResolver().getResource("/content/brand/category-page");
        List<ResourceStatus> statuses = subject.getStatuses(categoryPage);

        assertNotNull(statuses);
        assertEquals(1, statuses.size());

        ResourceStatus status = statuses.get(0);
        assertEquals("Category Page", status.getData().get("title"));
        assertEquals(EditorResourceStatus.Variant.WARNING.toString().toLowerCase(Locale.ROOT), status.getData().get("variant"));
        // no actions without addon version information available
        assertNull(status.getData().get("actionIds"));
    }

    @Test
    public void testContainsOpenTemplatePageActionForAddOnNewerThen202202241() {
        ProductInfo info = mock(ProductInfo.class);
        when(info.getVersion()).thenReturn(new Version("2022.05.31.1"));
        aemContext.registerService(ProductInfoProvider.class, () -> info, "name", "cif");
        aemContext.registerInjectActivateService(subject);

        Resource productPage = aemContext.resourceResolver().getResource("/content/brand/product-page");
        List<ResourceStatus> statuses = subject.getStatuses(productPage);

        assertNotNull(statuses);
        assertEquals(1, statuses.size());

        ResourceStatus status = statuses.get(0);
        List<String> actionIds = Arrays.asList((String[]) status.getData().get("actionIds"));
        assertThat(actionIds, Matchers.hasItem("open-template-page"));
        assertEquals("/content/brand/product-page", status.getData().get("template-page-path"));
    }

    @Test
    public void testDoesNotContainActionsForOlderAddOnVersions() {
        ProductInfo info = mock(ProductInfo.class);
        when(info.getVersion()).thenReturn(new Version("2022.04.28.0"));
        aemContext.registerService(ProductInfoProvider.class, () -> info, "name", "cif");
        aemContext.registerInjectActivateService(subject);

        Resource productPage = aemContext.resourceResolver().getResource("/content/brand/product-page");
        List<ResourceStatus> statuses = subject.getStatuses(productPage);

        assertNotNull(statuses);
        assertEquals(1, statuses.size());

        ResourceStatus status = statuses.get(0);
        assertNull(status.getData().get("actionIds"));
    }

}
