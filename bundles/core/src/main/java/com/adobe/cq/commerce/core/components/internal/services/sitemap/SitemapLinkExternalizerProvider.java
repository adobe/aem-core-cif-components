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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;

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
    @Reference
    private UrlProvider urlProvider;

    public SitemapLinkExternalizer getExternalizer(ResourceResolver resourceResolver) {
        // try to use the Sites SEO SitemapLinkExternalizer
        try {
            if (externalizerService instanceof com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer) {
                return new SeoSitemapLinkExternalizer(
                    (com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer) externalizerService, resourceResolver);
            }
        } catch (NoClassDefFoundError ex) {
            LOGGER.debug("Could not load com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer", ex);
        }

        // fallback to sling's SitemapLinkExternalizer
        return new SlingSitemapLinkExternalizer(externalizerService, resourceResolver);
    }

    private class SeoSitemapLinkExternalizer implements SitemapLinkExternalizer {

        private final com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer externalizer;
        private final ResourceResolver resourceResolver;

        SeoSitemapLinkExternalizer(com.adobe.aem.wcm.seo.sitemap.externalizer.SitemapLinkExternalizer externalizer,
                                   ResourceResolver resourceResolver) {
            this.externalizer = externalizer;
            this.resourceResolver = resourceResolver;
        }

        @Override
        public String toExternalProductUrl(SlingHttpServletRequest request, Page page, ProductUrlFormat.Params params) {
            return externalizer.externalize(resourceResolver, urlProvider.toProductUrl(request, page, params));
        }

        @Override
        public String toExternalCategoryUrl(SlingHttpServletRequest request, Page page, CategoryUrlFormat.Params params) {
            return externalizer.externalize(resourceResolver, urlProvider.formatCategoryUrl(request, page, params));
        }
    }

    private class SlingSitemapLinkExternalizer implements SitemapLinkExternalizer {

        private final org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizer;
        private final ResourceResolver resourceResolver;

        SlingSitemapLinkExternalizer(org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer externalizer,
                                     ResourceResolver resourceResolver) {
            this.externalizer = externalizer;
            this.resourceResolver = resourceResolver;
        }

        @Override
        public String toExternalProductUrl(SlingHttpServletRequest request, Page page, ProductUrlFormat.Params params) {
            String url = urlProvider.toProductUrl(request, page, params);
            Resource resolvedResource = resourceResolver.resolve(url);
            String externalPath = externalizer.externalize(resolvedResource);

            if (externalPath != null && url.startsWith(resolvedResource.getPath())) {
                return externalPath + url.substring(resolvedResource.getPath().length());
            } else {
                // the url does not start with the resource path, it may already be
                // externalised?
                return url;
            }
        }

        @Override
        public String toExternalCategoryUrl(SlingHttpServletRequest request, Page page, CategoryUrlFormat.Params params) {
            String url = urlProvider.formatCategoryUrl(request, page, params);
            Resource resolvedResource = resourceResolver.resolve(url);
            String externalPath = externalizer.externalize(resolvedResource);

            if (externalPath != null && url.startsWith(resolvedResource.getPath())) {
                return externalPath + url.substring(resolvedResource.getPath().length());
            } else {
                // the url does not start with the resource path, it may already be
                // externalised?
                return url;
            }
        }
    }
}
