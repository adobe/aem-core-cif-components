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
package com.adobe.cq.commerce.core.components.services;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import com.day.cq.wcm.api.Page;

@ProviderType
public interface SiteNavigation {

    /**
     * Sling resource type for catalog landing page.
     */
    String RT_CATALOG_PAGE = "core/cif/components/structure/catalogpage/v1/catalogpage";
    String RT_CATALOG_PAGE_V3 = "core/cif/components/structure/catalogpage/v3/catalogpage";

    String PN_NAV_ROOT = "navRoot";

    /**
     * Returns {@code true} when the given {@link Page} is a catalog page. Catalog pages can be identified by the specific resource types
     * {@link SiteNavigation#RT_CATALOG_PAGE} and {@link SiteNavigation#RT_CATALOG_PAGE_V3}.
     *
     * @param page
     * @return
     */
    boolean isCatalogPage(Page page);

    /**
     * Returns all product pages within a site.
     * <p>
     * A site is defined by a page that has the {@link SiteNavigation#PN_NAV_ROOT} property set to
     * {@code true}. Within the site there may be multiple catalog pages and each of them may have a product page.
     *
     * @param page
     * @return
     */
    List<Page> getProductPages(Page page);

    /**
     * Returns {@code true} when the given {@link Page} is a product page. This is the case when the given {@link Page} is equal to
     * or descendant of any product page returned by {@link SiteNavigation#getProductPages(Page)}.
     *
     * @param page
     * @return
     */
    boolean isProductPage(Page page);

    /**
     * Returns all category pages within a site.
     * <p>
     * A site is defined by a page that has the {@link SiteNavigation#PN_NAV_ROOT} property set to
     * {@code true}. Within the site there may be multiple catalog pages and each of them may have a category page.
     *
     * @param page
     * @return
     */
    List<Page> getCategoryPages(Page page);

    /**
     * Returns {@code true} when the given {@link Page} is a category page. This is the case when the given {@link Page} is equal to
     * or descendant of any category page returned by {@link SiteNavigation#getCategoryPages(Page)}.Ï
     *
     * @param page
     * @return
     */
    boolean isCategoryPage(Page page);

}
