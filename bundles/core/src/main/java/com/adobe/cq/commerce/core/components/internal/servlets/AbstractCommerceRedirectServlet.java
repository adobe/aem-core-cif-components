/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

abstract class AbstractCommerceRedirectServlet extends SlingSafeMethodsServlet {
    protected boolean verifyRequest(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String suffix = request.getRequestPathInfo().getSuffix();

        if (suffix == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing redirect suffix.");
            return false;
        }

        String suffixInfo = suffix.substring(1);
        if (suffixInfo.contains("/")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Redirect suffix has wrong format.");
            return false;
        }

        return true;
    }
}
