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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.day.cq.wcm.api.Page;

@Component(service = InvalidateCacheRegistry.class, immediate = true)
public class InvalidateCacheRegistry {

    private static final String INTERNAL_PACKAGE_PREFIX = "com.adobe.cq.commerce.core.cacheinvalidation.internal";
    private static final String COMPONENT_NAME_PROPERTY = "component.name";

    public static class StrategyInfo {
        private final CacheInvalidationStrategy strategy;
        private final Map<String, Object> properties;
        private final String packageName;
        private final String componentName;
        private final boolean isInternal;

        public StrategyInfo(CacheInvalidationStrategy strategy, Map<String, Object> properties, boolean isInternal) {
            this.strategy = strategy;
            this.properties = properties;
            this.packageName = strategy.getClass().getPackage().getName();
            this.componentName = (String) properties.get(COMPONENT_NAME_PROPERTY);
            this.isInternal = isInternal;
        }

        public CacheInvalidationStrategy getStrategy() {
            return strategy;
        }

        public Map<String, Object> getProperties() {
            return Collections.unmodifiableMap(properties);
        }

        public String getPackageName() {
            return packageName;
        }

        public String getComponentName() {
            return componentName;
        }

        public boolean isInternal() {
            return isInternal;
        }
    }

    public static class AttributeStrategies {
        private final List<StrategyInfo> strategies = new ArrayList<>();

        public void addStrategy(StrategyInfo strategy) {
            String componentName = strategy.getComponentName();
            if (componentName != null) {
                // Remove existing strategy for this component if it exists
                strategies.removeIf(s -> componentName.equals(s.getComponentName()));
                strategies.add(strategy);
            }
        }

        public void removeStrategy(String componentName) {
            strategies.removeIf(s -> componentName.equals(s.getComponentName()));
        }

        public List<StrategyInfo> getStrategies(boolean internalOnly) {
            return strategies.stream()
                .filter(strategy -> !internalOnly || strategy.isInternal())
                .collect(Collectors.toList());
        }

        public List<StrategyInfo> getInternalStrategies() {
            return getStrategies(true);
        }

        public List<StrategyInfo> getClientStrategies() {
            return getStrategies(false);
        }
    }

    private final Map<String, AttributeStrategies> invalidateCacheList = new HashMap<>();

    @Reference(
        service = CacheInvalidationStrategy.class,
        bind = "bindInvalidateCache",
        unbind = "unbindInvalidateCache",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY)
    void bindInvalidateCache(CacheInvalidationStrategy invalidateCache, Map<String, Object> properties) {
        String attribute = (String) properties.get(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER);
        if (attribute != null) {
            boolean isInternal = isInternalStrategy(invalidateCache);
            AttributeStrategies strategies = invalidateCacheList.computeIfAbsent(attribute, k -> new AttributeStrategies());
            strategies.addStrategy(new StrategyInfo(invalidateCache, properties, isInternal));
        }
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
        if (attribute != null) {
            boolean isInternal = isInternalStrategy(invalidateDispatcherCache);
            AttributeStrategies strategies = invalidateCacheList.computeIfAbsent(attribute, k -> new AttributeStrategies());
            strategies.addStrategy(new StrategyInfo(invalidateDispatcherCache, properties, isInternal));
        }
    }

    void unbindInvalidateDispatcherCache(Map<String, Object> properties) {
        unbindCache(properties);
    }

    private boolean isInternalStrategy(CacheInvalidationStrategy strategy) {
        return strategy.getClass().getPackage().getName().startsWith(INTERNAL_PACKAGE_PREFIX);
    }

    private void unbindCache(Map<String, Object> properties) {
        String attribute = (String) properties.get(InvalidateCacheSupport.PROPERTY_INVALIDATE_REQUEST_PARAMETER);
        if (attribute != null) {
            AttributeStrategies strategies = invalidateCacheList.get(attribute);
            if (strategies != null) {
                String componentName = (String) properties.get(COMPONENT_NAME_PROPERTY);
                if (componentName != null) {
                    strategies.removeStrategy(componentName);
                }
            }
        }
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

    public String[] getCorrespondingPagePaths(String attribute, Session session, String storePath, String dataList)
        throws CacheInvalidationException {
        AttributeStrategies strategies = invalidateCacheList.get(attribute);
        if (strategies == null) {
            return new String[0];
        }

        return strategies.getStrategies(false).stream()
            .filter(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy)
            .map(info -> ((DispatcherCacheInvalidationStrategy) info.getStrategy()))
            .map(strategy -> {
                try {
                    return strategy.getCorrespondingPagePaths(session, storePath, dataList);
                } catch (CacheInvalidationException e) {
                    return new String[0];
                }
            })
            .filter(paths -> paths.length > 0)
            .findFirst()
            .orElse(new String[0]);
    }

    public String getGraphqlQuery(String attribute, String[] data) {
        AttributeStrategies strategies = invalidateCacheList.get(attribute);
        if (strategies == null) {
            return null;
        }

        return strategies.getStrategies(false).stream()
            .filter(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy)
            .map(info -> ((DispatcherCacheInvalidationStrategy) info.getStrategy()))
            .map(strategy -> strategy.getGraphqlQuery(data))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    public String[] getPathsToInvalidate(String attribute, Page page, ResourceResolver resourceResolver,
        Map<String, Object> data, String storePath) {
        AttributeStrategies strategies = invalidateCacheList.get(attribute);
        if (strategies == null) {
            return new String[0];
        }

        return strategies.getStrategies(false).stream()
            .filter(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy)
            .map(info -> ((DispatcherCacheInvalidationStrategy) info.getStrategy()))
            .map(strategy -> strategy.getPathsToInvalidate(page, resourceResolver, data, storePath))
            .filter(paths -> paths.length > 0)
            .findFirst()
            .orElse(new String[0]);
    }

    public Set<String> getAttributes() {
        return Collections.unmodifiableSet(invalidateCacheList.keySet());
    }

    public AttributeStrategies getAttributeStrategies(String attribute) {
        return invalidateCacheList.get(attribute);
    }
}
