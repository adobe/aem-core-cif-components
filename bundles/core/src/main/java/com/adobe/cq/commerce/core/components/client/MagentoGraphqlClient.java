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

package com.adobe.cq.commerce.core.components.client;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.commons.inherit.ComponentInheritanceValueMap;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * This is a wrapper class for {@link GraphqlClient}. The constructor adapts a {@link Resource} to
 * the GraphqlClient class and also looks for the <code>magentoStore</code> property on the resource
 * path in order to set the Magento <code>Store</code> HTTP header. This wrapper also sets the custom
 * Magento Gson deserializer from {@link QueryDeserializer}.
 */
public class MagentoGraphqlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagentoGraphqlClient.class);

    public static final String STORE_CODE_PROPERTY = "magentoStore";

    private GraphqlClient graphqlClient;

    private RequestOptions requestOptions;

    /**
     * Instantiates and returns a new MagentoGraphqlClient.
     * This method returns <code>null</code> if the client cannot be instantiated.
     *
     * @param resource The JCR resource to use to adapt to the lower-level {@link GraphqlClient}.
     * @return A new MagentoGraphqlClient instance.
     */
    public static MagentoGraphqlClient create(Resource resource) {
        try {
            return new MagentoGraphqlClient(resource);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    public static MagentoGraphqlClient create(Resource resource, ConfigurationResourceResolver configurationResourceResolver) {
        try {
            return new MagentoGraphqlClient(resource, configurationResourceResolver);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    public MagentoGraphqlClient(Resource resource, ConfigurationResourceResolver configurationResourceResolver) {
        graphqlClient = resource.adaptTo(GraphqlClient.class);
        if (graphqlClient == null) {
            throw new RuntimeException("GraphQL client not available for resource " + resource.getPath());
        }

        requestOptions = new RequestOptions().withGson(QueryDeserializer.getGson());
        Page page = resource.getResourceResolver()
                            .adaptTo(PageManager.class)
                            .getContainingPage(resource);
        Resource configs = configurationResourceResolver.getResource(page.adaptTo(Resource.class), "settings", "commerce/default");

        if (configs == null) {
            throw new RuntimeException(String.format("Configuration not found at /conf/{0}/{1}", "settings", "commerce/default"));
        }

        ValueMap properties = configs.getValueMap();

        String storeCode = properties.get(STORE_CODE_PROPERTY, "");
        if (StringUtils.isNotEmpty(storeCode)) {
            Header storeHeader = new BasicHeader("Store", storeCode);
            requestOptions.withHeaders(Collections.singletonList(storeHeader));
        }

    }

    private MagentoGraphqlClient(Resource resource) {
        graphqlClient = resource.adaptTo(GraphqlClient.class);
        if (graphqlClient == null) {
            throw new RuntimeException("GraphQL client not available for resource " + resource.getPath());
        }

        requestOptions = new RequestOptions().withGson(QueryDeserializer.getGson());

        InheritanceValueMap properties;
        Page page = resource.getResourceResolver()
                            .adaptTo(PageManager.class)
                            .getContainingPage(resource);
        if (page != null) {
            properties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        } else {
            properties = new ComponentInheritanceValueMap(resource);
        }

        // get Magento store code
        String storeCode = properties.getInherited(STORE_CODE_PROPERTY, String.class);
        // for backward compatibility also check of the old cq:magentoStore property
        if (storeCode == null) {
            storeCode = properties.getInherited("cq:" + STORE_CODE_PROPERTY, String.class);
            if (storeCode != null) {
                LOGGER.warn("Deprecated 'cq:magentoStore' still in use for {}. Please update to 'magentoStore'.", resource.getPath());
            }
        }
        if (storeCode != null) {
            Header storeHeader = new BasicHeader("Store", storeCode);
            requestOptions.withHeaders(Collections.singletonList(storeHeader));
        }
    }

    /**
     * Executes the given Magento query and returns the response. This method will use
     * the default HTTP method defined in the OSGi configuration of the underlying {@link GraphqlClient}.
     * Use {@link #execute(String, HttpMethod)} if you want to specify the HTTP method yourself.
     *
     * @param query The GraphQL query.
     * @return The GraphQL response.
     */
    public GraphqlResponse<Query, Error> execute(String query) {
        return graphqlClient.execute(new GraphqlRequest(query), Query.class, Error.class, requestOptions);
    }

    /**
     * Executes the given Magento query and returns the response. This method
     * uses the given <code>httpMethod</code> to fetch the data.
     *
     * @param query      The GraphQL query.
     * @param httpMethod The HTTP method that will be used to fetch the data.
     * @return The GraphQL response.
     */
    public GraphqlResponse<Query, Error> execute(String query, HttpMethod httpMethod) {

        // We do not set the HTTP method in 'this.requestOptions' to avoid setting it as the new default
        RequestOptions options = new RequestOptions().withGson(requestOptions.getGson())
                                                     .withHeaders(requestOptions.getHeaders())
                                                     .withHttpMethod(httpMethod);

        return graphqlClient.execute(new GraphqlRequest(query), Query.class, Error.class, options);
    }
}
