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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegexPatternsInvalidateCacheTest {

    private RegexPatternsInvalidateCache regexPatternsInvalidateCache;

    @Before
    public void setUp() {
        regexPatternsInvalidateCache = new RegexPatternsInvalidateCache();
    }

    @Test
    public void testGetPatterns() {
        // Test that getPatterns returns the input invalidationParameters as a list
        String[] invalidationParameters = { "test1", "test2" };
        List<String> patterns = regexPatternsInvalidateCache.getPatterns(invalidationParameters);
        assertEquals(2, patterns.size());
        assertEquals("test1", patterns.get(0));
        assertEquals("test2", patterns.get(1));
    }

    @Test
    public void testGetInvalidationRequestType() {
        // Test that getInvalidationRequestType returns "regexPatterns"
        assertEquals("regexPatterns", regexPatternsInvalidateCache.getInvalidationRequestType());
    }
}
