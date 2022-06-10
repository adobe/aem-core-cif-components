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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.scripting.core.ScriptHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.CommerceComponentModelFinder;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureFactory;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
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
        "sling.filter.resource.pattern=/content(/.+)?",
        Constants.SERVICE_RANKING + ":Integer=-6000"
    })
public class CatalogPageNotFoundFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPageNotFoundFilter.class);

    @Reference
    private PageManagerFactory pageManagerFactory;
    @Reference
    private CommerceComponentModelFinder commerceModelFinder;
    @Reference
    private SiteStructureFactory siteStructureFactory;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

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
            SiteStructure siteStructure = siteStructureFactory.getSiteStructure(slingRequest, currentPage);
            boolean removeSlingScriptHelperFromBindings = false;

            if (siteStructure.isProductPage(currentPage)) {
                removeSlingScriptHelperFromBindings = addSlingScriptHelperIfNeeded(slingRequest, slingResponse);
                Product product = commerceModelFinder.findProductComponentModel(slingRequest, currentPage.getContentResource());
                if (product != null && !product.getFound()) {
                    slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
                    return;
                }
            } else if (siteStructure.isCategoryPage(currentPage)) {
                removeSlingScriptHelperFromBindings = addSlingScriptHelperIfNeeded(slingRequest, slingResponse);
                ProductList productList = commerceModelFinder.findProductListComponentModel(slingRequest, currentPage.getContentResource());
                if (productList != null) {
                    AbstractCategoryRetriever categoryRetriever = productList.getCategoryRetriever();
                    if (categoryRetriever == null || categoryRetriever.fetchCategory() == null) {
                        slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Category not found");
                        return;
                    }
                }
            }

            if (removeSlingScriptHelperFromBindings) {
                // remove the ScriptHelper if we added it before
                SlingBindings slingBindings = getSlingBindings(slingRequest);
                if (slingBindings != null) {
                    slingBindings.remove("sling");
                }
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {}

    /**
     * The {@link CommerceComponentModelFinder} uses
     * {@link org.apache.sling.models.factory.ModelFactory#getModelFromWrappedRequest(SlingHttpServletRequest, Resource, Class)}
     * to obtain the model of either {@link Product} or {@link ProductList}. That method invokes all
     * {@link org.apache.sling.scripting.api.BindingsValuesProvider}
     * while creating the wrapped request. In AEM 6.5 they are not executed lazily and depend on some existing bindings on construction of
     * which one requires the SlingScriptHelper.
     *
     * @param slingRequest
     */
    private boolean addSlingScriptHelperIfNeeded(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse) {
        SlingBindings slingBindings = getSlingBindings(slingRequest);
        if (slingBindings != null && slingBindings.getSling() == null) {
            slingBindings.put("sling", new ScriptHelper(bundleContext, null, slingRequest, slingResponse));
            return true;
        }
        return false;
    }

    private static SlingBindings getSlingBindings(SlingHttpServletRequest slingRequest) {
        Object attr = slingRequest.getAttribute(SlingBindings.class.getName());
        if (attr == null) {
            attr = new SlingBindings();
            slingRequest.setAttribute(SlingBindings.class.getName(), attr);
        }
        return attr instanceof SlingBindings ? (SlingBindings) attr : null;
    }
}
