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

package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import java.util.Calendar;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.commerce.core.components.models.page.Page;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ContainerExporter;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.commons.inherit.ComponentInheritanceValueMap;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { Page.class, ContainerExporter.class },
    resourceType = PageImpl.RESOURCE_TYPE)
public class PageImpl implements Page {

    protected static final String RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";
    private static final String STORE_CODE_PROPERTY = "cq:magentoStore";

    @Inject
    private com.day.cq.wcm.api.Page currentPage;

    @Inject
    private Resource resource;

    @Self
    @Via(type = ResourceSuperType.class)
    private com.adobe.cq.wcm.core.components.models.Page page;

    private String storeCode;

    @PostConstruct
    private void initModel() {
        InheritanceValueMap properties;
        if (page != null) {
            properties = new HierarchyNodeInheritanceValueMap(currentPage.getContentResource());
        } else {
            properties = new ComponentInheritanceValueMap(resource);
        }
        storeCode = properties.getInherited(STORE_CODE_PROPERTY, String.class);
        if (storeCode == null) {
            storeCode = "default";
        }
    }

    @Override
    public String getStoreCode() {
        return storeCode;
    }

    @Override
    public String getLanguage() {
        return page.getLanguage();
    }

    @Override
    public Calendar getLastModifiedDate() {
        return page.getLastModifiedDate();
    }

    @Override
    public String[] getKeywords() {
        return page.getKeywords();
    }

    @Override
    public String getDesignPath() {
        return page.getDesignPath();
    }

    @Override
    public String getStaticDesignPath() {
        return page.getStaticDesignPath();
    }

    @Override
    public Map<String, String> getFavicons() {
        return page.getFavicons();
    }

    @Override
    public String getTitle() {
        return page.getTitle();
    }

    @Override
    public String[] getClientLibCategories() {
        return page.getClientLibCategories();
    }

    @Override
    public String[] getClientLibCategoriesJsBody() {
        return page.getClientLibCategoriesJsBody();
    }

    @Override
    public String[] getClientLibCategoriesJsHead() {
        return page.getClientLibCategoriesJsHead();
    }

    @Override
    public String getTemplateName() {
        return page.getTemplateName();
    }

    @Override
    public String getAppResourcesPath() {
        return page.getAppResourcesPath();
    }

    @Override
    public String getCssClassNames() {
        return page.getCssClassNames();
    }

    @Override
    public NavigationItem getRedirectTarget() {
        return page.getRedirectTarget();
    }

    @Override
    public boolean hasCloudconfigSupport() {
        return page.hasCloudconfigSupport();
    }

    @Override
    public String[] getExportedItemsOrder() {
        return page.getExportedItemsOrder();
    }

    @Override
    public Map<String, ? extends ComponentExporter> getExportedItems() {
        return page.getExportedItems();
    }

    @Override
    public String getExportedType() {
        return page.getExportedType();
    }
}
