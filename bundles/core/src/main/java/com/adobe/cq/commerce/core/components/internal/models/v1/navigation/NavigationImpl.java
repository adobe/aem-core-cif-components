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
package com.adobe.cq.commerce.core.components.internal.models.v1.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ForcedResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.navigation.Navigation;
import com.adobe.cq.commerce.core.components.models.navigation.NavigationItem;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.SiteNavigation;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
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

    static final String PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER = "magentoRootCategoryId";
    static final String PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER_TYPE = "magentoRootCategoryIdType";
    static final String RESOURCE_TYPE = "core/cif/components/structure/navigation/v1/navigation";
    static final String ROOT_NAVIGATION_ID = "ROOT_NAVIGATION";
    static final int DEFAULT_STRUCTURE_DEPTH = 2;
    static final int MIN_STRUCTURE_DEPTH = 1;
    static final int MAX_STRUCTURE_DEPTH = 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationImpl.class);

    @ScriptVariable
    private Page currentPage = null;

    @Self
    @Via(type = ForcedResourceType.class, value = "core/wcm/components/navigation/v1/navigation")
    private com.adobe.cq.wcm.core.components.models.Navigation wcmNavigation = null;

    @Self
    private SlingHttpServletRequest request;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @SlingObject
    private Resource resource;

    @OSGiService
    private UrlProvider urlProvider;

    @OSGiService
    private SiteNavigation siteNavigation;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable
    private Style currentStyle;

    private GraphQLCategoryProvider graphQLCategoryProvider;
    private List<NavigationItem> items;
    private int structureDepth;

    @PostConstruct
    void initModel() {
        graphQLCategoryProvider = new GraphQLCategoryProvider(magentoGraphqlClient);
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
        if (currentWcmItem.getPath() != null && LaunchUtils.isLaunchBasedPath(currentWcmItem.getPath())) {
            String productionPagePath = currentWcmItem.getPath().substring(currentWcmItem.getPath().lastIndexOf("/content/"));
            Page productionPage = pageManager.getPage(productionPagePath);
            if (productionPage != null) {
                page = productionPage;
            } else {
                LOGGER.warn("Didn't find production page of given launch page: {}", page.getPath());
            }
        }

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

    private boolean shouldExpandCatalogRoot(Page page) {
        if (!siteNavigation.isCatalogPage(page)) {
            return false;
        }

        Boolean showMainCategories = page.getContentResource().getValueMap().get(PN_SHOW_MAIN_CATEGORIES, Boolean.TRUE);
        return showMainCategories;
    }

    private boolean isCatalogRoot(Page page) {
        if (!siteNavigation.isCatalogPage(page)) {
            return false;
        }

        Boolean showMainCategories = page.getContentResource().getValueMap().get(PN_SHOW_MAIN_CATEGORIES, Boolean.TRUE);
        return !showMainCategories;
    }

    private void expandCatalogRoot(Page catalogPage, List<NavigationItem> pages) {
        String rootCategoryIdentifier = readPageConfiguration(catalogPage, PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER);
        String rootCategoryIdentifierType =  readPageConfiguration(catalogPage, PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER_TYPE);

        if (rootCategoryIdentifier == null || StringUtils.isBlank(rootCategoryIdentifier)) {
            ComponentsConfiguration properties = catalogPage.getContentResource().adaptTo(ComponentsConfiguration.class);
            rootCategoryIdentifier = properties.get(PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER, String.class);
            rootCategoryIdentifierType = "uid";
        }

        if (rootCategoryIdentifier == null) {
            LOGGER.warn("Magento root category UID property (" + PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER + ") not found");
            return;
        }

        List<CategoryTree> children = "urlPath".equals(rootCategoryIdentifierType)
            ? graphQLCategoryProvider.getChildCategoriesByUrlPath(rootCategoryIdentifier, structureDepth)
            : graphQLCategoryProvider.getChildCategoriesByUid(rootCategoryIdentifier, structureDepth);

        if (children == null || children.isEmpty()) {
            LOGGER.warn("Magento top categories not found");
            return;
        }

        for (CategoryTree child : children) {
            CategoryUrlFormat.Params params = new CategoryUrlFormat.Params(child);
            String url = urlProvider.toCategoryUrl(request, currentPage, params);
            boolean active = request.getRequestURI().equals(url);
            CategoryNavigationItem navigationItem = new CategoryNavigationItem(null, child.getName(), url, active, child, request);
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

    private String readPageConfiguration(Page page, String propertyName) {
        InheritanceValueMap properties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        return properties.getInherited(propertyName, String.class);
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

        CategoryNavigationItem(AbstractNavigationItem parent, String title, String url, boolean active, CategoryTree category,
                               SlingHttpServletRequest request) {
            super(parent, title, url, active);
            this.category = category;
            this.request = request;
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
                CategoryUrlFormat.Params params = new CategoryUrlFormat.Params(child);
                String url = urlProvider.toCategoryUrl(request, currentPage, params);
                boolean active = request.getRequestURI().equals(url);
                pages.add(new CategoryNavigationItem(this, child.getName(), url, active, child, request));
            }

            return pages;
        }
    }
}
