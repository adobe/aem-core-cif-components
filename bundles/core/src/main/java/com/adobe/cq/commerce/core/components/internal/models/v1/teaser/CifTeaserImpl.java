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

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.teaser.CifTeaser;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.commons.DownloadResource;
import com.day.cq.commons.ImageResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Model(adaptables = SlingHttpServletRequest.class, adapters = CifTeaser.class, resourceType = CifTeaserImpl.RESOURCE_TYPE)
public class CifTeaserImpl implements CifTeaser {

    protected static final String RESOURCE_TYPE = "core/wcm/components/commerce/teaser/v1/teaser";

    private static final Logger LOGGER = LoggerFactory.getLogger(CifTeaserImpl.class);
    private final List<String> hiddenImageResourceProperties = new ArrayList<String>() {
        {
            add(JcrConstants.JCR_TITLE);
            add(JcrConstants.JCR_DESCRIPTION);
        }
    };
    private Page productPage;
    private Page categoryPage;
    private String title;
    private String description;
    private String linkURL;
    private String titleType;
    private boolean actionsEnabled = false;
    private boolean titleHidden = false;
    private boolean descriptionHidden = false;
    private boolean imageLinkHidden = false;
    private boolean titleLinkHidden = false;
    private boolean titleFromPage = false;
    private boolean descriptionFromPage = false;
    private List<ListItem> actions = new ArrayList<>();
    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @ScriptVariable
    private PageManager pageManager;

//    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
//    @JsonIgnore
//    protected Style currentStyle;

    @Self
    private SlingHttpServletRequest request;

    private Page targetPage;

    @PostConstruct
    private void initModel() {
        ValueMap properties = resource.getValueMap();
        actionsEnabled = properties.get(CifTeaser.PN_ACTIONS_ENABLED, actionsEnabled);

        productPage = Utils.getProductPage(currentPage);
        categoryPage = Utils.getCategoryPage(currentPage);

//        populateStyleProperties();

        titleFromPage = properties.get(CifTeaser.PN_TITLE_FROM_PAGE, titleFromPage);
        descriptionFromPage = properties.get(CifTeaser.PN_DESCRIPTION_FROM_PAGE, descriptionFromPage);
        linkURL = properties.get(ImageResource.PN_LINK_URL, String.class);

        if (actionsEnabled) {
            hiddenImageResourceProperties.add(ImageResource.PN_LINK_URL);
            linkURL = null;
            populateActions();
            if (actions.size() > 0) {
                ListItem firstAction = actions.get(0);
                if (firstAction != null) {
                    targetPage = pageManager.getPage(firstAction.getPath());
                }
            }
        } else {
            targetPage = pageManager.getPage(linkURL);
        }

        if (titleHidden) {
            title = null;
        } else {
            title = properties.get(JcrConstants.JCR_TITLE, String.class);
            if (titleFromPage) {
                if (targetPage != null) {
                    title = StringUtils.defaultIfEmpty(targetPage.getPageTitle(), targetPage.getTitle());
                } else {
                    title = null;
                }
            }
        }
        if (descriptionHidden) {
            description = null;
        } else {
            description = properties.get(JcrConstants.JCR_DESCRIPTION, String.class);
            if (descriptionFromPage) {
                if (targetPage != null) {
                    description = targetPage.getDescription();
                } else {
                    description = null;
                }
            }
        }
        String fileReference = properties.get(DownloadResource.PN_REFERENCE, String.class);
        boolean hasImage = true;
        if (StringUtils.isEmpty(linkURL)) {
            LOGGER.debug("Teaser component from " + request.getResource().getPath() + " does not define a link.");
        }
        if (StringUtils.isEmpty(fileReference)) {
            if (request.getResource().getChild(DownloadResource.NN_FILE) == null) {
                LOGGER.debug("Teaser component from " + request.getResource().getPath() + " does not have an asset or an image file " +
                    "configured.");
                hasImage = false;
            }
        } else {
            if (request.getResourceResolver().getResource(fileReference) == null) {
                LOGGER.error("Asset " + fileReference + " configured for the teaser component from " + request.getResource().getPath() +
                        " doesn't exist.");
                hasImage = false;
            }
        }
        // if (hasImage) {
        // setImageResource(component, request.getResource(), hiddenImageResourceProperties);
        // }
        // if (targetPage != null) {
        // linkURL = com.adobe.cq.wcm.core.components.internal.Utils.getURL(request, targetPage);
        // }
    }
    private void populateActions() {
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

    @Override
    public String getTitle() {
        return title;
    }

}
