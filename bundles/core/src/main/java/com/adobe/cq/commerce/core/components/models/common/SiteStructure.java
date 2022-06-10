/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.models.common;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import com.day.cq.wcm.api.Page;
import com.drew.lang.annotations.Nullable;

/**
 * The {@link SiteStructure} interface provides access to the CIF specific pages of a site, including category pages, product pages the
 * search results page and the landing page.
 * <p>
 * An instance of the {@link SiteStructure} can be obtained by adapting the current {@link org.apache.sling.api.SlingHttpServletRequest} to
 * or a {@link Page} to the interface. When adapted from the {@link org.apache.sling.api.SlingHttpServletRequest} the {@code currentPage}
 * variable from the script bindings is used instantiate the {@link SiteStructure}. If the currentPage is not yet available, the
 * {@link org.apache.sling.api.resource.Resource} of the page is used instead. It is recommended to get the {@link SiteStructure} from the
 * {@link org.apache.sling.api.SlingHttpServletRequest} as the instance is cached for reuse automatically.
 */
@ProviderType
public interface SiteStructure {

    /**
     * This interface describes the entries returned by the site structure when accessing specific pages like product pages, category pages
     * or the search results page. It adds additional information to provide access to the catalog page of a product or category page or the
     * landing page.
     */
    interface Entry {

        /**
         * Returns the catalog page of the {@link SiteStructure.Entry}. This may be null when the relationship is defined on the landing
         * page.
         *
         * @return
         */
        @Nullable
        Page getCatalogPage();

        /**
         * Returns the landing page, which is the first ancestor of the page which has {@link SiteStructure#PN_NAV_ROOT} property set to
         * {@code true}. The landing page is the same for each {@link Entry} returned by a {@link SiteStructure}.
         *
         * @return
         */
        Page getLandingPage();

        Page getPage();
    }

    /**
     * Sling resource type for catalog landing page.
     */
    String RT_CATALOG_PAGE = "core/cif/components/structure/catalogpage/v1/catalogpage";
    String RT_CATALOG_PAGE_V3 = "core/cif/components/structure/catalogpage/v3/catalogpage";

    String PN_NAV_ROOT = "navRoot";

    /**
     * Returns the landing page of the site structure, which is the first ancestor of page the site structure with the
     * {@link SiteStructure#PN_NAV_ROOT} set to true.
     *
     * @return
     */
    @Nullable
    Entry getLandingPage();

    @Nullable
    Entry getSearchResultsPage();

    /**
     * Returns all product pages within a site.
     * <p>
     * Within the site there may be multiple catalog pages and each of them may have a product page.
     *
     * @return
     */
    List<Entry> getProductPages();

    /**
     * Returns all category pages within a site.
     * <p>
     * Within the site there may be multiple catalog pages and each of them may have a category page.
     *
     * @return
     */
    List<Entry> getCategoryPages();

    /**
     * Returns the site navigation entry of the given page. If this is a product page or a category page within a catalog page, the
     * returned {@link SiteStructure.Entry} will contain the reference to that catalog page, if not it will at least contain the page
     * itself and the landing page.
     *
     * @param page
     * @return the {@link SiteStructure.Entry} of the given page, {@code null} if the page is not within the current site structure
     */
    @Nullable
    Entry getEntry(Page page);

    /**
     * Returns {@code true} when the given {@link Page} is a catalog page. Catalog pages can be identified by the specific resource types
     * {@link SiteStructure#RT_CATALOG_PAGE} and {@link SiteStructure#RT_CATALOG_PAGE_V3}.
     *
     * @param page
     * @return
     */
    boolean isCatalogPage(Page page);

    /**
     * Returns {@code true} when the given {@link Page} is a product page. This is the case when the given {@link Page} is equal to
     * or descendant of any product page returned by {@link SiteStructure#getProductPages()}.
     *
     * @param page
     * @return
     */
    boolean isProductPage(Page page);

    /**
     * Returns {@code true} when the given {@link Page} is a category page. This is the case when the given {@link Page} is equal to
     * or descendant of any category page returned by {@link SiteStructure#getCategoryPages()}.Ï
     *
     * @param page
     * @return
     */
    boolean isCategoryPage(Page page);
}
