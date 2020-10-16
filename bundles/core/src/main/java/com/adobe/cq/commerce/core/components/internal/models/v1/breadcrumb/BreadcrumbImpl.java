/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.models.v1.breadcrumb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ForcedResourceType;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.models.breadcrumb.Breadcrumb;
import com.adobe.cq.commerce.core.components.models.navigation.Navigation;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.CategoryIdentifierType;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ParamsBuilder;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

import static com.adobe.cq.wcm.core.components.models.Navigation.PN_STRUCTURE_DEPTH;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = com.adobe.cq.wcm.core.components.models.Breadcrumb.class,
    resourceType = BreadcrumbImpl.RESOURCE_TYPE)
public class BreadcrumbImpl extends DataLayerComponent implements Breadcrumb {

    protected static final String RESOURCE_TYPE = "core/cif/components/structure/breadcrumb/v1/breadcrumb";

    @Self
    @Via(type = ForcedResourceType.class, value = "core/wcm/components/breadcrumb/v2/breadcrumb")
    private com.adobe.cq.wcm.core.components.models.Breadcrumb breadcrumb;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @Inject
    private UrlProvider urlProvider;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable
    private Style currentStyle;

    private List<NavigationItem> items;
    private Page categoryPage;
    private Page productPage;
    private BreadcrumbRetriever retriever;
    private int structureDepth;

    @PostConstruct
    void initModel() {
        structureDepth = properties.get(PN_STRUCTURE_DEPTH, currentStyle.get(PN_STRUCTURE_DEPTH, Integer.MAX_VALUE));
    }

    @Override
    public Collection<NavigationItem> getItems() {
        // Useful for the template editor
        if (!currentPage.getPath().startsWith("/content")) {
            return Collections.emptyList();
        }

        if (items == null) {
            items = new ArrayList<>();
            Collection<NavigationItem> pageItems = breadcrumb.getItems();
            pageItems.forEach(item -> populateItems(item));
        }
        return Collections.unmodifiableList(items);
    }

    private void populateItems(NavigationItem item) {

        // We build the breadcrumb based on the production version of the page structure
        Page page = SiteNavigation.toLaunchProductionPage(item.getPage());
        Resource contentResource = page.getContentResource();

        // If we encounter the catalog page and it's configured to show the main categories, we skip that page
        if (contentResource != null && contentResource.isResourceType(Navigation.RT_CATALOG_PAGE)) {
            if (page.getContentResource().getValueMap().get(Navigation.PN_SHOW_MAIN_CATEGORIES, Boolean.TRUE)) {
                return;
            }
        }

        // For product and category pages, we fetch the breadcrumbs
        boolean isProductPage = isProductPage(page);
        boolean isCategoryPage = isCategoryPage(page);
        List<? extends CategoryInterface> categoriesBreadcrumbs = null;
        if (isProductPage) {
            categoriesBreadcrumbs = fetchProductBreadcrumbs();
        } else if (isCategoryPage) {
            categoriesBreadcrumbs = fetchCategoryBreadcrumbs();
        } else if (isSpecificPage(page)) {
            return; // it's a specific product or category page, it has already been processed by the generic product or category page
        } else {
            items.add(item);
            return; // we reached a content page
        }

        if (CollectionUtils.isEmpty(categoriesBreadcrumbs)) {
            return;
        }

        // A product can be in multiple categories so we select the "primary" category
        CategoryInterface categoryBreadcrumb = categoriesBreadcrumbs.get(0);
        if (isProductPage) {
            categoriesBreadcrumbs.sort(getCategoryInterfaceComparator());
            categoryBreadcrumb = categoriesBreadcrumbs.get(0);
        }

        // For products and categories, we display the category path in the breadcrumb
        List<com.adobe.cq.commerce.magento.graphql.Breadcrumb> breadcrumbs = categoryBreadcrumb.getBreadcrumbs();
        if (breadcrumbs != null) {
            int max = Integer.min(structureDepth, breadcrumbs.size());
            for (int i = 0; i < max; i++) {
                addBreadcrumbItem(breadcrumbs.get(i), false);
            }
        }

        // The category itself is not included by Magento in the breadcrumb, so we also add it
        addCategoryItem(categoryBreadcrumb, isCategoryPage);

        // We finally add the product if it's a product page
        if (isProductPage) {
            Pair<ProductIdentifierType, String> identifier = urlProvider.getProductIdentifier(request);
            ParamsBuilder paramsBuilder = new ParamsBuilder();
            if (ProductIdentifierType.SKU.equals(identifier.getLeft())) {
                paramsBuilder.sku(identifier.getRight());
            } else if (ProductIdentifierType.URL_KEY.equals(identifier.getLeft())) {
                paramsBuilder.urlKey(identifier.getRight());
            }

            String url = urlProvider.toProductUrl(request, productPage, paramsBuilder.map());
            NavigationItemImpl productItem = new NavigationItemImpl(retriever.fetchProductName(), url, true);
            items.add(productItem);
            return;
        }
    }

    private void addBreadcrumbItem(com.adobe.cq.commerce.magento.graphql.Breadcrumb b, boolean isActive) {
        addCategoryItem(b.getCategoryId(), b.getCategoryUrlKey(), b.getCategoryUrlPath(), b.getCategoryName(), isActive);
    }

    private void addCategoryItem(CategoryInterface category, boolean isActive) {
        addCategoryItem(category.getId(), category.getUrlKey(), category.getUrlPath(), category.getName(), isActive);
    }

    private void addCategoryItem(Integer id, String urlKey, String urlPath, String name, boolean isActive) {
        Map<String, String> params = new ParamsBuilder()
            .id(id.toString())
            .urlKey(urlKey)
            .urlPath(urlPath)
            .map();

        String url = urlProvider.toCategoryUrl(request, categoryPage, params);
        NavigationItemImpl categoryItem = new NavigationItemImpl(name, url, isActive);
        items.add(categoryItem);
    }

    /**
     * Returns the categories that exceed the maximum depth first.
     * This will be reversed later, see below.
     */
    private Function<CategoryInterface, Integer> structureDepthKey = c -> c.getUrlPath().split("/").length > structureDepth ? -1 : 1;

    /**
     * Orders the categories with deepest url path first.
     * This will be reversed later, see below.
     */
    private Function<CategoryInterface, Integer> depthKey = c -> c.getUrlPath().split("/").length;

    /**
     * Orders the categories with smallest id first.
     */
    private Function<CategoryInterface, Integer> idKey = c -> c.getId();

    @Override
    public Comparator<CategoryInterface> getCategoryInterfaceComparator() {
        return Comparator
            .comparing(structureDepthKey)
            .thenComparing(depthKey)
            .reversed()
            .thenComparing(idKey);
    }

    private List<? extends CategoryInterface> fetchProductBreadcrumbs() {
        Pair<ProductIdentifierType, String> identifier = urlProvider.getProductIdentifier(request);
        if (StringUtils.isEmpty(identifier.getRight())) {
            return null;
        }

        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage);
        if (magentoGraphqlClient == null) {
            return null;
        }

        retriever = new BreadcrumbRetriever(magentoGraphqlClient);
        retriever.setProductIdentifier(identifier.getLeft(), identifier.getRight());

        return retriever.fetchCategoriesBreadcrumbs();
    }

    private List<? extends CategoryInterface> fetchCategoryBreadcrumbs() {
        Pair<CategoryIdentifierType, String> identifier = urlProvider.getCategoryIdentifier(request);
        if (StringUtils.isEmpty(identifier.getRight())) {
            return null;
        }

        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage);
        if (magentoGraphqlClient == null) {
            return null;
        }

        retriever = new BreadcrumbRetriever(magentoGraphqlClient);
        retriever.setCategoryIdentifier(identifier.getLeft(), identifier.getRight());

        return retriever.fetchCategoriesBreadcrumbs();
    }

    private boolean isProductPage(Page page) {
        if (productPage == null) {
            productPage = SiteNavigation.getProductPage(currentPage);
        }
        // The product page might be in a Launch so we use 'endsWith' to compare the paths, for example
        // - product page: /content/launches/2020/09/15/mylaunch/content/venia/us/en/products/category-page
        // - current page: /content/venia/us/en/products/category-page
        return productPage != null ? productPage.getPath().endsWith(page.getPath()) : false;
    }

    private boolean isCategoryPage(Page page) {
        if (categoryPage == null) {
            categoryPage = SiteNavigation.getCategoryPage(currentPage);
        }
        // See comment above
        return categoryPage != null ? categoryPage.getPath().endsWith(page.getPath()) : false;
    }

    private boolean isSpecificPage(Page page) {
        // The product or category page might be in a Launch so we first extract the paths of the production versions
        String productPagePath = productPage.getPath().substring(productPage.getPath().lastIndexOf("/content/"));
        String categoryPagePath = categoryPage.getPath().substring(categoryPage.getPath().lastIndexOf("/content/"));

        String path = page.getPath();
        return (path.contains(productPagePath + "/") || path.contains(categoryPagePath + "/"));
    }
}
