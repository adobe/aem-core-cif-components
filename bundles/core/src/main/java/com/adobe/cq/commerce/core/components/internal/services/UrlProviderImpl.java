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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.dam.cfm.content.FragmentRenderService;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;

@Component(service = { UrlProvider.class, UrlProviderImpl.class })
@Designate(ocd = UrlProviderConfiguration.class)
public class UrlProviderImpl implements UrlProvider {

    public static final String CIF_IDENTIFIER_ATTR = "cif.identifier";

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlProviderImpl.class);
    private static final String SELECTOR_FILTER_PROPERTY = "selectorFilter";
    private static final String SELECTOR_FILTER_TYPE_PROPERTY = SELECTOR_FILTER_PROPERTY + "Type";
    private static final String INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories";
    private static final String UID_AND_URL_PATH_SEPARATOR = "|";

    private UrlFormat productPageUrlFormat;
    private UrlFormat categoryPageUrlFormat;

    @Activate
    public void activate(UrlProviderConfiguration conf) {
        productPageUrlFormat = UrlFormat.DEFAULT_PRODUCT_URL_FORMATS.get(conf.productPageUrlFormat());
        categoryPageUrlFormat = UrlFormat.DEFAULT_CATEGORY_URL_FORMATS.get(conf.categoryPageUrlFormat());
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return toUrl(request, page, params, productPageUrlFormat);
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, Page page, String productIdentifier) {
        ParamsBuilder params = new ParamsBuilder();
        if (StringUtils.isNotBlank(productIdentifier)) {
            params.sku(productIdentifier);

            MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);

            // for formats that require url_path or url_key we have to lookup them up
            Set<String> formatParameters = productPageUrlFormat.getParameterNames();
            if (magentoGraphqlClient != null &&
                (formatParameters.contains(URL_KEY_PARAM) || formatParameters.contains(URL_PATH_PARAM))) {
                ProductUrlParameterRetriever retriever = new ProductUrlParameterRetriever(magentoGraphqlClient);
                retriever.setIdentifier(productIdentifier);
                ProductInterface product = retriever.fetchProduct();
                if (product != null) {
                    params
                        .urlKey(product.getUrlKey())
                        .urlPath(product.getUrlPath());
                } else {
                    LOGGER.debug("Could not generate product page URL for {}.", productIdentifier);
                }
            }
        }
        return toUrl(request, page, params.map(), productPageUrlFormat);
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return toUrl(request, page, params, categoryPageUrlFormat);
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, String categoryIdentifier) {
        ParamsBuilder params = new ParamsBuilder().uid(categoryIdentifier);
        MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
        if (magentoGraphqlClient != null && StringUtils.isNotBlank(categoryIdentifier)) {
            CategoryUrlParameterRetriever retriever = new CategoryUrlParameterRetriever(magentoGraphqlClient);
            retriever.setIdentifier(categoryIdentifier);
            CategoryInterface category = retriever.fetchCategory();
            if (category != null) {
                params.urlKey(category.getUrlKey()).urlPath(category.getUrlPath());
            } else {
                LOGGER.debug("Could not generate category page URL for {}.", categoryIdentifier);
            }
        }
        return toUrl(request, page, params.map(), categoryPageUrlFormat);
    }

    private String toUrl(SlingHttpServletRequest request, Page page, Map<String, String> params, UrlFormat urlFormat) {
        if (page != null) {
            Resource pageResource = page.adaptTo(Resource.class);
            boolean deepLink = !WCMMode.DISABLED.equals(WCMMode.fromRequest(request));
            Set<String> selectorValues = new HashSet<>(params.values());

            if (deepLink) {
                Resource subPageResource = toSpecificPage(pageResource, selectorValues, request, params);
                if (subPageResource != null) {
                    pageResource = subPageResource;
                }
            }

            params.put(PAGE_PARAM, pageResource.getPath());
        }

        String url = urlFormat.format(params);
        if (url.contains(UrlFormat.OPENING_BRACKETS) || url.contains(UrlFormat.CLOSING_BRACKETS)) {
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
     * @param selectors The searched value for the <code>selectorFilter</code> property.
     * @return If found, a child page resource that contains the given <code>selectorFilter</code> value.
     *         If not found, this method returns null.
     */
    public static Resource toSpecificPage(Resource page, Set<String> selectors) {
        return toSpecificPage(page, selectors, null);
    }

    /**
     * This method checks if any of the children of the given <code>page</code> resource
     * is a page with a <code>selectorFilter</code> property set with the value
     * of the given <code>selector</code>.
     *
     * @param page The page resource, from where children pages will be checked.
     * @param selectors The searched value for the <code>selectorFilter</code> property.
     * @param request The current Sling HTTP Servlet request.
     * @return If found, a child page resource that contains the given <code>selectorFilter</code> value.
     *         If not found, this method returns null.
     */
    public static Resource toSpecificPage(Resource page, Set<String> selectors, SlingHttpServletRequest request) {
        return toSpecificPage(page, selectors, request, null);
    }

    private static Resource toSpecificPage(Resource page, Set<String> selectors, SlingHttpServletRequest request,
        Map<String, String> params) {

        ProductList productList = null;
        String currentUrlPath = null;

        Iterator<Resource> children = page.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            if (!NameConstants.NT_PAGE.equals(child.getResourceType())) {
                continue;
            }

            if (child.hasChildren()) {
                final Resource grandChild = toSpecificPage(child, selectors, request, params);
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
            Set<String> selectorFiltersSet = Arrays.asList(selectorFilters)
                .stream()
                .map(s -> ((StringUtils.equals(filterType, "uidAndUrlPath") && StringUtils.contains(s, UID_AND_URL_PATH_SEPARATOR))
                    ? StringUtils.substringAfter(s, UID_AND_URL_PATH_SEPARATOR)
                    : s))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

            if (!selectorFiltersSet.isEmpty()) {
                for (String selector : selectors) {
                    if (selectorFiltersSet.contains(selector)) {
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

                    for (String urlPath : selectorFiltersSet) {
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

    @Override
    public String getProductIdentifier(SlingHttpServletRequest request) {
        String identifier = getIdentifierFromRequest(request);
        if (identifier != null) {
            return identifier;
        }

        identifier = getIdentifierFromFragmentRenderRequest(request);
        if (identifier != null) {
            return identifier;
        }

        Map<String, String> productIdentifiers = productPageUrlFormat.parse(request.getRequestPathInfo());

        // if we get the product sku from URL no extra lookup is needed
        if (productIdentifiers.containsKey(SKU_PARAM)) {
            identifier = productIdentifiers.get(SKU_PARAM);
        } else {
            String urlKey = null;
            if (productIdentifiers.containsKey(URL_KEY_PARAM)) {
                urlKey = productIdentifiers.get(URL_KEY_PARAM);
            }

            if (StringUtils.isNotBlank(urlKey)) {
                // lookup internal product identifier (sku) based on URL product identifier (url_key)
                MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
                if (magentoGraphqlClient != null) {
                    UrlToProductRetriever productRetriever = new UrlToProductRetriever(magentoGraphqlClient);
                    productRetriever.setIdentifier(urlKey);
                    ProductInterface product = productRetriever.fetchProduct();
                    identifier = product != null ? product.getSku() : null;
                } else {
                    LOGGER.warn("No backend GraphQL client provided, cannot retrieve product identifier for {}", request.getRequestURL()
                        .toString());
                }
            }
        }

        if (identifier != null) {
            request.setAttribute(CIF_IDENTIFIER_ATTR, identifier);
        }

        return identifier;
    }

    @Override
    public String getCategoryIdentifier(SlingHttpServletRequest request) {
        String identifier = getIdentifierFromRequest(request);
        if (identifier != null) {
            return identifier;
        }

        identifier = getIdentifierFromFragmentRenderRequest(request);
        if (identifier != null) {
            return identifier;
        }

        Map<String, String> categoryIdentifiers = categoryPageUrlFormat.parse(request.getRequestPathInfo());

        if (categoryIdentifiers.containsKey(URL_KEY_PARAM)) {
            // lookup internal product identifier (sku) based on URL product identifier (url_key)
            MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
            if (magentoGraphqlClient != null) {
                UrlToCategoryRetriever categoryRetriever = new UrlToCategoryRetriever(magentoGraphqlClient);
                categoryRetriever.setIdentifier(categoryIdentifiers.get(URL_KEY_PARAM));
                CategoryInterface category = categoryRetriever.fetchCategory();
                identifier = category != null ? category.getUid().toString() : null;
            } else {
                LOGGER.warn("No backend GraphQL client provided, cannot retrieve product identifier for {}", request.getRequestURL()
                    .toString());
            }
        }

        if (identifier != null) {
            request.setAttribute(CIF_IDENTIFIER_ATTR, identifier);
        }

        return identifier;
    }

    /**
     * When the FragmentRenderService executes an internal request it passes its configuration as attribute to the internal request.
     * As this request may not be formatted in the way the UrlProvider was configured we have to pass the identifier parsed from the
     * original request as attribute in this configuration.
     *
     * @param request
     * @return
     */
    private String getIdentifierFromFragmentRenderRequest(SlingHttpServletRequest request) {
        Object fragmentRenderConfig = request.getAttribute(FragmentRenderService.class.getName() + ".config");
        if (fragmentRenderConfig instanceof ValueMap) {
            String identifier = ((ValueMap) fragmentRenderConfig).get(CIF_IDENTIFIER_ATTR, String.class);
            if (StringUtils.isNotEmpty(identifier)) {
                return identifier;
            }
        }

        return null;
    }

    /**
     * When the FragmentRenderService executes an internal request it passes its configuration as attribute to the internal request.
     * As this request may not be formatted in the way the UrlProvider was configured we have to pass the identifier parsed from the
     * original request as attribute in this configuration.
     *
     * @param request
     * @return
     */
    private String getIdentifierFromRequest(SlingHttpServletRequest request) {
        Object cachedIdentifier = request.getAttribute(CIF_IDENTIFIER_ATTR);
        if (cachedIdentifier instanceof String) {
            return (String) cachedIdentifier;
        }
        return null;
    }

    /**
     * Parses and returns the product sku or url_key used in the given Sling HTTP request based on the URLProvider configuration for product
     * page URLs.
     *
     * @param request The current Sling HTTP request.
     * @return The product sku or url_key from the URL.
     */
    public String parseProductUrlIdentifier(SlingHttpServletRequest request) {
        Map<String, String> productIdentifiers = productPageUrlFormat.parse(request.getRequestPathInfo());
        if (productIdentifiers.containsKey(SKU_PARAM)) {
            return productIdentifiers.get(SKU_PARAM);
        } else if (productIdentifiers.containsKey(URL_KEY_PARAM)) {
            return productIdentifiers.get(URL_PATH_PARAM);
        } else if (productIdentifiers.containsKey(URL_PATH_PARAM)) {
            return productIdentifiers.get(URL_PATH_PARAM);
        }
        return null;
    }

    /**
     * Parses and returns the category url_path used in the given Sling HTTP request based on the URLProvider configuration for product
     * page URLs.
     *
     * @param request The current Sling HTTP request.
     * @return The category url_path from the URL.
     */
    public String parseCategoryUrlIdentifier(SlingHttpServletRequest request) {
        Map<String, String> categoryIdentifiers = categoryPageUrlFormat.parse(request.getRequestPathInfo());
        return categoryIdentifiers.get(URL_PATH_PARAM);
    }
}
