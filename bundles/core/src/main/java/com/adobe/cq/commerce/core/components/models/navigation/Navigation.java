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

package com.adobe.cq.commerce.core.components.models.navigation;

import java.util.List;

/**
 * Sling model interface to represent a navigation.
 */
public interface Navigation {
    /**
     * Sling resource type for catalog landing page.
     */
    String RT_CATALOG_PAGE = "core/cif/components/structure/catalogpage/v1/catalogpage";

    /**
     * Boolean property for adding to navigation the main categories of the catalog instead of the catalog
     * root page.
     * It's set on the catalog root page.
     */
    String PN_SHOW_MAIN_CATEGORIES = "showMainCategories";

    /**
     * Returns the navigation items to be rendered by the navigation component.
     */
    List<NavigationItem> getItems();

    /**
     * Returns the identifier of this navigation.
     */
    String getId();

    /**
     * Returns the identifier of the parent navigation or null for the root navigation.
     */
    String getParentId();
}
