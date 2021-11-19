/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services.urlformats;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;

import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;

public class CategoryPageWithUrlPath extends UrlFormatBase implements CategoryUrlFormat {
    public static final CategoryUrlFormat INSTANCE = new CategoryPageWithUrlPath();
    public static final String PATTERN = "{{page}}.html/{{url_path}}.html";

    private CategoryPageWithUrlPath() {
        super();
    }

    @Override
    public String format(Params parameters) {
        String urlKey = StringUtils.defaultIfEmpty(parameters.getUrlKey(), "{{url_path}}");
        return StringUtils.defaultIfEmpty(parameters.getPage(), "{{page}}")
            + HTML_EXTENSION_AND_SUFFIX
            + StringUtils.defaultIfEmpty(parameters.getUrlPath(), urlKey)
            + HTML_EXTENSION;
    }

    @Override
    public Params parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap) {
        Params params = new Params();

        if (requestPathInfo == null) {
            return params;
        }

        params.setPage(removeJcrContent(requestPathInfo.getResourcePath()));
        String suffix = StringUtils.removeStart(StringUtils.removeEnd(requestPathInfo.getSuffix(), HTML_EXTENSION), "/");
        if (StringUtils.isNotBlank(suffix)) {
            params.setUrlPath(suffix);
            params.setUrlKey(suffix.indexOf("/") > 0 ? StringUtils.substringAfterLast(suffix, "/") : suffix);
        }
        return params;
    }
}
