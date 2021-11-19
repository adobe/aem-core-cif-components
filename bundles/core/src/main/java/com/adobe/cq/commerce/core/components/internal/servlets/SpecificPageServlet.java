/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.day.cq.wcm.api.WCMMode;

@Component(
    service = Servlet.class,
    immediate = true,
    property = {
        ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
        ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "="
            + com.adobe.cq.commerce.core.components.internal.models.v1.page.PageImpl.RESOURCE_TYPE,
        ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "="
            + com.adobe.cq.commerce.core.components.internal.models.v2.page.PageImpl.RESOURCE_TYPE,
        ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=html",
        ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + SpecificPageServlet.SELECTOR
    })
public class SpecificPageServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecificPageServlet.class);

    protected static final String SELECTOR = "cifpage";

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        // We get two selectors like "cifpage.sku-1" (the 2nd selector is the commerce identifier)
        String[] selectors = request.getRequestPathInfo().getSelectors();

        Resource page = request.getResource();
        WCMMode wcmMode = WCMMode.fromRequest(request);

        if (WCMMode.DISABLED.equals(wcmMode)) {
            LOGGER.debug("Checking sub-pages for {} {}", request.getRequestURI(), page.getPath());
            Resource subPage = UrlProviderImpl.toSpecificPage(page.getParent(), new HashSet<String>(Arrays.asList(selectors[1])), request);
            if (subPage != null) {
                page = subPage;
            }
        }

        // This removes the "cifpage" selector so that the forwarded request is no longer handled by this Servlet
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors(selectors[1]);

        RequestDispatcher dispatcher = request.getRequestDispatcher(page, options);
        dispatcher.forward(request, response);
    }
}
