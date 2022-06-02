/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.services.SiteNavigation;
import com.adobe.cq.launches.api.Launch;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.text.Text;

@Component(service = SiteNavigation.class)
public class SiteNavigationImpl implements SiteNavigation {

    private static final Logger LOG = LoggerFactory.getLogger(SiteNavigationImpl.class);

    static final String PN_CIF_CATEGORY_PAGE = "cq:cifCategoryPage";
    static final String PN_CIF_PRODUCT_PAGE = "cq:cifProductPage";

    @Override
    public boolean isCatalogPage(Page page) {
        return Optional.ofNullable(page)
            .map(Page::getContentResource)
            .map(contentResource -> contentResource.isResourceType(RT_CATALOG_PAGE) || contentResource.isResourceType(RT_CATALOG_PAGE_V3))
            .orElse(Boolean.FALSE);
    }

    @Override
    public List<Page> getProductPages(Page page) {
        return page != null
            ? listSearchRoots(page, PN_CIF_PRODUCT_PAGE).collect(Collectors.toList())
            : Collections.emptyList();

    }

    @Override
    public boolean isProductPage(Page page) {
        return page != null && listSearchRoots(page, PN_CIF_PRODUCT_PAGE)
            .anyMatch(searchRoot -> this.isEqualOrDescendant(page, searchRoot));
    }

    @Override
    public List<Page> getCategoryPages(Page page) {
        return page != null
            ? listSearchRoots(page, PN_CIF_CATEGORY_PAGE).collect(Collectors.toList())
            : Collections.emptyList();
    }

    @Override
    public boolean isCategoryPage(Page page) {
        return page != null && listSearchRoots(page, PN_CIF_CATEGORY_PAGE)
            .anyMatch(searchRoot -> this.isEqualOrDescendant(page, searchRoot));
    }

    private boolean isEqualOrDescendant(Page givenPage, Page ancestorPage) {
        String givenPagePath = givenPage.getPath().substring(givenPage.getPath().lastIndexOf("/content/"));
        String ancestorPagePath = ancestorPage.getPath().substring(ancestorPage.getPath().lastIndexOf("/content/"));
        return Text.isDescendantOrEqual(ancestorPagePath, givenPagePath);
    }

    /**
     * Return a list of candidates resolved from the references configured on, the navigation root or catalog pages that are direct
     * children of the navigation root.
     *
     * @param givenPage
     * @param referenceProperty
     * @return
     */
    private Stream<Page> listSearchRoots(Page givenPage, String referenceProperty) {
        Map<String, Page> catalogPages = new LinkedHashMap<>();
        Launch launch = getLaunch(givenPage);
        Page productionPage = givenPage;
        Page navigationRoot = null;

        if (launch != null) {
            productionPage = getProductionPage(givenPage);

            // the given page can be in a launch, which may
            // a) contain the navigation root page with all descendants (references rewritten to the launch)
            // b) contain the navigation root page without descendants (references not rewritten)
            // c) not contain the navigation root page
            Page launchNavigationRoot = findNavigationRoot(givenPage);

            // a) and b)
            if (launchNavigationRoot != null) {
                navigationRoot = launchNavigationRoot;
                for (Iterator<Page> children = launchNavigationRoot.listChildren(this::isCatalogPage); children.hasNext();) {
                    Page catalogPage = children.next();
                    // only consider catalog pages that are also contained in the Launch
                    if (launch.containsResource(catalogPage.adaptTo(Resource.class))) {
                        catalogPages.put(catalogPage.getName(), catalogPage);
                    }
                }
            }
        }

        // complete the list with catalog pages from the production page. the production page may not yet exist
        if (productionPage != null) {
            Page productionNavigationRoot = findNavigationRoot(productionPage);
            if (productionNavigationRoot != null) {
                if (navigationRoot == null) {
                    navigationRoot = productionNavigationRoot;
                }

                for (Iterator<Page> children = productionNavigationRoot.listChildren(this::isCatalogPage); children.hasNext();) {
                    Page catalogPage = children.next();
                    catalogPages.putIfAbsent(catalogPage.getName(), catalogPage);
                }
            }
        }

        if (navigationRoot == null) {
            LOG.debug("No navigation root found for: {}", givenPage.getPath());
            return Stream.empty();
        }

        return Stream.concat(catalogPages.values().stream(), Stream.of(navigationRoot))
            .map(catalogPage -> resolveReference(catalogPage, launch, referenceProperty))
            .filter(Objects::nonNull);
    }

    /**
     * Gets the given referenceProperty from the properties of the given Page and resolves it to a Page. If the given page is in a Launch
     * the candidate is resolved within the Launch. If it does not exist in the Launch it will be resolved from the production content.
     *
     * @param page
     * @param launch
     * @param referenceProperty
     * @return
     */
    private Page resolveReference(Page page, Launch launch, String referenceProperty) {
        String reference = page.getProperties().get(referenceProperty, String.class);

        if (reference == null) {
            LOG.debug("reference property {} not set on catalog page: {}", referenceProperty, page.getPath());
            return null;
        }

        PageManager pageManager = page.getPageManager();

        if (launch != null) {
            String launchPath = launch.getResource().getPath();

            // when the launch was created with the catalog page not including descendants the reference may not have been updated
            if (!Text.isDescendant(launchPath, reference)) {
                reference = launchPath + reference;
            }

            Page launchPage = pageManager.getPage(reference);
            if (launchPage != null) {
                // found the generic page in the launch
                return launchPage;
            } else {
                // did not find the generic page, cut the launch path from the reference and look for the production page
                reference = reference.substring(0, launchPath.length());
            }
        }

        return pageManager.getPage(reference);
    }

    private Page findNavigationRoot(Page page) {
        for (Page currentPage = page; currentPage != null; currentPage = currentPage.getParent()) {
            ValueMap properties = currentPage.getProperties();
            if (properties != null && properties.get(PN_NAV_ROOT, false)) {
                return currentPage;
            }
        }

        return null;
    }

    /**
     * Returns the {@link Launch} the given page is in, if any. Otherwise returns null.
     *
     * @param page
     * @return
     */
    private Launch getLaunch(Page page) {
        if (!LaunchUtils.isLaunchBasedPath(page.getPath())) {
            LOG.trace("Not a launch path: {}", page.getPath());
            return null;
        }

        Resource launchResource = LaunchUtils.getLaunchResource(page.getContentResource());

        if (launchResource == null) {
            LOG.debug("Launch resource not found for path in launch: {}", page.getPath());
            return null;
        }

        return launchResource.adaptTo(Launch.class);
    }

    private Page getProductionPage(Page launchPage) {
        return Optional.ofNullable(launchPage.adaptTo(Resource.class))
            .map(resource -> LaunchUtils.getTargetResource(resource, null))
            .map(productionResource -> productionResource.adaptTo(Page.class))
            .orElse(null);
    }
}
