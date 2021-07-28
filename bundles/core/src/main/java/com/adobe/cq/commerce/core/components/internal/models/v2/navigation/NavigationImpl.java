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

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ForcedResourceType;

import com.adobe.cq.wcm.core.components.models.Navigation;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Navigation.class,
    resourceType = NavigationImpl.RESOURCE_TYPE)
public class NavigationImpl implements Navigation {
    static final String RESOURCE_TYPE = "core/cif/components/structure/navigation/v2/navigation";

    @Inject
    private Resource resource = null;

    @Self
    @Via(type = ForcedResourceType.class, value = "core/wcm/components/navigation/v1/navigation")
    private com.adobe.cq.wcm.core.components.models.Navigation wcmNavigation = null;

    @Self
    @Via(type = ForcedResourceType.class, value = "core/cif/components/structure/navigation/v1/navigation")
    private com.adobe.cq.commerce.core.components.models.navigation.Navigation commerceNavigation = null;

    @Override
    public ComponentData getData() {
        return wcmNavigation.getData();
    }

    @Override
    public String getAccessibilityLabel() {
        return wcmNavigation.getAccessibilityLabel();
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }

    @Override
    public List<NavigationItem> getItems() {
        List<NavigationItem> items = new ArrayList<>();
        for (com.adobe.cq.commerce.core.components.models.navigation.NavigationItem item : commerceNavigation.getItems()) {
            items.add(new NavigationItemImpl(item, 0));
        }
        return items;
    }

    @Override
    public String getId() {
        return wcmNavigation.getId();
    }
}
