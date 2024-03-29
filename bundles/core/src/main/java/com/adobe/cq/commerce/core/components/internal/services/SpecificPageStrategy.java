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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureImpl;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.UrlFormatBase;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.day.cq.wcm.api.Page;

/**
 * This Component is used by the {@link UrlProviderImpl} to get a specific page for a given product page. If it is not enabled the
 * {@link UrlProviderImpl} will not create links to specific pages.
 */
@Component(service = SpecificPageStrategy.class)
@Designate(ocd = SpecificPageStrategy.Configuration.class)
public class SpecificPageStrategy {

    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";
    private static final String SELECTOR_FILTER_TYPE_PROPERTY = SELECTOR_FILTER_PROPERTY + "Type";
    private static final String INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories";
    private static final String UID_AND_URL_PATH_SEPARATOR = "|";
    /**
     * Same as {@link SpecificPageStrategy#SELECTOR_FILTER_PROPERTY} but used for product pages
     **/
    private static final String PN_USE_FOR_CATEGORIES = "useForCategories";

    @ObjectClassDefinition(name = "CIF URL Provider Specific Page Strategy")
    public @interface Configuration {

        @AttributeDefinition(
            name = "Generate Specific Page URLs",
            description = "If enabled the CIF Url Provider will look up specific "
                + "pages and return the path to it if available. If disabled, the CIF Url Provider will return the generic product page path "
                + "in any case. Defaults to disabled")
        boolean generateSpecificPageUrls() default false;
    }

    private boolean generateSpecificPageUrls;

    @Activate
    public void activate(Configuration configuration) {
        generateSpecificPageUrls = configuration.generateSpecificPageUrls();
    }

    /**
     * Returns true when the strategy is configured to generate specific page URLs.
     *
     * @return
     */
    public boolean isGenerateSpecificPageUrlsEnabled() {
        return generateSpecificPageUrls;
    }

    public Page getGenericPage(SiteStructure siteStructure, ProductUrlFormat.Params params) {
        return getGenericPage(siteStructure.getProductPages(), cp -> this.isSpecificCatalogPageFor(cp, params));
    }

    public Page getGenericPage(SiteStructure siteStructure, CategoryUrlFormat.Params params) {
        return getGenericPage(siteStructure.getCategoryPages(), cp -> this.isSpecificCatalogPageFor(cp, params));
    }

    private Page getGenericPage(List<SiteStructure.Entry> searchRoots, Predicate<Page> catalogPageCheck) {
        for (SiteStructure.Entry searchRoot : searchRoots) {
            // To prevent traversing all search roots for every page, we consider the search root only if
            // a) it is not a specific page itself (no filters set nor inherited from a catalog page), or
            // b) it is a specific page for the given parameters
            boolean isGenericSearchRoot = !isSpecificCatalogPage(searchRoot.getCatalogPage());
            if (isGenericSearchRoot || catalogPageCheck.test(searchRoot.getCatalogPage())) {
                return searchRoot.getPage();
            }
        }
        return null;
    }

    private Stream<Page> traverse(Page page) {
        Iterable<Page> children = page != null ? page::listChildren : Collections.emptyList();
        return StreamSupport.stream(children.spliterator(), false)
            // depth first traversal
            .flatMap(child -> Stream.concat(traverse(child), Stream.of(child)));
    }

    private Stream<Page> findSpecificPages(Page page) {
        return traverse(page).filter(this::isSpecificPage);
    }

    public Page getSpecificPage(Page page, ProductUrlFormat.Params params) {
        return findSpecificPages(page)
            .filter(candidate -> isSpecificPageFor(candidate, params))
            .findFirst()
            .orElse(null);
    }

    private boolean isSpecificPage(Page candidate) {
        // include only pages that have a filter set
        ValueMap candidateProperties = candidate.getProperties();
        return candidateProperties.get(SELECTOR_FILTER_PROPERTY, String[].class) != null
            || candidateProperties.get(PN_USE_FOR_CATEGORIES, String[].class) != null;
    }

    private boolean isSpecificPageFor(Page candidate, ProductUrlFormat.Params params) {
        CategoryUrlFormat.Params categoryParams = params.getCategoryUrlParams();
        boolean checkCategoryUrlPath = StringUtils.isNotEmpty(categoryParams.getUrlPath());
        boolean checkCategoryUrlKey = StringUtils.isNotEmpty(categoryParams.getUrlKey());
        ValueMap properties = candidate.getProperties();
        String[] productUrlKeys = properties.get(SELECTOR_FILTER_PROPERTY, new String[0]);

        for (String productUrlKey : productUrlKeys) {
            if (productUrlKey.equals(params.getUrlKey())
                || productUrlKey.equals(params.getSku())) {
                return true;
            }
        }

        if (!checkCategoryUrlKey && !checkCategoryUrlPath) {
            return false;
        }

        String[] categoryUrlPaths = properties.get(PN_USE_FOR_CATEGORIES, new String[0]);
        boolean includesSubCategories = properties.get(INCLUDES_SUBCATEGORIES_PROPERTY, false);

        if (checkCategoryUrlPath) {
            for (String categoryUrlPath : categoryUrlPaths) {
                if (matchesUrlPath(categoryParams.getUrlPath(), categoryUrlPath, includesSubCategories)) {
                    return true;
                }
            }
        }

        if (checkCategoryUrlKey) {
            for (String categoryUrlPath : categoryUrlPaths) {
                if (matchesUrlKey(categoryParams.getUrlKey(), categoryUrlPath)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Page getSpecificPage(Page page, CategoryUrlFormat.Params params) {
        return findSpecificPages(page)
            .filter(candidate -> isSpecificPageFor(candidate, params))
            .findFirst()
            .orElse(null);
    }

    private boolean isSpecificPageFor(Page candidate, CategoryUrlFormat.Params params) {
        // check for uids only as fallback when there is no url_path and url_key
        boolean checkUids = StringUtils.isNotEmpty(params.getUid());
        boolean checkUrlPath = StringUtils.isNotEmpty(params.getUrlPath());
        boolean checkUrlKey = StringUtils.isNotEmpty(params.getUrlKey());

        ValueMap properties = candidate.getProperties();
        String[] filters = properties.get(SELECTOR_FILTER_PROPERTY, new String[0]);
        String filterType = properties.get(SELECTOR_FILTER_TYPE_PROPERTY, "uidAndUrlPath");
        boolean includesSubCategories = properties.get(INCLUDES_SUBCATEGORIES_PROPERTY, false);

        List<String> categoryUrlPaths;
        List<String> categoryUids;

        if (filterType.equals("uidAndUrlPath")) {
            categoryUrlPaths = new ArrayList<>(filters.length);
            categoryUids = checkUids ? new ArrayList<>(filters.length) : null;
            // When used with the category picker and the 'uidAndUrlPath' option, the values might have a format like
            // 'Mjg=|men/men-tops'.
            // if there is no uid in the params we ignore the uid

            for (String filter : filters) {
                if (StringUtils.isNotEmpty(filter) && !StringUtils.contains(filter, UID_AND_URL_PATH_SEPARATOR)) {
                    // weird data, should not happen but is used in tests
                    // consider the filter to be both
                    categoryUrlPaths.add(filter);
                    if (checkUids) {
                        categoryUids.add(filter);
                    }
                    continue;
                }

                if (checkUids) {
                    String uid = StringUtils.substringBefore(filter, UID_AND_URL_PATH_SEPARATOR);
                    if (StringUtils.isNotEmpty(uid)) {
                        categoryUids.add(uid);
                    }
                }

                String urlPath = StringUtils.substringAfter(filter, UID_AND_URL_PATH_SEPARATOR);
                if (StringUtils.isNotEmpty(urlPath)) {
                    categoryUrlPaths.add(urlPath);
                }
            }
        } else {
            categoryUrlPaths = Arrays.asList(filters);
            categoryUids = null;
        }

        // check for url path
        if (checkUrlPath) {
            for (String categoryUrlPath : categoryUrlPaths) {
                if (matchesUrlPath(params.getUrlPath(), categoryUrlPath, includesSubCategories)) {
                    return true;
                }
            }
        }

        // check for url key
        if (checkUrlKey) {
            for (String categoryUrlPath : categoryUrlPaths) {
                if (matchesUrlKey(params.getUrlKey(), categoryUrlPath)) {
                    return true;
                }
            }
        }

        // check for uid last, as fallback
        if (checkUids && categoryUids != null) {
            for (String categoryUid : categoryUids) {
                if (categoryUid.equals(params.getUid())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isSpecificCatalogPage(Page catalogPage) {
        if (catalogPage == null) {
            return false;
        }

        String identifier = catalogPage.getProperties().get(SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER, String.class);
        String identifierType = catalogPage.getProperties().get(SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER_TYPE, String.class);

        return StringUtils.isNotEmpty(identifier) && "urlPath".equals(identifierType);
    }

    private boolean isSpecificCatalogPageFor(Page catalogPage, ProductUrlFormat.Params params) {
        String contextUrlPath = UrlFormatBase.selectUrlPath(params);
        String contextUrlKey = UrlFormatBase.getUrlKey(params);
        return isSpecificCatalogPageFor(catalogPage, contextUrlKey, contextUrlPath);
    }

    private boolean isSpecificCatalogPageFor(Page catalogPage, CategoryUrlFormat.Params params) {
        return isSpecificCatalogPageFor(catalogPage, params.getUrlKey(), params.getUrlPath());
    }

    private boolean isSpecificCatalogPageFor(Page catalogPage, String urlKey, String urlPath) {
        if (catalogPage == null) {
            return false;
        }

        String identifier = catalogPage.getProperties().get(SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER, String.class);
        String identifierType = catalogPage.getProperties().get(SiteStructureImpl.PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER_TYPE, String.class);

        if (StringUtils.isEmpty(identifier) || !"urlPath".equals(identifierType)) {
            // cannot match when the identifier is persisted as uid
            return false;
        }

        return matchesUrlPath(urlPath, identifier, true)
            || matchesUrlKey(urlKey, identifier);
    }

    private static boolean matchesUrlKey(String givenUrlKey, String categoryUrlPath) {
        String categoryUrlKey = StringUtils.substringAfterLast(categoryUrlPath, "/");
        return categoryUrlPath.equals(givenUrlKey) || categoryUrlKey.equals(givenUrlKey);
    }

    private static boolean matchesUrlPath(String givenUrlPath, String categoryUrlPath, boolean includeSubCategories) {
        return categoryUrlPath.equals(givenUrlPath)
            || (includeSubCategories && StringUtils.startsWith(givenUrlPath, categoryUrlPath + "/"));
    }
}
