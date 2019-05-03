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

package com.adobe.cq.commerce.core.components.internal.models.v1.header;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.header.Header;
import com.day.cq.wcm.api.Page;

/**
 * Concrete implementation of the Sling Model API for the Header component
 * @see Header
 */
@Model(adaptables = SlingHttpServletRequest.class,
        adapters = Header.class,
        resourceType = HeaderImpl.RESOURCE_TYPE)
public class HeaderImpl implements Header {

    static final String RESOURCE_TYPE = "venia/components/structure/header/v1/header" ;

    @Inject
    private Page currentPage;

    private Page searchResultsPage;

    @Override
    public String getSearchResultsPageUrl() {
        if (searchResultsPage == null) {
            searchResultsPage = Utils.getSearchResultsPage(currentPage);
        }

        return searchResultsPage.getPath()+".html";
    }
}
