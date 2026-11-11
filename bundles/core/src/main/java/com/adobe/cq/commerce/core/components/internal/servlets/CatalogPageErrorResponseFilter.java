/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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

import org.apache.sling.engine.EngineConstants;
import org.osgi.service.component.annotations.Component;

/**
 * Reapplies Retry-After during error rendering after Sling resets the response in sendError. Only transport-error responses
 * marked by {@link CatalogPageNotFoundFilter} are affected. The marker is used instead of the servlet error-status attribute,
 * which may not be initialized until the error handler runs after this filter.
 */
@Component(
    service = Filter.class,
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_ERROR })
public class CatalogPageErrorResponseFilter implements Filter {

    static final String RETRY_AFTER_ATTRIBUTE = CatalogPageErrorResponseFilter.class.getName() + ".retryAfterSeconds";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        Object retryAfter = request.getAttribute(RETRY_AFTER_ATTRIBUTE);
        if (response instanceof HttpServletResponse && retryAfter instanceof Integer && (Integer) retryAfter > 0) {
            request.removeAttribute(RETRY_AFTER_ATTRIBUTE);
            ((HttpServletResponse) response).setIntHeader("Retry-After", (Integer) retryAfter);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
