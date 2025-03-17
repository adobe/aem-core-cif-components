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

import java.util.Collections;
import java.util.Map;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;

/**
 * Represents metadata about a cache invalidation strategy.
 */
public class StrategyInfo {
    private static final String COMPONENT_NAME_PROPERTY = "component.name";

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
