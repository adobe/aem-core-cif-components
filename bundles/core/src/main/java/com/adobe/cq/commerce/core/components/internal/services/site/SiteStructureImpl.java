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
package com.adobe.cq.commerce.core.components.internal.services.site;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.launches.api.Launch;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.text.Text;

public class SiteStructureImpl implements SiteStructure {

    private static final Logger LOG = LoggerFactory.getLogger(SiteStructureImpl.class);

    public static final String PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER = "magentoRootCategoryId";
    public static final String PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER_TYPE = "magentoRootCategoryIdType";
    public static final String PN_CIF_CATEGORY_PAGE = "cq:cifCategoryPage";
    public static final String PN_CIF_PRODUCT_PAGE = "cq:cifProductPage";
    public static final String PN_CIF_SEARCH_RESULTS_PAGE = "cq:cifSearchResultsPage";

    private final Page currentPage;
    private final Launch launch;
    private Entry searchResultsPage;
    private List<Entry> catalogPages;
    private final Map<String, List<Entry>> genericPages = new HashMap<>(2);

    SiteStructureImpl(Page currentPage) {
        this.currentPage = currentPage;
        this.launch = getLaunch(currentPage);
    }

    @Override
    public Entry getEntry(Page givenPage) {
        if (givenPage == null) {
            return null;
        }

        Supplier<List<Entry>> productPages = this::getProductPages;
        Supplier<List<Entry>> categoryPages = this::getCategoryPages;

        for (Supplier<List<Entry>> supplier : Arrays.asList(productPages, categoryPages)) {
            for (Entry entry : supplier.get()) {
                if (isEqualOrDescendant(givenPage, entry.getPage())) {
                    return new EntryImpl(givenPage, entry.getCatalogPage(), entry.getLandingPage());
                }
            }
        }

        Entry navRoot = getLandingPage();
        if (navRoot != null && isEqualOrDescendant(givenPage, navRoot.getPage())) {
            return new EntryImpl(givenPage, null, navRoot.getPage());
        }

        return null;
    }

    @Override
    public Entry getLandingPage() {
        // getCatalogPages returns all catalog pages and the navigationRoot page, the navigation root page is the only entry that returns
        // null for getCatalogPage()
        return getCatalogPages()
            .stream()
            .filter(catalogPage -> catalogPage.getCatalogPage() == null)
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<Entry> getProductPages() {
        return Collections.unmodifiableList(getGenericPages(PN_CIF_PRODUCT_PAGE));
    }

    @Override
    public List<Entry> getCategoryPages() {
        return Collections.unmodifiableList(getGenericPages(PN_CIF_CATEGORY_PAGE));
    }

    @Override
    public boolean isCatalogPage(Page page) {
        // this only checks the page for the resource type, not if it is a descendant of the current site structure's navigation root
        // ideally we would if page is one of the catalog pages returned by getCatalogPages() but that would mean it has to be a child
        // of the site structures navigation page, which we did not enforce in the past. So that would be a breaking change.
        return Optional.ofNullable(page)
            .map(Page::getContentResource)
            .map(contentResource -> contentResource.isResourceType(RT_CATALOG_PAGE) || contentResource.isResourceType(RT_CATALOG_PAGE_V3))
            .orElse(Boolean.FALSE);
    }

    @Override
    public boolean isProductPage(Page page) {
        return page != null && getProductPages()
            .stream()
            .anyMatch(searchRoot -> this.isEqualOrDescendant(page, searchRoot.getPage()));
    }

    @Override
    public boolean isCategoryPage(Page page) {
        return page != null && getCategoryPages()
            .stream()
            .anyMatch(searchRoot -> this.isEqualOrDescendant(page, searchRoot.getPage()));
    }

    private Pair<String, String> getNormalizedPaths(Page givenPage, Page ancestorPage) {
        String givenPagePath = givenPage.getPath();
        String ancestorPagePath = ancestorPage.getPath();
        int givenPagePathLastIndexOfContent = givenPage.getPath().lastIndexOf("/content/");
        int ancestorPagePathLastIndexOfContent = ancestorPage.getPath().lastIndexOf("/content/");

        if (givenPagePathLastIndexOfContent > 0 || ancestorPagePathLastIndexOfContent > 0) {
            givenPagePath = givenPagePath.substring(givenPagePathLastIndexOfContent);
            ancestorPagePath = ancestorPagePath.substring(ancestorPagePathLastIndexOfContent);
        }

        return Pair.of(givenPagePath, ancestorPagePath);
    }

    private boolean isEqualOrDescendant(Page givenPage, Page ancestorPage) {
        Pair<String, String> paths = getNormalizedPaths(givenPage, ancestorPage);
        return Text.isDescendantOrEqual(paths.getRight(), paths.getLeft());
    }

    @Override
    public Entry getSearchResultsPage() {
        if (searchResultsPage != null) {
            return searchResultsPage;
        }

        Entry navigationRootEntry = getLandingPage();
        if (navigationRootEntry != null) {
            Page page = resolveReference(navigationRootEntry.getPage(), launch, PN_CIF_SEARCH_RESULTS_PAGE);
            if (page != null) {
                return searchResultsPage = new EntryImpl(page, null, navigationRootEntry.getLandingPage());
            }
        }
        return null;
    }

    /**
     * Return a list of candidates resolved from the references configured on, the navigation root or catalog pages that are direct
     * children of the navigation root.
     *
     * @param referenceProperty
     * @return
     */
    private List<Entry> getGenericPages(String referenceProperty) {
        return genericPages.computeIfAbsent(referenceProperty, k -> getCatalogPages().stream()
            .map(entry -> {
                Page catalogPage = entry.getCatalogPage();
                Page navigationRootPage = entry.getLandingPage();
                Page resolvedPage = resolveReference(catalogPage != null ? catalogPage : navigationRootPage, launch, referenceProperty);
                return resolvedPage != null ? new EntryImpl(resolvedPage, catalogPage, navigationRootPage) : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
    }

    private List<Entry> getCatalogPages() {
        if (catalogPages != null) {
            return catalogPages;
        }

        Set<String> catalogPageNames = new HashSet<>();
        Page productionPage = currentPage;
        Page navigationRoot = null;
        Stream<Entry> catalogPagesStream = null;

        if (launch != null) {
            // currentPage is in a Launch and may have a production page
            productionPage = getProductionPage(currentPage, launch);

            // the given page can be in a launch, which may
            // a) contain the navigation root page with all descendants (references rewritten to the launch)
            // b) contain the navigation root page without descendants (references not rewritten)
            // c) not contain the navigation root page
            Page launchNavigationRoot = findNavigationRoot(currentPage);

            // a) and b)
            if (launchNavigationRoot != null) {
                navigationRoot = launchNavigationRoot;
                Iterable<Page> catalogPages = () -> launchNavigationRoot.listChildren(this::isCatalogPage);
                catalogPagesStream = StreamSupport.stream(catalogPages.spliterator(), false)
                    .map(catalogPage -> new EntryImpl(catalogPage, catalogPage, launchNavigationRoot));
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
                Stream<Entry> stream = StreamSupport.stream(catalogPages.spliterator(), false)
                    .map(catalogPage -> new EntryImpl(catalogPage, catalogPage, productionNavigationRoot));

                catalogPagesStream = catalogPagesStream != null ? Stream.concat(catalogPagesStream, stream) : stream;
            }
        }

        if (navigationRoot != null) {
            catalogPages = Stream.concat(catalogPagesStream, Stream.of(new EntryImpl(navigationRoot, null, navigationRoot)))
                .filter(pair -> {
                    // distinct by catalog page name
                    Page catalogPage = pair.getCatalogPage();
                    return catalogPage == null || catalogPageNames.add(catalogPage.getName());
                })
                .collect(Collectors.toList());
        } else {
            LOG.debug("No navigation root found for: {}", currentPage.getPath());
            catalogPages = Collections.emptyList();
        }

        return catalogPages;
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

    private static Page findNavigationRoot(Page givenPage) {
        // walk up the tree using resources in order to support non-Page intermediates
        for (Resource pageResource = givenPage.adaptTo(Resource.class); pageResource != null; pageResource = pageResource.getParent()) {
            Page currentPage = pageResource.adaptTo(Page.class);
            ValueMap properties = currentPage != null ? currentPage.getProperties() : null;
            if (properties != null && properties.get(PN_NAV_ROOT, false)) {
                return currentPage;
            }
        }
        return null;
    }

    /**
     * Returns the {@link Launch} the given page is in, if any, otherwise {@code null}.
     *
     * @param givenPage
     * @return
     */
    private static Launch getLaunch(Page givenPage) {
        if (!LaunchUtils.isLaunchBasedPath(givenPage.getPath())) {
            LOG.trace("Not a launch path: {}", givenPage.getPath());
            return null;
        }

        Resource launchResource = LaunchUtils.getLaunchResource(givenPage.getContentResource());

        if (launchResource == null) {
            LOG.debug("Launch resource not found for path in launch: {}", givenPage.getPath());
            return null;
        }

        return launchResource.adaptTo(Launch.class);
    }

    private static Page getProductionPage(Page launchPage, Launch launch) {
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
}
