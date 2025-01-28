/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.io.IOException;
import java.util.Dictionary;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;

@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + BooleanRenderConditionServlet.RESOURCE_TYPE,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class BooleanRenderConditionServlet extends SlingSafeMethodsServlet {

    public static final String RESOURCE_TYPE = "core/cif/components/renderconditions/dispatcherConfigCondition";

    private static final Logger LOGGER = LoggerFactory.getLogger(BooleanRenderConditionServlet.class);

    private boolean isdispatcherconfigured = false;

    @Reference
    private ConfigurationAdmin configAdmin;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        boolean conditionMet = checkCondition();
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(conditionMet));
    }

    private boolean checkCondition() {

        try {
            Configuration config = configAdmin.getConfiguration(
                "com.adobe.cq.commerce.core.cacheinvalidation.internal.InvalidateCacheSupport");
            Dictionary<String, Object> properties = config.getProperties();
            if (properties != null && Boolean.TRUE.equals(properties.get("enableDispatcherCacheInvalidation"))) {
                isdispatcherconfigured = true;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to check dispatcher cache configuration: {}", e.getMessage(), e);
        }
        return isdispatcherconfigured;
    }
}
