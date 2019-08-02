/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.utils;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class SiteNavigation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteNavigation.class);
    private static final String PN_CIF_CATEGORY_PAGE = "cq:cifCategoryPage";
    private static final String PN_CIF_PRODUCT_PAGE = "cq:cifProductPage";
    private static final String PN_CIF_SEARCH_RESULTS_PAGE = "cq:cifSearchResultsPage";

    /**
     * Boolean property to mark the navigation root page.
     */
    static final String PN_NAV_ROOT = "navRoot";

    private SiteNavigation() {}

    /**
     * Retrieves the generic product page based on the current page or current page ancestors
     * using the page path configured via cq:cifProductPage property.
     *
     * @param page the current page
     * @return the generic product template page
     */
    public static Page getProductPage(Page page) {
        return getGenericPage(PN_CIF_PRODUCT_PAGE, page);
    }

    /**
     * Retrieves the generic category page based on a page or page ancestors using the page path configured
     * via cq:cifCategoryPage property.
     *
     * @param page the page for looking up the property
     * @return the generic category template page
     */
    @Nullable
    public static Page getCategoryPage(Page page) {
        return getGenericPage(PN_CIF_CATEGORY_PAGE, page);
    }

    /**
     * Retrieves the generic search page based on a page or page ancestors using the page path configured
     * via cq:cifSearchResultsPage property.
     *
     * @param page the page for looking up the property
     * @return the search results template page
     */
    @Nullable
    public static Page getSearchResultsPage(Page page) {
        return getGenericPage(PN_CIF_SEARCH_RESULTS_PAGE, page);
    }

    /**
     * Retrieves the navigation root related to the specified page.
     * The page and its parents is searched for the navRoot=true property, marking the navigation root.
     *
     * @param page the page
     *
     * @return the navigation root page if found, otherwise {@code null}
     */
    @Nullable
    public static Page getNavigationRootPage(Page page) {
        while (page != null) {
            if (page.getContentResource().getValueMap().get(PN_NAV_ROOT, false)) {
                break;
            }

            page = page.getParent();
        }
        return page;
    }

    /**
     * Retrieves a generic page based on a page or page ancestors using the page path configured
     * via the property of the root page.
     *
     * @param pageTypeProperty The name of the JCR property on the root page that points to the generic page
     * @param page the page for looking up the property
     * @return the generic page
     */
    @Nullable
    private static Page getGenericPage(String pageTypeProperty, Page page) {
        final InheritanceValueMap properties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        String utilityPagePath = properties.getInherited(pageTypeProperty, String.class);
        if (StringUtils.isBlank(utilityPagePath)) {
            LOGGER.warn("Page property {} not found at {}", pageTypeProperty, page.getPath());
            return null;
        }

        PageManager pageManager = page.getPageManager();
        Page categoryPage = pageManager.getPage(utilityPagePath);
        if (categoryPage == null) {
            LOGGER.warn("No page found at {}", utilityPagePath);
            return null;
        }
        return categoryPage;
    }

    /**
     * Builds and returns a product page URL based on the given page path and slug.
     * 
     * @param pagePath The base page path for the URL.
     * @param slug The slug of the product.
     * @return The product page URL.
     */
    public static String toProductUrl(String pagePath, String slug) {
        return String.format("%s.%s.html", pagePath, slug);
    }

}
