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

package com.adobe.cq.commerce.core.components.internal.models.v1.navigation;

import com.adobe.cq.commerce.core.components.models.navigation.NavigationItem;

/**
 * Base class for {@code NavigationItem} implementations.
 */
public abstract class AbstractNavigationItem implements NavigationItem {
    protected AbstractNavigationItem parent;
    protected String title;
    protected String url;
    protected boolean active;

    public AbstractNavigationItem(AbstractNavigationItem parent, String title, String url, boolean active) {
        this.parent = parent;
        this.active = active;
        this.url = url;
        this.title = title;
    }

    AbstractNavigationItem getParent() {
        return parent;
    }

    public String getTitle() {
        return title;
    }

    public String getURL() {
        return url;
    }

    public boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }
}
