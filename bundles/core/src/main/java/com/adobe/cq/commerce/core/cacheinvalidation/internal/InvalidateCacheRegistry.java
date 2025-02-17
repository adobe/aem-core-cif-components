/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.adobe.cq.commerce.core.cacheinvalidation.internal.spi.CacheInvalidationStrategy;
import com.adobe.cq.commerce.core.cacheinvalidation.internal.spi.DispatcherCacheInvalidationStrategy;
import com.day.cq.wcm.api.Page;

@Component(service = InvalidateCacheRegistry.class, immediate = true)
public class InvalidateCacheRegistry {

    private final Map<String, CacheInvalidationStrategy> invalidateCacheList = new HashMap<>();

    @Reference(
        service = CacheInvalidationStrategy.class,
        bind = "bindInvalidateCache",
        unbind = "unbindInvalidateCache",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY)
    void bindInvalidateCache(CacheInvalidationStrategy invalidateCache, Map<String, Object> properties) {
        String attribute = (String) properties.get(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER);
        invalidateCacheList.put(attribute, invalidateCache);
    }

    void unbindInvalidateCache(Map<String, Object> properties) {
        unbindCache(properties);
    }

    @Reference(
        service = DispatcherCacheInvalidationStrategy.class,
        bind = "bindInvalidateDispatcherCache",
        unbind = "unbindInvalidateDispatcherCache",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY)
    void bindInvalidateDispatcherCache(DispatcherCacheInvalidationStrategy invalidateDispatcherCache, Map<String, Object> properties) {
        String attribute = (String) properties.get(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER);
        invalidateCacheList.put(attribute, invalidateDispatcherCache);
    }

    void unbindInvalidateDispatcherCache(Map<String, Object> properties) {
        unbindCache(properties);
    }

    private void unbindCache(Map<String, Object> properties) {
        String attribute = (String) properties.get(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER);
        invalidateCacheList.remove(attribute);
    }

    public String getPattern(String attribute) {
        CacheInvalidationStrategy invalidateCache = invalidateCacheList.get(attribute);
        return invalidateCache != null ? invalidateCache.getPattern() : null;
    }

    public String getQuery(String attribute, String storePath, String dataList) {
        CacheInvalidationStrategy invalidateCache = invalidateCacheList.get(attribute);
        if (invalidateCache instanceof DispatcherCacheInvalidationStrategy) {
            return ((DispatcherCacheInvalidationStrategy) invalidateCache).getQuery(storePath, dataList);
        }
        return null;
    }

    public String getGraphqlQuery(String attribute, String[] data) {
        CacheInvalidationStrategy invalidateCache = invalidateCacheList.get(attribute);
        if (invalidateCache instanceof DispatcherCacheInvalidationStrategy) {
            return ((DispatcherCacheInvalidationStrategy) invalidateCache).getGraphqlQuery(data);
        }
        return null;
    }

    public String[] getPathsToInvalidate(String attribute, Page page, ResourceResolver resourceResolver, Map<String, Object> data,
        String storePath) {
        CacheInvalidationStrategy invalidateCache = invalidateCacheList.get(attribute);
        if (invalidateCache instanceof DispatcherCacheInvalidationStrategy) {
            return ((DispatcherCacheInvalidationStrategy) invalidateCache).getPathsToInvalidate(page, resourceResolver, data, storePath);
        }
        return new String[0];
    }

    public CacheInvalidationStrategy get(String attribute) {
        return invalidateCacheList.get(attribute);
    }

    public Set<String> getAttributes() {
        return invalidateCacheList.keySet();
    }
}
