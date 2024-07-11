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

import java.util.*;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImpl;
import com.adobe.cq.commerce.core.components.internal.models.v2.productlist.ProductListImpl;
import com.adobe.cq.commerce.core.components.models.RetrievingModel;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.services.ModelMappingProvider;
import com.drew.lang.annotations.Nullable;

/**
 * This service allows to traverse a {@link Resource} tree looking for a {@link Resource} of a set of particular resource types and if
 * found adapting them to given adapter type. This helps for example finding the product component on the page and return the Product model
 * from it.
 */
@Component(service = CommerceComponentModelFinder.class)
public class CommerceComponentModelFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceComponentModelFinder.class);
    private static final Collection<String> PRODUCT_RTS = Collections.singleton(ProductImpl.RESOURCE_TYPE);
    private static final Collection<String> PRODUCT_LIST_RTS = Arrays.asList(
        ProductListImpl.RESOURCE_TYPE,
        com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl.RESOURCE_TYPE);

    @Reference
    private ModelFactory modelFactory;

    @Reference
    private final List<ModelMappingProvider> modelProviders = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Class<?>> modelMap = Collections.synchronizedMap(new HashMap<>());

    @Reference(
        service = ModelMappingProvider.class,
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        unbind = "removeModelProvider")
    protected void addModelProvider(ModelMappingProvider provider) {
        if (provider instanceof DefaultModelMappingProvider) {
            modelProviders.add(0, provider);
        } else {
            modelProviders.add(provider);
        }
        updateModelMap();
    }

    protected void removeModelProvider(ModelMappingProvider provider) {
        modelProviders.remove(provider);
        updateModelMap();
    }

    private void updateModelMap() {
        modelMap.clear();
        for (ModelMappingProvider provider : modelProviders) {
            modelMap.putAll(provider.getModels());
        }
    }

    @Nullable
    public Product findProductComponentModel(SlingHttpServletRequest request) {
        return findComponentModel(request, PRODUCT_RTS, Product.class);
    }

    @Nullable
    public Product findProductComponentModel(SlingHttpServletRequest request, Resource root) {
        return findComponentModel(request, root, PRODUCT_RTS, Product.class);
    }

    @Nullable
    public ProductList findProductListComponentModel(SlingHttpServletRequest request) {
        return findComponentModel(request, PRODUCT_LIST_RTS, ProductList.class);
    }

    @Nullable
    public ProductList findProductListComponentModel(SlingHttpServletRequest request, Resource root) {
        return findComponentModel(request, root, PRODUCT_LIST_RTS, ProductList.class);
    }

    @Nullable
    public <T> T findComponentModel(SlingHttpServletRequest request, String resourceType, Class<T> adapterType) {
        return findComponentModel(request, Collections.singletonList(resourceType), adapterType);
    }

    @Nullable
    public <T> T findComponentModel(SlingHttpServletRequest request, Collection<String> resourceTypes, Class<T> adapterType) {
        return findComponentModel(request, request.getResource(), resourceTypes, adapterType);
    }

    @Nullable
    public <T> T findComponentModel(SlingHttpServletRequest request, Resource root, String resourceType, Class<T> adapterType) {
        return findComponentModel(request, root, Collections.singletonList(resourceType), adapterType);
    }

    @Nullable
    public <T> T findComponentModel(SlingHttpServletRequest request, Resource root, Collection<String> resourceTypes,
        Class<T> adapterType) {
        Resource componentResource = findChildResourceWithType(root, resourceTypes);
        if (componentResource != null) {
            return modelFactory.getModelFromWrappedRequest(request, componentResource, adapterType);
        } else {
            return null;
        }
    }

    private Resource findChildResourceWithType(Resource fromResource, Collection<String> resourceTypes) {
        if (fromResource == null) {
            return null;
        }

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

    public Collection<RetrievingModel> findModels(SlingHttpServletRequest request, Resource resource) {
        if (resource == null || modelMap.isEmpty()) {
            return Collections.emptyList();
        }

        LOGGER.trace("Looking for model of type '{}' at {}", modelMap, resource.getPath());
        Class<?> type = modelMap.get(resource.getResourceType());
        if (type == null) {
            for (Map.Entry<String, Class<?>> typeInfo : modelMap.entrySet()) {
                if (resource.isResourceType(typeInfo.getKey())) {
                    LOGGER.debug("Found model of type '{}' at {}", typeInfo, resource.getPath());
                    type = typeInfo.getValue();
                }
            }
        }

        ArrayList<RetrievingModel> models = new ArrayList<>();
        if (type != null) {
            Object model = modelFactory.getModelFromWrappedRequest(request, resource, type);
            if (model instanceof RetrievingModel) {
                LOGGER.debug("Created model of type '{}' at {}", type, resource.getPath());
                models.add((RetrievingModel) model);
            }
        }

        for (Resource child : resource.getChildren()) {
            models.addAll(findModels(request, child));
        }

        return models;
    }
}
