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

package com.adobe.cq.commerce.core.components.services;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;

import com.day.cq.wcm.api.Page;

public interface UrlProvider {

    /**
     * Defines the location of the product or ctegory identifier in the URL.
     */
    public static enum IdentifierLocation {
        SELECTOR, SUFFIX
    }

    /**
     * Defines the product identifier type used in product page urls.
     */
    public static enum ProductIdentifierType {
        URL_KEY, SKU
    }

    /**
     * Defines the category identifier type used in category page urls.
     */
    public static enum CategoryIdentifierType {
        UID, URL_PATH
    }

    /**
     * The <code>url_key</code> parameter of the product or category. In the case of a <code>ConfigurableProduct</code>,
     * this must hold the url_key of the configurable product and the variant url_key must be set with the
     * <code>variant_url_key</code> parameter.
     */
    public static final String URL_KEY_PARAM = "url_key";

    /**
     * The <code>url_path</code> parameter of the product or category.
     */
    public static final String URL_PATH_PARAM = "url_path";

    /**
     * The <code>sku</code> parameter of the product. In the case of a <code>ConfigurableProduct</code>,
     * this must hold the sku of the configurable product and the variant sku must be set with the
     * <code>variant_sku</code> parameter.
     */
    public static final String SKU_PARAM = "sku";

    /**
     * In the case of a <code>ConfigurableProduct</code>, the <code>variant_sku</code> parameter must
     * be set to the sku of the currently selected/chosen variant.
     */
    public static final String VARIANT_SKU_PARAM = "variant_sku";

    /**
     * In the case of a <code>ConfigurableProduct</code>, the <code>variant_url_key</code> parameter must
     * be set to the url_key of the currently selected/chosen variant.
     */
    public static final String VARIANT_URL_KEY_PARAM = "variant_url_key";

    /**
     * The <code>uid</code> of the category.
     */
    public static final String UID_PARAM = "uid";

    /**
     * Use this parameter name to set the <b>page</b> part of the URL. This ensures that implementations of the
     * UrlProvider can easily "find out" if the page part of the URL is being statically set.
     */
    public static final String PAGE_PARAM = "page";

    /**
     * Returns the product page URL.
     * 
     * @param request The current Sling HTTP request.
     * @param page The target page, if any. This parameter can be null if the URL template does not use the <code>${page}</code> parameter.
     * @param params The parameters used in the URL template.
     * @return The product URL.
     */
    public String toProductUrl(SlingHttpServletRequest request, @Nullable Page page, Map<String, String> params);

    /**
     * Returns the category page URL.
     * 
     * @param request The current Sling HTTP request.
     * @param page The target page, if any. This parameter can be null if the URL template does not use the <code>${page}</code> parameter.
     * @param params The parameters used in the URL template.
     * @return The category URL.
     */
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, Map<String, String> params);

    /**
     * Returns the type and value of the product identifier used in the given Sling HTTP request.
     * 
     * @param request The current Sling HTTP request.
     * @return The type and value of the product identifier.
     */
    public Pair<ProductIdentifierType, String> getProductIdentifier(SlingHttpServletRequest request);

    /**
     * Returns the type and value of the category identifier used in the given Sling HTTP request.
     * 
     * @param request The current Sling HTTP request.
     * @return The type and value of the category identifier.
     */
    public Pair<CategoryIdentifierType, String> getCategoryIdentifier(SlingHttpServletRequest request);

    /**
     * A helper class used to easily build parameters for the URL templates.
     */
    public static class ParamsBuilder {

        private Map<String, String> params = new HashMap<>();

        public ParamsBuilder urlPath(String urlPath) {
            params.put(URL_PATH_PARAM, urlPath);
            return this;
        }

        /**
         * Sets the <code>url_key</code> of the product.
         * In the case of a <code>ConfigurableProduct</code>, this sets the <code>url_key</code> of the configurable
         * product and the variant <code>url_key</code> must be set with {@link #variantUrlKey(String)}.
         * 
         * @param urlKey The <code>url_key</code> of the product or category.
         * @return This ParamsBuilder.
         */
        public ParamsBuilder urlKey(String urlKey) {
            params.put(URL_KEY_PARAM, urlKey);
            return this;
        }

        /**
         * In the case of a <code>ConfigurableProduct</code>, the <code>variantUrlKey</code> parameter must
         * be set to the <code>url_key</code> of the currently selected/chosen variant.
         * 
         * @param variantUrlKey The <code>url_key</code> of the selected/chosen variant.
         * @return This ParamsBuilder.
         */
        public ParamsBuilder variantUrlKey(String variantUrlKey) {
            params.put(VARIANT_URL_KEY_PARAM, variantUrlKey);
            return this;
        }

        /**
         * Sets the <code>sku</code> of the product.
         * In the case of a <code>ConfigurableProduct</code>, this must hold the <code>sku</code> of the configurable
         * product and the variant <code>sku</code> must be set with {@link #variantSku(String)}.
         * 
         * @param sku The <code>sku</code> of the product.
         * @return This ParamsBuilder.
         */
        public ParamsBuilder sku(String sku) {
            params.put(SKU_PARAM, sku);
            return this;
        }

        /**
         * In the case of a <code>ConfigurableProduct</code>, the <code>variantSku</code> parameter must
         * be set to the <code>sku</code> of the currently selected/chosen variant.
         * 
         * @param variantSku The <code>sku</code> of the selected/chosen variant.
         * @return This ParamsBuilder.
         */
        public ParamsBuilder variantSku(String variantSku) {
            params.put(VARIANT_SKU_PARAM, variantSku);
            return this;
        }

        /**
         * Sets the <code>uid</code> of the category.
         * 
         * @param uid The <code>uid</code> of the category.
         * @return This ParamsBuilder.
         */
        public ParamsBuilder uid(String uid) {
            params.put(UID_PARAM, uid);
            return this;
        }

        /**
         * Can be used to statically set the <code>page</code> parameter of the URL.
         * 
         * @param page The page that will be used to build the URL.
         * @return This ParamsBuilder.
         */
        public ParamsBuilder page(String page) {
            params.put(PAGE_PARAM, page);
            return this;
        }

        /**
         * @return The map of parameters.
         */
        public Map<String, String> map() {
            return params;
        }
    }
}
