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

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
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
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.CommerceComponentModelFinder;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureFactory;
import com.adobe.cq.commerce.core.components.internal.utils.GraphqlErrors;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.api.WCMMode;

/**
 * Validates product and category page rendering requests, including catalog {@code .model.json} requests and specific-page forwards.
 * Direct content-tree reads and non-catalog pages are excluded.
 *
 * <p>
 * With {@link CatalogPageNotFoundFilterConfiguration#serviceUnavailableOnBackendError()} enabled, transport failures are handled only
 * when {@link WCMMode#fromRequest(javax.servlet.ServletRequest)} is {@link WCMMode#DISABLED}, including author requests with
 * {@code wcmmode=disabled}. Author Edit and Preview WCM modes retain their existing behavior. The filter's service ranking is
 * {@code -6000}; it consumes the WCM mode initialized by upstream AEM request processing.
 *
 * <p>
 * A transport error has a category exactly equal to
 * {@link com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient#RUNTIME_ERROR_CATEGORY}. This includes connection/socket
 * timeouts, I/O errors, non-200 backend HTTP responses (including 429 and 5xx), and an open GraphQL client circuit breaker.
 *
 * <table>
 * <caption>Catalog status decisions in disabled WCM mode with backend error handling enabled</caption>
 * <tr>
 * <th>Query outcome</th>
 * <th>HTTP status</th>
 * </tr>
 * <tr>
 * <td>Product or category lookup fails with a transport error</td>
 * <td>503</td>
 * </tr>
 * <tr>
 * <td>Category exists but its products query fails with a transport error</td>
 * <td>503</td>
 * </tr>
 * <tr>
 * <td>Filter-metadata query alone fails with a transport error, even if products succeed</td>
 * <td>503</td>
 * </tr>
 * <tr>
 * <td>Genuinely missing product or category without errors</td>
 * <td>404</td>
 * </tr>
 * <tr>
 * <td>GraphQL-only errors or successful results (including empty products)</td>
 * <td>Unchanged</td>
 * </tr>
 * </table>
 *
 * <p>
 * GraphQL-only pagination errors such as {@code page=999} retain the previous category status (200). Unavailable or uninitialized error
 * lists fall back to the previous status behavior. A 503 response contains no backend details.
 */
@Component(
    service = Filter.class,
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_FORWARD,
        // handle the catalog page in REQUEST scope
        EngineConstants.SLING_FILTER_RESOURCETYPES + "=" + NameConstants.NT_PAGE,
        // handle SpecificPageFilterFactory forwards to the specific page content in FORWARD scope
        EngineConstants.SLING_FILTER_RESOURCETYPES + "="
            + com.adobe.cq.commerce.core.components.internal.models.v1.page.PageImpl.RESOURCE_TYPE,
        EngineConstants.SLING_FILTER_RESOURCETYPES + "="
            + com.adobe.cq.commerce.core.components.internal.models.v2.page.PageImpl.RESOURCE_TYPE,
        EngineConstants.SLING_FILTER_RESOURCETYPES + "="
            + com.adobe.cq.commerce.core.components.internal.models.v3.page.PageImpl.RESOURCE_TYPE,
        // limit to typical content rendering requests
        EngineConstants.SLING_FILTER_EXTENSIONS + "=html",
        EngineConstants.SLING_FILTER_EXTENSIONS + "=json",
        // since 6.5 / Sling Engine Impl 2.7
        "sling.filter.resource.pattern=/content(/.+)?",
        Constants.SERVICE_RANKING + ":Integer=-6000"
    })
@Designate(ocd = CatalogPageNotFoundFilterConfiguration.class)
public class CatalogPageNotFoundFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPageNotFoundFilter.class);

    @Reference
    private PageManagerFactory pageManagerFactory;
    @Reference
    private CommerceComponentModelFinder commerceModelFinder;
    @Reference
    private SiteStructureFactory siteStructureFactory;

    private BundleContext bundleContext;
    private volatile CatalogPageNotFoundFilterConfiguration configuration;

    @Activate
    @Modified
    protected void activate(BundleContext bundleContext, CatalogPageNotFoundFilterConfiguration configuration) {
        this.bundleContext = bundleContext;
        this.configuration = configuration;
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

        // Skip direct content-tree reads: the filter validates page-level rendering requests only.
        // Forwarded requests from SpecificPageFilterFactory retain the SKU/category context and must be validated.
        String path = slingRequest.getResource().getPath();
        String jcrContentSegment = "/" + JcrConstants.JCR_CONTENT;
        boolean isJcrContentResource = path.endsWith(jcrContentSegment) || path.contains(jcrContentSegment + "/");
        boolean isSpecificPageForward = path
            .equals(slingRequest.getAttribute(SpecificPageFilterFactory.SPECIFIC_PAGE_FORWARD_ATTRIBUTE));
        if (isJcrContentResource && !isSpecificPageForward) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        PageManager pageManager = pageManagerFactory.getPageManager(slingRequest.getResourceResolver());
        Page currentPage = pageManager.getContainingPage(slingRequest.getResource());

        LOGGER.debug("Check if content on catalog page exists: {}", slingRequest.getRequestURI());

        if (currentPage != null) {
            SiteStructure siteStructure = siteStructureFactory.getSiteStructure(slingRequest, currentPage);
            boolean removeSlingScriptHelperFromBindings = false;
            CatalogPageNotFoundFilterConfiguration config = configuration;
            boolean checkBackendErrors = config.serviceUnavailableOnBackendError()
                && WCMMode.fromRequest(slingRequest) == WCMMode.DISABLED;

            if (siteStructure.isProductPage(currentPage)) {
                removeSlingScriptHelperFromBindings = addSlingScriptHelperIfNeeded(slingRequest, slingResponse);
                Product product = commerceModelFinder.findProductComponentModel(slingRequest, currentPage.getContentResource());
                if (product != null) {
                    boolean found = product.getFound();
                    if (checkBackendErrors) {
                        Error error = GraphqlErrors.getTransportError(product.getProductRetriever());
                        if (error != null) {
                            sendServiceUnavailable(slingResponse, currentPage, error, config);
                            return;
                        }
                    }
                    if (!found) {
                        slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
                        return;
                    }
                }
            } else if (siteStructure.isCategoryPage(currentPage)) {
                removeSlingScriptHelperFromBindings = addSlingScriptHelperIfNeeded(slingRequest, slingResponse);
                ProductList productList = commerceModelFinder.findProductListComponentModel(slingRequest, currentPage.getContentResource());
                if (productList != null) {
                    AbstractCategoryRetriever categoryRetriever = productList.getCategoryRetriever();
                    // since CIF-2916 the categoryRetriever is null when using the placeholder data, however the product list still
                    // returns the placeholder products and so we check additionally if the list is empty.
                    boolean notFound = (categoryRetriever == null || categoryRetriever.fetchCategory() == null)
                        && productList.getProducts().isEmpty();
                    if (checkBackendErrors) {
                        Error error = GraphqlErrors.getTransportError(categoryRetriever);
                        if (error == null) {
                            // Force the lazy search, including when the category itself exists.
                            error = GraphqlErrors.getTransportError(productList.getSearchResultsSet());
                        }
                        if (error != null) {
                            sendServiceUnavailable(slingResponse, currentPage, error, config);
                            return;
                        }
                    }
                    if (notFound) {
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

    private static void sendServiceUnavailable(SlingHttpServletResponse response, Page page, Error error,
        CatalogPageNotFoundFilterConfiguration config) throws IOException {
        LOGGER.warn("Commerce backend unavailable for catalog page {}: {}", page.getPath(),
            StringUtils.replaceChars(error.getMessage(), "\r\n", "  "));
        if (config.retryAfterSeconds() > 0) {
            response.setIntHeader("Retry-After", config.retryAfterSeconds());
        }
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

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
