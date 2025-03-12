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

/**
 * Configuration class for dispatcher base path pattern and match values.
 */
public class DispatcherBasePathConfiguration {
    private final String pattern;
    private final String match;

    /**
     * Creates a new dispatcher base path configuration.
     *
     * @param pattern The pattern to match against store paths
     * @param match The replacement pattern to use when the pattern matches
     */
    public DispatcherBasePathConfiguration(String pattern, String match) {
        this.pattern = pattern;
        this.match = match;
    }

    /**
     * Gets the pattern used for matching store paths.
     *
     * @return The pattern string
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Gets the replacement pattern used when a match is found.
     *
     * @return The match string
     */
    public String getMatch() {
        return match;
    }

    /**
     * Creates a default configuration with empty pattern and match strings.
     *
     * @return A default configuration
     */
    public static DispatcherBasePathConfiguration createDefault() {
        return new DispatcherBasePathConfiguration("", "");
    }
}