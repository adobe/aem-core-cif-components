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

package com.adobe.cq.commerce.core.components.internal.services;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class UrlProviderImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-page-filter.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    private UrlProviderImpl urlProvider;
    private MockSlingHttpServletRequest request;

    @Before
    public void setup() {
        urlProvider = new UrlProviderImpl();
        urlProvider.activate(new MockUrlProviderConfiguration());

        request = new MockSlingHttpServletRequest(context.resourceResolver());
    }

    @Test
    public void testProductUrl() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new HashMap<>();
        params.put(UrlProvider.URL_KEY_PARAM, "beaumont-summit-kit");

        String url = urlProvider.toProductUrl(request, page, params);
        Assert.assertEquals("/content/product-page.beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithCustomPage() {
        Map<String, String> params = new HashMap<>();
        params.put(UrlProvider.URL_KEY_PARAM, "beaumont-summit-kit");
        params.put(UrlProvider.PAGE_PARAM, "/content/custom-page");

        String url = urlProvider.toProductUrl(request, null, params);
        Assert.assertEquals("/content/custom-page.beaumont-summit-kit.html", url);
    }

    @Test
    public void testProductUrlWithSubpageAndAnchor() {
        Page page = context.currentPage("/content/product-page");
        request.setAttribute(WCMMode.class.getName(), WCMMode.EDIT);

        Map<String, String> params = new HashMap<>();
        params.put(UrlProvider.URL_KEY_PARAM, "productId1");
        params.put(UrlProvider.VARIANT_SKU_PARAM, "variantSku");

        String url = urlProvider.toProductUrl(request, page, params);
        Assert.assertEquals("/content/product-page/sub-page.productId1.html#variantSku", url);
    }
}
