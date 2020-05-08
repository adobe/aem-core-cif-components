/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

import javax.annotation.Nonnull;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * {@code PageTypeRenderConditionServlet} implements a {@code granite:rendercondition} used to decide if the product picker
 * and category picker should be displayed in the page properties dialog of custom product pages and custom category pages.
 */
@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + PageTypeRenderConditionServlet.RESOURCE_TYPE,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class PageTypeRenderConditionServlet extends SlingSafeMethodsServlet {

    public final static String RESOURCE_TYPE = "core/cif/components/renderconditions/pagetype";

    private static final Logger LOGGER = LoggerFactory.getLogger(PageTypeRenderConditionServlet.class);
    private static final String PAGE_PROPERTIES = "wcm/core/content/sites/properties";
    private static final String PAGE_TYPE_PROPERTY = "pageType";
    private static final String PAGE_TYPE_PRODUCT = "product";
    private static final String PAGE_TYPE_CATEGORY = "category";

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        String pageType = request.getResource().getValueMap().get(PAGE_TYPE_PROPERTY, "");
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(checkPageType(request, pageType)));
    }

    private boolean checkPageType(SlingHttpServletRequest slingRequest, String pageType) {
        if (StringUtils.isBlank(pageType)) {
            LOGGER.error("{} property is not defined at {}", PAGE_TYPE_PROPERTY, slingRequest.getResource().getPath());
            return false;
        }

        // the caller is a page properties dialog
        if (!StringUtils.contains(slingRequest.getPathInfo(), PAGE_PROPERTIES)) {
            return false;
        }

        PageManager pageManager = slingRequest.getResourceResolver().adaptTo(PageManager.class);
        // the page path is in the "item" request parameter
        String pagePath = slingRequest.getParameter("item");
        Page page = pageManager.getPage(pagePath);
        if (page == null) {
            return false;
        }

        Page parentPage = page.getParent();
        if (parentPage == null) {
            return false;
        }

        // perform the appropriate checks according to the pageType property
        if (PAGE_TYPE_PRODUCT.equals(pageType)) {
            Page productPage = SiteNavigation.getProductPage(page);
            return productPage != null && productPage.getPath().equals(parentPage.getPath());
        } else if (PAGE_TYPE_CATEGORY.equals(pageType)) {
            Page categoryPage = SiteNavigation.getCategoryPage(page);
            return categoryPage != null && categoryPage.getPath().equals(parentPage.getPath());
        }

        return false;
    }
}
