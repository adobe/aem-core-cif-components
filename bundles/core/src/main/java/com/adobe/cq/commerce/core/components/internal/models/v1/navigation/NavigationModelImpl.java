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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.core.components.models.navigation.Navigation;
import com.adobe.cq.commerce.core.components.models.navigation.NavigationItem;
import com.adobe.cq.commerce.core.components.models.navigation.NavigationModel;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = NavigationModel.class,
    resourceType = NavigationImpl.RESOURCE_TYPE)
public class NavigationModelImpl implements NavigationModel {
    @Self
    @Via
    private Navigation rootNavigation = null;

    @Self
    private SlingHttpServletRequest request = null;

    private Navigation currentNavigation;
    private List<Navigation> navigationList;

    @Override
    public Navigation getActiveNavigation() {
        if (currentNavigation == null) {
            String requestURI = request.getRequestURI();
            for (Navigation navigation : getNavigationList()) {
                for (NavigationItem item : navigation.getItems()) {
                    if (requestURI.equals(item.getURL())) {
                        this.currentNavigation = navigation;

                        if (item instanceof AbstractNavigationItem) {
                            AbstractNavigationItem abstractItem = (AbstractNavigationItem) item;
                            while (abstractItem != null) {
                                abstractItem.setActive(true);
                                abstractItem = abstractItem.getParent();
                            }
                        }

                        return currentNavigation;
                    }
                }
            }

            currentNavigation = rootNavigation;
        }
        return currentNavigation;
    }

    @Override
    public List<Navigation> getNavigationList() {
        if (navigationList == null) {
            navigationList = new ArrayList<>();
            navigationList.add(rootNavigation);
            populateNavigationList(rootNavigation.getId(), rootNavigation.getParentId(), rootNavigation.getItems(), navigationList);
        }

        return navigationList;
    }

    private void populateNavigationList(String id, String parentId, List<NavigationItem> items, List<Navigation> navigationList) {
        if (items == null || items.isEmpty())
            return;

        final Navigation navigation;
        if (parentId == null) {
            navigation = rootNavigation;
        } else {
            navigation = new Navigation() {
                @Override
                public List<NavigationItem> getItems() {
                    return items;
                }

                @Override
                public String getId() {
                    return id;
                }

                @Override
                public String getParentId() {
                    return parentId;
                }
            };
            navigationList.add(navigation);
        }

        for (NavigationItem item : items) {
            populateNavigationList(item.getURL(), navigation.getId(), item.getItems(), navigationList);
        }
    }
}
