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

import java.util.Map;

import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;

import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;

@Deprecated
public class ProductPageUrlFormatAdapter implements ProductUrlFormat {

    private final UrlFormat delegate;

    public ProductPageUrlFormatAdapter(UrlFormat urlFormat) {
        this.delegate = urlFormat;
    }

    @Override
    public String format(Params parameters) {
        return delegate.format(parameters.asMap());
    }

    @Override
    public Params parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap) {
        return new Params(delegate.parse(requestPathInfo, parameterMap));
    }

    @Override
    public ProductUrlFormat.Params retainParsableParameters(ProductUrlFormat.Params parameters) {
        Map<String, String> map = parameters.asMap();
        map.keySet().retainAll(delegate.getParameterNames());
        return new ProductUrlFormat.Params(map);
    }
}
