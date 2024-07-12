/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.CommerceIdentifierImpl;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoriesRetriever;
import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaser;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.Teaser;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @deprecated use {@link com.adobe.cq.commerce.core.components.internal.models.v3.teaser.CommerceTeaserImpl} instead
 */
@Deprecated
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { Teaser.class, ComponentExporter.class },
    resourceType = CommerceTeaserImpl.RESOURCE_TYPE)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class CommerceTeaserImpl implements CommerceTeaser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceTeaserImpl.class);

    protected static final String RESOURCE_TYPE = "core/cif/components/content/teaser/v1/teaser";

    private List<ListItem> actions = new ArrayList<>();

    @SlingObject
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

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

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

            // collect all configured category actions to query url_path
            List<String> categoryIds = StreamSupport
                .stream(configuredActions.spliterator(), false).filter(res -> res.getValueMap().containsKey(PN_ACTION_CATEGORY_ID))
                .map(res -> res.getValueMap().get(PN_ACTION_CATEGORY_ID, String.class))
                .distinct().collect(Collectors.toList());

            AbstractCategoriesRetriever categoriesRetriever = null;
            if (categoryIds.size() > 0 && magentoGraphqlClient != null) {
                categoriesRetriever = new CategoriesRetriever(magentoGraphqlClient);
                categoriesRetriever.setIdentifiers(categoryIds);
            }

            // build teaser action items for all configured actions
            for (Resource action : configuredActions) {
                ValueMap properties = action.getValueMap();
                String productSlug = properties.get(PN_ACTION_PRODUCT_SLUG, String.class);
                String categoryId = properties.get(PN_ACTION_CATEGORY_ID, String.class);
                String link = properties.get(Teaser.PN_ACTION_LINK, String.class);
                String title = properties.get(PN_ACTION_TEXT, String.class);

                String actionUrl;
                CommerceIdentifier identifier = null;

                if (categoryId != null) {
                    CategoryUrlFormat.Params params = null;
                    try {
                        params = Optional.ofNullable(categoriesRetriever)
                            .map(AbstractCategoriesRetriever::fetchCategories)
                            .map(List::stream)
                            .flatMap(categories -> categories
                                .filter(category -> category.getUid().toString().equals(categoryId))
                                .findAny())
                            .map(CategoryUrlFormat.Params::new)
                            .orElse(null);

                    } catch (RuntimeException x) {
                        LOGGER.warn("Failed to fetch category for id: {}", categoryId);
                    }
                    if (params == null) {
                        params = new CategoryUrlFormat.Params();
                        params.setUid(categoryId);
                    }
                    actionUrl = urlProvider.toCategoryUrl(request, currentPage, params);
                    identifier = new CommerceIdentifierImpl(categoryId, CommerceIdentifier.IdentifierType.UID,
                        CommerceIdentifier.EntityType.CATEGORY);
                } else if (productSlug != null) {
                    ProductUrlFormat.Params params = new ProductUrlFormat.Params();
                    params.setUrlKey(productSlug);
                    actionUrl = urlProvider.toProductUrl(request, currentPage, params);
                    identifier = new CommerceIdentifierImpl(productSlug, CommerceIdentifier.IdentifierType.URL_KEY,
                        CommerceIdentifier.EntityType.PRODUCT);
                } else if (link != null) {
                    actionUrl = link + ".html";
                } else {
                    actionUrl = currentPage.getPath() + ".html";
                }

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
