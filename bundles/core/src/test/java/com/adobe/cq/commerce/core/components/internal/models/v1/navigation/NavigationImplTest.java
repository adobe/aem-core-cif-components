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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import static com.adobe.cq.commerce.core.components.internal.models.v1.navigation.NavigationImpl.PN_MAGENTO_ROOT_CATEGORY_ID;
import static com.adobe.cq.commerce.core.components.models.navigation.Navigation.PN_SHOW_MAIN_CATEGORIES;
import static com.adobe.cq.commerce.core.components.models.navigation.Navigation.RT_CATALOG_PAGE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NavigationImplTest {
    private static final String CATALOG_PAGE_PATH = "catalog_page_path";
    private static final String CATEGORY_PAGE_PATH = "category_page_path";
    NavigationImpl navigation;
    com.adobe.cq.wcm.core.components.internal.models.v1.NavigationImpl wcmNavigation;
    GraphQLCategoryProvider categoryProvider;
    PageManager pageManager;
    List<NavigationItem> navigationItems;
    List<CategoryTree> categoryList;

    @Before
    public void init() {
        navigation = new NavigationImpl();

        // current page
        Page currentPage = mock(Page.class);
        pageManager = mock(PageManager.class);
        when(currentPage.getPageManager()).thenReturn(pageManager);
        Resource currentPageContent = mock(Resource.class);
        Map<String, Object> currentPageProperties = new HashMap<>();
        currentPageProperties.put("cq:cifCategoryPage", CATEGORY_PAGE_PATH);
        Page categoryPage = mock(Page.class);
        when(categoryPage.getPath()).thenReturn(CATEGORY_PAGE_PATH);
        when(pageManager.getPage(CATEGORY_PAGE_PATH)).thenReturn(categoryPage);
        when(currentPageContent.getValueMap()).thenReturn(new ValueMapDecorator(currentPageProperties));
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        Whitebox.setInternalState(navigation, "currentPage", currentPage);

        // WCM navigation model
        wcmNavigation = mock(com.adobe.cq.wcm.core.components.internal.models.v1.NavigationImpl.class);
        Whitebox.setInternalState(navigation, "wcmNavigation", wcmNavigation);
        navigationItems = new ArrayList<>();
        when(wcmNavigation.getItems()).thenReturn(navigationItems);

        // Magento category provider
        categoryProvider = mock(GraphQLCategoryProvider.class);
        Whitebox.setInternalState(navigation, "graphQLCategoryProvider", categoryProvider);
        categoryList = new ArrayList<>();
        when(categoryProvider.getChildCategories(any())).thenReturn(categoryList);

        // current request
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Whitebox.setInternalState(navigation, "request", request);
        when(request.getRequestURI()).thenReturn("uri");
    }

    @Test
    public void testEmptyNavigationNoPagesNoCategories() {
        Assert.assertTrue(navigation.getItems().isEmpty());
    }

    @Test
    public void testNavigationCatalogRootDisabled() {
        String catalogTitle = "Catalog 1";

        initCatalogPage(false, false);

        NavigationItem catalogItem = mock(NavigationItem.class);
        when(catalogItem.getPath()).thenReturn(CATALOG_PAGE_PATH);
        when(catalogItem.getTitle()).thenReturn(catalogTitle);
        navigationItems.add(catalogItem);

        Assert.assertEquals(1, navigation.getItems().size());
        Assert.assertEquals(catalogTitle, navigation.getItems().get(0).getTitle());
    }

    @Test
    public void testNavigationCategoriesDisabled() {
        String catalogTitle = "Catalog 1";

        initCatalogPage(true, false);

        NavigationItem catalogItem = mock(NavigationItem.class);
        when(catalogItem.getPath()).thenReturn(CATALOG_PAGE_PATH);
        when(catalogItem.getTitle()).thenReturn(catalogTitle);
        navigationItems.add(catalogItem);

        Assert.assertEquals(1, navigation.getItems().size());
        Assert.assertEquals(catalogTitle, navigation.getItems().get(0).getTitle());
    }

    @Test
    public void testNavigationPagesOnly() {
        // check the properties of a navigation item related to a normal page

        String pageTitle = "Page 1";
        String pageURL = "/page1";
        boolean active = true;

        NavigationItem item = mock(NavigationItem.class);
        when(item.getTitle()).thenReturn(pageTitle);
        when(item.getURL()).thenReturn(pageURL);
        when(item.isActive()).thenReturn(active);
        navigationItems.add(item);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);
        Assert.assertEquals(pageTitle, navigationItem.getTitle());
        Assert.assertEquals(pageURL, navigationItem.getURL());
        Assert.assertEquals(active, navigationItem.isActive());
    }

    @Test
    public void testNavigationCategoriesOnly() {
        // check the properties of a navigation item related to a category

        Integer categoryId = 0;
        String categoryName = "Category 1";

        initCatalogPage(true, true);

        NavigationItem item = mock(NavigationItem.class);
        when(item.getPath()).thenReturn(CATALOG_PAGE_PATH);
        navigationItems.add(item);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn(categoryName);
        categoryList.add(category);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);
        Assert.assertEquals(categoryName, navigationItem.getTitle());
        Assert.assertEquals(CATEGORY_PAGE_PATH + "." + categoryId + ".html", navigationItem.getURL());
    }

    @Test
    public void testNavigationPageBeforeCategory() {
        testNavigationItemOrdering(true);
    }

    @Test
    public void testNavigationCategoryBeforePage() {
        testNavigationItemOrdering(false);
    }

    public void testNavigationItemOrdering(boolean pageBeforeCategory) {
        // checks that the navigation items are ordered according to the underlying page nodes

        String pageTitle = "Page 1";
        String categoryTitle = "Category 1";

        initCatalogPage(true, true);

        NavigationItem pageItem = mock(NavigationItem.class);
        when(pageItem.getTitle()).thenReturn(pageTitle);

        NavigationItem catalogItem = mock(NavigationItem.class);
        when(catalogItem.getPath()).thenReturn(CATALOG_PAGE_PATH);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getName()).thenReturn(categoryTitle);
        categoryList.add(category);

        if (pageBeforeCategory) {
            navigationItems.add(pageItem);
            navigationItems.add(catalogItem);
        } else {
            navigationItems.add(catalogItem);
            navigationItems.add(pageItem);
        }

        Assert.assertEquals(2, navigation.getItems().size());
        if (pageBeforeCategory) {
            Assert.assertEquals(pageTitle, navigation.getItems().get(0).getTitle());
            Assert.assertEquals(categoryTitle, navigation.getItems().get(1).getTitle());
        } else {
            Assert.assertEquals(categoryTitle, navigation.getItems().get(0).getTitle());
            Assert.assertEquals(pageTitle, navigation.getItems().get(1).getTitle());
        }
    }

    private void initCatalogPage(boolean catalogRoot, boolean showMainCategories) {
        Page catalogPage = mock(Page.class);
        Resource catalogPageContent = mock(Resource.class);
        when(catalogPageContent.isResourceType(RT_CATALOG_PAGE)).thenReturn(catalogRoot);
        Map<String, Object> catalogPageProperties = new HashMap<>();
        catalogPageProperties.put(PN_SHOW_MAIN_CATEGORIES, showMainCategories);
        catalogPageProperties.put(PN_MAGENTO_ROOT_CATEGORY_ID, 4);
        when(catalogPageContent.getValueMap()).thenReturn(new ValueMapDecorator(catalogPageProperties));
        when(catalogPage.getContentResource()).thenReturn(catalogPageContent);
        when(pageManager.getPage(CATALOG_PAGE_PATH)).thenReturn(catalogPage);
    }
}
