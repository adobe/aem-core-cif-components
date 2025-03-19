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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class AttributeStrategiesTest {

    private AttributeStrategies attributeStrategies;

    @Mock
    private StrategyInfo mockStrategy1;
    @Mock
    private StrategyInfo mockStrategy2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        attributeStrategies = new AttributeStrategies();
    }

    @Test
    public void testAddStrategy() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn("component1");

        // Test adding a strategy
        attributeStrategies.addStrategy(mockStrategy1);
        List<StrategyInfo> strategies = attributeStrategies.getStrategies(false);
        assertEquals(1, strategies.size());
        assertEquals(mockStrategy1, strategies.get(0));
    }

    @Test
    public void testAddStrategyWithNullComponentName() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn(null);

        // Test adding a strategy with null component name
        attributeStrategies.addStrategy(mockStrategy1);
        List<StrategyInfo> strategies = attributeStrategies.getStrategies(false);
        assertTrue(strategies.isEmpty());
    }

    @Test
    public void testAddStrategyReplaceExisting() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn("component1");
        when(mockStrategy2.getComponentName()).thenReturn("component1");

        // Add first strategy
        attributeStrategies.addStrategy(mockStrategy1);

        // Add second strategy with same component name
        attributeStrategies.addStrategy(mockStrategy2);

        // Verify only the second strategy remains
        List<StrategyInfo> strategies = attributeStrategies.getStrategies(false);
        assertEquals(1, strategies.size());
        assertEquals(mockStrategy2, strategies.get(0));
    }

    @Test
    public void testRemoveStrategy() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn("component1");
        attributeStrategies.addStrategy(mockStrategy1);

        // Test removing the strategy
        attributeStrategies.removeStrategy("component1");
        List<StrategyInfo> strategies = attributeStrategies.getStrategies(false);
        assertTrue(strategies.isEmpty());
    }

    @Test
    public void testGetStrategiesInternalOnly() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn("component1");
        when(mockStrategy2.getComponentName()).thenReturn("component2");
        when(mockStrategy1.isInternal()).thenReturn(true);
        when(mockStrategy2.isInternal()).thenReturn(false);

        attributeStrategies.addStrategy(mockStrategy1);
        attributeStrategies.addStrategy(mockStrategy2);

        // Test getting internal strategies only
        List<StrategyInfo> internalStrategies = attributeStrategies.getStrategies(true);
        assertEquals(1, internalStrategies.size());
        assertEquals(mockStrategy1, internalStrategies.get(0));

        // Test getting all strategies
        List<StrategyInfo> allStrategies = attributeStrategies.getStrategies(false);
        assertEquals(2, allStrategies.size());
    }

}
