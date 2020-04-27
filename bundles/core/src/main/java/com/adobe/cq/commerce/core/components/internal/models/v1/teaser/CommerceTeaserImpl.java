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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaser;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class, adapters = CommerceTeaser.class, resourceType = CommerceTeaserImpl.RESOURCE_TYPE)
public class CommerceTeaserImpl implements CommerceTeaser {

    protected static final String RESOURCE_TYPE = "core/cif/components/content/teaser/v1/teaser";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceTeaserImpl.class);

    private Page productPage;
    private Page categoryPage;
    private boolean actionsEnabled = false;
    private List<ListItem> actions = new ArrayList<>();

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @Inject
    private UrlProvider urlProvider;

    @Self
    private SlingHttpServletRequest request;

    @PostConstruct
    void initModel() {
        setActionsEnabled();
        productPage = SiteNavigation.getProductPage(currentPage);
        categoryPage = SiteNavigation.getCategoryPage(currentPage);

        if (actionsEnabled) {
            populateActions();
        }
    }

    void setActionsEnabled() {
        ValueMap properties = resource.getValueMap();
        actionsEnabled = properties.get(CommerceTeaser.PN_ACTIONS_ENABLED, actionsEnabled);
    }

    void populateActions() {
        Resource actionsNode = resource.getChild(CommerceTeaser.NN_ACTIONS);
        if (actionsNode != null) {
            for (Resource action : actionsNode.getChildren()) {

                ValueMap properties = action.getValueMap();
                String title = properties.get(PN_ACTION_TEXT, String.class);
                String productSlug = properties.get(PN_ACTION_PRODUCT_SLUG, String.class);
                String categoryId = properties.get(PN_ACTION_CATEGORY_ID, String.class);
                String selector = null;
                Page page;

                if (categoryId != null) {
                    page = categoryPage;
                    selector = categoryId;
                } else if (productSlug != null) {
                    page = productPage;
                    selector = productSlug;
                } else {
                    page = currentPage;

                }
                actions.add(new CommerceTeaserActionItem(title, selector, page, request, urlProvider, productSlug != null));
            }
        }
    }

    @Override
    public boolean isActionsEnabled() {
        return actionsEnabled;
    }

    @Override
    public List<ListItem> getActions() {
        return actions;
    }

}
