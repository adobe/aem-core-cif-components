/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.core.search.models;

import java.util.Map;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Represents an aggregation option.
 */
@ConsumerType
public interface SearchAggregationOption {

    /**
     * Get the filter value for a aggregation option.
     *
     * @return the filter value
     */
    @Nonnull
    String getFilterValue();

    /**
     * Get the display label for a aggregation option.
     *
     * @return the display label for the aggregation option
     */
    @Nonnull
    String getDisplayLabel();

    /**
     * Get the number of results for this particular aggregation option.
     *
     * @return the product count for this aggregation option
     */
    @Nonnull
    int getCount();

    /**
     * Get the key value map for this aggregation option.
     *
     * @return key value for this aggregation option
     */
    @Nonnull
    Map<String, String> getAddFilterMap();

    /**
     * Get the page URL for this aggregation option.
     *
     * @return page url
     */
    default String getPageUrl() {
        return null;
    }

}
