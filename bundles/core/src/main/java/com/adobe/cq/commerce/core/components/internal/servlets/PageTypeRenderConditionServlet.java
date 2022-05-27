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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
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
 * {@code PageTypeRenderConditionServlet} implements a {@code granite:rendercondition} used which evaluates to true for catalog pages,
 * custom category pages and custom product pages.
 * It requires the {@code pageType} parameter which should have one of the values: {@code catalog}, {@code category}, {@code product}.
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
    private static final String PRODUCT_PAGE_TYPE = "product";
    private static final String CATEGORY_PAGE_TYPE = "category";
    private static final String CATALOG_PAGE_TYPE = "catalog";
    private static final String LANDING_PAGE_TYPE = "landing";
    private static final Set<String> CATALOG_PAGE_RESOURCE_TYPES = new HashSet<>();
    private static final String PN_NAV_ROOT = "navRoot";

    static {
        CATALOG_PAGE_RESOURCE_TYPES.add("core/cif/components/structure/catalogpage/v1/catalogpage");
        CATALOG_PAGE_RESOURCE_TYPES.add("core/cif/components/structure/catalogpage/v3/catalogpage");
    }

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

        if (!Arrays.asList(new String[] { CATALOG_PAGE_TYPE, CATEGORY_PAGE_TYPE, PRODUCT_PAGE_TYPE, LANDING_PAGE_TYPE }).contains(
            pageType)) {
            LOGGER.error("{} property has invalid value at {}: {}", PAGE_TYPE_PROPERTY, slingRequest.getResource().getPath(), pageType);
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

        if (LANDING_PAGE_TYPE.equals(pageType)) {
            ValueMap properties = page.getContentResource().getValueMap();
            return properties.get(PN_NAV_ROOT, false);
        }

        if (CATALOG_PAGE_TYPE.equals(pageType)) {
            return page.hasContent() &&
                CATALOG_PAGE_RESOURCE_TYPES.stream().anyMatch(rt -> page.getContentResource().isResourceType(rt));
        }

        // perform the appropriate checks according to the pageType property
        if (PRODUCT_PAGE_TYPE.equals(pageType)) {
            Page productPage = SiteNavigation.getProductPage(page);
            return productPage != null && pagePath.contains(productPage.getPath());
        } else if (CATEGORY_PAGE_TYPE.equals(pageType)) {
            Page categoryPage = SiteNavigation.getCategoryPage(page);
            return categoryPage != null && pagePath.contains(categoryPage.getPath());
        }

        return false;
    }
}
