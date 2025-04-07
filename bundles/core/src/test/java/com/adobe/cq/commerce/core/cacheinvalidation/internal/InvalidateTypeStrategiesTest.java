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

public class InvalidateTypeStrategiesTest {

    private InvalidateTypeStrategies invalidateTypeStrategies;

    @Mock
    private StrategyInfo mockStrategy1;
    @Mock
    private StrategyInfo mockStrategy2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        invalidateTypeStrategies = new InvalidateTypeStrategies();
    }

    @Test
    public void testAddStrategy() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn("component1");

        // Test adding a strategy
        invalidateTypeStrategies.addStrategy(mockStrategy1);
        List<StrategyInfo> strategies = invalidateTypeStrategies.getStrategies(false);
        assertEquals(1, strategies.size());
        assertEquals(mockStrategy1, strategies.get(0));
    }

    @Test
    public void testAddStrategyWithNullComponentName() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn(null);

        // Test adding a strategy with null component name
        invalidateTypeStrategies.addStrategy(mockStrategy1);
        List<StrategyInfo> strategies = invalidateTypeStrategies.getStrategies(false);
        assertTrue(strategies.isEmpty());
    }

    @Test
    public void testAddStrategyReplaceExisting() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn("component1");
        when(mockStrategy2.getComponentName()).thenReturn("component1");

        // Add first strategy
        invalidateTypeStrategies.addStrategy(mockStrategy1);

        // Add second strategy with same component name
        invalidateTypeStrategies.addStrategy(mockStrategy2);

        // Verify only the second strategy remains
        List<StrategyInfo> strategies = invalidateTypeStrategies.getStrategies(false);
        assertEquals(1, strategies.size());
        assertEquals(mockStrategy2, strategies.get(0));
    }

    @Test
    public void testRemoveStrategy() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn("component1");
        invalidateTypeStrategies.addStrategy(mockStrategy1);

        // Test removing the strategy
        invalidateTypeStrategies.removeStrategy("component1");
        List<StrategyInfo> strategies = invalidateTypeStrategies.getStrategies(false);
        assertTrue(strategies.isEmpty());
    }

    @Test
    public void testGetStrategiesInternalOnly() {
        // Setup
        when(mockStrategy1.getComponentName()).thenReturn("component1");
        when(mockStrategy2.getComponentName()).thenReturn("component2");
        when(mockStrategy1.isInternal()).thenReturn(true);
        when(mockStrategy2.isInternal()).thenReturn(false);

        invalidateTypeStrategies.addStrategy(mockStrategy1);
        invalidateTypeStrategies.addStrategy(mockStrategy2);

        // Test getting internal strategies only
        List<StrategyInfo> internalStrategies = invalidateTypeStrategies.getStrategies(true);
        assertEquals(1, internalStrategies.size());
        assertEquals(mockStrategy1, internalStrategies.get(0));

        // Test getting all strategies
        List<StrategyInfo> allStrategies = invalidateTypeStrategies.getStrategies(false);
        assertEquals(2, allStrategies.size());
    }

}
