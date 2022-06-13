/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.utils;

import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * @deprecated use {@link com.adobe.cq.commerce.core.components.models.common.SiteStructure} instead
 */
@Deprecated
public class SiteNavigation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteNavigation.class);
    private static final String PN_CIF_CATEGORY_PAGE = "cq:cifCategoryPage";
    private static final String PN_CIF_PRODUCT_PAGE = "cq:cifProductPage";
    private static final String PN_CIF_SEARCH_RESULTS_PAGE = "cq:cifSearchResultsPage";

    private static final String COMBINED_SKU_SEPARATOR = "#";

    /**
     * Boolean property to mark the navigation root page.
     */
    static final String PN_NAV_ROOT = "navRoot";

    private SlingHttpServletRequest request;

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
            LOGGER.debug("Page property {} not found at {}", pageTypeProperty, page.getPath());
            return null;
        }

        PageManager pageManager = page.getPageManager();
        Page genericPage = pageManager.getPage(genericPagePath);
        if (genericPage == null) {
            LOGGER.debug("No page found at {}", genericPagePath);
            return null;
        }

        return genericPage;
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
     * Checks if the given page is a Launch page, and if yes, returns the production version of the page.
     * If the page is not a Launch page, this method returns the page itself. This allows writing code
     * like<br>
     * <br>
     * <code>page = SiteNavigation.toLaunchProductionPage(page);</code>
     * 
     * @param givenPage The page to be checked.
     * @return The production version of the page if it is a Launch page, or the page itself.
     */
    public static Page toLaunchProductionPage(Page givenPage) {
        return Optional.ofNullable(givenPage)
            .map(page -> page.adaptTo(Resource.class))
            .filter(resource -> LaunchUtils.isLaunchBasedPath(resource.getPath()))
            .map(resource -> LaunchUtils.getTargetResource(resource, null))
            .map(productionResource -> productionResource.adaptTo(Page.class))
            .orElse(givenPage);
    }

    /**
     * Returns true if the <code>currentPage</code> is the product page referenced by the <code>cq:cifProductPage</code>
     * property in the page hierarchy. This method does support that the current page and/or the product page is
     * located inside a Launch.
     * 
     * @param currentPage The page to be checked.
     * @return true if the current page is the product page.
     */
    public static boolean isProductPage(Page currentPage) {
        Page productPage = getProductPage(currentPage);
        if (productPage == null) {
            return false;
        }

        // The product page might be in a Launch, so we first extract the paths of the production versions
        String currentPagePath = currentPage.getPath().substring(currentPage.getPath().lastIndexOf("/content/"));
        String productPagePath = productPage.getPath().substring(productPage.getPath().lastIndexOf("/content/"));

        return currentPagePath.equals(productPagePath) || currentPagePath.startsWith(productPagePath + "/");
    }

    /**
     * Returns true if the <code>currentPage</code> is the category page referenced by the <code>cq:cifCategoryPage</code>
     * property in the page hierarchy. This method does support that the current page and/or the category page is
     * located inside a Launch.
     * 
     * @param currentPage The page to be checked.
     * @return true if the current page is the category page.
     */
    public static boolean isCategoryPage(Page currentPage) {
        Page categoryPage = getCategoryPage(currentPage);
        if (categoryPage == null) {
            return false;
        }

        // The category page might be in a Launch so we first extract the paths of the production versions
        String currentPagePath = currentPage.getPath().substring(currentPage.getPath().lastIndexOf("/content/"));
        String categoryPagePath = categoryPage.getPath().substring(categoryPage.getPath().lastIndexOf("/content/"));

        return currentPagePath.equals(categoryPagePath) || currentPagePath.startsWith(categoryPagePath + "/");
    }
}
