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
package com.adobe.cq.commerce.core.components.services.urls;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;

/**
 * This interface represents a specific implementation of the {@link GenericUrlFormat} for product urls.
 * <p>
 * Implementations must be registered as OSGI service for this service to replace the configured behaviour of the {@link UrlProvider}. If
 * multiple implementations of the service exist, the one with the highest service ranking will be used. Implementing this service is
 * optional.
 */
@ConsumerType
public interface ProductUrlFormat extends GenericUrlFormat<ProductUrlFormat.Params> {

    /**
     * Instances of this class hold the parameters used by implementations of the {@link ProductUrlFormat}.
     */
    @ProviderType
    final class Params {

        private String page;
        private String sku;
        private String variantSku;
        private String urlKey;
        private String variantUrlKey;
        private String urlPath;
        private List<String> urlRewrites = Collections.emptyList();
        private CategoryUrlFormat.Params categoryUrlParams;

        public Params() {
            super();
        }

        public Params(ProductInterface product) {
            this.sku = product.getSku();
            this.urlKey = product.getUrlKey();
            this.urlPath = product.getUrlPath();
            this.urlRewrites = convertUrlRewrites(product.getUrlRewrites());
        }

        public Params(Params other) {
            this.page = other.getPage();
            this.sku = other.getSku();
            this.variantSku = other.getVariantSku();
            this.urlKey = other.getUrlKey();
            this.variantUrlKey = other.getVariantUrlKey();
            this.urlPath = other.getUrlPath();
            this.urlRewrites = other.getUrlRewrites();
        }

        @Deprecated
        public Params(Map<String, String> parameters) {
            this.page = parameters.get(UrlProvider.PAGE_PARAM);
            this.sku = parameters.get(UrlProvider.SKU_PARAM);
            this.variantSku = parameters.get(UrlProvider.VARIANT_SKU_PARAM);
            this.urlKey = parameters.get(UrlProvider.URL_KEY_PARAM);
            this.variantUrlKey = parameters.get(UrlProvider.VARIANT_URL_KEY_PARAM);
            this.urlPath = parameters.get(UrlProvider.URL_PATH_PARAM);
        }

        public String getPage() {
            return page;
        }

        public void setPage(String page) {
            this.page = page;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getVariantSku() {
            return variantSku;
        }

        public void setVariantSku(String variantSku) {
            this.variantSku = variantSku;
        }

        public String getUrlKey() {
            return urlKey;
        }

        public void setUrlKey(String urlKey) {
            this.urlKey = urlKey;
        }

        @Deprecated
        public String getVariantUrlKey() {
            return variantUrlKey;
        }

        @Deprecated
        public void setVariantUrlKey(String variantUrlKey) {
            this.variantUrlKey = variantUrlKey;
        }

        @Deprecated
        public String getUrlPath() {
            return urlPath;
        }

        @Deprecated
        public void setUrlPath(String urlPath) {
            this.urlPath = urlPath;
        }

        public List<String> getUrlRewrites() {
            return Collections.unmodifiableList(urlRewrites);
        }

        public void setUrlRewrites(List<UrlRewrite> urlRewrites) {
            this.urlRewrites = convertUrlRewrites(urlRewrites);
        }

        /**
         * The returned object may contain parameters of the category the product represented by the {@link ProductUrlFormat.Params}
         * belongs to. This is in particular the case when the product url encodes category identifiers.
         *
         * @return
         */
        public CategoryUrlFormat.Params getCategoryUrlParams() {
            if (categoryUrlParams == null) {
                categoryUrlParams = new CategoryUrlFormat.Params();
            }
            return categoryUrlParams;
        }

        @Deprecated
        public Map<String, String> asMap() {
            return new UrlProvider.ParamsBuilder()
                .page(page)
                .sku(sku)
                .variantSku(variantSku)
                .urlKey(urlKey)
                .variantUrlKey(variantUrlKey)
                .urlPath(urlPath)
                .map();
        }

        /**
         * Flattens the list {@link UrlRewrite} to a list of Strings, also removing the extension if any.
         * <p>
         * Converts
         *
         * <pre>
         *  {@code
         * [{ url: "bar.html" }, {url: "foo/bar.html" }]
         * </pre>
         * <p>
         * to
         *
         * <pre>
         *  {@code
         * ["bar", "foo/bar"]
         * </pre>
         *
         * @param urlRewrites
         * 
         * @return
         */
        private static List<String> convertUrlRewrites(List<UrlRewrite> urlRewrites) {
            return Optional.ofNullable(urlRewrites)
                .map(List::stream)
                .map(stream -> stream
                    .map(UrlRewrite::getUrl)
                    .map(url -> StringUtils.removeEnd(url, ".html"))
                    .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
        }
    }
}
