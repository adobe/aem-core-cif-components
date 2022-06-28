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
package com.adobe.cq.commerce.core.components.internal.models.v1.searchbar;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.searchbar.Searchbar;
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

    @ScriptVariable
    private Page currentPage;

    @Self
    private SiteStructure siteStructure;

    private Page searchResultsPage;

    @Override
    public String getSearchResultsPageUrl() {
        if (searchResultsPage == null) {
            SiteStructure.Entry entry = siteStructure.getSearchResultsPage();
            searchResultsPage = entry != null ? entry.getPage() : currentPage;
        }

        return searchResultsPage.getPath() + ".html";
    }
}
