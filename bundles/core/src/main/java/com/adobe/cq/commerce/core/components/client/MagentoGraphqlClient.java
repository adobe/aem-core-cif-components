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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.CachingStrategy;
import com.adobe.cq.commerce.graphql.client.CachingStrategy.DataFetchingPolicy;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.launches.api.Launch;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.adobe.granite.ui.components.ds.ValueMapResource;
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

    public static final String CONFIGURATION_NAME = "cloudconfigs/commerce";

    private GraphqlClient graphqlClient;

    private RequestOptions requestOptions;

    /**
     * Instantiates and returns a new MagentoGraphqlClient.
     * This method returns <code>null</code> if the client cannot be instantiated.<br>
     * <br>
     * <b>Important:</b> components defined in a page template should use {@link #create(Resource, Page)}
     * so the page can be used
     * to adapt to the lower-level {@link GraphqlClient}, while the component resource can be used for caching purposes.
     *
     * @param resource The JCR resource to use to adapt to the lower-level {@link GraphqlClient}. This is used for caching purposes, where
     *            the resource type is used as the cache key.
     * @return A new MagentoGraphqlClient instance.
     */
    public static MagentoGraphqlClient create(Resource resource) {
        PageManager pageManager = resource.adaptTo(PageManager.class);
        return create(resource, pageManager != null ? pageManager.getContainingPage(resource) : null);
    }

    /**
     * Instantiates and returns a new MagentoGraphqlClient.
     * This method returns <code>null</code> if the client cannot be instantiated.
     *
     * @param resource The JCR resource of the component being rendered. This is used for caching purposes, where the resource type is used
     *            as the cache key. An OSGi service should pass a synthetic resource, where the resource type should be set to the
     *            fully-qualified class name of the service.
     * @param page The current AEM page. This is used to adapt to the lower-level {@link GraphqlClient}.
     *            This is needed because it is not possible to get the current page for components added to the page template.
     *            If null, the resource will be used to adapt to the client, but this might fail for components defined on page templates.
     * @return A new MagentoGraphqlClient instance.
     */
    public static MagentoGraphqlClient create(Resource resource, Page page) {
        try {
            return new MagentoGraphqlClient(resource, page);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    private MagentoGraphqlClient(Resource resource, Page page) {

        Resource configurationResource = page != null ? page.adaptTo(Resource.class) : resource;

        // If the page is an AEM Launch, we get the configuration from the production page
        Launch launch = null;
        if (page != null && LaunchUtils.isLaunchBasedPath(page.getPath())) {
            Resource launchResource = LaunchUtils.getLaunchResource(configurationResource);
            launch = launchResource.adaptTo(Launch.class);
            configurationResource = LaunchUtils.getTargetResource(configurationResource, null);
        }

        LOGGER.debug("Try to get a graphql client from the resource at {}", configurationResource.getPath());

        ComponentsConfiguration configuration = configurationResource.adaptTo(ComponentsConfiguration.class);
        if (configuration.size() == 0) {
            LOGGER.warn("Context configuration not found, attempt to read the configuration from the page");
            graphqlClient = configurationResource.adaptTo(GraphqlClient.class);
        } else {
            LOGGER.debug("Crafting a configuration resource and attempting to get a GraphQL client from it...");
            // The Context-Aware Configuration API does return a ValueMap with all the collected properties from /conf and /libs,
            // but if you ask it for a resource via ConfigurationResourceResolver#getConfigurationResource() you get the resource that
            // resolves first (e.g. /conf/.../settings/cloudonfigs/commerce). This resource might not contain the properties
            // we need to adapt it to a graphql client so we just craft our own resource using the value map provided above.
            Resource configResource = new ValueMapResource(configurationResource.getResourceResolver(),
                configurationResource.getPath(),
                configurationResource.getResourceType(),
                configuration.getValueMap());

            graphqlClient = configResource.adaptTo(GraphqlClient.class);
        }
        if (graphqlClient == null) {
            throw new RuntimeException("GraphQL client not available for resource " + configurationResource.getPath());
        }
        requestOptions = new RequestOptions().withGson(QueryDeserializer.getGson());

        CachingStrategy cachingStrategy = new CachingStrategy()
            .withCacheName(resource.getResourceType())
            .withDataFetchingPolicy(DataFetchingPolicy.CACHE_FIRST);
        requestOptions.withCachingStrategy(cachingStrategy);

        List<Header> headers = new ArrayList<>();

        String storeCode;
        if (configuration.size() > 0) {
            storeCode = configuration.get(STORE_CODE_PROPERTY, String.class);
            if (storeCode == null) {
                storeCode = readFallBackConfiguration(configurationResource, STORE_CODE_PROPERTY);
            }
        } else {
            storeCode = readFallBackConfiguration(configurationResource, STORE_CODE_PROPERTY);
        }
        if (StringUtils.isNotEmpty(storeCode)) {
            headers.add(new BasicHeader("Store", storeCode));
        }

        if (launch != null) {
            Calendar liveDate = launch.getLiveDate();
            if (liveDate != null) {
                TimeZone timeZone = liveDate.getTimeZone();
                OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(liveDate.toInstant(), timeZone.toZoneId());
                Long previewVersion = offsetDateTime.toEpochSecond();
                headers.add(new BasicHeader("Preview-Version", String.valueOf(previewVersion)));

                // We use POST to ensure that Magento doesn't return a cached response
                requestOptions.withHttpMethod(HttpMethod.POST);
            }
        }

        if (!headers.isEmpty()) {
            requestOptions.withHeaders(headers);
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
     * @param query The GraphQL query.
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

    private String readFallBackConfiguration(Resource resource, String propertyName) {

        InheritanceValueMap properties;
        String storeCode;

        Page page = resource.getResourceResolver()
            .adaptTo(PageManager.class)
            .getContainingPage(resource);
        if (page != null) {
            properties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        } else {
            properties = new ComponentInheritanceValueMap(resource);
        }
        storeCode = properties.getInherited(propertyName, String.class);
        if (storeCode == null) {
            storeCode = properties.getInherited("cq:" + propertyName, String.class);
            if (storeCode != null) {
                LOGGER.warn("Deprecated 'cq:magentoStore' still in use for {}. Please update to 'magentoStore'.", resource.getPath());
            }
        }
        return storeCode;
    }
}
