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

package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;

@Component(service = UrlProvider.class, immediate = true)
@Designate(ocd = UrlProviderConfiguration.class)
public class UrlProviderImpl implements UrlProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlProviderImpl.class);

    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";
    private static final String INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories";
    private static final String ID_AND_URL_PATH_SEPARATOR = "|";

    private String productUrlTemplate;
    private Pair<IdentifierLocation, ProductIdentifierType> productIdentifierConfig;

    private String categoryUrlTemplate;
    private Pair<IdentifierLocation, CategoryIdentifierType> categoryIdentifierConfig;

    @Activate
    public void activate(UrlProviderConfiguration conf) {
        productUrlTemplate = conf.productUrlTemplate();
        productIdentifierConfig = Pair.of(conf.productIdentifierLocation(), conf.productIdentifierType());

        categoryUrlTemplate = conf.categoryUrlTemplate();
        categoryIdentifierConfig = Pair.of(conf.categoryIdentifierLocation(), conf.categoryIdentifierType());
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return toUrl(request, page, params, productUrlTemplate, UrlProvider.URL_KEY_PARAM);
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return toUrl(request, page, params, categoryUrlTemplate, UrlProvider.ID_PARAM);
    }

    private String toUrl(SlingHttpServletRequest request, Page page, Map<String, String> params, String template, String selectorFilter) {
        if (page != null) {
            Resource pageResource = page.adaptTo(Resource.class);
            boolean deepLink = !WCMMode.DISABLED.equals(WCMMode.fromRequest(request));
            if (deepLink && params.containsKey(selectorFilter)) {
                Resource subPageResource = toSpecificPage(pageResource, params.get(selectorFilter), request, params);
                if (subPageResource != null) {
                    pageResource = subPageResource;
                }
            }

            params.put(PAGE_PARAM, pageResource.getPath());
        }

        String prefix = "${", suffix = "}"; // variables have the format ${var}
        if (template.contains("{{")) {
            prefix = "{{";
            suffix = "}}"; // variables have the format {{var}}
        }

        StringSubstitutor sub = new StringSubstitutor(params, prefix, suffix);
        String url = sub.replace(template);
        url = StringUtils.substringBeforeLast(url, "#" + prefix); // remove anchor if it hasn't been substituted

        if (url.contains(prefix)) {
            LOGGER.warn("Missing params for URL substitution. Resulted URL: {}", url);
        }

        return url;
    }

    /**
     * This method checks if any of the children of the given <code>page</code> resource
     * is a page with a <code>selectorFilter</code> property set with the value
     * of the given <code>selector</code>.
     * 
     * @param page The page resource, from where children pages will be checked.
     * @param selector The searched value for the <code>selectorFilter</code> property.
     * @return If found, a child page resource that contains the given <code>selectorFilter</code> value.
     *         If not found, this method returns null.
     */
    public static Resource toSpecificPage(Resource page, String selector) {
        return toSpecificPage(page, selector, null);
    }

    /**
     * This method checks if any of the children of the given <code>page</code> resource
     * is a page with a <code>selectorFilter</code> property set with the value
     * of the given <code>selector</code>.
     * 
     * @param page The page resource, from where children pages will be checked.
     * @param selector The searched value for the <code>selectorFilter</code> property.
     * @param request The current Sling HTTP Servlet request.
     * @return If found, a child page resource that contains the given <code>selectorFilter</code> value.
     *         If not found, this method returns null.
     */
    public static Resource toSpecificPage(Resource page, String selector, SlingHttpServletRequest request) {
        return toSpecificPage(page, selector, request, null);
    }

    private static Resource toSpecificPage(Resource page, String selector, SlingHttpServletRequest request, Map<String, String> params) {

        ProductList productList = null;
        String currentUrlPath = null;

        Iterator<Resource> children = page.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            if (!NameConstants.NT_PAGE.equals(child.getResourceType())) {
                continue;
            }

            if (child.hasChildren()) {
                final Resource grandChild = toSpecificPage(child, selector, request, params);
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

            // The property is saved as a String when it's a simple selection, or an array when a multi-selection is done
            String[] selectorFilters = filter.getClass().isArray() ? ((String[]) filter) : ArrayUtils.toArray((String) filter);

            // When used with the category picker and the 'idAndUrlPath' option, the values might have a format like '12|men/men-tops'
            // --> so we split them to first extract the category ids
            Set<String> selectors = Arrays.asList(selectorFilters)
                .stream()
                .map(s -> StringUtils.substringBefore(s, ID_AND_URL_PATH_SEPARATOR))
                .collect(Collectors.toSet());

            if (selectors.contains(selector)) {
                LOGGER.debug("Page has a matching sub-page for selector {} at {}", selector, child.getPath());
                return child;
            }

            boolean includesSubCategories = jcrContent.getValueMap().get(INCLUDES_SUBCATEGORIES_PROPERTY, false);
            if (includesSubCategories) {

                List<String> urlPaths = Arrays.asList(selectorFilters)
                    .stream()
                    .map(s -> StringUtils.substringAfter(s, ID_AND_URL_PATH_SEPARATOR))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

                if (urlPaths.isEmpty()) {
                    continue;
                }

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

                for (String urlPath : urlPaths) {
                    if (StringUtils.startsWith(currentUrlPath, urlPath + "/")) {
                        LOGGER.debug("Page has a matching sub-page for url_path {} at {}", urlPath, child.getPath());
                        return child;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Pair<ProductIdentifierType, String> getProductIdentifier(SlingHttpServletRequest request) {
        return Pair.of(productIdentifierConfig.getRight(), parseIdentifier(productIdentifierConfig.getLeft(), request));
    }

    @Override
    public Pair<CategoryIdentifierType, String> getCategoryIdentifier(SlingHttpServletRequest request) {
        return Pair.of(categoryIdentifierConfig.getRight(), parseIdentifier(categoryIdentifierConfig.getLeft(), request));
    }

    /**
     * Returns the identifier used in the URL, based on the configuration of the UrlProvider service.
     *
     * @return The identifier.
     */
    private String parseIdentifier(IdentifierLocation identifierLocation, SlingHttpServletRequest request) {
        if (IdentifierLocation.SELECTOR.equals(identifierLocation)) {
            // In case there are multiple selectors, the id is the last like in 'productlist.lazy.1.html`
            String[] selectors = request.getRequestPathInfo().getSelectors();
            return selectors.length == 0 ? null : selectors[selectors.length - 1];
        } else if (IdentifierLocation.SUFFIX.equals(identifierLocation)) {
            return request.getRequestPathInfo().getSuffix().substring(1); // Remove leading /
        } else {
            throw new RuntimeException("Identifier location " + identifierLocation + " is not supported");
        }
    }

    static class StringSubstitutor {

        private final String[] searchList;
        private final String[] replacementList;

        public StringSubstitutor(Map<String, String> params, String prefix, String suffix) {
            replacementList = params.values().toArray(new String[0]);
            searchList = params.keySet().toArray(new String[0]);
            if (StringUtils.isNotBlank(prefix) && StringUtils.isNotBlank(suffix)) {
                for (int i = 0; i < searchList.length; ++i) {
                    searchList[i] = prefix + searchList[i] + suffix;
                }
            }
        }

        public String replace(String source) {
            return StringUtils.replaceEach(source, searchList, replacementList);
        }
    }
}
