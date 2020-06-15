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

package com.adobe.cq.commerce.core.search.services;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.day.cq.wcm.api.Page;

/**
 * This service is responsible for retrieving search filter and attribute metadata from the commerce backend.
 */
@ProviderType
public interface SearchFilterService {

    /**
     * Service to retrieve available search filters from the backing commerce system.
     *
     * @return a {@link List< FilterAttributeMetadata >} of available search filters
     */
    List<FilterAttributeMetadata> retrieveCurrentlyAvailableCommerceFilters(Page page);

}
