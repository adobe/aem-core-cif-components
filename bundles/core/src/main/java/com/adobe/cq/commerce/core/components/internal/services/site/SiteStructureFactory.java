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

import org.apache.commons.lang3.StringUtils;
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
import com.day.cq.commons.inherit.ComponentInheritanceValueMap;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
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
    private static final String EXPERIENCE_FRAGMENTS_PATH = "/content/experience-fragments";
    private static final String PN_CIF_PREVIEW_PAGE = "cq:cifPreviewPage";

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
        if (currentPage.getPath().startsWith(EXPERIENCE_FRAGMENTS_PATH)) {
            currentPage = getCurrentPageForXf(currentPage);
        }

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

    /**
     * Gets the appropriate current page for experience fragments by either finding
     * a navigation root with cq:cifPreviewPage or using progressive path lookup.
     *
     * @param xfPage The experience fragment page
     * @return The resolved current page for site structure
     */
    private Page getCurrentPageForXf(Page xfPage) {
        // First try to find preview page with cq:cifPreviewPage
        Page previewPage = findPreviewPage(xfPage);
        if (previewPage != null) {
            return previewPage;
        }

        // Fallback to progressive path lookup
        return findCorrespondingPage(xfPage);
    }

    /**
     * Recursively tries to find a corresponding page in the main content structure
     * by progressively removing path segments from the experience fragment path.
     *
     * @param xfPage The experience fragment page
     * @return The found page or the original page if no match found
     */
    private Page findCorrespondingPage(Page xfPage) {
        String experienceFragmentPath = xfPage.getPath();
        String contentPath = experienceFragmentPath.replace(EXPERIENCE_FRAGMENTS_PATH, "/content");

        PageManager pageManager = xfPage.getPageManager();
        Page foundPage = findCorrespondingPageIterative(pageManager, contentPath);

        return foundPage != null ? foundPage : xfPage;
    }

    /**
     * Iteratively tries to find a corresponding page in the main content structure
     * by progressively removing path segments from the experience fragment path.
     *
     * @param pageManager The page manager to use for page lookups
     * @param contentPath The full content path (e.g., "/content/venia/us/en/site/header/master")
     * @return The found page or null if no page exists
     */
    private Page findCorrespondingPageIterative(PageManager pageManager, String contentPath) {
        if (contentPath == null || contentPath.isEmpty()) {
            return null;
        }

        // Use iterative approach instead of recursion to avoid stack overflow
        String currentPath = contentPath;

        while (StringUtils.isNotEmpty(currentPath)) {
            // Try the current path
            Page page = pageManager.getPage(currentPath);
            if (page != null) {
                return page;
            }

            // Remove the last segment and try again
            int lastSlashIndex = currentPath.lastIndexOf('/');
            if (lastSlashIndex <= 0) {
                break; // Reached root level
            }

            currentPath = currentPath.substring(0, lastSlashIndex);
        }

        return null;
    }

    /**
     * Finds the preview page by looking for a node with a non-empty cq:cifPreviewPage attribute.
     * Uses InheritanceValueMap to traverse up the hierarchy automatically.
     *
     * @param currentPage The current page to start the search from
     * @return The preview page if found, otherwise null
     */
    private Page findPreviewPage(Page currentPage) {
        if (currentPage == null) {
            return null;
        }

        // Use InheritanceValueMap to automatically traverse up the hierarchy
        InheritanceValueMap properties = new ComponentInheritanceValueMap(currentPage.adaptTo(Resource.class));
        String previewPagePath = properties.getInherited(PN_CIF_PREVIEW_PAGE, String.class);

        // Fallback to HierarchyNodeInheritanceValueMap if not found
        if (previewPagePath == null || previewPagePath.trim().isEmpty()) {
            properties = new HierarchyNodeInheritanceValueMap(currentPage.getContentResource());
            previewPagePath = properties.getInherited(PN_CIF_PREVIEW_PAGE, String.class);
        }

        if (previewPagePath != null && !previewPagePath.trim().isEmpty()) {
            PageManager pageManager = currentPage.getPageManager();
            Page previewPage = pageManager.getPage(previewPagePath);
            if (previewPage != null) {
                return previewPage;
            }
        }

        return null;
    }
}
