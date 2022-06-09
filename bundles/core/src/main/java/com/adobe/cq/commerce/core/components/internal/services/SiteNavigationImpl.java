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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Pair;
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

    public static final String PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER = "magentoRootCategoryId";
    public static final String PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER_TYPE = "magentoRootCategoryIdType";

    static final String PN_CIF_CATEGORY_PAGE = "cq:cifCategoryPage";
    static final String PN_CIF_PRODUCT_PAGE = "cq:cifProductPage";

    static final String PN_CIF_SEARCH_RESULTS_PAGE = "cq:cifSearchResultsPage";

    @Override
    public Entry getEntry(Page givenPage) {
        if (givenPage == null) {
            return null;
        }

        Page navigationRoot = null;

        for (Entry entry : (Iterable<Entry>) listSearchRoots(givenPage, PN_CIF_CATEGORY_PAGE, PN_CIF_PRODUCT_PAGE)::iterator) {
            if (isEqualOrDescendant(givenPage, entry.getPage())) {
                return new EntryImpl(givenPage, entry.getCatalogPage(), entry.getNavigationRootPage());
            }

            // remember the navigationRootPage of the last entry
            navigationRoot = entry.getNavigationRootPage();
        }

        if (navigationRoot == null) {
            // if no entries were found before we may at least find a navigation root
            navigationRoot = findNavigationRoot(givenPage);
        }
        return navigationRoot != null ? new EntryImpl(givenPage, null, navigationRoot) : null;
    }

    @Override
    public boolean isCatalogPage(Page page) {
        return Optional.ofNullable(page)
            .map(Page::getContentResource)
            .map(contentResource -> contentResource.isResourceType(RT_CATALOG_PAGE) || contentResource.isResourceType(RT_CATALOG_PAGE_V3))
            .orElse(Boolean.FALSE);
    }

    @Override
    public List<Entry> getProductPages(Page page) {
        return page != null
            ? listSearchRoots(page, PN_CIF_PRODUCT_PAGE).collect(Collectors.toList())
            : Collections.emptyList();
    }

    @Override
    public boolean isProductPage(Page page) {
        return page != null && listSearchRoots(page, PN_CIF_PRODUCT_PAGE)
            .anyMatch(searchRoot -> this.isEqualOrDescendant(page, searchRoot.getPage()));
    }

    @Override
    public List<Entry> getCategoryPages(Page page) {
        return page != null
            ? listSearchRoots(page, PN_CIF_CATEGORY_PAGE).collect(Collectors.toList())
            : Collections.emptyList();
    }

    @Override
    public boolean isCategoryPage(Page page) {
        return page != null && listSearchRoots(page, PN_CIF_CATEGORY_PAGE)
            .anyMatch(searchRoot -> this.isEqualOrDescendant(page, searchRoot.getPage()));
    }

    private boolean isEqualOrDescendant(Page givenPage, Page ancestorPage) {
        String givenPagePath = givenPage.getPath().substring(givenPage.getPath().lastIndexOf("/content/"));
        String ancestorPagePath = ancestorPage.getPath().substring(ancestorPage.getPath().lastIndexOf("/content/"));
        return Text.isDescendantOrEqual(ancestorPagePath, givenPagePath);
    }

    @Override
    public Entry getNavigationRootPage(Page page) {
        Page rootPage = findNavigationRoot(page);
        if (rootPage == null && LaunchUtils.isLaunchBasedPath(page.getPath())) {
            // if in a Launch without a navigation root page, search again on the production page
            Launch launch = getLaunch(page);
            if (launch != null) {
                page = getProductionPage(page, launch);
                rootPage = findNavigationRoot(page);
            }
        }
        return rootPage != null ? new EntryImpl(rootPage, null, rootPage) : null;
    }

    @Override
    public Entry getSearchResultsPage(Page page) {
        Entry navigationRootEntry = getNavigationRootPage(page);
        if (navigationRootEntry != null) {
            Launch launch = LaunchUtils.isLaunchBasedPath(page.getPath())
                ? getLaunch(page)
                : null;
            Page searchResultsPage = resolveReference(navigationRootEntry.getPage(), launch, PN_CIF_SEARCH_RESULTS_PAGE);
            if (searchResultsPage != null) {
                return new EntryImpl(searchResultsPage,null,navigationRootEntry.getNavigationRootPage());
            }
        }
        return null;
    }

    /**
     * Return a list of candidates resolved from the references configured on, the navigation root or catalog pages that are direct
     * children of the navigation root.
     *
     * @param givenPage
     * @param referenceProperties
     * @return
     */
    private Stream<Entry> listSearchRoots(Page givenPage, String... referenceProperties) {
        Set<String> catalogPageNames = new HashSet<>();
        Launch launch = getLaunch(givenPage);
        Page productionPage = givenPage;
        Page navigationRoot = null;
        Stream<Pair<Page, Page>> catalogPagesStream = null;

        if (launch != null) {
            productionPage = getProductionPage(givenPage, launch);

            // the given page can be in a launch, which may
            // a) contain the navigation root page with all descendants (references rewritten to the launch)
            // b) contain the navigation root page without descendants (references not rewritten)
            // c) not contain the navigation root page
            Page launchNavigationRoot = findNavigationRoot(givenPage);

            // a) and b)
            if (launchNavigationRoot != null) {
                navigationRoot = launchNavigationRoot;
                Iterable<Page> catalogPages = () -> launchNavigationRoot.listChildren(this::isCatalogPage);
                catalogPagesStream = StreamSupport.stream(catalogPages.spliterator(), false)
                    .map(catalogPage -> Pair.of(catalogPage, launchNavigationRoot));
            }
        }

        // complete the list with catalog pages from the production page. the production page may not yet exist
        if (productionPage != null) {
            Page productionNavigationRoot = findNavigationRoot(productionPage);
            if (productionNavigationRoot != null) {
                if (navigationRoot == null) {
                    navigationRoot = productionNavigationRoot;
                }

                Iterable<Page> catalogPages = () -> productionNavigationRoot.listChildren(this::isCatalogPage);
                Stream<Pair<Page, Page>> stream = StreamSupport.stream(catalogPages.spliterator(), false)
                    .map(catalogPage -> Pair.of(catalogPage, productionNavigationRoot));

                catalogPagesStream = catalogPagesStream != null ? Stream.concat(catalogPagesStream, stream) : stream;
            }
        }

        if (navigationRoot == null) {
            LOG.debug("No navigation root found for: {}", givenPage.getPath());
            return Stream.empty();
        }

        return Stream.concat(catalogPagesStream, Stream.of(Pair.of((Page) null, navigationRoot)))
            .filter(pair -> {
                // distinct by catalog page name
                Page catalogPage = pair.getLeft();
                return catalogPage == null || catalogPageNames.add(catalogPage.getName());
            })
            .flatMap(pair -> resolveReferences(pair.getLeft(), pair.getRight(), launch, referenceProperties))
            .filter(Objects::nonNull);
    }

    private Stream<Entry> resolveReferences(Page catalogPage, Page navigationRootPage, Launch launch, String[] referenceProperties) {
        return Arrays.stream(referenceProperties).map(referenceProperty -> {
            Page resolvedPage = resolveReference(catalogPage != null ? catalogPage : navigationRootPage, launch, referenceProperty);
            return resolvedPage != null ? new EntryImpl(resolvedPage, catalogPage, navigationRootPage) : null;
        });
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
                reference = reference.substring(launchPath.length());
            }
        }

        return pageManager.getPage(reference);
    }

    private Page findNavigationRoot(Page page) {
        // walk up the tree using resources in order to support non-Page intermediates
        for (Resource pageResource = page.adaptTo(Resource.class); pageResource != null; pageResource = pageResource.getParent()) {
            Page currentPage = pageResource.adaptTo(Page.class);
            ValueMap properties = currentPage != null ? currentPage.getProperties() : null;
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

    private Page getProductionPage(Page launchPage, Launch launch) {
        Resource launchResource = launchPage.adaptTo(Resource.class);
        Resource productionResource = null;

        while (launchResource != null && launch.containsResource(launchResource)) {
            productionResource = LaunchUtils.getTargetResource(launchResource, null);

            if (productionResource != null) {
                break;
            } else {
                // go up the tree to include parents of the given launchPage as well. this makes it possible to get a production page of a
                // newly created page of which only an ancestor exists in the production
                launchResource = launchResource.getParent();
            }
        }

        return productionResource != null ? productionResource.adaptTo(Page.class) : null;
    }

    static class EntryImpl implements Entry {
        private final Page page;
        private final Page catalogPage;
        private final Page navigationRootPage;

        EntryImpl(Page page, Page catalogPage, Page navigationRootPage) {
            this.page = page;
            this.catalogPage = catalogPage;
            this.navigationRootPage = navigationRootPage;
        }

        @Override
        public Page getCatalogPage() {
            return catalogPage;
        }

        @Override
        public Page getNavigationRootPage() {
            return navigationRootPage;
        }

        @Override
        public Page getPage() {
            return page;
        }
    }
}
