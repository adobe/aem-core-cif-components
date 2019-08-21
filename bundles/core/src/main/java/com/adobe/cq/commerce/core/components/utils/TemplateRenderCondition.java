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

package com.adobe.cq.commerce.core.components.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;

/**
 * This class checks if the parent page of a page is configured with a given page template.
 * It is used by a <code>granite:rendercondition</code> component, in order to decide
 * if some page properties should be displayed or not.
 * 
 * Note that it checks the template of the parent page, so that it's possible to apply a
 * different template to the child page.
 */
public class TemplateRenderCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateRenderCondition.class);
    private static final String PAGE_PROPERTIES = "wcm/core/content/sites/properties";

    public static boolean isTemplate(SlingHttpServletRequest slingRequest, String templatePath) {

        if (StringUtils.isBlank(templatePath)) {
            LOGGER.error("Template path is not defined!");
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