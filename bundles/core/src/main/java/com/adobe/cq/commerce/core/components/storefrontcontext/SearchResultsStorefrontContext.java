/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.storefrontcontext;

import java.util.List;

/**
 * Interface for search results context required by the MSE frontend API
 */
public interface SearchResultsStorefrontContext extends CommerceStorefrontContext {

    String getSearchUnitId();

    String getSearchRequestId();

    List<SearchResultProduct> getProducts();

    List<SearchResultCategory> getCategories();

    List<SearchResultSuggestion> getSuggestions();

    int getPage();

    int getPerPage();

    List<SearchFacet> getFacets();
}
