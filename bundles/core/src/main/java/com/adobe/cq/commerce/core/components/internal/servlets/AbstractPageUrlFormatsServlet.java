/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;

public abstract class AbstractPageUrlFormatsServlet extends SlingSafeMethodsServlet {
    static final String COMPONENT_NAME_KEY = "component.name";
    static final String VALUE_PN = "value";
    static final String TEXT_PN = "text";

    public DataSource getDataSource(SlingHttpServletRequest request, List<String> predefinedUrlFormats,
        List<String> customFormatNames,
        String useAsFilter) {
        List<Resource> resources = new ArrayList<>();

        ValueMap properties = new ValueMapDecorator(new HashMap<>());
        properties.put(TEXT_PN, "System default");
        properties.put(VALUE_PN, StringUtils.EMPTY);
        resources.add(new ValueMapResource(
            request.getResourceResolver(), "/" + this.getClass().getCanonicalName() + "/item",
            Resource.RESOURCE_TYPE_NON_EXISTING,
            properties));

        predefinedUrlFormats.forEach(f -> resources.add(
            buildValueMapResourceForDefaultFormats(
                f, request.getResourceResolver(), useAsFilter)));

        customFormatNames.forEach(r -> resources.add(
            buildValueMapResourceForCustomFormats(r, request.getResourceResolver(), useAsFilter)));

        return new SimpleDataSource(resources.iterator());
    }

    private Resource buildValueMapResourceForDefaultFormats(String format,
        ResourceResolver resourceResolver, String name) {
        ValueMap properties = new ValueMapDecorator(new HashMap<>());
        properties.put(TEXT_PN, format.replace("\\", ""));
        properties.put(VALUE_PN, format);
        return new ValueMapResource(resourceResolver, "/" + name + "/item",
            Resource.RESOURCE_TYPE_NON_EXISTING,
            properties);
    }

    private Resource buildValueMapResourceForCustomFormats(String formatClassName,
        ResourceResolver resourceResolver, String name) {
        ValueMap properties = new ValueMapDecorator(new HashMap<>());
        properties.put(TEXT_PN, formatClassName);
        properties.put(VALUE_PN, formatClassName);
        return new ValueMapResource(
            resourceResolver, "/" + name + "/item",
            Resource.RESOURCE_TYPE_NON_EXISTING,
            properties);
    }
}
