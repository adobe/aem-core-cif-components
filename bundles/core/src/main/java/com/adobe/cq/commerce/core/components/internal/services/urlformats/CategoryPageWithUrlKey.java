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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestPathInfo;

import com.adobe.cq.commerce.core.components.internal.services.UrlFormat;
import com.google.common.collect.Sets;

import static com.adobe.cq.commerce.core.components.services.UrlProvider.PAGE_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.URL_KEY_PARAM;

public class CategoryPageWithUrlKey extends AbstractUrlFormat {
    public static final UrlFormat INSTANCE = new CategoryPageWithUrlKey();
    public static final String PATTERN = "{{page}}.html/{{url_key}}.html";

    private CategoryPageWithUrlKey() {
        super();
    }

    @Override
    public String format(Map<String, String> parameters) {
        return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
            parameters.getOrDefault(URL_KEY_PARAM, "{{" + URL_KEY_PARAM + "}}") + HTML_EXTENSION;
    }

    @Override
    public Map<String, String> parse(RequestPathInfo requestPathInfo) {
        if (requestPathInfo == null) {
            return Collections.emptyMap();
        }
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PAGE_PARAM, removeJcrContent(requestPathInfo.getResourcePath()));
        String suffix = StringUtils.removeStart(StringUtils.removeEnd(requestPathInfo.getSuffix(), HTML_EXTENSION), "/");
        if (StringUtils.isNotBlank(suffix)) {
            parameters.put(URL_KEY_PARAM, suffix);
        }
        return parameters;
    }

    @Override
    public Set<String> getParameterNames() {
        return Sets.newHashSet(PAGE_PARAM, URL_KEY_PARAM);
    }
}
