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

import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;

/**
 * This interface represents a specific implementation of the {@link GenericUrlFormat} for category urls.
 * <p>
 * Implementations must be registered as OSGI service for this service to replace the configured behaviour of the {@link UrlProvider}. If
 * multiple implementations of the service exist, the one with the highest service ranking will be used. Implementing this service is
 * optional.
 */
@ConsumerType
public interface CategoryUrlFormat extends GenericUrlFormat<CategoryUrlFormat.Params> {

    /**
     * Validates the given parameters are required ones for this format. Avoid checking of the page parameter here
     * Implementations should override this method to provide their own validation
     *
     * @param params the parameters to validate
     * @return boolean
     */
    default boolean validateRequiredParams(CategoryUrlFormat.Params params) {
        return false;
    }

    /**
     * Instances of this class hold the parameters used by implementations of the {@link CategoryUrlFormat}.
     */
    @ProviderType
    final class Params {

        private String page;
        private String uid;
        private String urlKey;
        private String urlPath;

        public Params() {
            super();
        }

        public Params(CategoryInterface category) {
            if (category.getUid() != null) {
                this.uid = category.getUid().toString();
            }
            this.urlKey = category.getUrlKey();
            this.urlPath = category.getUrlPath();
        }

        public Params(Params other) {
            this.page = other.getPage();
            this.uid = other.getUid();
            this.urlKey = other.getUrlKey();
            this.urlPath = other.getUrlPath();
        }

        @Deprecated
        public Params(Map<String, String> parameters) {
            this.page = parameters.get(UrlProvider.PAGE_PARAM);
            this.uid = parameters.get(UrlProvider.UID_PARAM);
            this.urlKey = parameters.get(UrlProvider.URL_KEY_PARAM);
            this.urlPath = parameters.get(UrlProvider.URL_PATH_PARAM);
        }

        public String getPage() {
            return page;
        }

        public void setPage(String page) {
            this.page = page;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getUrlKey() {
            return urlKey;
        }

        public void setUrlKey(String urlKey) {
            this.urlKey = urlKey;
        }

        public String getUrlPath() {
            return urlPath;
        }

        public void setUrlPath(String urlPath) {
            this.urlPath = urlPath;
        }

        @Deprecated
        public Map<String, String> asMap() {
            return new UrlProvider.ParamsBuilder()
                .page(page)
                .urlKey(urlKey)
                .urlPath(urlPath)
                .uid(uid)
                .map();
        }
    }
}
