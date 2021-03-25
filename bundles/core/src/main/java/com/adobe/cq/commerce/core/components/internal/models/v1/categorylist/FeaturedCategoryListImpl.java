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

package com.adobe.cq.commerce.core.components.internal.models.v1.categorylist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.commerce.core.components.internal.datalayer.CategoryDataImpl;
import com.adobe.cq.commerce.core.components.internal.datalayer.CategoryListDataImpl;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.CommerceIdentifierImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.TitleTypeProvider;
import com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryList;
import com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryListItem;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoriesRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { FeaturedCategoryList.class, ComponentExporter.class },
    resourceType = com.adobe.cq.commerce.core.components.internal.models.v1.categorylist.FeaturedCategoryListImpl.RESOURCE_TYPE)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class FeaturedCategoryListImpl extends DataLayerComponent implements FeaturedCategoryList {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturedCategoryListImpl.class);

    private static final String RENDITION_WEB = "web";
    private static final String RENDITION_ORIGINAL = "original";
    private static final String SELECTION_TYPE = "selectionType";
    private static final String ASSET_PROP = "asset";
    private static final String ITEMS_PROP = "items";
    private static final Map<String, UrlProvider.CategoryIdentifierType> IDENTIFIER_TYPE_MAPPING = ImmutableMap.<String, UrlProvider.CategoryIdentifierType>builder()
        .put("categoryId", UrlProvider.CategoryIdentifierType.ID)
        .put("categoryUID", UrlProvider.CategoryIdentifierType.UID)
        .build();

    @Inject
    private Page currentPage;

    @Inject
    private UrlProvider urlProvider;

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    protected Style currentStyle;

    private Map<String, Asset> assetOverride;
    private Page categoryPage;
    private AbstractCategoriesRetriever categoriesRetriever;
    private String categoryIdentifierType;

    @PostConstruct
    private void initModel() {
        categoryPage = SiteNavigation.getCategoryPage(currentPage);
        if (categoryPage == null) {
            categoryPage = currentPage;
        }

        List<String> categoryIdentifiers = new ArrayList<>();
        assetOverride = new HashMap<>();

        // Iterate entries of composite multifield
        Resource items = resource.getChild(ITEMS_PROP);
        categoryIdentifierType = resource.getValueMap().get(SELECTION_TYPE, "categoryId");
        if (items != null) {
            for (Resource item : items.getChildren()) {
                ValueMap props = item.getValueMap();
                String categoryIdentifier = props.get(categoryIdentifierType, String.class);
                if (StringUtils.isEmpty(categoryIdentifier)) {
                    continue;
                }
                categoryIdentifiers.add(categoryIdentifier);

                // Check if an override asset was set. If yes, store it in a map for later use.
                String assetPath = props.get(ASSET_PROP, String.class);
                if (StringUtils.isEmpty((assetPath))) {
                    continue;
                }

                Resource assetResource = resource.getResourceResolver().getResource(assetPath);
                if (assetResource == null) {
                    continue;
                }

                Asset overrideAsset = assetResource.adaptTo(Asset.class);
                assetOverride.put(categoryIdentifier, overrideAsset);
            }

            if (!categoryIdentifiers.isEmpty()) {
                MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage, request);
                if (magentoGraphqlClient != null) {
                    categoriesRetriever = new CategoriesRetriever(magentoGraphqlClient);
                    categoriesRetriever.setIdentifiers(categoryIdentifiers, IDENTIFIER_TYPE_MAPPING.get(categoryIdentifierType));
                }
            }
        }
    }

    @Override
    @JsonIgnore
    public List<CategoryTree> getCategories() {
        if (categoriesRetriever == null) {
            return Collections.emptyList();
        }

        List<CategoryTree> categories = categoriesRetriever.fetchCategories();
        for (CategoryTree category : categories) {
            Map<String, String> params = new ParamsBuilder()
                .id(category.getId().toString())
                .urlPath(category.getUrlPath())
                .map();
            category.setPath(urlProvider.toCategoryUrl(request, categoryPage, params));

            // Replace image if there is an asset override
            String id = category.getId().toString();
            if (assetOverride.containsKey(id)) {
                Asset asset = assetOverride.get(id);
                Rendition rendition = asset.getRendition(RENDITION_WEB);
                if (rendition == null) {
                    rendition = asset.getRendition(RENDITION_ORIGINAL);
                }
                if (rendition != null) {
                    category.setImage(rendition.getPath());
                }
            }
        }
        return categories;
    }

    @Override
    public List<FeaturedCategoryListItem> getCategoryItems() {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        List<FeaturedCategoryListItem> categories = new ArrayList<>();

        resource.getChild(ITEMS_PROP).getChildren().forEach(resource -> {
            ValueMap props = resource.adaptTo(ValueMap.class);
            String categoryId = props.get(categoryIdentifierType, String.class);
            String assetPath = props.get(ASSET_PROP, String.class);
            if (StringUtils.isNotEmpty(categoryId)) {
                categories.add(
                    new FeaturedCategoryListItemImpl(new CommerceIdentifierImpl(categoryId, CommerceIdentifier.IdentifierType.ID,
                        CommerceIdentifier.EntityType.CATEGORY), assetPath));
            }
        });

        return categories;
    }

    @Override
    public boolean isConfigured() {
        return resource.getChild(ITEMS_PROP) != null;
    }

    @Override
    @JsonIgnore
    public AbstractCategoriesRetriever getCategoriesRetriever() {
        return this.categoriesRetriever;
    }

    // DataLayer methods

    @Override
    protected ComponentData getComponentData() {
        return new CategoryListDataImpl(this, resource);
    }

    @Override
    public CategoryData[] getDataLayerCategories() {
        return getCategories().stream()
            .map(c -> new CategoryDataImpl(c.getId().toString(), c.getName(), c.getImage()))
            .toArray(CategoryData[]::new);
    }

    @Override
    public String getTitleType() {
        return TitleTypeProvider.getTitleType(currentStyle, resource.getValueMap());
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
