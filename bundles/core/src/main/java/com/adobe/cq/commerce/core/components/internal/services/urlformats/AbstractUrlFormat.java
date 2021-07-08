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
package com.adobe.cq.commerce.core.components.internal.services.urlformats;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceUtil;

import com.adobe.cq.commerce.core.components.internal.services.UrlFormat;

abstract class AbstractUrlFormat implements UrlFormat {
    protected static final String HTML_EXTENSION = ".html";

    protected static String removeJcrContent(String path) {
        if (path == null) {
            return null;
        } else if (JcrConstants.JCR_CONTENT.equals(ResourceUtil.getName(path))) {
            return ResourceUtil.getParent(path);
        } else {
            return path;
        }
    }
}
