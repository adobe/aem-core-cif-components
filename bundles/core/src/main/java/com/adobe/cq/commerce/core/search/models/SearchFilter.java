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

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Represents a search filter.
 */
@ConsumerType
public interface SearchFilter {

    /**
     * Get the filter value.
     *
     * @return filter value
     */
    @Nonnull
    String getValue();

    /**
     * Get the filter display label.
     *
     * @return filter label
     */
    @Nonnull
    String getDisplayLabel();

    /**
     * Get the map of attributes that will remove this filter from results.
     *
     * @return the filters without this filter value
     */
    @Nonnull
    Map<String, String[]> getRemoveFilterMap();
}
