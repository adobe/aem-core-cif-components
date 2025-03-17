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

import static org.junit.Assert.assertEquals;

public class PatternConfigTest {

    @Test
    public void testConstructorAndGetters() {
        String pattern = "testPattern";
        String match = "testMatch";
        PatternConfig config = new PatternConfig(pattern, match);

        assertEquals("Pattern should match the constructor argument", pattern, config.getPattern());
        assertEquals("Match should match the constructor argument", match, config.getMatch());
    }

    @Test
    public void testWithEmptyStrings() {
        String pattern = "";
        String match = "";
        PatternConfig config = new PatternConfig(pattern, match);

        assertEquals("Pattern should be empty string", pattern, config.getPattern());
        assertEquals("Match should be empty string", match, config.getMatch());
    }

    @Test
    public void testWithNullValues() {
        String pattern = null;
        String match = null;
        PatternConfig config = new PatternConfig(pattern, match);

        assertEquals("Pattern should be null", pattern, config.getPattern());
        assertEquals("Match should be null", match, config.getMatch());
    }
}
