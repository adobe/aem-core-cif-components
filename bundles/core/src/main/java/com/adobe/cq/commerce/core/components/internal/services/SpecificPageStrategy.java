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
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecificPageStrategy.class);
    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";
    private static final String SELECTOR_FILTER_TYPE_PROPERTY = SELECTOR_FILTER_PROPERTY + "Type";
    private static final String INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories";
    private static final String UID_AND_URL_PATH_SEPARATOR = "|";

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

    private Stream<Page> traverse(Page startPage) {
        Iterable<Page> children = startPage != null ? startPage::listChildren : Collections.emptyList();
        return StreamSupport.stream(children.spliterator(), false)
            // depth first traversal
            .flatMap(child -> Stream.concat(traverse(child), Stream.of(child)))
            // include only pages that have a filter set
            .filter(child -> Optional.ofNullable(child.getProperties())
                .filter(properties -> properties.get(SELECTOR_FILTER_PROPERTY, String[].class) != null)
                .isPresent());
    }

    public Page getSpecificPage(Page startPage, ProductUrlFormat.Params params) {
        if (StringUtils.isEmpty(params.getUrlKey())) {
            // specific product pages only support lookup by slug/url_key yet
            return null;
        }

        Iterable<Page> candidates = traverse(startPage)::iterator;
        for (Page candidate : candidates) {
            ValueMap properties = candidate.getProperties();
            String[] productUrlKeys = properties.get(SELECTOR_FILTER_PROPERTY, String[].class);
            for (String productUrlKey : productUrlKeys) {
                if (productUrlKey.equals(params.getUrlKey())) {
                    return candidate;
                }
            }
        }

        return null;
    }

    public Page getSpecificPage(Page startPage, CategoryUrlFormat.Params params) {
        boolean checkUids = StringUtils.isNotEmpty(params.getUid());
        boolean checkUrlPath = StringUtils.isNotEmpty(params.getUrlPath());
        boolean checkUrlKey = StringUtils.isNotEmpty(params.getUrlKey());

        Iterable<Page> candidates = traverse(startPage)::iterator;
        for (Page candidate : candidates) {
            ValueMap properties = candidate.getProperties();
            String[] filters = properties.get(SELECTOR_FILTER_PROPERTY, String[].class);
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

            // check for uid first
            if (checkUids && categoryUids != null) {
                for (String categoryUid : categoryUids) {
                    if (categoryUid.equals(params.getUid())) {
                        return candidate;
                    }
                }
            }

            // check for url path
            if (checkUrlPath) {
                for (String categoryUrlPath : categoryUrlPaths) {
                    if (categoryUrlPath.equals(params.getUrlPath())
                        || (includesSubCategories && StringUtils.startsWith(params.getUrlPath(), categoryUrlPath + "/"))) {
                        return candidate;
                    }
                }
            }

            // check for url key
            if (checkUrlKey) {
                for (String categoryUrlPath : categoryUrlPaths) {
                    String categoryUrlKey = StringUtils.substringAfterLast(categoryUrlPath, "/");
                    if (categoryUrlKey.equals(params.getUrlKey())) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }
}
