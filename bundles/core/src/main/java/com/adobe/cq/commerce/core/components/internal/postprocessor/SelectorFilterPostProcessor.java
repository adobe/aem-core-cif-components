/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.postprocessor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;

import static com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl.SELECTOR_FILTER_PROPERTY;
import static com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl.URL_PATH_PROPERTY;

@Component(service = { SlingPostProcessor.class })
public class SelectorFilterPostProcessor implements SlingPostProcessor {

    private static final String SLING_PROPERTY_PREFIX = "./";
    private static final String ID_AND_URL_PATH_SEPARATOR = "|";

    @Override
    public void process(SlingHttpServletRequest request, List<Modification> modifications) throws Exception {

        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        if (!requestParameterMap.containsKey(SLING_PROPERTY_PREFIX + SELECTOR_FILTER_PROPERTY)) {
            return;
        }

        Resource resource = request.getResource();
        ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);

        // We always first remove the properties to allow changes from String to String arrays (see below)
        properties.remove(SELECTOR_FILTER_PROPERTY);
        properties.remove(URL_PATH_PROPERTY);

        RequestParameter[] params = requestParameterMap.getValues(SLING_PROPERTY_PREFIX + SELECTOR_FILTER_PROPERTY);

        if (params.length == 1) {
            String selectorFilter = params[0].getString();
            if (!StringUtils.contains(selectorFilter, ID_AND_URL_PATH_SEPARATOR)) {
                return;
            }

            // There is only a single value, so we store the two properties as Strings
            properties.put(SELECTOR_FILTER_PROPERTY, StringUtils.substringBefore(selectorFilter, ID_AND_URL_PATH_SEPARATOR));
            properties.put(URL_PATH_PROPERTY, StringUtils.substringAfter(selectorFilter, ID_AND_URL_PATH_SEPARATOR));
        } else {
            // There are multiple values, so we store the two properties as String arrays
            List<String> ids = new ArrayList<>();
            List<String> urlPaths = new ArrayList<>();

            for (RequestParameter param : params) {
                String selectorFilter = param.getString();
                if (!StringUtils.contains(selectorFilter, ID_AND_URL_PATH_SEPARATOR)) {
                    continue;
                }

                ids.add(StringUtils.substringBefore(selectorFilter, ID_AND_URL_PATH_SEPARATOR));
                urlPaths.add(StringUtils.substringAfter(selectorFilter, ID_AND_URL_PATH_SEPARATOR));
            }

            properties.put(SELECTOR_FILTER_PROPERTY, ids.toArray());
            properties.put(URL_PATH_PROPERTY, urlPaths.toArray());
        }

        modifications.add(Modification.onModified(resource.getPath()));
    }
}
