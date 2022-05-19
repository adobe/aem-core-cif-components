/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v3.teaser;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.commerce.core.components.internal.models.v1.common.CommerceIdentifierImpl;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaser;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.Teaser;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { Teaser.class, ComponentExporter.class },
    resourceType = CommerceTeaserImpl.RESOURCE_TYPE)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class CommerceTeaserImpl implements CommerceTeaser {
    protected static final String RESOURCE_TYPE = "core/cif/components/content/teaser/v3/teaser";

    private List<ListItem> actions = new ArrayList<>();

    @ScriptVariable
    private Resource resource;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private UrlProvider urlProvider;

    @Self
    private SlingHttpServletRequest request;

    @Self
    @Via(type = ResourceSuperType.class)
    private Teaser wcmTeaser;

    @PostConstruct
    void initModel() {
        if (isActionsEnabled()) {
            populateActions();
        }
    }

    void populateActions() {
        Resource actionsNode = resource.getChild(CommerceTeaser.NN_ACTIONS);
        if (actionsNode != null) {
            Iterable<Resource> configuredActions = actionsNode.getChildren();

            Page productPage = SiteNavigation.getProductPage(currentPage);
            Page categoryPage = SiteNavigation.getCategoryPage(currentPage);

            // build teaser action items for all configured actions
            for (Resource action : configuredActions) {
                ValueMap actionProperties = action.getValueMap();
                String productSku = actionProperties.get(PN_ACTION_PRODUCT_SKU, String.class);
                String categoryUid = actionProperties.get(PN_ACTION_CATEGORY_ID, String.class);
                String link = actionProperties.get(Teaser.PN_ACTION_LINK, String.class);

                String actionUrl = null;
                CommerceIdentifier identifier = null;

                if (StringUtils.isNotBlank(categoryUid)) {
                    actionUrl = urlProvider.toCategoryUrl(request, categoryPage, categoryUid);
                    identifier = new CommerceIdentifierImpl(categoryUid, CommerceIdentifier.IdentifierType.UID,
                        CommerceIdentifier.EntityType.CATEGORY);
                } else if (StringUtils.isNotBlank(productSku)) {
                    actionUrl = urlProvider.toProductUrl(request, productPage, productSku);
                    identifier = new CommerceIdentifierImpl(productSku, CommerceIdentifier.IdentifierType.SKU,
                        CommerceIdentifier.EntityType.PRODUCT);
                } else if (StringUtils.isNotBlank(link)) {
                    actionUrl = link + ".html";
                } else {
                    actionUrl = currentPage.getPath() + ".html";
                }

                String title = actionProperties.get(PN_ACTION_TEXT, String.class);
                actions.add(new CommerceTeaserActionItemImpl(title, actionUrl, identifier, action, wcmTeaser.getId()));
            }
        }
    }

    @Override
    public List<ListItem> getActions() {
        return actions;
    }

    @Override
    public boolean isActionsEnabled() {
        return wcmTeaser.isActionsEnabled();
    }

    @Override
    public String getLinkURL() {
        return wcmTeaser.getLinkURL();
    }

    @Override
    public Link getLink() {
        return wcmTeaser.getLink();
    }

    @Override
    public String getAssetPath() {
        Resource imageResource = getImageResource();
        if (imageResource == null) {
            return null;
        }
        ValueMap props = imageResource.adaptTo(ValueMap.class);
        return props.get("fileReference", String.class);
    }

    @Override
    @JsonIgnore
    public Resource getImageResource() {
        return wcmTeaser.getImageResource();
    }

    @Override
    public boolean isImageLinkHidden() {
        return wcmTeaser.isImageLinkHidden();
    }

    @Override
    public String getPretitle() {
        return wcmTeaser.getPretitle();
    }

    @Override
    public String getTitle() {
        return wcmTeaser.getTitle();
    }

    @Override
    public boolean isTitleLinkHidden() {
        return wcmTeaser.isTitleLinkHidden();
    }

    @Override
    public String getDescription() {
        return wcmTeaser.getDescription();
    }

    @Override
    public String getTitleType() {
        return wcmTeaser.getTitleType();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getId() {
        return wcmTeaser.getId();
    }

    @Override
    public ComponentData getData() {
        return wcmTeaser.getData();
    }
}
