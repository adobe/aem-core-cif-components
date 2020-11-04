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

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.storeconfigexporter.StoreConfigExporter;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.Page;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { StoreConfigExporter.class },
    resourceType = StoreConfigExporterImpl.RESOURCE_TYPE)
public class StoreConfigExporterImpl implements StoreConfigExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreConfigExporterImpl.class);

    protected static final String RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";

    private static final String STORE_CODE_PROPERTY = "magentoStore";
    private static final String GRAPHQL_ENDPOINT_PROPERTY = "magentoGraphqlEndpoint";

    @Inject
    private Page currentPage;

    @Inject
    private Resource resource;

    private String storeView;
    private String graphqlEndpoint = "/magento/graphql";
    private HttpMethod method = HttpMethod.POST;

    private Page storeRootPage;

    @PostConstruct
    void initModel() {
        // Get configuration from CIF Sling CA config
        Resource pageContent = currentPage.getContentResource();
        ComponentsConfiguration properties = null;
        if (LaunchUtils.isLaunchBasedPath(currentPage.getPath())) {
            properties = LaunchUtils.getTargetResource(pageContent, null).adaptTo(ComponentsConfiguration.class);
        } else {
            properties = pageContent.adaptTo(ComponentsConfiguration.class);
        }

        storeView = properties.get(STORE_CODE_PROPERTY, "default");
        graphqlEndpoint = properties.get(GRAPHQL_ENDPOINT_PROPERTY, "/magento/graphql");

        // Get configuration from GraphQL client
        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage);
        if (magentoGraphqlClient != null) {
            GraphqlClientConfiguration graphqlClientConfiguration = magentoGraphqlClient.getConfiguration();
            method = graphqlClientConfiguration.httpMethod();
        }
    }

    @Override
    public String getStoreView() {
        return storeView;
    }

    @Override
    public String getGraphqlEndpoint() {
        return graphqlEndpoint;
    }

    @Override
    public String getMethod() {
        return method.toString();
    }

    @Override
    public String getStoreRootURL() {
        if (storeRootPage == null) {
            storeRootPage = SiteNavigation.getNavigationRootPage(currentPage);
        }

        if (storeRootPage == null) {    
            LOGGER.error("Store root page not found for page " + currentPage.getPath());
            return null;
        }

        return storeRootPage.getPath() + ".html";
    }
}
