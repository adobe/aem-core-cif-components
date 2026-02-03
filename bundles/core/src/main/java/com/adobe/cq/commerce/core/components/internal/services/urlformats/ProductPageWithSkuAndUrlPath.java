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

import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;

import static com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlPath.extractCategoryUrlFormatParams;

public class ProductPageWithSkuAndUrlPath extends UrlFormatBase implements ProductUrlFormat {
    public static final ProductUrlFormat INSTANCE = new ProductPageWithSkuAndUrlPath();
    public static final String PATTERN = "{{page}}.html/{{sku}}/{{url_path}}.html#{{variant_sku}}";

    private ProductPageWithSkuAndUrlPath() {
        super();
    }

    @Override
    public String format(Params parameters) {
        String urlPath = selectUrlPath(parameters);
        return StringUtils.defaultIfEmpty(parameters.getPage(), "{{page}}")
            + HTML_EXTENSION_AND_SUFFIX
            + StringUtils.defaultIfEmpty(parameters.getSku(), "{{sku}}")
            + (urlPath != null ? "/" + urlPath + HTML_EXTENSION : HTML_EXTENSION)
            + getOptionalAnchor(parameters.getVariantSku());
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
            if (suffix.indexOf("/") > 0) {
                params.setSku(StringUtils.substringBefore(suffix, "/"));
                String urlPath = StringUtils.substringAfter(suffix, "/");
                int lastSlash = urlPath.lastIndexOf("/");

                if (lastSlash > 0) {
                    params.setUrlKey(urlPath.substring(lastSlash + 1));
                    String[] categoryParams = extractCategoryUrlFormatParams(urlPath);
                    params.getCategoryUrlParams().setUrlKey(categoryParams[0]);
                    params.getCategoryUrlParams().setUrlPath(categoryParams[1]);
                } else {
                    params.setUrlKey(urlPath);
                }
                params.setUrlPath(urlPath);
            } else {
                params.setSku(suffix);
            }
        }
        return params;
    }

    @Override
    public Params retainParsableParameters(Params parameters) {
        String urlKey = getUrlKey(parameters);
        String urlPath = selectUrlPath(parameters);
        String[] categoryParams = extractCategoryUrlFormatParams(urlPath);

        Params copy = new Params();
        copy.setPage(parameters.getPage());
        copy.setSku(parameters.getSku());
        copy.setUrlKey(urlKey);
        copy.setUrlPath(urlPath);
        copy.getCategoryUrlParams().setUrlKey(categoryParams[0]);
        copy.getCategoryUrlParams().setUrlPath(categoryParams[1]);

        return copy;
    }
}
