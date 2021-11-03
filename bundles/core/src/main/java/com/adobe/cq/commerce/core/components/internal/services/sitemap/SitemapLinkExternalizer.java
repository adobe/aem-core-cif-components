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
import java.util.function.Function;

import org.apache.sling.api.resource.ResourceResolver;

import com.drew.lang.annotations.NotNull;

/**
 * An instance of this interface is provided by the {@link SitemapLinkExternalizerProvider} and implements a compatibility layer for either
 * Sling's SitemapLinkExternalizer or, if available at runtime the Sites SEO's SitemapLinkExternalizer. The latter provides an advanced
 * interface that allows to externalize a path directly.
 */
public interface SitemapLinkExternalizer {

    /**
     * Externalizes the url returned by the given urlProvider function. This can either be done by externalizing the page parameter in the
     * params map and passing the modified params map to the urlProvider function, or by passing the original params map to the url provider
     * function and externalizing the result it returns.
     *
     * @param resourceResolver
     * @param params
     * @param urlProvider
     * @return
     */
    @NotNull
    String externalize(ResourceResolver resourceResolver, Map<String, String> params,
        Function<Map<String, String>, String> urlProvider);
}
