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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages a collection of cache invalidation strategies for a specific attribute.
 */
public class AttributeStrategies {
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
}
