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

import java.util.*;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;

@Component(service = InvalidateCacheRegistry.class, immediate = true)
public class InvalidateCacheRegistry {

    private static final String INTERNAL_PACKAGE_PREFIX = "com.adobe.cq.commerce.core.cacheinvalidation.internal";

    private final Map<String, InvalidateTypeStrategies> invalidateCacheList = new HashMap<>();

    @Reference(
        service = CacheInvalidationStrategy.class,
        bind = "bindInvalidateCache",
        unbind = "unbindInvalidateCache",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY)
    void bindInvalidateCache(CacheInvalidationStrategy invalidateCache, Map<String, Object> properties) {
        String invalidateType = invalidateCache.getInvalidationRequestType();
        if (invalidateType != null) {
            boolean isInternal = isInternalStrategy(invalidateCache);
            InvalidateTypeStrategies strategies = invalidateCacheList.computeIfAbsent(invalidateType, k -> new InvalidateTypeStrategies());
            strategies.addStrategy(new StrategyInfo(invalidateCache, properties, isInternal));
        }
    }

    void unbindInvalidateCache(CacheInvalidationStrategy invalidateCache, Map<String, Object> properties) {
        String invalidateType = invalidateCache.getInvalidationRequestType();
        if (invalidateType != null) {
            InvalidateTypeStrategies strategies = invalidateCacheList.get(invalidateType);
            if (strategies != null) {
                String componentName = (String) properties.get("component.name");
                if (componentName != null) {
                    strategies.removeStrategy(componentName);
                }
            }
        }
    }

    @Reference(
        service = DispatcherCacheInvalidationStrategy.class,
        bind = "bindInvalidateDispatcherCache",
        unbind = "unbindInvalidateDispatcherCache",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY)
    void bindInvalidateDispatcherCache(DispatcherCacheInvalidationStrategy invalidateDispatcherCache, Map<String, Object> properties) {
        String invalidateType = invalidateDispatcherCache.getInvalidationRequestType();
        if (invalidateType != null) {
            boolean isInternal = isInternalStrategy(invalidateDispatcherCache);
            InvalidateTypeStrategies strategies = invalidateCacheList.computeIfAbsent(invalidateType, k -> new InvalidateTypeStrategies());
            strategies.addStrategy(new StrategyInfo(invalidateDispatcherCache, properties, isInternal));
        }
    }

    void unbindInvalidateDispatcherCache(DispatcherCacheInvalidationStrategy invalidateDispatcherCache, Map<String, Object> properties) {
        String invalidateType = invalidateDispatcherCache.getInvalidationRequestType();
        if (invalidateType != null) {
            InvalidateTypeStrategies strategies = invalidateCacheList.get(invalidateType);
            if (strategies != null) {
                String componentName = (String) properties.get("component.name");
                if (componentName != null) {
                    strategies.removeStrategy(componentName);
                }
            }
        }
    }

    private boolean isInternalStrategy(CacheInvalidationStrategy strategy) {
        return strategy.getClass().getPackage().getName().startsWith(INTERNAL_PACKAGE_PREFIX);
    }

    public Set<String> getInvalidateTypes() {
        return Collections.unmodifiableSet(invalidateCacheList.keySet());
    }

    public InvalidateTypeStrategies getInvalidateTypeStrategies(String invalidateType) {
        return invalidateCacheList.get(invalidateType);
    }
}
