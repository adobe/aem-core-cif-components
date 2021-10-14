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
import java.util.Optional;
import java.util.function.Function;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;

/**
 * This provider can be used to get an instance of {@link SitemapLinkExternalizer}. It takes into account that the Sites SEO api may not be
 * available at all or in a version that does not contain the SitemapLinkExternalizer yet. In this case the returned
 * {@link SitemapLinkExternalizer} will use the Apache Sling one as fallback.
 */
@Component(service = SitemapLinkExternalizerProvider.class)
public class SitemapLinkExternalizerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapLinkExternalizerProvider.class);

    @Reference
    private org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizerService;

    private SitemapLinkExternalizer externalizer;

    SitemapLinkExternalizer getExternalizer() {
        if (externalizer == null) {
            // try to use the Sites SEO SitemapLinkExternalizer
            try {
                if (externalizerService instanceof com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer) {
                    externalizer = new SeoSitemapLinkExternalizer(
                        (com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer) externalizerService);
                    return externalizer;
                }
            } catch (NoClassDefFoundError ex) {
                LOGGER.debug("Could not load com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer", ex);
            }

            // fallback to sling's SitemapLinkExternalizer
            externalizer = new SlingSitemapLinkExternalizer(externalizerService);
        }

        return externalizer;
    }

    private class SeoSitemapLinkExternalizer implements SitemapLinkExternalizer {

        private final com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer externalizer;

        SeoSitemapLinkExternalizer(com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer externalizer) {
            this.externalizer = externalizer;
        }

        @Override
        public String externalize(ResourceResolver resourceResolver, Map<String, String> params,
            Function<Map<String, String>, String> urlProvider) {
            // directly invoke the url provider and pass the returned path to the seo externalizer
            return externalizer.externalize(resourceResolver, urlProvider.apply(params));
        }
    }

    private class SlingSitemapLinkExternalizer implements SitemapLinkExternalizer {

        private final org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizer;

        SlingSitemapLinkExternalizer(org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizer) {
            this.externalizer = externalizer;
        }

        @Override
        public String externalize(ResourceResolver resourceResolver, Map<String, String> params,
            Function<Map<String, String>, String> urlProvider) {
            // get the page param, resolve and externalize it, then replace the param and pass it to the resource provider
            return Optional.ofNullable(params.get(UrlProvider.PAGE_PARAM))
                .map(resourceResolver::getResource)
                .map(externalizer::externalize)
                .map(externalPath -> {
                    params.put(UrlProvider.PAGE_PARAM, externalPath);
                    return urlProvider.apply(params);
                })
                .orElse(urlProvider.apply(params));
        }
    }
}
