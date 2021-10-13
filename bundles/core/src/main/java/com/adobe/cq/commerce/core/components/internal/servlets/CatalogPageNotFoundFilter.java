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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.EngineConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.CommerceComponentModelFinder;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;

@Component(
    service = Filter.class,
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
        // since 6.5 / Sling Engine Impl 2.7
        "sling.filter.resource.pattern=/content(/.+)?"
    })
public class CatalogPageNotFoundFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPageNotFoundFilter.class);

    @Reference
    private PageManagerFactory pageManagerFactory;
    @Reference
    private CommerceComponentModelFinder commerceModelFinder;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        if (!(servletRequest instanceof SlingHttpServletRequest) || !(servletResponse instanceof SlingHttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) servletRequest;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) servletResponse;
        PageManager pageManager = pageManagerFactory.getPageManager(slingRequest.getResourceResolver());
        Page currentPage = pageManager.getContainingPage(slingRequest.getResource());

        LOGGER.debug("Check if content on catalog page exists: {}", slingRequest.getRequestURI());

        if (currentPage != null) {
            if (SiteNavigation.isProductPage(currentPage)) {
                Product product = commerceModelFinder.findProduct(slingRequest, currentPage.getContentResource());
                if (product != null && !product.getFound()) {
                    slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
                    return;
                }
            } else if (SiteNavigation.isCategoryPage(currentPage)) {
                ProductList productList = commerceModelFinder.findProductList(slingRequest, currentPage.getContentResource());
                if (productList != null) {
                    AbstractCategoryRetriever categoryRetriever = productList.getCategoryRetriever();
                    if (categoryRetriever == null || categoryRetriever.fetchCategory() == null) {
                        slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Category not found");
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {}
}
