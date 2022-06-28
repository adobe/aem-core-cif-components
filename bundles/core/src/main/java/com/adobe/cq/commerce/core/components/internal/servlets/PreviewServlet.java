/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;

@Component(
    service = Servlet.class,
    immediate = true,
    property = {
        "sling.servlet.methods=GET",
        "sling.servlet.paths=/bin/wcm/cif",
        "sling.servlet.selectors=" + PreviewServlet.PREVIEW_PRODUCT + ", " + PreviewServlet.PREVIEW_CATEGORY,
        "sling.servlet.extensions=html"
    })
public class PreviewServlet extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewServlet.class);

    private static final String PAGE_EDITOR_PATH = "editor.html";
    protected static final String PREVIEW_PRODUCT = "previewproduct";
    protected static final String PREVIEW_CATEGORY = "previewcategory";

    @Reference
    private UrlProvider urlProvider;

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        if (WCMMode.DISABLED == WCMMode.fromRequest(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "The request not permitted");
            return;
        }

        Page page = getRefererPage(request);
        if (page == null) {
            LOGGER.warn("The path of the edited page cannot be determined");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The path of the edited page cannot be determined");
            return;
        }

        // prepare all possible parameters
        Map<String, String> params = new UrlProvider.ParamsBuilder()
            .page(page.getPath())
            .uid(request.getParameter((UrlProvider.UID_PARAM)))
            .sku(request.getParameter(UrlProvider.SKU_PARAM))
            .variantSku(request.getParameter(UrlProvider.VARIANT_SKU_PARAM))
            .urlKey(request.getParameter(UrlProvider.URL_KEY_PARAM))
            .urlPath(request.getParameter(UrlProvider.URL_PATH_PARAM))
            .variantUrlKey(request.getParameter(UrlProvider.VARIANT_URL_KEY_PARAM))
            .map();

        String previewSelector = request.getRequestPathInfo().getSelectors()[0];
        String url = PREVIEW_PRODUCT.equals(previewSelector) ? urlProvider.toProductUrl(request, page, params)
            : urlProvider.toCategoryUrl(request, page, params);

        response.sendRedirect(url);
    }

    private Page getRefererPage(SlingHttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (StringUtils.isBlank(referer) || !referer.contains(PAGE_EDITOR_PATH + "/")) {
            return null;
        }

        int idx = referer.lastIndexOf(PAGE_EDITOR_PATH);
        String pagePath = referer.substring(idx + PAGE_EDITOR_PATH.length());
        Resource pageResource = request.getResourceResolver().resolve(pagePath);
        return pageResource.adaptTo(Page.class);
    }
}
