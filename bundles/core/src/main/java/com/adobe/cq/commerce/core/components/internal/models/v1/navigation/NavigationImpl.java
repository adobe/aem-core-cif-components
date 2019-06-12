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

package com.adobe.cq.commerce.core.components.internal.models.v1.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.navigation.Navigation;
import com.adobe.cq.commerce.core.components.models.navigation.NavigationItem;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Navigation.class,
    resourceType = NavigationImpl.RESOURCE_TYPE)
public class NavigationImpl implements Navigation {
    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationImpl.class);
    static final String PN_MAGENTO_ROOT_CATEGORY_ID = "magentoRootCategoryId";
    static final String RESOURCE_TYPE = "core/cif/components/structure/navigation/v1/navigation";

    @ScriptVariable
    private Page currentPage = null;

    @Self
    @Via
    private com.adobe.cq.wcm.core.components.models.Navigation wcmNavigation = null;

    @Self
    private SlingHttpServletRequest request = null;

    private GraphQLCategoryProvider graphQLCategoryProvider;
    private List<NavigationItem> items;

    @PostConstruct
    private void initModel() {
        graphQLCategoryProvider = new GraphQLCategoryProvider(currentPage);
    }

    @Override
    public List<NavigationItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
            PageManager pageManager = currentPage.getPageManager();
            for (com.adobe.cq.wcm.core.components.models.NavigationItem wcmItem : wcmNavigation.getItems()) {
                Page page = pageManager.getPage(wcmItem.getPath());
                if (shouldExpandCatalogRoot(page)) {
                    expandCatalogRoot(page, items);
                } else {
                    final String title = wcmItem.getTitle();
                    final String url = wcmItem.getURL();
                    final boolean active = wcmItem.isActive();
                    NavigationItem item = new NavigationItemImpl(title, url, active);
                    items.add(item);
                }

            }
        }
        return items;
    }

    private boolean shouldExpandCatalogRoot(Page page) {
        if (page == null) {
            return false;
        }

        Resource contentResource = page.getContentResource();
        if (contentResource == null) {
            return false;
        }

        if (!contentResource.isResourceType(RT_CATALOG_PAGE)) {
            return false;
        }

        Boolean showMainCategories = contentResource.getValueMap().get(PN_SHOW_MAIN_CATEGORIES, Boolean.class);
        return Boolean.TRUE.equals(showMainCategories);
    }

    private void expandCatalogRoot(Page catalogPage, List<NavigationItem> pages) {
        Page categoryPage = Utils.getCategoryPage(currentPage);
        if (categoryPage == null) {
            return;
        }

        final InheritanceValueMap catalogPageProperties = new HierarchyNodeInheritanceValueMap(catalogPage.getContentResource());
        Integer rootCategoryId = catalogPageProperties.getInherited(PN_MAGENTO_ROOT_CATEGORY_ID, Integer.class);
        if (rootCategoryId == null) {
            LOGGER.warn("Magento root category ID property (" + PN_MAGENTO_ROOT_CATEGORY_ID + ") not found");
            return;
        }

        final List<CategoryTree> children = graphQLCategoryProvider.getChildCategories(rootCategoryId);
        if (children.isEmpty()) {
            LOGGER.warn("Magento top categories not found");
            return;
        }

        children.sort(Comparator.comparing(CategoryTree::getPosition));

        String categoryPagePath = categoryPage.getPath();
        for (CategoryTree child : children) {
            String title = child.getName();
            String url = categoryPagePath + "." + child.getId() + ".html";
            boolean active = request.getRequestURI().equals(url);
            pages.add(new NavigationItemImpl(title, url, active));
        }
    }

}
