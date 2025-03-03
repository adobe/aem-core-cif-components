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

import java.util.List;
import java.util.Map;

public class DispatcherUrlConfig {
    private final String storePath;
    private final Map<String, List<PatternConfig>> patternConfigs;

    public DispatcherUrlConfig(String storePath, Map<String, List<PatternConfig>> patternConfigs) {
        this.storePath = storePath;
        this.patternConfigs = patternConfigs;
    }

    public String getStorePath() {
        return storePath;
    }

    public Map<String, List<PatternConfig>> getPatternConfigs() {
        return patternConfigs;
    }
}
