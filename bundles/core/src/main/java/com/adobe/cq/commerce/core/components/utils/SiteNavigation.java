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

import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;

public class SiteNavigation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteNavigation.class);
    private static final String PN_CIF_CATEGORY_PAGE = "cq:cifCategoryPage";
    private static final String PN_CIF_PRODUCT_PAGE = "cq:cifProductPage";
    private static final String PN_CIF_SEARCH_RESULTS_PAGE = "cq:cifSearchResultsPage";
    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";

    private static final String COMBINED_SKU_SEPARATOR = "#";

    /**
     * Boolean property to mark the navigation root page.
     */
    static final String PN_NAV_ROOT = "navRoot";

    private SlingHttpServletRequest request;

    /**
     * Based on the request, the SiteNavigation instance might look for specific pages when
     * returning product and category URLs.
     * 
     * @param request The current Sling HTTP request.
     */
    public SiteNavigation(SlingHttpServletRequest request) {
        this.request = request;
    }

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
        page = toLaunchProductionPage(page);

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
    protected static Page getGenericPage(String pageTypeProperty, Page page) {

        // We first lookup the property from the current page up the hierarchy
        // If the page is in a Launch, the property can be found if the Launch includes the landing-page
        InheritanceValueMap properties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        String genericPagePath = properties.getInherited(pageTypeProperty, String.class);

        // If not found and the page is in an AEM Launch
        if (StringUtils.isBlank(genericPagePath) && LaunchUtils.isLaunchBasedPath(page.getPath())) {

            // We lookup the property from the production page up the hierarchy
            Page productionPage = toLaunchProductionPage(page);
            if (productionPage != page) {
                properties = new HierarchyNodeInheritanceValueMap(productionPage.getContentResource());
                genericPagePath = properties.getInherited(pageTypeProperty, String.class);

                // If the property is found, we check if the page exists in the Launch
                // --> useful if the Launch does not contain the landing-page but does contain the (product|category|search) pages
                if (genericPagePath != null) {
                    Resource launchResource = LaunchUtils.getLaunchResource(page.getContentResource());
                    String genericPagePathInLaunch = launchResource.getPath() + genericPagePath;
                    if (page.getPageManager().getPage(genericPagePathInLaunch) != null) {
                        genericPagePath = genericPagePathInLaunch;
                    }
                }
            }
        }

        if (StringUtils.isBlank(genericPagePath)) {
            LOGGER.warn("Page property {} not found at {}", pageTypeProperty, page.getPath());
            return null;
        }

        PageManager pageManager = page.getPageManager();
        Page genericPage = pageManager.getPage(genericPagePath);
        if (genericPage == null) {
            LOGGER.warn("No page found at {}", genericPagePath);
            return null;
        }

        return genericPage;
    }

    /**
     * Builds and returns a product page URL based on the given page path and slug.
     * 
     * @param pagePath The base page path for the URL.
     * @param slug The slug of the product.
     * @return The product page URL.
     * @deprecated Use {@link UrlProvider#toProductUrl(SlingHttpServletRequest, Page, Map)} instead.
     */
    @Deprecated
    public static String toProductUrl(String pagePath, String slug) {
        return toProductUrl(pagePath, slug, null);
    }

    /**
     * Builds and returns a product page URL based on the given page path, slug, and variant sku.
     * 
     * @param pagePath The base page path for the URL.
     * @param slug The slug of the product.
     * @param variantSku An optional sku of the variant that will be "selected" on the product page, can be null.
     * @return The product page URL.
     * @deprecated Use {@link UrlProvider#toProductUrl(SlingHttpServletRequest, Page, Map)} instead.
     */
    @Deprecated
    public static String toProductUrl(String pagePath, String slug, String variantSku) {
        if (StringUtils.isNotBlank(variantSku)) {
            return String.format("%s.%s.html%s%s", pagePath, slug, COMBINED_SKU_SEPARATOR, variantSku);
        } else {
            return String.format("%s.%s.html", pagePath, slug);
        }
    }

    /**
     * Returns the base product sku and variant sku of the given <code>combinedSku</code>.
     * The base product sku is returned in the <code>left</code> element of the pair, while
     * the variant product sku is returned in the <code>right</code> element of the pair.
     * 
     * If the <code>combinedSku</code> refers to a base product without any selected variant,
     * the <code>right</code> element of the pair will be null.
     * 
     * @param combinedSku The combined sku, typically selected and set via the product picker.
     * @return The pair of skus.
     */
    public static Pair<String, String> toProductSkus(String combinedSku) {
        String baseSku = StringUtils.substringBefore(combinedSku, COMBINED_SKU_SEPARATOR);
        String variantSku = StringUtils.substringAfter(combinedSku, COMBINED_SKU_SEPARATOR);
        return Pair.of(baseSku, variantSku.isEmpty() ? null : variantSku);
    }

    /**
     * Builds and returns a category or product page URL based on the given page and slug.
     * This method might return the URL of a specific subpage configured for that particular page.
     * 
     * @param page The page used to build the URL.
     * @param slug The slug of the product or the category.
     * @return The page URL.
     * @deprecated Use {@link UrlProvider} instead.
     */
    @Deprecated
    public String toPageUrl(Page page, String slug) {
        return toProductUrl(page, slug, null);
    }

    /**
     * Builds and returns a product page URL based on the given page, slug, and variant sku.
     * This method might return the URL of a specific subpage configured for that particular page.
     * 
     * @param page The page used to build the URL.
     * @param slug The slug of the product.
     * @param variantSku An optional sku of the variant that will be "selected" on the product page, can be null.
     * @return The product page URL.
     * @deprecated Use {@link UrlProvider#toProductUrl(SlingHttpServletRequest, Page, Map)}
     */
    @Deprecated
    public String toProductUrl(Page page, String slug, String variantSku) {
        Resource pageResource = page.adaptTo(Resource.class);
        boolean deepLink = !WCMMode.DISABLED.equals(WCMMode.fromRequest(request));
        if (deepLink) {
            Resource subPageResource = UrlProviderImpl.toSpecificPage(pageResource, slug);
            if (subPageResource != null) {
                pageResource = subPageResource;
            }
        }

        if (StringUtils.isNotBlank(variantSku)) {
            return String.format("%s.%s.html%s%s", pageResource.getPath(), slug, COMBINED_SKU_SEPARATOR, variantSku);
        } else {
            return String.format("%s.%s.html", pageResource.getPath(), slug);
        }
    }

    /**
     * Checks if the given page is a Launch page, and if yes, returns the production version of the page.
     * If the page is not a Launch page, this method returns the page itself. This allows writing code
     * like<br>
     * <br>
     * <code>page = SiteNavigation.toLaunchProductionPage(page);</code>
     * 
     * @param page The page to be checked.
     * @return The production version of the page if it is a Launch page, or the page itself.
     */
    public static Page toLaunchProductionPage(Page page) {
        if (page == null || page.getPath() == null) {
            return page;
        }

        PageManager pageManager = page.getPageManager();
        if (pageManager != null && LaunchUtils.isLaunchBasedPath(page.getPath())) {
            Resource targetResource = LaunchUtils.getTargetResource(page.adaptTo(Resource.class), null);
            Page targetPage = pageManager.getPage(targetResource.getPath());
            page = targetPage != null ? targetPage : page;
        }
        return page;
    }
}
