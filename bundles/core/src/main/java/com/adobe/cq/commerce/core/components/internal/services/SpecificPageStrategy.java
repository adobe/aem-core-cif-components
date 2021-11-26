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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;

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

    /**
     * This method checks if any of the children of the given <code>page</code> resource is a page with a <code>selectorFilter</code>
     * property set with the value of the given <code>selector</code>.
     *
     * @param page The page resource, from where children pages will be checked.
     * @param selectors The searched value for the <code>selectorFilter</code> property.
     * @param request The current Sling HTTP Servlet request.
     * @param params The map of parameters relevant to create a url
     * @return If found, a child page resource that contains the given <code>selectorFilter</code> value. If not found, this method returns
     *         null.
     */
    public Resource getSpecificPage(Resource page, Set<String> selectors, SlingHttpServletRequest request, Map<String, String> params) {
        ProductList productList = null;
        String currentUrlPath = null;
        Iterable<Resource> children = page != null ? page.getChildren() : Collections.emptyList();

        for (Resource child : children) {
            if (!NameConstants.NT_PAGE.equals(child.getResourceType())) {
                continue;
            }

            if (child.hasChildren()) {
                final Resource grandChild = getSpecificPage(child, selectors, request, params);
                if (grandChild != null) {
                    return grandChild;
                }
            }

            Resource jcrContent = child.getChild(JcrConstants.JCR_CONTENT);
            if (jcrContent == null) {
                continue;
            }

            Object filter = jcrContent.getValueMap().get(SELECTOR_FILTER_PROPERTY);
            if (filter == null) {
                continue;
            }

            // get the filterType property set by the picker
            String filterType = jcrContent.getValueMap().get(SELECTOR_FILTER_TYPE_PROPERTY, "uidAndUrlPath");

            // The property is saved as a String when it's a simple selection, or an array when a multi-selection is done
            String[] selectorFilters = filter.getClass().isArray() ? ((String[]) filter) : ArrayUtils.toArray((String) filter);

            // When used with the category picker and the 'uidAndUrlPath' option, the values might have a format like 'Mjg=|men/men-tops'
            // --> so we split them to first extract the category ids
            // V2 of the component uses 'urlPath' and does not requiere any processing
            Stream<String> selectorFilterStream = Arrays.stream(selectorFilters);
            if (StringUtils.equals(filterType, "uidAndUrlPath")) {
                selectorFilterStream = selectorFilterStream
                    .map(s -> StringUtils.contains(s, UID_AND_URL_PATH_SEPARATOR)
                        ? StringUtils.substringAfter(s, UID_AND_URL_PATH_SEPARATOR)
                        : s);
            }

            Set<String> selectorFilterSet = selectorFilterStream.filter(StringUtils::isNotEmpty).collect(Collectors.toSet());

            if (!selectorFilterSet.isEmpty()) {
                for (String selector : selectors) {
                    if (selectorFilterSet.contains(selector)) {
                        LOGGER.debug("Page has a matching sub-page for selector {} at {}", selector, child.getPath());
                        return child;
                    }
                }

                boolean includesSubCategories = jcrContent.getValueMap().get(INCLUDES_SUBCATEGORIES_PROPERTY, false);
                if (includesSubCategories) {
                    // The currentUrlPath being processed is either coming from:
                    // 1) the ProductList model when a category page is being rendered
                    // 2) the params map when the any model renders a category link

                    if (currentUrlPath == null) {
                        if (params != null && params.containsKey(UrlProvider.URL_PATH_PARAM)) {
                            currentUrlPath = params.get(UrlProvider.URL_PATH_PARAM);
                        } else if (request != null && productList == null) {
                            productList = request.adaptTo(ProductList.class);
                            if (productList instanceof ProductListImpl) {
                                currentUrlPath = ((ProductListImpl) productList).getUrlPath();
                            }
                        }
                    }

                    for (String urlPath : selectorFilterSet) {
                        if (StringUtils.startsWith(currentUrlPath, urlPath + "/")) {
                            LOGGER.debug("Page has a matching sub-page for url_path {} at {}", urlPath, child.getPath());
                            return child;
                        }
                    }
                }
            }
        }

        return null;
    }
}
