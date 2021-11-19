/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.day.cq.wcm.api.WCMMode;

@Component(
    property = {
        "sling.filter.scope=REQUEST",
        "sling.filter.methods=GET",
        "sling.filter.extensions=html",
        "service.ranking:Integer=" + Integer.MIN_VALUE
    })
@Designate(ocd = SpecificPageFilterConfiguration.class, factory = true)
public class SpecificPageFilterFactory implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecificPageFilterFactory.class);

    @Reference
    private UrlProviderImpl urlProvider;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

        // Skip filter on AEM author
        WCMMode wcmMode = WCMMode.fromRequest(request);
        if (!WCMMode.DISABLED.equals(wcmMode)) {
            chain.doFilter(request, response);
            return;
        }

        // detect the commerce identifier
        String identifier = urlProvider.parseProductUrlIdentifier(slingRequest);
        if (StringUtils.isEmpty(identifier)) {
            identifier = urlProvider.parseCategoryUrlIdentifier(slingRequest);
        }
        if (StringUtils.isEmpty(identifier)) {
            chain.doFilter(request, response);
            return;
        }

        // We add a specific selector to the request to forward the request to SpecificPageServlet
        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors(SpecificPageServlet.SELECTOR + "." + identifier);

        Resource page = slingRequest.getResource();
        LOGGER.debug("Adding {} selector for {} {}", SpecificPageServlet.SELECTOR, slingRequest.getRequestURI(), page.getPath());
        RequestDispatcher dispatcher = slingRequest.getRequestDispatcher(page, options);
        dispatcher.forward(slingRequest, response);
    }

    @Override
    public void destroy() {}
}
