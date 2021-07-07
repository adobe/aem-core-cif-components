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

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;

public abstract class AbstractCommerceRedirectServlet extends SlingSafeMethodsServlet {

    protected Page productPage;
    protected Page categoryPage;

    @Reference
    protected UrlProvider urlProvider;

    @Reference
    protected PageManagerFactory pageManagerFactory;

    protected boolean verifyRequest(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String suffix = request.getRequestPathInfo().getSuffix();

        if (suffix == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing redirect suffix.");
            return false;
        }

        String suffixInfo = suffix.substring(1);
        if (suffixInfo.contains("/")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Redirect suffix has wrong format.");
            return false;
        }

        PageManager pageManager = pageManagerFactory.getPageManager(request.getResourceResolver());

        Page currentPage = pageManager.getContainingPage(request.getResource());
        productPage = SiteNavigation.getProductPage(currentPage);
        categoryPage = SiteNavigation.getCategoryPage(currentPage);

        return true;
    }
}
