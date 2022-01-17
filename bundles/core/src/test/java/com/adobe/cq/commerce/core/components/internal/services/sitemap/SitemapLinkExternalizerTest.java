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
package com.adobe.cq.commerce.core.components.internal.services.sitemap;

import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.apache.sling.hamcrest.ResourceMatchers.path;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SitemapLinkExternalizerTest {

    @Rule
    public final AemContext aemContext = TestContext.newAemContext();
    private final SitemapLinkExternalizerProvider subject = new SitemapLinkExternalizerProvider();

    @Test
    public void testExternalizerWithSlingExternalizer() {
        // given
        Page page = aemContext.create().page("/content/venia/us/en");
        aemContext.currentPage(page);

        org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizerService = mock(
            org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class);
        when(externalizerService.externalize(argThat(path(page.getPath())))).thenReturn("http://venia.local/us/en");
        aemContext.registerService(org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class, externalizerService);
        aemContext.registerInjectActivateService(subject);

        // when
        SitemapLinkExternalizer externalizer = subject.getExternalizer(aemContext.resourceResolver());
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage(page.getPath());
        params.setUrlKey("foobar");

        // then
        String canonicalUrl = externalizer.toExternalProductUrl(null, page, params);
        assertEquals("http://venia.local/us/en.html/foobar.html", canonicalUrl);
    }

    @Test
    public void testExternalizerFallbackWithSlingExternalizer() {
        // given
        org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizerService = mock(
            org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class);
        aemContext.registerService(org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class, externalizerService);
        aemContext.registerInjectActivateService(subject);

        // when
        SitemapLinkExternalizer externalizer = subject.getExternalizer(aemContext.resourceResolver());
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/does/not/exist");
        params.setUrlKey("foobar");
        Page page = aemContext.create().page("/does/not/exist");
        aemContext.currentPage(page);

        // then
        String canonicalUrl = externalizer.toExternalProductUrl(aemContext.request(), null, params);
        assertEquals("/does/not/exist.html/foobar.html", canonicalUrl);
    }

    @Test
    public void testExternalizerWithSitesSeoExternalizer() {
        // given
        Page page = aemContext.create().page("/content/venia/us/en");
        aemContext.currentPage(page);

        com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer externalizerService = mock(
            com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer.class);
        when(externalizerService.externalize(aemContext.resourceResolver(), page.getPath() + ".html/foobar.html"))
            .thenReturn("http://venia.local/us/en.html/foobar.html");
        aemContext.registerService(org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class, externalizerService);
        aemContext.registerInjectActivateService(subject);

        // when
        SitemapLinkExternalizer externalizer = subject.getExternalizer(aemContext.resourceResolver());
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage(page.getPath());
        params.setUrlKey("foobar");
        String externalUrl = externalizer.toExternalProductUrl(aemContext.request(), null, params);

        // then
        assertEquals("http://venia.local/us/en.html/foobar.html", externalUrl);
    }
}
