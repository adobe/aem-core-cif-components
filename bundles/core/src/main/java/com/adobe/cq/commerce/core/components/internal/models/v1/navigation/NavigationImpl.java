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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.navigation.Navigation;
import com.adobe.cq.commerce.core.components.models.navigation.NavigationItem;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Style;

import static com.adobe.cq.wcm.core.components.models.Navigation.PN_STRUCTURE_DEPTH;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Navigation.class,
    resourceType = NavigationImpl.RESOURCE_TYPE)
public class NavigationImpl implements Navigation {

    static final String PN_MAGENTO_ROOT_CATEGORY_ID = "magentoRootCategoryId";
    static final String RESOURCE_TYPE = "core/cif/components/structure/navigation/v1/navigation";
    static final String ROOT_NAVIGATION_ID = "ROOT_NAVIGATION";
    static final int DEFAULT_STRUCTURE_DEPTH = 2;
    static final int MIN_STRUCTURE_DEPTH = 1;
    static final int MAX_STRUCTURE_DEPTH = 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationImpl.class);
    @ScriptVariable
    private Page currentPage = null;

    @Self
    @Via
    private com.adobe.cq.wcm.core.components.models.Navigation wcmNavigation = null;

    @Self
    private SlingHttpServletRequest request = null;

    @Inject
    private Resource resource;

    @Inject
    private UrlProvider urlProvider;

    @ScriptVariable
    private ValueMap properties = null;

    @ScriptVariable
    private Style currentStyle = null;

    private GraphQLCategoryProvider graphQLCategoryProvider;
    private List<NavigationItem> items;
    private int structureDepth;

    @PostConstruct
    void initModel() {
        graphQLCategoryProvider = new GraphQLCategoryProvider(resource, currentPage, request);
        structureDepth = properties.get(PN_STRUCTURE_DEPTH, currentStyle.get(PN_STRUCTURE_DEPTH, DEFAULT_STRUCTURE_DEPTH));
        if (structureDepth < MIN_STRUCTURE_DEPTH) {
            LOGGER.warn("Navigation structure depth ({}) is bellow min value ({}). Using min value.", PN_STRUCTURE_DEPTH,
                MIN_STRUCTURE_DEPTH);
            structureDepth = MIN_STRUCTURE_DEPTH;
        }
        if (structureDepth > MAX_STRUCTURE_DEPTH) {
            LOGGER.warn("Navigation structure depth ({}) is above max value ({}). Using max value.", PN_STRUCTURE_DEPTH,
                MAX_STRUCTURE_DEPTH);
            structureDepth = MAX_STRUCTURE_DEPTH;
        }
    }

    @Override
    public List<NavigationItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
            PageManager pageManager = currentPage.getPageManager();
            for (com.adobe.cq.wcm.core.components.models.NavigationItem wcmItem : wcmNavigation.getItems()) {
                addItems(pageManager, null, wcmItem, this.items);
            }
        }
        return items;
    }

    private void addItems(PageManager pageManager, AbstractNavigationItem parent,
        com.adobe.cq.wcm.core.components.models.NavigationItem currentWcmItem, List<NavigationItem> itemList) {
        Page page = pageManager.getPage(currentWcmItem.getPath());

        // Go to production version to get the configuration of the navigation panel
        page = SiteNavigation.toLaunchProductionPage(page);

        if (shouldExpandCatalogRoot(page)) {
            expandCatalogRoot(page, itemList);
        } else {
            NavigationItem item;
            if (isCatalogRoot(page)) {
                item = new CatalogPageNavigationItem(null, page, currentWcmItem);
            } else {
                String title = currentWcmItem.getTitle();
                String url = currentWcmItem.getURL();
                boolean active = currentWcmItem.isActive();
                item = new PageNavigationItem(parent, title, url, active, currentWcmItem);
            }
            itemList.add(item);
        }
    }

    private boolean isCatalogPage(Page page) {
        if (page == null) {
            return false;
        }

        Resource contentResource = page.getContentResource();
        if (contentResource == null) {
            return false;
        }

        return contentResource.isResourceType(RT_CATALOG_PAGE);
    }

    private boolean shouldExpandCatalogRoot(Page page) {
        if (!isCatalogPage(page)) {
            return false;
        }

        Boolean showMainCategories = page.getContentResource().getValueMap().get(PN_SHOW_MAIN_CATEGORIES, Boolean.TRUE);
        return showMainCategories;
    }

    private boolean isCatalogRoot(Page page) {
        if (!isCatalogPage(page)) {
            return false;
        }

        Boolean showMainCategories = page.getContentResource().getValueMap().get(PN_SHOW_MAIN_CATEGORIES, Boolean.TRUE);
        return !showMainCategories;
    }

    private void expandCatalogRoot(Page catalogPage, List<NavigationItem> pages) {
        Page categoryPage = SiteNavigation.getCategoryPage(currentPage);
        if (categoryPage == null) {
            return;
        }

        Integer rootCategoryId = readPageConfiguration(catalogPage, PN_MAGENTO_ROOT_CATEGORY_ID);
        if (rootCategoryId == null) {
            ComponentsConfiguration properties = catalogPage.getContentResource().adaptTo(ComponentsConfiguration.class);
            rootCategoryId = properties.get(PN_MAGENTO_ROOT_CATEGORY_ID, Integer.class);
        }

        if (rootCategoryId == null) {
            LOGGER.warn("Magento root category ID property (" + PN_MAGENTO_ROOT_CATEGORY_ID + ") not found");
            return;
        }

        List<CategoryTree> children = graphQLCategoryProvider.getChildCategories(rootCategoryId, structureDepth);
        if (children == null || children.isEmpty()) {
            LOGGER.warn("Magento top categories not found");
            return;
        }

        children = children.stream().filter(c -> c != null && c.getName() != null).collect(Collectors.toList());
        children.sort(Comparator.comparing(CategoryTree::getPosition));

        for (CategoryTree child : children) {
            Map<String, String> params = new ParamsBuilder()
                .id(child.getId().toString())
                .urlKey(child.getUrlKey())
                .urlPath(child.getUrlPath())
                .map();

            String url = urlProvider.toCategoryUrl(request, categoryPage, params);
            boolean active = request.getRequestURI().equals(url);
            CategoryNavigationItem navigationItem = new CategoryNavigationItem(null, child.getName(), url, active, child, request,
                categoryPage);
            pages.add(navigationItem);
        }
    }

    @Override
    public String getId() {
        return ROOT_NAVIGATION_ID;
    }

    @Override
    public String getParentId() {
        return null;
    }

    private Integer readPageConfiguration(Page page, String propertyName) {
        InheritanceValueMap properties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        return properties.getInherited(propertyName, Integer.class);
    }

    class PageNavigationItem extends AbstractNavigationItem {
        private final com.adobe.cq.wcm.core.components.models.NavigationItem wcmItem;

        PageNavigationItem(AbstractNavigationItem parent, String title, String url, boolean active,
                           com.adobe.cq.wcm.core.components.models.NavigationItem wcmItem) {
            super(parent, title, url, active);
            this.wcmItem = wcmItem;
        }

        @Override
        public List<NavigationItem> getItems() {
            final List<com.adobe.cq.wcm.core.components.models.NavigationItem> children = wcmItem.getChildren();
            if (children == null) {
                return Collections.emptyList();
            }

            List<NavigationItem> items = new ArrayList<>();
            for (com.adobe.cq.wcm.core.components.models.NavigationItem item : children) {
                PageManager pageManager = currentPage.getPageManager();
                addItems(pageManager, this, item, items);
            }
            return items;
        }
    }

    class CatalogPageNavigationItem extends PageNavigationItem {
        private final Page page;

        CatalogPageNavigationItem(AbstractNavigationItem parent, Page page,
                                  com.adobe.cq.wcm.core.components.models.NavigationItem wcmItem) {
            super(parent, wcmItem.getTitle(), wcmItem.getURL(), wcmItem.isActive(), wcmItem);
            this.page = page;
        }

        @Override
        public List<NavigationItem> getItems() {
            final ArrayList<NavigationItem> items = new ArrayList<>();
            expandCatalogRoot(page, items);
            return items;
        }
    }

    class CategoryNavigationItem extends AbstractNavigationItem implements NavigationItem {
        private CategoryTree category;
        private SlingHttpServletRequest request;
        private Page categoryPage;

        CategoryNavigationItem(AbstractNavigationItem parent, String title, String url, boolean active, CategoryTree category,
                               SlingHttpServletRequest request, Page categoryPage) {
            super(parent, title, url, active);
            this.category = category;
            this.request = request;
            this.categoryPage = categoryPage;
        }

        @Override
        public List<NavigationItem> getItems() {

            if (category == null) {
                return Collections.emptyList();
            }

            List<CategoryTree> children = category.getChildren();
            if (children == null || children.isEmpty()) {
                return Collections.emptyList();
            }

            children = children.stream().filter(c -> c != null && c.getName() != null).collect(Collectors.toList());
            children.sort(Comparator.comparing(CategoryTree::getPosition));

            List<NavigationItem> pages = new ArrayList<>();

            for (CategoryTree child : children) {
                Map<String, String> params = new ParamsBuilder()
                    .id(child.getId().toString())
                    .urlKey(child.getUrlKey())
                    .urlPath(child.getUrlPath())
                    .map();

                String url = urlProvider.toCategoryUrl(request, categoryPage, params);
                boolean active = request.getRequestURI().equals(url);
                pages.add(new CategoryNavigationItem(this, child.getName(), url, active, child, request, categoryPage));
            }

            return pages;
        }
    }
}
