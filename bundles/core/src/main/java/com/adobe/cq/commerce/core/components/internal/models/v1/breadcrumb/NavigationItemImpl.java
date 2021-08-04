/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.breadcrumb;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerListItem;
import com.adobe.cq.wcm.core.components.models.NavigationItem;

public class NavigationItemImpl extends DataLayerListItem implements NavigationItem {

    protected String title;
    protected String url;
    protected boolean isActive;

    public NavigationItemImpl(String title, String url, boolean isActive, String parentId, Resource resource) {
        super(parentId, resource);
        this.title = title;
        this.url = url;
        this.isActive = isActive;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    // DataLayer methods

    @Override
    protected String getIdentifier() {
        return getURL();
    }

    @Override
    public String getDataLayerLinkUrl() {
        return getURL();
    }

    @Override
    public String getDataLayerTitle() {
        return getTitle();
    }
}
