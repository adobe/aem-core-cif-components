/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Represents a search aggregation.
 */
@ConsumerType
public interface SearchAggregation {

    /**
     * Get the currently applied filter value.
     *
     * @return the applied filter value
     */
    @Nonnull
    Optional<String> getAppliedFilterValue();

    /**
     * Get the filter display label.
     *
     * @return the applied filter display label
     */
    @Nonnull
    Optional<String> getAppliedFilterDisplayLabel();

    /**
     * Whether or not this aggregation can actually be used to filter results.
     *
     * @return true if available for filtering
     */
    @Nonnull
    boolean getFilterable();

    /**
     * The identifier (e.g. the attribute code) for this aggregation.
     *
     * @return the identifier for the aggregation
     */
    @Nonnull
    String getIdentifier();

    /**
     * Get the aggregation display label.
     *
     * @return the aggregation display label
     */
    @Nonnull
    String getDisplayLabel();

    /**
     * Get the number of aggregation options exist for the current aggregation.
     *
     * @return the number of aggregation options
     */
    @Nonnull
    int getOptionCount();

    /**
     * Get the search aggregation options available for this aggregation.
     *
     * @return the options
     */
    @Nonnull
    List<SearchAggregationOption> getOptions();

    /**
     * Get the map of attributes that will remove this aggregation from results.
     *
     * @return the filters without this aggregation filter
     */
    @Nonnull
    Map<String, String> getRemoveFilterMap();

}
