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

import org.junit.Test;

import static org.junit.Assert.*;

public class DispatcherBasePathConfigurationTest {

    private static final String TEST_PATTERN = "/content/.*";
    private static final String TEST_MATCH = "/content/test";

    @Test
    public void testConstructorAndGetters() {
        DispatcherBasePathConfiguration config = new DispatcherBasePathConfiguration(TEST_PATTERN, TEST_MATCH);

        assertEquals("Pattern should match the constructor argument", TEST_PATTERN, config.getPattern());
        assertEquals("Match should match the constructor argument", TEST_MATCH, config.getMatch());
    }

    @Test
    public void testCreateDefault() {
        DispatcherBasePathConfiguration defaultConfig = DispatcherBasePathConfiguration.createDefault();

        assertEquals("Default pattern should be empty string", "", defaultConfig.getPattern());
        assertEquals("Default match should be empty string", "", defaultConfig.getMatch());
    }

    @Test
    public void testConstructorWithNullValues() {
        DispatcherBasePathConfiguration configWithNull = new DispatcherBasePathConfiguration(null, null);

        assertNull("Pattern should be null when constructed with null", configWithNull.getPattern());
        assertNull("Match should be null when constructed with null", configWithNull.getMatch());
    }
}
