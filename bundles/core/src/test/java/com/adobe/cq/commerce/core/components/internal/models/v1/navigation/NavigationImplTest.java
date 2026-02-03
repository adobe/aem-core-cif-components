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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureFactory;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureImpl;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.navigation.Navigation;
import com.adobe.cq.commerce.core.components.models.navigation.NavigationModel;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Style;
import com.google.common.collect.ImmutableMap;
import com.shopify.graphql.support.ID;

import static com.adobe.cq.commerce.core.components.internal.models.v1.navigation.NavigationImpl.DEFAULT_STRUCTURE_DEPTH;
import static com.adobe.cq.commerce.core.components.internal.models.v1.navigation.NavigationImpl.MAX_STRUCTURE_DEPTH;
import static com.adobe.cq.commerce.core.components.internal.models.v1.navigation.NavigationImpl.MIN_STRUCTURE_DEPTH;
import static com.adobe.cq.commerce.core.components.models.navigation.Navigation.PN_SHOW_MAIN_CATEGORIES;
import static com.adobe.cq.commerce.core.components.models.navigation.Navigation.RT_CATALOG_PAGE;
import static com.adobe.cq.wcm.core.components.models.Navigation.PN_STRUCTURE_DEPTH;
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
    NavigationModel navigationModel;
    SlingHttpServletRequest request;
    SiteStructure siteStructure;

    // TODO: Use aem mocks
    @Before
    public void init() {
        navigation = new NavigationImpl();

        // current page
        Page currentPage = mock(Page.class);
        pageManager = mock(PageManager.class);
        when(currentPage.getPageManager()).thenReturn(pageManager);
        when(currentPage.getPath()).thenReturn("/content/currentPage");
        Resource currentPageContent = mock(Resource.class);
        Resource contentResource = mock(Resource.class);
        Page categoryPage = mock(Page.class);
        when(categoryPage.getPath()).thenReturn(CATEGORY_PAGE_PATH);
        when(currentPageContent.getPath()).thenReturn(CATEGORY_PAGE_PATH + "/jcr:content");
        when(pageManager.getPage(CATEGORY_PAGE_PATH)).thenReturn(categoryPage);
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        Resource categoryPageResource = new SyntheticResource(null, CATEGORY_PAGE_PATH, null);
        when(categoryPage.adaptTo(Resource.class)).thenReturn(categoryPageResource);
        when(categoryPage.getContentResource()).thenReturn(categoryPageResource);
        when(categoryPage.getProperties()).thenReturn(categoryPageResource.getValueMap());
        when(categoryPage.listChildren()).thenReturn(Collections.emptyIterator());
        when(contentResource.adaptTo(ComponentsConfiguration.class)).thenReturn(null);
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
        when(categoryProvider.getChildCategoriesByUid(any(), any())).thenReturn(categoryList);

        // URL provider
        siteStructure = mock(SiteStructure.class);
        Whitebox.setInternalState(navigation, "siteStructure", siteStructure);
        SiteStructure.Entry mockEntry = mock(SiteStructure.Entry.class);
        when(mockEntry.getPage()).thenReturn(categoryPage);
        when(siteStructure.getCategoryPages()).thenReturn(Collections.singletonList(mockEntry));
        SiteStructureFactory siteStructureFactory = mock(SiteStructureFactory.class);
        when(siteStructureFactory.getSiteStructure(any(), any())).thenReturn(siteStructure);

        SpecificPageStrategy specificPageStrategy = new SpecificPageStrategy();
        SpecificPageStrategy.Configuration specificPageStrategyConfig = mock(SpecificPageStrategy.Configuration.class);
        when(specificPageStrategyConfig.generateSpecificPageUrls()).thenReturn(false);
        specificPageStrategy.activate(specificPageStrategyConfig);
        UrlProviderImpl urlProvider = new UrlProviderImpl();
        urlProvider.activate(mock(UrlProviderConfiguration.class));
        Whitebox.setInternalState(urlProvider, "specificPageStrategy", specificPageStrategy);
        Whitebox.setInternalState(urlProvider, "siteStructureFactory", siteStructureFactory);
        Whitebox.setInternalState(navigation, "urlProvider", urlProvider);

        // current request
        request = mock(SlingHttpServletRequest.class);
        Whitebox.setInternalState(navigation, "request", request);
        when(request.getRequestURI()).thenReturn("uri");
        when(request.getResource()).thenReturn(contentResource);

        navigationModel = new NavigationModelImpl();
        Whitebox.setInternalState(navigationModel, "rootNavigation", navigation);
        Whitebox.setInternalState(navigationModel, "request", request);
    }

    @Test
    public void testStructureDepthProperty() {
        // set up
        when(request.getRequestURI()).thenReturn("/page1/page11");
        RequestPathInfo rpi = mock(RequestPathInfo.class);
        when(rpi.getSelectors()).thenReturn(new String[0]);
        when(request.getRequestPathInfo()).thenReturn(rpi);
        ValueMapDecorator properties = new ValueMapDecorator(new HashMap<>());
        Whitebox.setInternalState(navigation, "properties", properties);
        Style style = mock(Style.class);
        Whitebox.setInternalState(navigation, "currentStyle", style);
        when(style.get(PN_STRUCTURE_DEPTH, DEFAULT_STRUCTURE_DEPTH)).thenReturn(DEFAULT_STRUCTURE_DEPTH);
        navigation.initModel();

        // structure depth not in properties or style
        Assert.assertEquals(DEFAULT_STRUCTURE_DEPTH, Whitebox.getInternalState(navigation, "structureDepth"));

        // structure depth in style bellow min value
        when(style.get(PN_STRUCTURE_DEPTH, DEFAULT_STRUCTURE_DEPTH)).thenReturn(MIN_STRUCTURE_DEPTH - 1);
        navigation.initModel();

        Assert.assertEquals(MIN_STRUCTURE_DEPTH, Whitebox.getInternalState(navigation, "structureDepth"));

        // structure depth in style above max value
        when(style.get(PN_STRUCTURE_DEPTH, DEFAULT_STRUCTURE_DEPTH)).thenReturn(MAX_STRUCTURE_DEPTH + 1);
        navigation.initModel();

        Assert.assertEquals(MAX_STRUCTURE_DEPTH, Whitebox.getInternalState(navigation, "structureDepth"));

        // structure depth in style OK
        when(style.get(PN_STRUCTURE_DEPTH, DEFAULT_STRUCTURE_DEPTH)).thenReturn(DEFAULT_STRUCTURE_DEPTH + 1);
        navigation.initModel();

        Assert.assertEquals(DEFAULT_STRUCTURE_DEPTH + 1, Whitebox.getInternalState(navigation, "structureDepth"));

        // structure depth in properties bellow min value
        properties.put(PN_STRUCTURE_DEPTH, MIN_STRUCTURE_DEPTH - 1);
        navigation.initModel();

        Assert.assertEquals(MIN_STRUCTURE_DEPTH, Whitebox.getInternalState(navigation, "structureDepth"));

        // structure depth in properties above max value
        properties.put(PN_STRUCTURE_DEPTH, MAX_STRUCTURE_DEPTH + 1);
        navigation.initModel();

        Assert.assertEquals(MAX_STRUCTURE_DEPTH, Whitebox.getInternalState(navigation, "structureDepth"));

        // structure depth in properties OK
        properties.put(PN_STRUCTURE_DEPTH, DEFAULT_STRUCTURE_DEPTH + 1);
        navigation.initModel();

        Assert.assertEquals(DEFAULT_STRUCTURE_DEPTH + 1, Whitebox.getInternalState(navigation, "structureDepth"));
    }

    @Test
    public void testEmptyNavigationNoPagesNoCategories() {
        Assert.assertTrue(navigation.getItems().isEmpty());

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Assert.assertEquals(NavigationImpl.ROOT_NAVIGATION_ID, navigation.getId());
        Assert.assertNull(navigation.getParentId());
        Assert.assertEquals(navigation, activeNavigation);

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(1, navigationList.size());
        Assert.assertEquals(navigation, navigationList.get(0));
    }

    @Test
    public void testNavigationCatalogRootDisabled() {
        String catalogTitle = "Catalog 1";

        initCatalogPage(false, false, false);

        NavigationItem catalogItem = mock(NavigationItem.class);
        when(catalogItem.getPath()).thenReturn(CATALOG_PAGE_PATH);
        when(catalogItem.getTitle()).thenReturn(catalogTitle);
        navigationItems.add(catalogItem);

        checkNavigationRoot(catalogTitle);
    }

    @Test
    public void testNavigationCategoriesDisabled() {
        String catalogTitle = "Catalog 1";

        initCatalogPage(true, false, false);

        NavigationItem catalogItem = mock(NavigationItem.class);
        when(catalogItem.getPath()).thenReturn(CATALOG_PAGE_PATH);
        when(catalogItem.getTitle()).thenReturn(catalogTitle);
        navigationItems.add(catalogItem);

        checkNavigationRoot(catalogTitle);
    }

    private void checkNavigationRoot(String catalogTitle) {
        Assert.assertEquals(1, navigation.getItems().size());
        Assert.assertEquals(catalogTitle, navigation.getItems().get(0).getTitle());

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Assert.assertEquals(navigation, activeNavigation);

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(1, navigationList.size());
        Assert.assertEquals(navigation, navigationList.get(0));
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

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Assert.assertEquals(navigation, activeNavigation);

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(1, navigationList.size());
        Assert.assertEquals(navigation, navigationList.get(0));
    }

    @Test
    public void testNavigationPagesHierarchy() {
        // check the properties of a navigation item related to a normal page

        String pageTitle = "Page 1";
        String pageURL = "/page1";
        boolean active = true;

        NavigationItem item = mock(NavigationItem.class);
        when(item.getTitle()).thenReturn(pageTitle);
        when(item.getURL()).thenReturn(pageURL);
        when(item.isActive()).thenReturn(active);
        navigationItems.add(item);

        String childPageTitle = "Page 1 1";
        String childPageURL = "/page1/page11";
        boolean childActive = true;

        NavigationItem childItem = mock(NavigationItem.class);
        when(childItem.getTitle()).thenReturn(childPageTitle);
        when(childItem.getURL()).thenReturn(childPageURL);
        when(childItem.isActive()).thenReturn(childActive);

        List<NavigationItem> children = new ArrayList<>();
        children.add(childItem);

        when(item.getChildren()).thenReturn(children);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);
        Assert.assertEquals(pageTitle, navigationItem.getTitle());
        Assert.assertEquals(pageURL, navigationItem.getURL());
        Assert.assertEquals(active, navigationItem.isActive());

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Assert.assertEquals(navigation, activeNavigation);

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(2, navigationList.size());
        Assert.assertEquals(navigation, navigationList.get(0));
    }

    @Test
    public void testNavigationPagesHierarchySelection() {
        // check the properties of a navigation item related to a normal page

        String pageTitle = "Page 1";
        String pageURL = "/page1";
        boolean active = true;

        NavigationItem item = mock(NavigationItem.class);
        when(item.getTitle()).thenReturn(pageTitle);
        when(item.getURL()).thenReturn(pageURL);
        when(item.isActive()).thenReturn(active);
        navigationItems.add(item);

        String childPageTitle = "Page 1 1";
        String childPageURL = "/page1/page11";
        boolean childActive = true;

        NavigationItem childItem = mock(NavigationItem.class);
        when(childItem.getTitle()).thenReturn(childPageTitle);
        when(childItem.getURL()).thenReturn(childPageURL);
        when(childItem.isActive()).thenReturn(childActive);

        List<NavigationItem> children = new ArrayList<>();
        children.add(childItem);

        when(item.getChildren()).thenReturn(children);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);
        Assert.assertEquals(pageTitle, navigationItem.getTitle());
        Assert.assertEquals(pageURL, navigationItem.getURL());
        Assert.assertEquals(active, navigationItem.isActive());

        when(request.getRequestURI()).thenReturn("/page1/page11");

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(2, navigationList.size());
        final Navigation navigation0 = navigationList.get(0);
        Assert.assertEquals(this.navigation, navigation0);
        Assert.assertTrue(navigation0.getItems().get(0).isActive());

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Navigation navigation1 = navigationList.get(1);
        Assert.assertEquals(activeNavigation, navigation1);
        Assert.assertTrue(navigation1.getItems().get(0).isActive());

        Assert.assertEquals(navigation0.getId(), navigation1.getParentId());
    }

    @Test
    public void testNavigationCategoriesOnly() {
        // check the properties of a navigation item related to a category

        String categoryId = "uid-0";
        String categoryUrlPath = "category-1";
        String categoryName = "Category 1";

        initCatalogPage(true, true, false);

        NavigationItem item = mock(NavigationItem.class);
        when(item.getPath()).thenReturn(CATALOG_PAGE_PATH);
        navigationItems.add(item);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getUid()).thenReturn(new ID(categoryId));
        when(category.getUrlPath()).thenReturn(categoryUrlPath);
        when(category.getName()).thenReturn(categoryName);
        categoryList.add(category);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);
        Assert.assertEquals(categoryName, navigationItem.getTitle());
        Assert.assertEquals(CATEGORY_PAGE_PATH + ".html/" + categoryUrlPath + ".html", navigationItem.getURL());

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Assert.assertEquals(navigation, activeNavigation);

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(1, navigationList.size());
        Assert.assertEquals(navigation, navigationList.get(0));
    }

    @Test
    public void testNavigationCategoryHierarchy() {
        // check the properties of a navigation item related to a category

        String categoryId = "uid-0";
        String categoryUrlPath = "category-1";
        String categoryName = "Category 1";

        initCatalogPage(true, true, true);

        NavigationItem item = mock(NavigationItem.class);
        when(item.getPath()).thenReturn(CATALOG_PAGE_PATH);
        navigationItems.add(item);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getUid()).thenReturn(new ID(categoryId));
        when(category.getUrlPath()).thenReturn(categoryUrlPath);
        when(category.getName()).thenReturn(categoryName);
        categoryList.add(category);

        List<CategoryTree> children = new ArrayList<>();
        String childCategoryId = "uid-1";
        String childCategoryUrlPath = "category-1-1";
        String childCategoryName = "Category 1 1";
        CategoryTree childCategory = mock(CategoryTree.class);
        when(childCategory.getUid()).thenReturn(new ID(childCategoryId));
        when(childCategory.getUrlPath()).thenReturn(childCategoryUrlPath);
        when(childCategory.getName()).thenReturn(childCategoryName);
        children.add(childCategory);

        when(category.getChildren()).thenReturn(children);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);
        Assert.assertEquals(categoryName, navigationItem.getTitle());
        Assert.assertEquals(CATEGORY_PAGE_PATH + ".html/" + categoryUrlPath + ".html", navigationItem.getURL());

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Assert.assertEquals(navigation, activeNavigation);

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(2, navigationList.size());
        Assert.assertEquals(navigation, navigationList.get(0));

        Navigation childNavigation = navigationList.get(1);
        final List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> childItems = childNavigation.getItems();
        Assert.assertEquals(1, childItems.size());

        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem childNavigationItem = childItems.get(0);
        Assert.assertEquals(childCategoryName, childNavigationItem.getTitle());
        Assert.assertEquals(CATEGORY_PAGE_PATH + ".html/" + childCategoryUrlPath + ".html", childNavigationItem.getURL());

    }

    @Test
    public void testNavigationCategoryHierarchySelection() {
        // check the properties of a navigation item related to a category

        String categoryId = "uid-0";
        String categoryUrlPath = "category-1";
        String categoryName = "Category 1";

        initCatalogPage(true, true, false);

        NavigationItem item = mock(NavigationItem.class);
        when(item.getPath()).thenReturn(CATALOG_PAGE_PATH);
        navigationItems.add(item);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getUid()).thenReturn(new ID(categoryId));
        when(category.getUrlPath()).thenReturn(categoryUrlPath);
        when(category.getName()).thenReturn(categoryName);
        categoryList.add(category);

        List<CategoryTree> children = new ArrayList<>();
        String childCategoryId = "uid-1";
        String childCategoryUrlPath = "category-1-1";
        String childCategoryName = "Category 1 1";
        CategoryTree childCategory = mock(CategoryTree.class);
        when(childCategory.getUid()).thenReturn(new ID(childCategoryId));
        when(childCategory.getUrlPath()).thenReturn(childCategoryUrlPath);
        when(childCategory.getName()).thenReturn(childCategoryName);
        children.add(childCategory);

        when(category.getChildren()).thenReturn(children);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);
        Assert.assertEquals(categoryName, navigationItem.getTitle());
        Assert.assertEquals(CATEGORY_PAGE_PATH + ".html/" + categoryUrlPath + ".html", navigationItem.getURL());

        when(request.getRequestURI()).thenReturn(CATEGORY_PAGE_PATH + ".html/" + childCategoryUrlPath + ".html");

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(2, navigationList.size());
        final Navigation navigation0 = navigationList.get(0);
        Assert.assertEquals(navigation, navigation0);

        Navigation childNavigation = navigationList.get(1);

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Assert.assertEquals(childNavigation, activeNavigation);
        Assert.assertEquals(navigation0.getId(), childNavigation.getParentId());
        final List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items0 = navigation0.getItems();
        Assert.assertEquals(1, items0.size());
        Assert.assertTrue(items0.get(0).isActive());

        final List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> childItems = childNavigation.getItems();
        Assert.assertEquals(1, childItems.size());

        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem childNavigationItem = childItems.get(0);
        Assert.assertEquals(childCategoryName, childNavigationItem.getTitle());
        Assert.assertEquals(CATEGORY_PAGE_PATH + ".html/" + childCategoryUrlPath + ".html", childNavigationItem.getURL());
        Assert.assertTrue(childNavigationItem.isActive());

    }

    @Test
    public void testNavigationPageBeforeCategory() {
        testNavigationItemOrdering(true);
    }

    @Test
    public void testNavigationCategoryBeforePage() {
        testNavigationItemOrdering(false);
    }

    private void testNavigationItemOrdering(boolean pageBeforeCategory) {
        // checks that the navigation items are ordered according to the underlying page nodes

        String pageTitle = "Page 1";
        String categoryTitle = "Category 1";

        initCatalogPage(true, true, false);

        NavigationItem pageItem = mock(NavigationItem.class);
        when(pageItem.getTitle()).thenReturn(pageTitle);

        NavigationItem catalogItem = mock(NavigationItem.class);
        when(catalogItem.getPath()).thenReturn(CATALOG_PAGE_PATH);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getName()).thenReturn(categoryTitle);
        when(category.getUid()).thenReturn(new ID("uid-0"));
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

        Navigation activeNavigation = navigationModel.getActiveNavigation();
        Assert.assertEquals(navigation, activeNavigation);

        List<Navigation> navigationList = navigationModel.getNavigationList();
        Assert.assertEquals(1, navigationList.size());
        Assert.assertEquals(navigation, navigationList.get(0));
    }

    private void initCatalogPage(boolean catalogRoot, boolean showMainCategories, boolean useCaConfig) {
        initCatalogPage(catalogRoot, showMainCategories, useCaConfig, CATALOG_PAGE_PATH, CATEGORY_PAGE_PATH);
    }

    private void initCatalogPage(boolean catalogRoot, boolean showMainCategories, boolean useCaConfig, String catalogPagePath,
        String categoryPagePath) {
        Page catalogPage = mock(Page.class);
        Resource catalogPageContent = mock(Resource.class);
        when(catalogPageContent.isResourceType(RT_CATALOG_PAGE)).thenReturn(catalogRoot);
        when(catalogPage.getPageManager()).thenReturn(pageManager);
        Map<String, Object> catalogPageProperties = new HashMap<>();
        catalogPageProperties.put(PN_SHOW_MAIN_CATEGORIES, showMainCategories);
        catalogPageProperties.put("cq:cifCategoryPage", categoryPagePath);

        if (!useCaConfig) {
            catalogPageProperties.put(SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER, 4);
        }
        when(catalogPageContent.adaptTo(ComponentsConfiguration.class)).thenReturn(new ComponentsConfiguration(new ValueMapDecorator(
            ImmutableMap.of(SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER, 4))));
        when(catalogPageContent.getValueMap()).thenReturn(new ValueMapDecorator(catalogPageProperties));
        when(catalogPage.getContentResource()).thenReturn(catalogPageContent);
        when(catalogPage.getPath()).thenReturn("/content/catalog");
        when(catalogPageContent.getPath()).thenReturn("/content/catalog/jcr:content");
        ResourceResolver mockResourceResolver = mock(ResourceResolver.class);
        when(mockResourceResolver.getResource(any(String.class))).thenReturn(null);
        when(catalogPageContent.getResourceResolver()).thenReturn(mockResourceResolver);
        when(pageManager.getPage(catalogPagePath)).thenReturn(catalogPage);

        ValueMap configProperties = new ValueMapDecorator(ImmutableMap.of(SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER, 4));

        when(catalogPage.adaptTo(ComponentsConfiguration.class)).thenReturn(new ComponentsConfiguration(configProperties));
        when(siteStructure.isCatalogPage(catalogPage)).thenReturn(Boolean.TRUE);
    }

    @Test
    public void testNavigationPageItemGetLink() {
        // Test that page-based navigation items return the link from the underlying WCM item

        String pageTitle = "Page 1";
        String pageURL = "/page1";
        boolean active = true;

        NavigationItem item = mock(NavigationItem.class);
        when(item.getTitle()).thenReturn(pageTitle);
        when(item.getURL()).thenReturn(pageURL);
        when(item.isActive()).thenReturn(active);

        // Mock the Link object
        Link mockLink = mock(Link.class);
        when(mockLink.getURL()).thenReturn(pageURL);
        when(item.getLink()).thenReturn(mockLink);

        navigationItems.add(item);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);

        // Verify that getLink() returns the link from the underlying WCM item
        Link link = navigationItem.getLink();
        Assert.assertNotNull("getLink() should return a non-null Link for page-based navigation items", link);
        Assert.assertEquals(pageURL, link.getURL());
    }

    @Test
    public void testNavigationCategoryItemGetLinkReturnsNull() {
        // Test that category-based navigation items return null for getLink()

        String categoryId = "uid-0";
        String categoryUrlPath = "category-1";
        String categoryName = "Category 1";

        initCatalogPage(true, true, false);

        NavigationItem item = mock(NavigationItem.class);
        when(item.getPath()).thenReturn(CATALOG_PAGE_PATH);
        navigationItems.add(item);

        CategoryTree category = mock(CategoryTree.class);
        when(category.getUid()).thenReturn(new ID(categoryId));
        when(category.getUrlPath()).thenReturn(categoryUrlPath);
        when(category.getName()).thenReturn(categoryName);
        categoryList.add(category);

        List<com.adobe.cq.commerce.core.components.models.navigation.NavigationItem> items = navigation.getItems();
        Assert.assertEquals(1, items.size());
        com.adobe.cq.commerce.core.components.models.navigation.NavigationItem navigationItem = items.get(0);

        // Verify that getLink() returns null for category-based navigation items
        Link link = navigationItem.getLink();
        Assert.assertNull("getLink() should return null for category-based navigation items", link);
    }
}
