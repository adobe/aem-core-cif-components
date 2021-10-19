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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;

import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.google.common.collect.Sets;

import static com.adobe.cq.commerce.core.components.services.urls.UrlProvider.PAGE_PARAM;
import static com.adobe.cq.commerce.core.components.services.urls.UrlProvider.SKU_PARAM;
import static com.adobe.cq.commerce.core.components.services.urls.UrlProvider.VARIANT_SKU_PARAM;

public class ProductPageWithSku extends AbstractUrlFormat {
    public static final UrlFormat INSTANCE = new ProductPageWithSku();
    public static final String PATTERN = "{{page}}.html/{{sku}}.html#{{variant_sku}}";

    private ProductPageWithSku() {
        super();
    }

    @Override
    public String format(Map<String, String> parameters) {
        removeEmptyValues(parameters);
        return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
            parameters.getOrDefault(SKU_PARAM, "{{" + SKU_PARAM + "}}") + HTML_EXTENSION +
            (StringUtils.isNotBlank(parameters.get(VARIANT_SKU_PARAM)) ? "#" + parameters.get(VARIANT_SKU_PARAM) : "");
    }

    @Override
    public Map<String, String> parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap) {
        if (requestPathInfo == null) {
            return Collections.emptyMap();
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put(PAGE_PARAM, removeJcrContent(requestPathInfo.getResourcePath()));
        String suffix = StringUtils.removeStart(StringUtils.removeEnd(requestPathInfo.getSuffix(), HTML_EXTENSION), "/");
        if (StringUtils.isNotBlank(suffix)) {
            parameters.put(SKU_PARAM, suffix);
        }
        return parameters;
    }

    @Override
    public Set<String> getParameterNames() {
        return Sets.newHashSet(PAGE_PARAM, SKU_PARAM, VARIANT_SKU_PARAM);
    }
}
