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
package com.adobe.cq.commerce.core.components.internal.models.v1.header;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.header.Header;
import com.adobe.cq.commerce.core.components.services.SiteNavigation;
import com.day.cq.wcm.api.Page;

/**
 * Concrete implementation of the Sling Model API for the Header component
 * 
 * @see Header
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Header.class,
    resourceType = HeaderImpl.RESOURCE_TYPE)
public class HeaderImpl implements Header {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderImpl.class);

    static final String RESOURCE_TYPE = "core/cif/components/structure/header/v1/header";
    static final String MINICART_NODE_NAME = "minicart";
    static final String SEARCHBAR_NODE_NAME = "searchbar";
    static final String MINIACCOUNT_NODE_NAME = "miniaccount";

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private Resource resource;

    @OSGiService
    private SiteNavigation siteNavigation;

    private Page navigationRootPage;

    @PostConstruct
    private void initModel() {
        navigationRootPage = siteNavigation.getNavigationRootPage(currentPage);

        if (navigationRootPage == null) {
            LOGGER.warn("Navigation root page not found for page " + currentPage.getPath());
        }
    }

    @Override
    public String getNavigationRootPageUrl() {
        if (navigationRootPage != null) {
            return navigationRootPage.getPath() + ".html";
        }

        return null;
    }

    @Override
    public String getNavigationRootPageTitle() {
        if (navigationRootPage != null) {
            String title = navigationRootPage.getPageTitle();
            if (StringUtils.isNotBlank(title)) {
                return title;
            }

            title = navigationRootPage.getTitle();
            if (StringUtils.isNotBlank(title)) {
                return title;
            }

            return null;
        }

        return null;
    }

    public Resource getMinicartResource() {
        return resource.getChild(MINICART_NODE_NAME);
    }

    public Resource getSearchbarResource() {
        return resource.getChild(SEARCHBAR_NODE_NAME);
    }

    public Resource getMiniaccountResource() {
        return resource.getChild(MINIACCOUNT_NODE_NAME);
    }
}
