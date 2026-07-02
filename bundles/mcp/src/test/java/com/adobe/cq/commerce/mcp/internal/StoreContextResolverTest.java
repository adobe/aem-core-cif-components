/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.mcp.internal;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreContextResolverTest {
    @Rule
    public final AemContext context = new AemContext();
    private StoreContextResolver resolver;

    @Before
    public void setup() {
        context.load().json("/context/venia-navroot.json", "/content");
        resolver = new StoreContextResolver();
    }

    private SlingHttpServletRequest requestFor(String pagePath) {
        context.currentResource(pagePath + "/jcr:content");
        SiteStructure ss = mock(SiteStructure.class);
        when(ss.getLandingPage()).thenReturn(context.pageManager().getPage("/content/store"));
        when(ss.getSearchResultsPage()).thenReturn(null);
        context.registerAdapter(Page.class, SiteStructure.class, ss);
        context.registerAdapter(SlingHttpServletRequest.class, MagentoGraphqlClient.class, mock(MagentoGraphqlClient.class));
        return context.request();
    }

    @Test
    public void navRootPageIsRecognised() {
        assertTrue(resolver.isNavRoot(requestFor("/content/store")));
    }

    @Test
    public void nonNavRootPageIsRejected() {
        assertFalse(resolver.isNavRoot(requestFor("/content/store-products")));
    }

    @Test
    public void resolveFallsBackToLandingPageForProductPage() {
        StoreContext ctx = resolver.resolve(requestFor("/content/store"));
        assertEquals("/content/store", ctx.getLandingPage().getPath());
        assertEquals("/content/store", ctx.getProductPage().getPath());
        assertNotNull(ctx.getClient());
    }
}
