/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services.site;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.scripting.WCMBindingsConstants;

@Component(
    service = {
        AdapterFactory.class,
        SiteStructureFactory.class
    },
    property = {
        AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.SlingHttpServletRequest",
        AdapterFactory.ADAPTABLE_CLASSES + "=com.day.cq.wcm.api.Page",
        AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource",
        AdapterFactory.ADAPTER_CLASSES + "=com.adobe.cq.commerce.core.components.models.common.SiteStructure"
    })
public class SiteStructureFactory implements AdapterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteStructureFactory.class);

    @Reference
    private PageManagerFactory pageManagerFactory;

    @Override
    public <AdapterType> @Nullable AdapterType getAdapter(@NotNull Object o, @NotNull Class<AdapterType> aClass) {
        if (SiteStructure.class.equals(aClass)) {
            if (o instanceof SlingHttpServletRequest) {
                return (AdapterType) getAdapter((SlingHttpServletRequest) o);
            } else if (o instanceof Page) {
                return (AdapterType) getAdapter((Page) o);
            } else if (o instanceof Resource) {
                return (AdapterType) getAdapter((Resource) o);
            }
        }
        return null;
    }

    private SiteStructure getAdapter(Page currentPage) {
        return new SiteStructureImpl(currentPage);
    }

    private SiteStructure getAdapter(Resource resource) {
        PageManager pageManager = pageManagerFactory.getPageManager(resource.getResourceResolver());
        Page page = pageManager.getPage(resource.getPath());

        if (page == null) {
            page = pageManager.getContainingPage(resource);
        }

        if (page != null) {
            return getAdapter(page);
        }

        return null;
    }

    private SiteStructure getAdapter(SlingHttpServletRequest request) {
        SiteStructure siteStructure = (SiteStructure) request.getAttribute(SiteStructure.class.getName());

        if (siteStructure != null) {
            return siteStructure;
        }

        SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        Page currentPage = bindings != null ? (Page) bindings.get(WCMBindingsConstants.NAME_CURRENT_PAGE) : null;

        if (currentPage == null) {
            siteStructure = getAdapter(request.getResource());
        } else {
            siteStructure = getAdapter(currentPage);
        }

        if (siteStructure != null) {
            request.setAttribute(SiteStructure.class.getName(), siteStructure);
        }

        return siteStructure;
    }

    public SiteStructure getSiteStructure(Page page) {
        SiteStructure siteStructure = page.adaptTo(SiteStructure.class);

        if (siteStructure == null) {
            siteStructure = getAdapter(page);
        }

        return siteStructure;
    }

    public SiteStructure getSiteStructure(Resource resource) {
        SiteStructure siteStructure = resource.adaptTo(SiteStructure.class);

        if (siteStructure == null) {
            siteStructure = getAdapter(resource);
            if (siteStructure == null) {
                LOGGER.warn("Cloud not find site structure for given resource: {}", resource.getPath());
                siteStructure = UnknownSiteStructure.INSTANCE;
            }
        }

        return siteStructure;
    }

    public SiteStructure getSiteStructure(SlingHttpServletRequest request, Page page) {
        SiteStructure siteStructure = null;

        if (request != null) {
            siteStructure = request.adaptTo(SiteStructure.class);
            if (siteStructure == null) {
                siteStructure = getAdapter(request);
            }
        } else if (page != null) {
            siteStructure = page.adaptTo(SiteStructure.class);
            if (siteStructure == null) {
                siteStructure = getAdapter(page);
            }
        }

        if (siteStructure == null) {
            LOGGER.warn("Cloud not find site structure for given page: {}", page != null ? page.getPath() : page);
            siteStructure = UnknownSiteStructure.INSTANCE;
        }

        return siteStructure;
    }
}
