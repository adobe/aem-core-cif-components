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

package com.adobe.cq.commerce.core.components.internal.models.v1.storeconfigexporter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.models.storeconfigexporter.StoreConfigExporter;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { StoreConfigExporter.class },
    resourceType = StoreConfigExporterImpl.RESOURCE_TYPE)
public class StoreConfigExporterImpl implements StoreConfigExporter {

    protected static final String RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";

    private static final String STORE_CODE_PROPERTY = "magentoStore";
    private static final String GRAPHQL_ENDPOINT_PROPERTY = "magentoGraphqlEndpoint";

    @Inject
    private Page currentPage;

    private String storeView;
    private String graphqlEndpoint = "/magento/graphql";

    @PostConstruct
    void initModel() {
        Resource pageContent = currentPage.getContentResource();
        ComponentsConfiguration properties = null;
        if (LaunchUtils.isLaunchBasedPath(currentPage.getPath())) {
            properties = LaunchUtils.getTargetResource(pageContent, null).adaptTo(ComponentsConfiguration.class);
        } else {
            properties = pageContent.adaptTo(ComponentsConfiguration.class);
        }

        storeView = properties.get(STORE_CODE_PROPERTY, "default");
        graphqlEndpoint = properties.get(GRAPHQL_ENDPOINT_PROPERTY, "/magento/graphql");
    }

    @Override
    public String getStoreView() {
        return storeView;
    }

    @Override
    public String getGraphqlEndpoint() {
        return graphqlEndpoint;
    }
}
