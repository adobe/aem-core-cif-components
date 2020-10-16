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

package com.adobe.cq.commerce.core.components.internal.models.v1.searchbar;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.models.searchbar.Searchbar;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.Page;

/**
 * Concrete implementation of the Sling Model API for the Searchbar component
 * 
 * @see Searchbar
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Searchbar.class,
    resourceType = SearchbarImpl.RESOURCE_TYPE)
public class SearchbarImpl extends DataLayerComponent implements Searchbar {
    static final String RESOURCE_TYPE = "core/cif/components/commerce/searchbar/v1/searchbar";

    @Inject
    private Page currentPage;

    private Page searchResultsPage;

    @Override
    public String getSearchResultsPageUrl() {
        if (searchResultsPage == null) {
            searchResultsPage = SiteNavigation.getSearchResultsPage(currentPage);
        }

        return searchResultsPage.getPath() + ".html";
    }
}
