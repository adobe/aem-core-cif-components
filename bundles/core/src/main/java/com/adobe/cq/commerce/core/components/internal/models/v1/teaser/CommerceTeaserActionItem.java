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

package com.adobe.cq.commerce.core.components.internal.models.v1.teaser;

import javax.annotation.Nonnull;

import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;

public class CommerceTeaserActionItem implements ListItem {

    private String title;
    private String selector = "";
    private Page page = null;
    private final SiteNavigation siteNavigation;

    public CommerceTeaserActionItem(String title, String selector, Page page, SlingHttpServletRequest request) {
        this.title = title;
        this.selector = selector;
        this.page = page;
        this.siteNavigation = new SiteNavigation(request);
    }

    @Nonnull
    @Override
    public String getTitle() {
        return title;
    }

    @Nonnull
    @Override
    public String getURL() {
        return (selector == null || selector.trim().equalsIgnoreCase("")) ? (page.getPath() + ".html")
            : siteNavigation.toPageUrl(page, selector);
    }
}
