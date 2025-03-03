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
import java.util.Map;

public class DispatcherUrlConfigurationList {
    private final Map<String, DispatcherUrlConfig> configurations;

    public DispatcherUrlConfigurationList(Map<String, DispatcherUrlConfig> configurations) {
        this.configurations = configurations;
    }

    public Map<String, DispatcherUrlConfig> getConfigurations() {
        return configurations;
    }

    public static DispatcherUrlConfigurationList parseConfigurations(String[] configurations) {
        Map<String, DispatcherUrlConfig> dispatcherUrlConfigsMap = new HashMap<>();
        for (String config : configurations) {
            String[] parts = config.split(":");
            if (parts.length == 4) {
                String storePath = parts[0];
                String urlPathType = normalizeUrlPathType(parts[1]);
                String matchPattern = parts[2];
                String convertPattern = parts[3];

                PatternConfig patternConfig = new PatternConfig(matchPattern, convertPattern);
                DispatcherUrlConfig dispatcherUrlConfig = dispatcherUrlConfigsMap
                    .computeIfAbsent(storePath, k -> new DispatcherUrlConfig(storePath, new HashMap<>()));
                dispatcherUrlConfig.getPatternConfigs()
                    .computeIfAbsent(urlPathType, k -> new ArrayList<>())
                    .add(patternConfig);
            }
        }
        return new DispatcherUrlConfigurationList(dispatcherUrlConfigsMap);
    }

    private static String normalizeUrlPathType(String urlPathType) {
        return urlPathType.replaceAll("-\\d+$", "");
    }
}