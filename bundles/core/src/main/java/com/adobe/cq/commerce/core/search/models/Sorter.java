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

import java.util.List;

/**
 * Provides sorting support for product search results and represents the data model for the product sorter UI.
 */
public interface Sorter {
    /**
     * The sort key request parameter name.
     */
    String PARAMETER_SORT_KEY = "sort_key";
    /**
     * The sort order request parameter name.
     */
    String PARAMETER_SORT_ORDER = "sort_order";

    /**
     * Enum to define the sort order: ascending and descending.
     */
    enum Order {
        ASC, DESC;

        /**
         * Returns the opposite sort order of this sort order.
         */
        public Order opposite() {
            return this == ASC ? DESC : ASC;
        }
    }

    /**
     * Returns the list of all available sorter keys.
     */
    List<SorterKey> getKeys();

    /**
     * Returns the current sorter key.
     */
    SorterKey getCurrentKey();
}
