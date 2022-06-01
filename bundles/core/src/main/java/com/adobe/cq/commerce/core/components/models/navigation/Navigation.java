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
package com.adobe.cq.commerce.core.components.models.navigation;

import java.util.List;

import com.adobe.cq.commerce.core.components.services.SiteNavigation;

/**
 * Sling model interface to represent a navigation.
 */
public interface Navigation {

    /**
     * Sling resource type for catalog landing page.
     */
    String RT_CATALOG_PAGE = SiteNavigation.RT_CATALOG_PAGE;
    String RT_CATALOG_PAGE_V3 = SiteNavigation.RT_CATALOG_PAGE_V3;

    /**
     * Boolean property for adding to navigation the main categories of the catalog instead of the catalog
     * root page.
     * It's set on the catalog root page.
     */
    String PN_SHOW_MAIN_CATEGORIES = "showMainCategories";

    /**
     * @return The navigation items to be rendered by the navigation component.
     */
    List<NavigationItem> getItems();

    /**
     * @return The identifier of this navigation.
     */
    String getId();

    /**
     * @return The identifier of the parent navigation or null for the root navigation.
     */
    String getParentId();
}
