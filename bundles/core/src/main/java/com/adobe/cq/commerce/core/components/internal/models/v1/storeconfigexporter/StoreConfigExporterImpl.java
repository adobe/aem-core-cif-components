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
package com.adobe.cq.commerce.core.components.internal.models.v1.storeconfigexporter;

import java.util.Collections;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.MapUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.storeconfigexporter.StoreConfigExporter;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.day.cq.wcm.api.Page;
import com.drew.lang.annotations.Nullable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { StoreConfigExporter.class },
    resourceType = StoreConfigExporterImpl.RESOURCE_TYPE)
public class StoreConfigExporterImpl implements StoreConfigExporter {

    protected static final String RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";

    private static final Logger LOGGER = LoggerFactory.getLogger(StoreConfigExporterImpl.class);
    private static final String EMPTY_JSON_OBJECT = "{}";

    private static final String STORE_CODE_PROPERTY = "magentoStore";
    private static final String GRAPHQL_ENDPOINT_PROPERTY = "magentoGraphqlEndpoint";

    private static final String DEFAULT_GRAPHQL_ENDPOINT = "/api/graphql";

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private Resource resource;

    private String storeView;
    private String graphqlEndpoint = DEFAULT_GRAPHQL_ENDPOINT;
    private HttpMethod method = HttpMethod.POST;
    private Page storeRootPage;
    private Map<String, String> httpHeaders = Collections.emptyMap();

    @PostConstruct
    void initModel() {
        // Get configuration from CIF Sling CA config
        Resource configResource = currentPage.getContentResource();
        ComponentsConfiguration properties = configResource.adaptTo(ComponentsConfiguration.class);
        if (properties != null) {
            storeView = properties.get(STORE_CODE_PROPERTY, String.class);
            graphqlEndpoint = properties.get(GRAPHQL_ENDPOINT_PROPERTY, DEFAULT_GRAPHQL_ENDPOINT);
        }

        if (magentoGraphqlClient != null) {
            GraphqlClientConfiguration graphqlClientConfiguration = magentoGraphqlClient.getConfiguration();
            method = graphqlClientConfiguration.httpMethod();
            httpHeaders = magentoGraphqlClient.getHttpHeaders();
        }
    }

    @Override
    @Nullable
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
    public String getHttpHeaders() {
        if (MapUtils.isEmpty(httpHeaders)) {
            return EMPTY_JSON_OBJECT;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();

        for (Map.Entry<String, String> header : httpHeaders.entrySet()) {
            objectNode.put(header.getKey(), header.getValue());
        }

        try {
            return mapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            return EMPTY_JSON_OBJECT;
        }
    }

    @Override
    public String getStoreRootUrl() {
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
