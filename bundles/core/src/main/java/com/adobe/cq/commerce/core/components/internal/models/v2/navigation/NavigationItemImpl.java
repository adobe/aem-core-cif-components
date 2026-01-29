/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v2.navigation;

import java.util.ArrayList;
import java.util.List;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.NavigationItem;

class NavigationItemImpl implements NavigationItem {
    private final com.adobe.cq.commerce.core.components.models.navigation.NavigationItem commerceNavigationItem;
    private final int level;

    public NavigationItemImpl(com.adobe.cq.commerce.core.components.models.navigation.NavigationItem commerceNavigationItem, int level) {
        this.commerceNavigationItem = commerceNavigationItem;
        this.level = level;
    }

    @Override
    public List<NavigationItem> getChildren() {
        List<NavigationItem> children = new ArrayList<>();
        for (com.adobe.cq.commerce.core.components.models.navigation.NavigationItem item : commerceNavigationItem.getItems()) {
            children.add(new NavigationItemImpl(item, level + 1));
        }
        return children;
    }

    @Override
    public boolean isActive() {
        return commerceNavigationItem.isActive();
    }

    @Override
    public String getURL() {
        return commerceNavigationItem.getURL();
    }

    @Override
    public String getTitle() {
        return commerceNavigationItem.getTitle();
    }

    @Override
    public String getName() {
        return commerceNavigationItem.getTitle();
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public Link getLink() {
        return commerceNavigationItem.getLink();
    }
}
