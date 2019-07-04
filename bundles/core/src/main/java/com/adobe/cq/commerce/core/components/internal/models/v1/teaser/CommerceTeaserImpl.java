/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated
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
package com.adobe.cq.commerce.core.components.internal.models.v1.teaser;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaser;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Model(adaptables = SlingHttpServletRequest.class, adapters = CifTeaser.class, resourceType = CifTeaserImpl.RESOURCE_TYPE)
public class CommerceTeaserImpl implements CommerceTeaser {

    protected static final String RESOURCE_TYPE = "core/cif/components/content/teaser/v1/teaser";

    private static final Logger LOGGER = LoggerFactory.getLogger(CifTeaserImpl.class);

    private Page productPage;
    private Page categoryPage;
    private boolean actionsEnabled = false;
    private List<ListItem> actions = new ArrayList<>();

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @Self
    private SlingHttpServletRequest request;

    @PostConstruct
    private void initModel() {
        ValueMap properties = resource.getValueMap();
        actionsEnabled = properties.get(CifTeaser.PN_ACTIONS_ENABLED, actionsEnabled);

        productPage = Utils.getProductPage(currentPage);
        categoryPage = Utils.getCategoryPage(currentPage);

        if (actionsEnabled) {
            populateActions();
            if (actions.size() > 0) {
                ListItem firstAction = actions.get(0);
            }
        }
    }

    void populateActions() {
        Resource actionsNode = resource.getChild(CifTeaser.NN_ACTIONS);
        if (actionsNode != null) {
            for (Resource action : actionsNode.getChildren()) {
                actions.add(new ListItem() {

                    private ValueMap properties = action.getValueMap();
                    private String title = properties.get(PN_ACTION_TEXT, String.class);
                    private String productSKU = properties.get(PN_ACTION_PRODUCT_SKU, String.class);
                    private String categoryId = properties.get(PN_ACTION_CATEGORY_ID, String.class);
                    private String selector = "";
                    private Page page = null;

                    {
                        if (categoryId != null) {
                            page = categoryPage;
                            selector = categoryId;
                        } else if (productSKU != null) {
                            page = productPage;
                            selector = productSKU;
                        } else {
                            page = currentPage;

                        }
                    }

                    @Nullable
                    @Override
                    public String getTitle() {
                        return title;
                    }

                    @Nullable
                    @Override
                    @JsonIgnore
                    public String getPath() {
                        return Utils.constructUrlfromSlug(page.getPath(), selector);
                    }

                    @Nullable
                    @Override
                    public String getURL() {
                        if (page != null) {
                            return Utils.constructUrlfromSlug(page.getPath(), selector);
                        } else {
                            String vanityURL = currentPage.getVanityUrl();
                            return StringUtils.isEmpty(vanityURL) ? request.getContextPath() + currentPage.getPath() + ".html"
                                : request.getContextPath() + vanityURL;
                        }
                    }
                });
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
