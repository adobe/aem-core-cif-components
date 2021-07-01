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
package com.adobe.cq.commerce.core.components.internal.client;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.DeniedHttpHeaders;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.CachingStrategy;
import com.adobe.cq.commerce.graphql.client.CachingStrategy.DataFetchingPolicy;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
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
@Model(
    adaptables = { SlingHttpServletRequest.class, Resource.class },
    adapters = { MagentoGraphqlClient.class, MagentoGraphqlClientImpl.class },
    cache = true)
public class MagentoGraphqlClientImpl implements MagentoGraphqlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagentoGraphqlClient.class);

    private SlingHttpServletRequest request;
    private Resource resource;
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Page currentPage;

    private GraphqlClient graphqlClient;
    private RequestOptions requestOptions;
    private List<Header> httpHeaders;

    public MagentoGraphqlClientImpl(Resource resource) {
        this.resource = resource;
    }

    public MagentoGraphqlClientImpl(SlingHttpServletRequest request) {
        this.request = request;
        this.resource = request.getResource();
    }

    /**
     * @deprecated use sling models in all cases instead
     */
    @Deprecated
    public MagentoGraphqlClientImpl(Resource resource, Page page, SlingHttpServletRequest request) {
        initModel(resource, page, request);
    }

    @PostConstruct
    protected void postConstruct() {
        if (currentPage == null) {
            currentPage = getPageFromResource(resource);
        }

        initModel(resource, currentPage, request);
        // set to null to make the model cacheable
        currentPage = null;
        request = null;
        resource = null;
    }

    private void initModel(Resource resource, Page page, SlingHttpServletRequest request) {
        Resource configurationResource;
        String storeCode;
        List<Header> headers;
        HttpMethod httpMethod = null;
        Launch launch = null;
        Long previewVersion = null;

        if (page != null) {
            configurationResource = Objects.requireNonNull(page.adaptTo(Resource.class), "page is not a Resource");

            // If the page is an AEM Launch, we get the configuration from the production page
            if (LaunchUtils.isLaunchBasedPath(page.getPath())) {
                Resource launchResource = LaunchUtils.getLaunchResource(configurationResource);
                launch = launchResource.adaptTo(Launch.class);
            }
        } else {
            configurationResource = resource;
        }

        LOGGER.debug("Try to get a graphql client from the resource at {}", configurationResource.getPath());
        ComponentsConfiguration configuration = configurationResource.adaptTo(ComponentsConfiguration.class);

        if (configuration == null || configuration.size() == 0) {
            LOGGER.warn("Context configuration not found, attempt to read the configuration from the page");
            graphqlClient = adaptToGraphqlClient(configurationResource);
            headers = new ArrayList<>();
            storeCode = readFallBackConfiguration(configurationResource, STORE_CODE_PROPERTY);
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
            graphqlClient = adaptToGraphqlClient(configResource);
            headers = getCustomHttpHeaders(configuration);
            storeCode = configuration.get(STORE_CODE_PROPERTY, String.class);
            if (storeCode == null) {
                storeCode = readFallBackConfiguration(configurationResource, STORE_CODE_PROPERTY);
            }
        }

        if (StringUtils.isNotEmpty(storeCode)) {
            headers.add(new BasicHeader("Store", storeCode));
        }

        if (launch != null) {
            Calendar liveDate = launch.getLiveDate();
            if (liveDate != null) {
                TimeZone timeZone = liveDate.getTimeZone();
                OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(liveDate.toInstant(), timeZone.toZoneId());
                previewVersion = offsetDateTime.toEpochSecond();
            }
        } else if (request != null) {
            Long timewarp = getTimeWarpEpoch(request);
            if (timewarp != null) {
                Calendar time = Calendar.getInstance();
                time.setTimeInMillis(timewarp);
                if (time.after(Calendar.getInstance())) {
                    previewVersion = timewarp / 1000; // timewarp is in milliseconds, Magento Preview-Version header is in seconds
                }
            }
        }

        if (previewVersion != null) {
            headers.add(new BasicHeader("Preview-Version", String.valueOf(previewVersion)));
            // We use POST to ensure that Magento doesn't return a cached response
            httpMethod = HttpMethod.POST;
        }

        this.httpHeaders = headers;
        this.requestOptions = new RequestOptions()
            .withGson(QueryDeserializer.getGson())
            .withCachingStrategy(new CachingStrategy()
                .withCacheName(resource.getResourceType())
                .withDataFetchingPolicy(DataFetchingPolicy.CACHE_FIRST))
            .withHeaders(headers.size() > 0 ? headers : null)
            .withHttpMethod(httpMethod);
    }

    @Override
    public GraphqlResponse<Query, Error> execute(String query) {
        return graphqlClient.execute(new GraphqlRequest(query), Query.class, Error.class, requestOptions);
    }

    @Override
    public GraphqlResponse<Query, Error> execute(String query, HttpMethod httpMethod) {

        // We do not set the HTTP method in 'this.requestOptions' to avoid setting it as the new default
        RequestOptions options = new RequestOptions().withGson(requestOptions.getGson())
            .withHeaders(requestOptions.getHeaders())
            .withHttpMethod(httpMethod);

        return graphqlClient.execute(new GraphqlRequest(query), Query.class, Error.class, options);
    }

    @Override
    public GraphqlClientConfiguration getConfiguration() {
        return graphqlClient.getConfiguration();
    }

    @Override
    public Map<String, String> getHttpHeaders() {
        return httpHeaders.stream().collect(Collectors.toMap(Header::getName, Header::getValue));
    }

    private static List<Header> getCustomHttpHeaders(ComponentsConfiguration configuration) {
        List<Header> headers = new ArrayList<>();

        String[] customHeaders = configuration.get("httpHeaders", String[].class);

        if (customHeaders != null) {
            headers = Arrays.stream(customHeaders)
                .map(headerConfig -> {
                    String name = headerConfig.substring(0, headerConfig.indexOf('='));
                    if (DeniedHttpHeaders.DENYLIST.stream().noneMatch(name::equalsIgnoreCase)) {
                        String value = headerConfig.substring(headerConfig.indexOf('=') + 1, headerConfig.length());
                        return new BasicHeader(name, value);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        return headers;
    }

    private static Long getTimeWarpEpoch(SlingHttpServletRequest request) {
        String timeWarp = request.getParameter("timewarp");
        if (timeWarp == null) {
            Cookie cookie = request.getCookie("timewarp");
            if (cookie != null) {
                timeWarp = cookie.getValue();
            }
        }
        try {
            return timeWarp != null ? Long.valueOf(timeWarp) : null;
        } catch (NumberFormatException e) {
            LOGGER.warn("Cannot parse timewarp timestamp '{}'", timeWarp);
            return null;
        }
    }

    private static GraphqlClient adaptToGraphqlClient(Resource resource) {
        GraphqlClient graphqlClient = resource.adaptTo(GraphqlClient.class);
        if (graphqlClient == null) {
            throw new IllegalStateException("GraphQL client not available for resource " + resource.getPath());
        }
        return graphqlClient;
    }

    private static String readFallBackConfiguration(Resource resource, String propertyName) {
        InheritanceValueMap properties;
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager != null ? pageManager.getContainingPage(resource) : null;

        if (page != null) {
            properties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        } else {
            properties = new ComponentInheritanceValueMap(resource);
        }

        String storeCode = properties.getInherited(propertyName, String.class);
        if (storeCode == null) {
            storeCode = properties.getInherited("cq:" + propertyName, String.class);
            if (storeCode != null) {
                LOGGER.warn("Deprecated 'cq:magentoStore' still in use for {}. Please update to 'magentoStore'.", resource.getPath());
            }
        }

        return storeCode;
    }

    /**
     * Returns the {@link Page} the {@link Resource} belongs to. This may be the {@link Page} object at the path of the {@link Resource}
     * when the {@link Resource} is a cq:Page, or the {@link Page} returned from {@link PageManager#getContainingPage(Resource)}.
     *
     * @param resource
     * @return
     */
    private static Page getPageFromResource(Resource resource) {
        Page page = resource.adaptTo(Page.class);
        if (page == null) {
            PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
            if (pageManager != null) {
                page = pageManager.getContainingPage(resource);
            }
        }
        return page;
    }
}
