/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.search.models;

import java.util.Map;

/**
 * Represents a sort key with sort order in the data model of product sorter UI.
 */
public interface SorterKey {
    /**
     * @return The sort key.
     */
    String getName();

    /**
     * @return A user friendly localizable label of a sort key.
     */
    String getLabel();

    /**
     * @return The ordering related to this key.
     */
    Sorter.Order getOrder();

    /**
     * @return {@code true} if this is the currently selected key, {@code false} otherwise.
     */
    boolean isSelected();

    /**
     * @return The filtering parameters related to the current sort key and sort order.
     */
    Map<String, String> getCurrentOrderParameters();

    /**
     * @return The filtering parameters related to the current sort key but opposite sort order.
     */
    Map<String, String> getOppositeOrderParameters();
}
