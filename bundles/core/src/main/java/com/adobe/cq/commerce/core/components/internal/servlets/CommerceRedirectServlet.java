/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.IdentifierLocation;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;

@Component(
    service = Servlet.class,
    immediate = true,
    property = {
        ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
        ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + CommerceRedirectServlet.RESOURCE_TYPE,
        ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + CommerceRedirectServlet.PRODUCT_SELECTOR + ","
            + CommerceRedirectServlet.CATEGORY_SELECTOR,
        ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + CommerceRedirectServlet.EXTENSION
    })
public class CommerceRedirectServlet extends SlingSafeMethodsServlet {

    protected static final String PRODUCT_SELECTOR = "cifproductredirect";
    protected static final String CATEGORY_SELECTOR = "cifcategoryredirect";
    protected static final String EXTENSION = "html";
    protected static final String RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";

    private Pair<IdentifierLocation, ProductIdentifierType> productIdentifierConfig;
    private Page productPage;
    private Page categoryPage;

    @Reference
    private UrlProvider urlProvider;

    @Reference
    PageManagerFactory pageManagerFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String suffix = request.getRequestPathInfo().getSuffix();
        String[] selectors = request.getRequestPathInfo().getSelectors();

        if (suffix == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing redirect suffix.");
            return;
        }

        String[] suffixInfo = suffix.substring(1).split("/");
        if (suffixInfo.length != 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Redirect suffix has wrong format.");
            return;
        }

        PageManager pageManager = pageManagerFactory.getPageManager(request.getResourceResolver());

        Page currentPage = pageManager.getContainingPage(request.getResource());
        productPage = SiteNavigation.getProductPage(currentPage);
        categoryPage = SiteNavigation.getCategoryPage(currentPage);

        if (PRODUCT_SELECTOR.equals(selectors[0])) {
            redirectToProductPage(request, response, suffixInfo[0]);
        } else if (CATEGORY_SELECTOR.equals(selectors[0])) {
            redirectToCategoryPage(request, response, suffixInfo[0]);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "The requested redirect is not available.");
        }
    }

    private void redirectToProductPage(SlingHttpServletRequest request, SlingHttpServletResponse response, String sku) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", urlProvider.toProductUrl(request, productPage, sku));
    }

    private void redirectToCategoryPage(SlingHttpServletRequest request, SlingHttpServletResponse response, String categoryUid) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", urlProvider.toCategoryUrl(request, categoryPage, categoryUid));
    }
}
