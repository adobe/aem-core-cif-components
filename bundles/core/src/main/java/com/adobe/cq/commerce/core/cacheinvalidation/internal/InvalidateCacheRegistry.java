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
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;

@Component(service = InvalidateCacheRegistry.class, immediate = true)
public class InvalidateCacheRegistry {

    private static final String INTERNAL_PACKAGE_PREFIX = "com.adobe.cq.commerce.core.cacheinvalidation.internal";

    private final Map<String, AttributeStrategies> invalidateCacheList = new HashMap<>();

    @Reference(
        service = CacheInvalidationStrategy.class,
        bind = "bindInvalidateCache",
        unbind = "unbindInvalidateCache",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY)
    void bindInvalidateCache(CacheInvalidationStrategy invalidateCache, Map<String, Object> properties) {
        String attribute = invalidateCache.getInvalidationRequestType();
        if (attribute != null) {
            boolean isInternal = isInternalStrategy(invalidateCache);
            AttributeStrategies strategies = invalidateCacheList.computeIfAbsent(attribute, k -> new AttributeStrategies());
            strategies.addStrategy(new StrategyInfo(invalidateCache, properties, isInternal));
        }
    }

    void unbindInvalidateCache(CacheInvalidationStrategy invalidateCache, Map<String, Object> properties) {
        String attribute = invalidateCache.getInvalidationRequestType();
        if (attribute != null) {
            AttributeStrategies strategies = invalidateCacheList.get(attribute);
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
        String attribute = invalidateDispatcherCache.getInvalidationRequestType();
        if (attribute != null) {
            boolean isInternal = isInternalStrategy(invalidateDispatcherCache);
            AttributeStrategies strategies = invalidateCacheList.computeIfAbsent(attribute, k -> new AttributeStrategies());
            strategies.addStrategy(new StrategyInfo(invalidateDispatcherCache, properties, isInternal));
        }
    }

    void unbindInvalidateDispatcherCache(DispatcherCacheInvalidationStrategy invalidateDispatcherCache, Map<String, Object> properties) {
        String attribute = invalidateDispatcherCache.getInvalidationRequestType();
        if (attribute != null) {
            AttributeStrategies strategies = invalidateCacheList.get(attribute);
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

    public Set<String> getPattern(String attribute) {
        AttributeStrategies strategies = invalidateCacheList.get(attribute);
        if (strategies == null) {
            return Collections.emptySet();
        }

        return strategies.getStrategies(false).stream()
            .map(info -> info.getStrategy().getPattern())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public List<String> getPathsToInvalidate(DispatcherCacheInvalidationContext dispatcherCacheInvalidationContext) {
        List<String> attributeData = dispatcherCacheInvalidationContext.getAttributeData();
        if (attributeData == null || attributeData.isEmpty()) {
            return Collections.emptyList();
        }

        String attribute = attributeData.get(0); // Get the first attribute from the list
        AttributeStrategies strategies = invalidateCacheList.get(attribute);
        if (strategies == null) {
            return Collections.emptyList();
        }

        return strategies.getStrategies(false).stream()
            .filter(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy)
            .map(info -> ((DispatcherCacheInvalidationStrategy) info.getStrategy()))
            .map(strategy -> strategy.getPathsToInvalidate(dispatcherCacheInvalidationContext))
            .filter(paths -> paths != null && !paths.isEmpty())
            .findFirst()
            .orElse(Collections.emptyList());
    }

    public Set<String> getAttributes() {
        return Collections.unmodifiableSet(invalidateCacheList.keySet());
    }

    public AttributeStrategies getAttributeStrategies(String attribute) {
        return invalidateCacheList.get(attribute);
    }
}
