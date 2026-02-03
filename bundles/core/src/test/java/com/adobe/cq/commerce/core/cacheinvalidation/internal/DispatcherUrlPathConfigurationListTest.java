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

import org.junit.Test;

import static org.junit.Assert.*;

public class DispatcherUrlPathConfigurationListTest {

    @Test
    public void testParseConfigurations() {
        // Test input
        String[] configurations = {
            "product:^/content/catalog/.*:exact",
            "category:/content/shop/(.*):regex",
            "product-1:/content/products/(.*):wildcard"
        };

        // Parse configurations
        DispatcherUrlPathConfigurationList configList = DispatcherUrlPathConfigurationList.parseConfigurations(configurations);

        // Get the parsed configurations map
        Map<String, List<PatternConfig>> parsedConfigs = configList.getConfigurations();

        // Verify the map contains expected keys
        assertTrue(parsedConfigs.containsKey("product"));
        assertTrue(parsedConfigs.containsKey("category"));

        // Verify product configurations
        List<PatternConfig> productConfigs = parsedConfigs.get("product");
        assertEquals(2, productConfigs.size());

        // Verify first product configuration
        assertEquals("^/content/catalog/.*", productConfigs.get(0).getPattern());
        assertEquals("exact", productConfigs.get(0).getMatch());

        // Verify second product configuration
        assertEquals("/content/products/(.*)", productConfigs.get(1).getPattern());
        assertEquals("wildcard", productConfigs.get(1).getMatch());

        // Verify category configurations
        List<PatternConfig> categoryConfigs = parsedConfigs.get("category");
        assertEquals(1, categoryConfigs.size());
        assertEquals("/content/shop/(.*)", categoryConfigs.get(0).getPattern());
        assertEquals("regex", categoryConfigs.get(0).getMatch());
    }

    @Test
    public void testNormalizeUrlPathType() {
        // Test input with numbered suffixes
        String[] configurations = {
            "product-1:^/content/catalog/.*:exact",
            "product-2:^/content/products/.*:exact",
            "category-1:/content/shop/(.*):regex"
        };

        DispatcherUrlPathConfigurationList configList = DispatcherUrlPathConfigurationList.parseConfigurations(configurations);
        Map<String, List<PatternConfig>> parsedConfigs = configList.getConfigurations();

        // Verify that numbered suffixes are removed
        assertTrue(parsedConfigs.containsKey("product"));
        assertTrue(parsedConfigs.containsKey("category"));

        // Verify that multiple product configurations are grouped
        List<PatternConfig> productConfigs = parsedConfigs.get("product");
        assertEquals(2, productConfigs.size());
    }

    @Test
    public void testGetPatternConfigsForType() {
        String[] configurations = {
            "product:^/content/catalog/.*:exact",
            "category:/content/shop/(.*):regex"
        };

        DispatcherUrlPathConfigurationList configList = DispatcherUrlPathConfigurationList.parseConfigurations(configurations);

        // Test existing type
        List<PatternConfig> productConfigs = configList.getPatternConfigsForType("product");
        assertNotNull(productConfigs);
        assertEquals(1, productConfigs.size());

        // Test non-existing type
        List<PatternConfig> nonExistingConfigs = configList.getPatternConfigsForType("non-existing");
        assertNotNull(nonExistingConfigs);
        assertTrue(nonExistingConfigs.isEmpty());
    }

    @Test
    public void testInvalidConfigurationFormat() {
        // Test input with invalid format
        String[] configurations = {
            "product:^/content/catalog/.*", // Missing match part
            "invalid_format",
            "category:/content/shop/(.*):regex" // This one is valid
        };

        DispatcherUrlPathConfigurationList configList = DispatcherUrlPathConfigurationList.parseConfigurations(configurations);
        Map<String, List<PatternConfig>> parsedConfigs = configList.getConfigurations();

        // Only the valid configuration should be parsed
        assertEquals(1, parsedConfigs.size());
        assertTrue(parsedConfigs.containsKey("category"));
    }
}
