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

import java.util.Map;

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
        ID
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
     * The <code>id</code> of the category.
     */
    public static final String ID_PARAM = "id";

    /**
     * Use this parameter name to set the <b>page</b> part of the URL. This ensures that implementations of the
     * UrlProvider can easily "find out" if the page part of the URL is being statically set.
     */
    public static final String PAGE_PARAM = "page";

    public String toProductUrl(SlingHttpServletRequest request, Page page, Map<String, String> params);

    public String toCategoryUrl(SlingHttpServletRequest request, Page page, Map<String, String> params);

    public Pair<IdentifierLocation, ProductIdentifierType> getProductIdentifierConfig();

    public Pair<IdentifierLocation, CategoryIdentifierType> getCategoryIdentifierConfig();
}
