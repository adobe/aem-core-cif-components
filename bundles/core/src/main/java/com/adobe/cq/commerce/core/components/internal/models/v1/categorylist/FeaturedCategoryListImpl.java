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
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = FeaturedCategoryList.class,
    resourceType = com.adobe.cq.commerce.core.components.internal.models.v1.categorylist.FeaturedCategoryListImpl.RESOURCE_TYPE)
public class FeaturedCategoryListImpl implements FeaturedCategoryList {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturedCategoryListImpl.class);
    private static final String CATEGORY_ID_PROP = "categoryIds";
    private static final String CATEGORY_IMAGE_FOLDER = "catalog/category/";

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @ScriptVariable
    private ValueMap properties;

    private List<String> categoryIds;

    private List<CategoryInterface> categories;

    private Page categoryPage;

    private AbstractCategoryRetriever categoryRetriever;

    @PostConstruct
    private void initModel() {
        categoryPage = SiteNavigation.getCategoryPage(currentPage);
        if (categoryPage == null) {
            categoryPage = currentPage;
        }
        String[] categoryIdArray = properties.get(CATEGORY_ID_PROP, String[].class);

        if (categoryIdArray != null) {
            categoryIds = Arrays.asList(categoryIdArray);
            MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource);
            categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
        } else {
            LOGGER.debug("There are no categories configured for CategoryList Component.");
        }
    }

    private void fetchCategoriesData(List<String> categoryIds) {
        // CIF-930 raised to fix Alias support in category query ,
        // this will be improved rather than using a loop.
        categories = new ArrayList<>();
        categoryIds.forEach(categoryId -> {
            categoryRetriever.setIdentifier(categoryId);
            CategoryTree category = (CategoryTree) categoryRetriever.getCategory();

            category.setPath(String.format("%s.%s.html", categoryPage.getPath(), categoryId));
            if (category.getImage() != null) {
                category.setImage(categoryRetriever.getMediaBaseUrl() + CATEGORY_IMAGE_FOLDER + category.getImage());
            }

            categories.add(category);
        });
    }

    @Override
    public List<CategoryInterface> getCategories() {
        if (categories == null) {
            fetchCategoriesData(categoryIds);
        }
        return categories;
    }

    @Override
    public AbstractCategoryRetriever getCategoryRetriever() {
        return this.categoryRetriever;
    }
}
