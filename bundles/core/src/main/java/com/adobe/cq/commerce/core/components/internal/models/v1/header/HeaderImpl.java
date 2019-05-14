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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderImpl.class);

    static final String RESOURCE_TYPE = "venia/components/structure/header/v1/header" ;

    @Inject
    private Page currentPage;

    private Page searchResultsPage;
    private Page navigationRootPage;

    @Override
    public String getSearchResultsPageUrl() {
        if (searchResultsPage == null) {
            searchResultsPage = Utils.getSearchResultsPage(currentPage);
        }

        return searchResultsPage.getPath()+".html";
    }

    @Override
    public String getNavigationRootPageUrl() {
        if (navigationRootPage == null) {
            navigationRootPage = Utils.getNavigationRootPage(currentPage);
        }

        if (navigationRootPage == null) {
            LOGGER.warn("Navigation root page not found for page " + currentPage.getPath());
            return null;
        }

        return navigationRootPage.getPath() + ".html";
    }
}
