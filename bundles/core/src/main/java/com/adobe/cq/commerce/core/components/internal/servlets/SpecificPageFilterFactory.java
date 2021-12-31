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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
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
    @Reference
    private SpecificPageStrategy specificPageStrategy;
    @Reference
    private PageManagerFactory pageManagerFactory;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        // Skip filter if deep linking is enabled
        if (specificPageStrategy.isGenerateSpecificPageUrlsEnabled()) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        // if not find the specific page for the url parameters
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) servletRequest;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) servletResponse;
        PageManager pageManager = pageManagerFactory.getPageManager(slingRequest.getResourceResolver());
        Page currentPage = pageManager.getContainingPage(slingRequest.getResource());
        Page specificPage = null;

        if (currentPage != null) {
            if (SiteNavigation.isProductPage(currentPage)) {
                ProductUrlFormat.Params params = urlProvider.parseProductUrlIdentifier(slingRequest);
                specificPage = specificPageStrategy.getSpecificPage(currentPage, params);
            } else if (SiteNavigation.isCategoryPage(currentPage)) {
                CategoryUrlFormat.Params params = urlProvider.parseCategoryUrlIdentifier(slingRequest);
                specificPage = specificPageStrategy.getSpecificPage(currentPage,params);
            }
        }

        if (specificPage == null) {
            LOGGER.debug("No specific page found for: {}", slingRequest.getRequestURI());
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        RequestDispatcher dispatcher = slingRequest.getRequestDispatcher(specificPage.getContentResource());
        dispatcher.forward(slingRequest, slingResponse);
    }

    @Override
    public void destroy() {}
}
