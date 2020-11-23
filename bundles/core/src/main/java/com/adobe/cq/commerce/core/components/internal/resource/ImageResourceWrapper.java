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
package com.adobe.cq.commerce.core.components.internal.resource;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.day.cq.commons.DownloadResource;

public class ImageResourceWrapper extends ResourceWrapper {

    private ValueMap valueMap;
    private String resourceType;

    // provide the image file reference if we already have it
    public ImageResourceWrapper(Resource resource, String resourceType, String fileReference) {
        super(resource);
        if (StringUtils.isEmpty(resourceType)) {
            throw new IllegalArgumentException(
                "The " + ImageResourceWrapper.class.getName() + " needs to override the resource type of "
                    + "the wrapped resource, but the resourceType argument was null or empty.");
        }
        this.resourceType = resourceType;
        valueMap = new ValueMapDecorator(new HashMap<>(resource.getValueMap()));
        valueMap.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, resourceType);
        if (StringUtils.isNotBlank(fileReference)) {
            valueMap.put(DownloadResource.PN_REFERENCE, fileReference);
        }
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) valueMap;
        }
        return super.adaptTo(type);
    }

    @Override
    public ValueMap getValueMap() {
        return valueMap;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public boolean isResourceType(String resourceType) {
        return this.getResourceResolver().isResourceType(this, resourceType);
    }
}
