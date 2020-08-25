/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.models.v1.breadcrumb;

import com.adobe.cq.wcm.core.components.models.NavigationItem;

public class NavigationItemImpl implements NavigationItem {

    protected String title;
    protected String url;
    protected boolean isActive;

    public NavigationItemImpl(String title, String url, boolean isActive) {
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
}
