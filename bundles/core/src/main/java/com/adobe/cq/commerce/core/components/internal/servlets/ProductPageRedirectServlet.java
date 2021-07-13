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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;

@Component(
    service = Servlet.class,
    immediate = true,
    property = {
        ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
        ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ProductPageRedirectServlet.RESOURCE_TYPE,
        ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + ProductPageRedirectServlet.SELECTOR,
        ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + ProductPageRedirectServlet.EXTENSION
    })
public class ProductPageRedirectServlet extends AbstractCommerceRedirectServlet {
    protected static final String SELECTOR = "cifproductredirect";
    protected static final String EXTENSION = "html";
    protected static final String RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";

    @Reference
    protected UrlProvider urlProvider;

    @Reference
    protected PageManagerFactory pageManagerFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        if (verifyRequest(request, response)) {
            PageManager pageManager = pageManagerFactory.getPageManager(request.getResourceResolver());
            Page currentPage = pageManager.getContainingPage(request.getResource());
            Page productPage = SiteNavigation.getProductPage(currentPage);
            if (productPage != null) {
                String identifier = request.getRequestPathInfo().getSuffix().substring(1);
                String location = urlProvider.toProductUrl(request, productPage, identifier);
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", location);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}
