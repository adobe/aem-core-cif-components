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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DispatcherUrlPathConfigurationList {
    private final Map<String, List<PatternConfig>> configurations;

    public DispatcherUrlPathConfigurationList(Map<String, List<PatternConfig>> configurations) {
        this.configurations = configurations;
    }

    public Map<String, List<PatternConfig>> getConfigurations() {
        return configurations;
    }

    public static DispatcherUrlPathConfigurationList parseConfigurations(String[] configurations) {
        Map<String, List<PatternConfig>> configMap = new HashMap<>();

        for (String config : configurations) {
            String[] parts = config.split(":");
            if (parts.length == 3) {
                String urlPathType = normalizeUrlPathType(parts[0]);
                String pattern = parts[1];
                String match = parts[2];

                PatternConfig patternConfig = new PatternConfig(pattern, match);
                configMap.computeIfAbsent(urlPathType, k -> new ArrayList<>())
                    .add(patternConfig);
            }
        }
        return new DispatcherUrlPathConfigurationList(configMap);
    }

    private static String normalizeUrlPathType(String urlPathType) {
        return urlPathType.replaceAll("-\\d+$", "");
    }

    public List<PatternConfig> getPatternConfigsForType(String urlPathType) {
        return configurations.getOrDefault(urlPathType, new ArrayList<>());
    }
}
