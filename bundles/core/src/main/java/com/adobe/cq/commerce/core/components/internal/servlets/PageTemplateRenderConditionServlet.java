/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;

/**
 * This servlet handles <code>granite:rendercondition</code> requests from the CIF page component,
 * in order to decide if the product and category pickers should be displayed in the page properties dialog.
 *
 * @deprecated
 */
@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + PageTemplateRenderConditionServlet.RESOURCE_TYPE,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
@Deprecated
public class PageTemplateRenderConditionServlet extends SlingSafeMethodsServlet {

    public final static String RESOURCE_TYPE = "core/cif/components/renderconditions/pagetemplate";

    private static final Logger LOGGER = LoggerFactory.getLogger(PageTemplateRenderConditionServlet.class);
    private static final String PAGE_PROPERTIES = "wcm/core/content/sites/properties";
    private static final String TEMPLATE_PATH_PROPERTY = "templatePath";

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        String path = request.getResource().getValueMap().get(TEMPLATE_PATH_PROPERTY, "");
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(isTemplate(request, path)));
    }

    private static boolean isTemplate(SlingHttpServletRequest slingRequest, String templatePath) {

        if (StringUtils.isBlank(templatePath)) {
            LOGGER.error("{} property is not defined at {}", TEMPLATE_PATH_PROPERTY, slingRequest.getResource().getPath());
            return false;
        }

        // The dialog is a page properties dialog
        if (StringUtils.contains(slingRequest.getPathInfo(), PAGE_PROPERTIES)) {

            // The page URL is in the request "item" parameter
            String pagePath = slingRequest.getParameter("item");

            Resource pageResource = slingRequest.getResourceResolver().getResource(pagePath);
            if (!pageResource.isResourceType(NameConstants.NT_PAGE)) {
                return false;
            }

            // Get the parent page, and check the template of the parent page
            Resource parentPageResource = pageResource.getParent();
            if (!parentPageResource.isResourceType(NameConstants.NT_PAGE)) {
                return false;
            }

            Resource jcrContent = parentPageResource.getChild(JcrConstants.JCR_CONTENT);
            if (jcrContent == null) {
                return false;
            }

            // Get page template path and compare with the expected path
            return templatePath.equals(jcrContent.getValueMap().get(NameConstants.NN_TEMPLATE, String.class));
        }

        return false;
    }
}
