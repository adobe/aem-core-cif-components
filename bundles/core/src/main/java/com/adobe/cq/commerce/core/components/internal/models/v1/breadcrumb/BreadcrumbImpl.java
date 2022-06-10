/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.breadcrumb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
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
import org.apache.sling.models.annotations.via.ForcedResourceType;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.UrlFormatBase;
import com.adobe.cq.commerce.core.components.models.breadcrumb.Breadcrumb;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.navigation.Navigation;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Style;
import com.shopify.graphql.support.ID;

import static com.adobe.cq.wcm.core.components.models.Navigation.PN_STRUCTURE_DEPTH;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = com.adobe.cq.wcm.core.components.models.Breadcrumb.class,
    resourceType = BreadcrumbImpl.RESOURCE_TYPE)
public class BreadcrumbImpl extends DataLayerComponent implements Breadcrumb {

    protected static final String RESOURCE_TYPE = "core/cif/components/structure/breadcrumb/v1/breadcrumb";

    private static String PAGE_PLACEHOLDER = UrlFormat.OPENING_BRACKETS + UrlProvider.PAGE_PARAM + UrlFormat.CLOSING_BRACKETS;

    @Self
    @Via(type = ForcedResourceType.class, value = "core/wcm/components/breadcrumb/v2/breadcrumb")
    private com.adobe.cq.wcm.core.components.models.Breadcrumb breadcrumb;

    @Self
    private SlingHttpServletRequest request;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @OSGiService
    private UrlProvider urlProvider;

    @Self
    private SiteStructure siteStructure;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable
    private Style currentStyle;

    private List<NavigationItem> items;
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
            if (magentoGraphqlClient != null) {
                Collection<NavigationItem> pageItems = breadcrumb.getItems();
                for (NavigationItem item : pageItems) {
                    if (!populateItems(item)) {
                        break;
                    }
                }
            }
        }
        return Collections.unmodifiableList(items);
    }

    /**
     * Populates the breadcrumb items with the given item. If the item
     * a) is a content page it is kept as is
     * b) if it is the catalog page that is configured to show the categories, it is skipped
     * c) if it is a product or category page the respective breadcrumb is added
     *
     * @param item
     * @return true if more original items should be considered for the breadcrumb, otherwise false
     */
    private boolean populateItems(NavigationItem item) {
        Page page = item.getPage();
        Resource contentResource;

        // We build the breadcrumb based on the production version of the page structure
        if (page != null && LaunchUtils.isLaunchBasedPath(page.getPath())) {
            PageManager pageManager = page.getPageManager();
            contentResource = LaunchUtils.getTargetResource(page.getContentResource(), null);
            page = pageManager.getContainingPage(contentResource);
        }

        contentResource = page != null ? page.getContentResource() : null;

        // If we encounter the catalog page and it's configured to show the main categories, we skip that page
        if (siteStructure.isCatalogPage(page)) {
            if (contentResource.getValueMap().get(Navigation.PN_SHOW_MAIN_CATEGORIES, Boolean.TRUE)) {
                return true;
            }
        }

        // For product and category pages, we fetch the breadcrumbs
        boolean isProductPage = siteStructure.isProductPage(page);
        boolean isCategoryPage = siteStructure.isCategoryPage(page);
        List<? extends CategoryInterface> categoriesBreadcrumbs = null;
        String productSku = null;
        if (isProductPage) {
            productSku = urlProvider.getProductIdentifier(request);
            if (StringUtils.isEmpty(productSku)) {
                return false;
            }
            ProductUrlFormat.Params urlParams = urlProvider.parseProductUrlFormatParameters(request);
            categoriesBreadcrumbs = fetchProductBreadcrumbs(productSku, urlParams, magentoGraphqlClient);
        } else if (isCategoryPage) {
            String categoryUid = urlProvider.getCategoryIdentifier(request);
            if (StringUtils.isEmpty(categoryUid)) {
                return false;
            }
            categoriesBreadcrumbs = fetchCategoryBreadcrumbs(categoryUid, magentoGraphqlClient);
        } else {
            items.add(item);
            return true; // we reached a content page
        }

        if (CollectionUtils.isEmpty(categoriesBreadcrumbs)) {
            return false;
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
        if (isProductPage && StringUtils.isNotBlank(productSku)) {
            String url = urlProvider.toProductUrl(request, currentPage, productSku);
            NavigationItemImpl productItem = newNavigationItem(retriever.fetchProductName(), url, true);
            items.add(productItem);
        }

        return false;
    }

    private void addBreadcrumbItem(com.adobe.cq.commerce.magento.graphql.Breadcrumb b, boolean isActive) {
        addCategoryItem(b.getCategoryUid(), b.getCategoryUrlKey(), b.getCategoryUrlPath(), b.getCategoryName(),
            isActive);
    }

    private void addCategoryItem(CategoryInterface category, boolean isActive) {
        addCategoryItem(category.getUid(), category.getUrlKey(), category.getUrlPath(), category.getName(), isActive);
    }

    private void addCategoryItem(ID uid, String urlKey, String urlPath, String name, boolean isActive) {
        // if the dynamic category page is null, the category item is not rendered
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUid(uid.toString());
        params.setUrlKey(urlKey);
        params.setUrlPath(urlPath);
        String url = urlProvider.toCategoryUrl(request, currentPage, params);
        // if there is no category page, the url will contain the placeholder {{page}}
        if (!url.contains(PAGE_PLACEHOLDER)) {
            NavigationItemImpl categoryItem = newNavigationItem(name, url, isActive);
            items.add(categoryItem);
        }
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

    private NavigationItemImpl newNavigationItem(String name, String url, boolean isActive) {
        return new NavigationItemImpl(name, url, isActive, this.getId(), currentPage.getContentResource());
    }

    @Override
    public Comparator<CategoryInterface> getCategoryInterfaceComparator() {
        return Comparator
            .comparing(structureDepthKey)
            .thenComparing(depthKey)
            .reversed();
    }

    private List<? extends CategoryInterface> fetchProductBreadcrumbs(String productSku, ProductUrlFormat.Params urlParams,
        MagentoGraphqlClient magentoGraphqlClient) {
        retriever = new BreadcrumbRetriever(magentoGraphqlClient);
        retriever.setProductIdentifier(productSku);

        List<? extends CategoryInterface> categories = retriever.fetchCategoriesBreadcrumbs();
        List<String> alternatives = categories.stream().map(CategoryInterface::getUrlPath).collect(Collectors.toList());
        String contextUrlPath = UrlFormatBase.selectUrlPath(null, alternatives, null, urlParams.getCategoryUrlParams().getUrlKey(),
            urlParams.getCategoryUrlParams().getUrlPath());

        // include only categories that are ancestors or descendants of the contextUrlPath
        return categories.stream()
            .filter(category -> contextUrlPath.startsWith(category.getUrlPath() + "/") || contextUrlPath.equals(category.getUrlPath()))
            .collect(Collectors.toList());
    }

    private List<? extends CategoryInterface> fetchCategoryBreadcrumbs(String categoryUid, MagentoGraphqlClient magentoGraphqlClient) {
        retriever = new BreadcrumbRetriever(magentoGraphqlClient);
        retriever.setCategoryIdentifier(categoryUid);

        return retriever.fetchCategoriesBreadcrumbs();
    }
}
