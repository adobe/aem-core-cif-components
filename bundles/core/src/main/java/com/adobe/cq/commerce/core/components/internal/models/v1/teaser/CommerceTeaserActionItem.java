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

import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;

public class CommerceTeaserActionItem implements ListItem {

    private String title;
    private String selector;
    private Page page;
    private SlingHttpServletRequest request;
    private UrlProvider urlProvider;
    private boolean isProduct;

    public CommerceTeaserActionItem(String title, String selector, Page page, SlingHttpServletRequest request, UrlProvider urlProvider,
                                    boolean isProduct) {
        this.title = title;
        this.selector = selector;
        this.page = page;
        this.request = request;
        this.urlProvider = urlProvider;
        this.isProduct = isProduct;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return title;
    }

    @Nonnull
    @Override
    public String getURL() {
        if (StringUtils.isBlank(selector)) {
            return page.getPath() + ".html";
        } else if (isProduct) {
            Map<String, String> params = new ParamsBuilder().urlKey(selector).map();
            return urlProvider.toProductUrl(request, page, params);
        } else {
            Map<String, String> params = new ParamsBuilder().id(selector).map();
            return urlProvider.toCategoryUrl(request, page, params);
        }
    }
}
