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
import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.day.cq.wcm.api.Page;
import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

/**
 * An instance of this interface is provided by the {@link SitemapLinkExternalizerProvider} and implements a compatibility layer for either
 * Sling's SitemapLinkExternalizer or, if available at runtime the Sites SEO's SitemapLinkExternalizer. The latter provides an advanced
 * interface that allows to externalize a path directly.
 */
@ProviderType
public interface SitemapLinkExternalizer {

    /**
     * This method returns an external, canonical url for the given {@link ProductUrlFormat.Params}. It uses the
     * {@link com.adobe.cq.commerce.core.components.services.urls.UrlProvider} internally.
     *
     * @param request
     * @param page
     * @param params
     * @return
     */
    @NotNull
    String toExternalProductUrl(@Nullable SlingHttpServletRequest request, @Nullable Page page, ProductUrlFormat.Params params);

    /**
     * This method returns an external, canonical url for the given {@link CategoryUrlFormat.Params}. It uses the
     * {@link com.adobe.cq.commerce.core.components.services.urls.UrlProvider} internally.
     *
     * @param request
     * @param page
     * @param params
     * @return
     */
    @NotNull
    String toExternalCategoryUrl(@Nullable SlingHttpServletRequest request, @Nullable Page page, CategoryUrlFormat.Params params);
}
