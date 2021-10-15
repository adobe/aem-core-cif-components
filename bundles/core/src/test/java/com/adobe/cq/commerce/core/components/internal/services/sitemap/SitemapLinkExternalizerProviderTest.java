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

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.apache.sling.hamcrest.ResourceMatchers.path;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SitemapLinkExternalizerProviderTest {

    @Rule
    public final AemContext aemContext = new AemContext();
    private final SitemapLinkExternalizerProvider subject = new SitemapLinkExternalizerProvider();

    @Test
    public void testExternalizerWithSlingExternalizer() {
        // given
        Page page = aemContext.create().page("/content/venia/us/en");

        org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizerService = mock(
            org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class);
        when(externalizerService.externalize(argThat(path(page.getPath())))).thenReturn("http://venia.local/us/en");
        aemContext.registerService(org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class, externalizerService);
        aemContext.registerInjectActivateService(subject);

        // when
        SitemapLinkExternalizer externalizer = subject.getExternalizer();
        Map<String, String> params = new UrlProvider.ParamsBuilder().page(page.getPath()).map();

        // then
        externalizer.externalize(aemContext.resourceResolver(), params, map -> {
            // verify the page parameter got externalized
            assertEquals("http://venia.local/us/en", map.get(UrlProvider.PAGE_PARAM));
            return "";
        });
    }

    @Test
    public void testExternalizerFallbackWithSlingExternalizer() {
        // given
        org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizerService = mock(
            org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class);
        aemContext.registerService(org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class, externalizerService);
        aemContext.registerInjectActivateService(subject);

        // when
        SitemapLinkExternalizer externalizer = subject.getExternalizer();
        Map<String, String> params = new UrlProvider.ParamsBuilder().page("/does/not/exist").map();

        // then
        externalizer.externalize(aemContext.resourceResolver(), params, map -> {
            // verify the page parameter got externalized
            assertEquals("/does/not/exist", map.get(UrlProvider.PAGE_PARAM));
            return "";
        });
    }

    @Test
    public void testExternalizerWithSitesSeoExternalizer() {
        // given
        Page page = aemContext.create().page("/content/venia/us/en");

        com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer externalizerService = mock(
            com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer.class);
        when(externalizerService.externalize(aemContext.resourceResolver(), page.getPath())).thenReturn("http://venia.local/us/en");
        aemContext.registerService(org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer.class, externalizerService);
        aemContext.registerInjectActivateService(subject);

        // when
        SitemapLinkExternalizer externalizer = subject.getExternalizer();
        Map<String, String> params = new UrlProvider.ParamsBuilder().page(page.getPath()).map();

        // then
        assertEquals("http://venia.local/us/en",
            externalizer.externalize(aemContext.resourceResolver(), params, map -> map.get(UrlProvider.PAGE_PARAM)));
    }
}
