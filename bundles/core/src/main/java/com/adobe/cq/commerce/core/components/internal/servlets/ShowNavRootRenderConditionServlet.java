/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureFactory;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * {@code ShowNavRootRenderConditionServlet} implements a {@code granite:rendercondition} used to determine if the navRoot option should be
 * displayed on the properties page of a page.
 */
@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + ShowNavRootRenderConditionServlet.RESOURCE_TYPE,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class ShowNavRootRenderConditionServlet extends SlingSafeMethodsServlet {
    public final static String RESOURCE_TYPE = "core/cif/components/renderconditions/showNavRoot";

    @Reference
    private SiteStructureFactory siteStructureFactory;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
        throws ServletException, IOException {
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(showNavRoot(request)));
    }

    private boolean showNavRoot(SlingHttpServletRequest request) {
        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        if (pageManager == null) {
            return false;
        }

        String pagePath = request.getParameter("item");
        Page page = pageManager.getPage(pagePath);
        if (page == null) {
            return false;
        }

        SiteStructure siteStructure = siteStructureFactory.getSiteStructure(page);
        Page landingPage = siteStructure.getLandingPage();

        // Show navRoot option if there is no landing page or if the current page is the landing page.
        return landingPage == null || landingPage.equals(page);
    }
}
