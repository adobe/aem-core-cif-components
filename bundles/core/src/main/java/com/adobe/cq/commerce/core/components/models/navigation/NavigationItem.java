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

import org.jetbrains.annotations.Nullable;

import com.adobe.cq.wcm.core.components.commons.link.Link;

/**
 * Simple data model for a navigation item of the navigation component.
 */
public interface NavigationItem {

    /**
     * @return The item title to be displayed in the navigation.
     */
    String getTitle();

    /**
     * @return The URL for the item.
     */
    String getURL();

    /**
     * @return True if the current page is referred to by this navigation item.
     */
    boolean isActive();

    /**
     * @return The navigation items to be rendered by the navigation component.
     */
    List<NavigationItem> getItems();

    /**
     * Returns the link for this navigation item.
     * <p>
     * For page-based navigation items, this returns the link to the underlying page,
     * including any link attributes such as target. For category-based navigation items,
     * this may return {@code null}.
     *
     * @return the {@link Link} object for this navigation item, or {@code null} if not available
     * @since 2.7.0
     */
    @Nullable
    default Link getLink() {
        return null;
    }
}
