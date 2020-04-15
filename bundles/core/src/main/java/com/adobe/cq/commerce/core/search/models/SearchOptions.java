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

import java.util.Map;
import java.util.Optional;

/**
 * Represents a set of parameters that can be used to query a product search service. Rather than collecting a bunch of disparate parameters
 * as individual strings, hash maps, etc, this object contains all of the information AEM might want to gather in preparation for a search
 * query being executed.
 */
public interface SearchOptions {

    Optional<String> getSearchQuery();

    Optional<String> getCategoryId();

    int getCurrentPage();

    int getPageSize();

    Map<String, String> getAttributeFilters();

    /**
     * Retrieves all filters, including the category id which is being treated as a special case as a developer convenience. This method
     * essentially returns all of the attribute filters but also includes the category id if it was set.
     *
     * @return a key value pair of the attribute codes or identifiers with the chosen value
     */
    Map<String, String> getAllFilters();

}
