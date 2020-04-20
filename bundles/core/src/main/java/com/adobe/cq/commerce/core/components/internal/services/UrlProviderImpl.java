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

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (page != null && params.containsKey(selectorFilter)) {
            Resource pageResource = page.adaptTo(Resource.class);
            boolean deepLink = !WCMMode.DISABLED.equals(WCMMode.fromRequest(request));
            if (deepLink) {
                Resource subPageResource = toSpecificPage(pageResource, params.get(selectorFilter));
                if (subPageResource != null) {
                    pageResource = subPageResource;
                }
            }

            params.put(PAGE_PARAM, pageResource.getPath());
        }

        StringSubstitutor sub = new StringSubstitutor(params);
        String url = sub.replace(template);
        return StringUtils.substringBeforeLast(url, "#${"); // remove anchor if it hasn't been substituted
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
        Iterator<Resource> children = page.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            if (!NameConstants.NT_PAGE.equals(child.getResourceType())) {
                continue;
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
            String[] selectors = filter.getClass().isArray() ? ((String[]) filter) : ArrayUtils.toArray((String) filter);

            if (ArrayUtils.contains(selectors, selector)) {
                LOGGER.debug("Page has a matching sub-page for selector {} at {}", selector, child.getPath());
                return child;
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
            return request.getRequestPathInfo().getSelectorString();
        } else if (IdentifierLocation.SUFFIX.equals(identifierLocation)) {
            return request.getRequestPathInfo().getSuffix().substring(1); // Remove leading /
        } else {
            throw new RuntimeException("Identifier location " + identifierLocation + " is not supported");
        }
    }
}
