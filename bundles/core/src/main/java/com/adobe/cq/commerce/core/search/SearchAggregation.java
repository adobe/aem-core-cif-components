/*
 *   Copyright 2019 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.adobe.cq.commerce.core.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Represents an aggregation
 */
@ConsumerType
public interface SearchAggregation {

    @Nonnull
    Optional<String> getAppliedFilterValue();

    @Nonnull
    Optional<String> getAppliedFilterDisplayLabel();

    @Nonnull
    boolean getFilterable();

    @Nonnull
    String getIdentifier();

    @Nonnull
    String getDisplayLabel();

    @Nonnull
    int getOptionCount();

    @Nonnull
    List<SearchAggregationOption> getOptions();

    @Nonnull
    Map<String, String> getRemoveFilterMap();

}
