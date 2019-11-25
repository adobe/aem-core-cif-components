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

package com.adobe.cq.commerce.core.components.internal.models.v1.storeviewexporter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.models.storeviewexporter.StoreViewExporter;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { StoreViewExporter.class },
    resourceType = StoreViewExporterImpl.RESOURCE_TYPE)
public class StoreViewExporterImpl implements StoreViewExporter {

    protected static final String RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";
    private static final String STORE_CODE_PROPERTY = "cq:magentoStore";

    @Inject
    private Page currentPage;

    private String storeView;

    @PostConstruct
    void initModel() {
        InheritanceValueMap properties = new HierarchyNodeInheritanceValueMap(currentPage.getContentResource());
        storeView = properties.getInherited(STORE_CODE_PROPERTY, String.class);
        if (storeView == null) {
            storeView = "default";
        }
    }

    @Override
    public String getStoreView() {
        return storeView;
    }
}
