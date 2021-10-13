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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Collection;
import java.util.Collections;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.lang.annotations.Nullable;

/**
 * This service allows to traverse a {@link Resource} tree looking for a {@link Resource} of a set of particular resource types and if
 * found adapting them to given adapter type. This helps for example finding the product component on the page and return the Product model
 * from it.
 */
@Component
public class CommerceModelFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceModelFinder.class);

    @Reference
    private ModelFactory modelFactory;

    @Nullable
    public <T> T find(SlingHttpServletRequest request, String resourceType, Class<T> adapterType) {
        return find(request, Collections.singletonList(resourceType), adapterType);
    }

    @Nullable
    public <T> T find(SlingHttpServletRequest request, Collection<String> resourceTypes, Class<T> adapterType) {
        return find(request, request.getResource(), resourceTypes, adapterType);
    }

    @Nullable
    public <T> T find(SlingHttpServletRequest request, Resource root, String resourceType, Class<T> adapterType) {
        return find(request, root, Collections.singletonList(resourceType), adapterType);
    }

    @Nullable
    public <T> T find(SlingHttpServletRequest request, Resource root, Collection<String> resourceTypes, Class<T> adapterType) {
        Resource componentResource = findChildResourceWithType(root, resourceTypes);
        if (componentResource != null) {
            return modelFactory.getModelFromWrappedRequest(request, componentResource, adapterType);
        } else {
            return null;
        }
    }

    private Resource findChildResourceWithType(Resource fromResource, Collection<String> resourceTypes) {
        LOGGER.debug("Looking for child resource type '{}' from {}", resourceTypes, fromResource.getPath());

        for (Resource child : fromResource.getChildren()) {
            for (String resourceType : resourceTypes) {
                if (child.isResourceType(resourceType)) {
                    LOGGER.debug("Found child resource type '{}' at {}", resourceType, child.getPath());
                    return child;
                }
            }

            Resource resource = findChildResourceWithType(child, resourceTypes);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }
}
