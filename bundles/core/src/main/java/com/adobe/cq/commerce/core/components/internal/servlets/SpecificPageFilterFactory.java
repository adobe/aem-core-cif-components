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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.EngineConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureFactory;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;

@Component(
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        // as this is in REQUEST scope it is called for the resource before it got forwarded to jcr:content
        EngineConstants.SLING_FILTER_RESOURCETYPES + "=" + NameConstants.NT_PAGE,
        // but we also want to cover cases where the page content is requested directly
        EngineConstants.SLING_FILTER_RESOURCETYPES + "="
            + com.adobe.cq.commerce.core.components.internal.models.v1.page.PageImpl.RESOURCE_TYPE,
        EngineConstants.SLING_FILTER_RESOURCETYPES + "="
            + com.adobe.cq.commerce.core.components.internal.models.v2.page.PageImpl.RESOURCE_TYPE,
        // limit to typical content rendering requests
        EngineConstants.SLING_FILTER_EXTENSIONS + "=html",
        EngineConstants.SLING_FILTER_EXTENSIONS + "=json",
        // make sure the filter comes late but earlier then the CatalogPageNotFoundFilter
        Constants.SERVICE_RANKING + ":Integer=-5000"
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
    @Reference
    private SiteStructureFactory siteStructureFactory;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException,
        ServletException {
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
            SiteStructure siteStructure = siteStructureFactory.getSiteStructure(slingRequest, currentPage);

            if (siteStructure.isProductPage(currentPage)) {
                ProductUrlFormat.Params params = urlProvider.parseProductUrlFormatParameters(slingRequest);
                specificPage = specificPageStrategy.getSpecificPage(currentPage, params);
            } else if (siteStructure.isCategoryPage(currentPage)) {
                CategoryUrlFormat.Params params = urlProvider.parseCategoryUrlFormatParameters(slingRequest);
                specificPage = specificPageStrategy.getSpecificPage(currentPage, params);
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
