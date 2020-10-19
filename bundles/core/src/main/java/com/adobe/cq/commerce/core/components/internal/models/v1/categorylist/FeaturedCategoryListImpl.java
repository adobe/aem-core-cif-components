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
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.commerce.core.components.internal.datalayer.CategoryListDataImpl;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.TitleTypeProvider;
import com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoriesRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = FeaturedCategoryList.class,
    resourceType = com.adobe.cq.commerce.core.components.internal.models.v1.categorylist.FeaturedCategoryListImpl.RESOURCE_TYPE)
public class FeaturedCategoryListImpl extends DataLayerComponent implements FeaturedCategoryList {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturedCategoryListImpl.class);

    private static final String RENDITION_WEB = "web";
    private static final String RENDITION_ORIGINAL = "original";
    private static final String CATEGORY_ID_PROP = "categoryId";
    private static final String ASSET_PROP = "asset";
    private static final String ITEMS_PROP = "items";

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

    @PostConstruct
    private void initModel() {
        categoryPage = SiteNavigation.getCategoryPage(currentPage);
        if (categoryPage == null) {
            categoryPage = currentPage;
        }

        List<String> categoryIds = new ArrayList<>();
        assetOverride = new HashMap<>();

        // Iterate entries of composite multifield
        Resource items = resource.getChild(ITEMS_PROP);
        if (items != null) {
            for (Resource item : items.getChildren()) {
                ValueMap props = item.getValueMap();
                String categoryId = props.get(CATEGORY_ID_PROP, String.class);
                if (StringUtils.isEmpty(categoryId)) {
                    continue;
                }
                categoryIds.add(categoryId);

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
                assetOverride.put(categoryId, overrideAsset);
            }

            if (!categoryIds.isEmpty()) {
                MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage);
                if (magentoGraphqlClient != null) {
                    categoriesRetriever = new CategoriesRetriever(magentoGraphqlClient);
                    categoriesRetriever.setIdentifiers(categoryIds);
                }
            }
        }
    }

    @Override
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
    public boolean isConfigured() {
        return resource.getChild(ITEMS_PROP) != null;
    }

    @Override
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
            .map(c -> new CategoryListDataImpl.CategoryDataImpl(c.getId().toString(), c.getName(), c.getImage()))
            .toArray(CategoryData[]::new);
    }

    @Override
    public String getTitleType() {
        return TitleTypeProvider.getTitleType(currentStyle, resource.getValueMap());
    }
}
